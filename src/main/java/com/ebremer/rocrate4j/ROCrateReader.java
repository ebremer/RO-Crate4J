package com.ebremer.rocrate4j;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.ebremer.rocrate4j.readers.FolderReader;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import com.ebremer.rocrate4j.readers.Reader;
import com.ebremer.rocrate4j.readers.ZipReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.jsonld.TitaniumToJena;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.DCTerms;

/**
 * Reads an RO-Crate from a directory or zip archive. The official RO-Crate
 * contexts are resolved from bundled copies, so no network access is needed
 * for conformant 1.1/1.2/1.3 crates.
 *
 * @author erich
 */
public final class ROCrateReader implements AutoCloseable {
    private static final String HTTPS_SCHEMA_ORG = "https://schema.org/";
    private final Model manifest;
    private final Reader reader;
    private final String ref;

    public ROCrateReader(URI uri) throws IOException {
        this(uri, uri);
    }

    public ROCrateReader(URI uri, URI base) throws IOException {
        switch (uri.getScheme()) {
            case "file":
                File f = new File(uri);
                if (f.isDirectory()) {
                    reader = new FolderReader(f);
                } else {
                    reader = new ZipReader(f);
                }
                break;
            case "http":
            case "https":
                throw new UnsupportedOperationException("HTTP(S) crates are not supported yet: "+uri);
            default:
                throw new IllegalArgumentException("unsupported URI scheme: "+uri);
        }
        String xx = base.toString();
        if (xx.startsWith("file:") && !xx.startsWith("file://")) {
            xx = "file:///"+ xx.substring("file:/".length());
        }
        if (xx.endsWith("/")) {
            xx = xx.substring(0, xx.length()-1);
        }
        ref = xx;
        manifest = loadManifest();
    }

    public boolean hasManifest() {
        return reader.hasManifest();
    }

    @Override
    public void close() {
        reader.close();
    }

    public SeekableByteChannel getSeekableByteChannel(String name) {
        return reader.retrieve(relativize(name));
    }

    public InputStream getInputStream(String name) {
        return reader.getInputStream(relativize(name));
    }

    /**
     * Accepts either an entry path relative to the crate root or an absolute
     * IRI within this crate (as found in the manifest) and returns the
     * percent-decoded root-relative entry path expected by the underlying
     * Reader.
     */
    private String relativize(String name) {
        String rel;
        if (name.startsWith(ref+"/")) {
            rel = name.substring(ref.length()+1);
        } else if (name.startsWith(ref)) {
            rel = name.substring(ref.length());
        } else {
            rel = name;
        }
        try {
            String decoded = new URI(rel).getPath();
            return decoded != null ? decoded : rel;
        } catch (URISyntaxException ex) {
            return rel;
        }
    }

    public Model getManifest() {
        return manifest;
    }

    /**
     * Follows the metadata descriptor's about property to the root data
     * entity, per the RO-Crate 1.2+ root discovery algorithm. Returns null
     * for crates without a descriptor entity.
     */
    public Resource getRootDataEntity() {
        Statement about = manifest.getResource(ref+"/"+ROCrate.MANIFEST).getProperty(ROC.about);
        return about == null ? null : about.getObject().asResource();
    }

    /**
     * The specification (or profile) IRIs the metadata descriptor declares
     * via conformsTo, e.g. "https://w3id.org/ro/crate/1.3". Empty for crates
     * without a descriptor entity.
     */
    public List<String> getConformsTo() {
        return manifest.getResource(ref+"/"+ROCrate.MANIFEST)
            .listProperties(DCTerms.conformsTo)
            .mapWith(st -> st.getObject().isURIResource()
                ? st.getObject().asResource().getURI()
                : st.getObject().toString())
            .toList();
    }

    private Model loadManifest() throws IOException {
        if (!reader.hasManifest()) {
            throw new FileNotFoundException("no "+ROCrate.MANIFEST+" found in "+ref);
        }
        JsonObject raw;
        try (InputStream inputStream = reader.getInputStream(ROCrate.MANIFEST);
             JsonReader jsonReader = Json.createReader(inputStream)) {
            raw = jsonReader.readObject();
        }
        // Relative @ids are resolved here rather than by the JSON-LD engine:
        // titanium percent-decodes during relative resolution, turning encoded
        // ids like "almost%2050%25.bin" into non-well-formed IRIs it then drops.
        JsonObject resolved = resolveIds(raw).asJsonObject();
        DatasetGraph dsg = DatasetGraphFactory.create();
        try {
            JsonLdOptions options = new JsonLdOptions();
            options.setDocumentLoader(ROCrateContexts.LOADER);
            TitaniumToJena.convert(JsonDocument.of(resolved), options, StreamRDFLib.dataset(dsg), RiotLib.dftProfile());
        } catch (JsonLdError ex) {
            throw new ROCrateException("cannot parse "+ROCrate.MANIFEST+" in "+ref, ex);
        }
        Model m = normalizeSchemaOrg(ModelFactory.createModelForGraph(dsg.getDefaultGraph()));
        UpdateRequest update = UpdateFactory.create();
        ParameterizedSparqlString pss = new ParameterizedSparqlString(
        """
            delete {?old ?p ?o}
            insert {?new ?p ?o}
            where {
                ?old ?p ?o
            }
        """);
        pss.setIri("old", ref+"/");
        pss.setIri("new", ref);
        update.add(pss.toString());
        pss = new ParameterizedSparqlString(
        """
            delete {?s ?p ?old}
            insert {?s ?p ?new}
            where {
                ?s ?p ?old
            }
        """);
        pss.setIri("old", ref+"/");
        pss.setIri("new", ref);
        update.add(pss.toString());
        UpdateAction.execute(update, m);
        return m;
    }

    private JsonValue resolveIds(JsonValue value) {
        switch (value.getValueType()) {
            case OBJECT -> {
                JsonObjectBuilder builder = Json.createObjectBuilder();
                value.asJsonObject().forEach((key, member) -> {
                    if (key.equals("@context")) {
                        builder.add(key, member);
                    } else if (key.equals("@id") && member.getValueType() == JsonValue.ValueType.STRING) {
                        builder.add(key, resolveId(((JsonString) member).getString()));
                    } else {
                        builder.add(key, resolveIds(member));
                    }
                });
                return builder.build();
            }
            case ARRAY -> {
                JsonArrayBuilder builder = Json.createArrayBuilder();
                value.asJsonArray().forEach(item -> builder.add(resolveIds(item)));
                return builder.build();
            }
            default -> {
                return value;
            }
        }
    }

    private String resolveId(String id) {
        if (id.startsWith("_:")) {
            return id;
        }
        try {
            if (new URI(id).isAbsolute()) {
                return id;
            }
        } catch (URISyntaxException ex) {
            // treat as a relative reference
        }
        String stripped = id.startsWith("./") ? id.substring(2) : id;
        return ref + "/" + stripped;
    }

    /**
     * Canonicalizes https://schema.org/ IRIs to the http://schema.org/ form
     * the RO-Crate context uses, so pre-1.3 producers (including older
     * versions of this library) parse into the same vocabulary.
     */
    private static Model normalizeSchemaOrg(Model in) {
        Model out = ModelFactory.createDefaultModel();
        out.setNsPrefixes(in);
        in.listStatements().forEach(st -> {
            Resource s = (Resource) fixNode(out, st.getSubject());
            Property p = out.createProperty(fixIRI(st.getPredicate().getURI()));
            RDFNode o = fixNode(out, st.getObject());
            out.add(s, p, o);
        });
        return out;
    }

    private static RDFNode fixNode(Model out, RDFNode node) {
        if (node.isURIResource()) {
            String iri = node.asResource().getURI();
            String fixed = fixIRI(iri);
            if (!fixed.equals(iri)) {
                return out.createResource(fixed);
            }
        }
        return node;
    }

    private static String fixIRI(String iri) {
        return iri.startsWith(HTTPS_SCHEMA_ORG) ? ROC.NS + iri.substring(HTTPS_SCHEMA_ORG.length()) : iri;
    }
}
