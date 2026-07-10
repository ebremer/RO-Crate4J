package com.ebremer.rocrate4j;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.lang.Keywords;
import static com.ebremer.rocrate4j.ROCrate.MANIFEST;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.system.jsonld.JenaToTitanium;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.VOID;
import org.apache.jena.vocabulary.XSD;

/**
 * The metadata document of an RO-Crate under construction. Entities live in a
 * Jena model under a random per-crate base IRI; on serialization the base is
 * relativized so the root data entity becomes "./", the metadata descriptor
 * "ro-crate-metadata.json", and payload entries plain relative paths.
 *
 * @author erich
 */
public class Manifest {
    private final Model manifest;
    private final Resource rde;
    private final Resource manifestIRI;
    private final String base;
    private final Map<String, String> extraTerms = new LinkedHashMap<>();
    private static final Map<String, ?> configIndented = Map.of(JsonGenerator.PRETTY_PRINTING, true);

    public Manifest() {
        base = "http://"+UUID.randomUUID().toString()+".com/";
        manifest = ModelFactory.createDefaultModel();
        setDefaultNamespaces(manifest);
        rde = manifest.createResource(base);
        manifestIRI = manifest.createResource(base+MANIFEST);
        rde.addProperty(RDF.type, ROC.Dataset);
        manifestIRI
            .addProperty(RDF.type, ROC.CreativeWork)
            .addProperty(DCTerms.conformsTo, ROCrate.ROCSpec13)
            .addProperty(ROC.about, rde);
    }

    public Model getManifestModel() {
        return manifest;
    }

    public Resource getRDE() {
        return rde;
    }

    public final void setDefaultNamespaces(Model m) {
        m.setNsPrefix("schema", ROC.NS);
        m.setNsPrefix("oa", OA.NS);
        m.setNsPrefix("xmls", XSD.NS);
        m.setNsPrefix("rdfs", RDFS.uri);
        m.setNsPrefix("void", VOID.NS);
        m.setNsPrefix("dcterms", DCTerms.NS);
    }

    /**
     * Registers an additional namespace prefix included in both the Turtle
     * rendering and the JSON-LD context, e.g.
     * addPrefix("hal", "https://halcyon.is/ns/").
     */
    public Manifest addPrefix(String prefix, String namespace) {
        manifest.setNsPrefix(prefix, namespace);
        extraTerms.put(prefix, namespace);
        return this;
    }

    /**
     * Registers an additional JSON-LD context term, e.g.
     * addTerm("tileSizeX", "hal:tileSizeX"). Any prefix used in the value
     * must be defined via addPrefix. Applies to the JSON-LD rendering only.
     */
    public Manifest addTerm(String term, String value) {
        extraTerms.put(term, value);
        return this;
    }

    public String getManifest() {
        return getManifest(RDFFormat.JSONLD11);
    }

    public String getManifest(RDFFormat lang) {
        if (lang.equals(RDFFormat.JSONLD11)) {
            return toJSONLD(manifest);
        } else if (lang.equals(RDFFormat.TRIG_PRETTY)) {
            return toTurtle(manifest);
        }
        throw new IllegalArgumentException("unsupported manifest format: "+lang);
    }

    public Manifest setName(String name) {
        rde.removeAll(ROC.name);
        rde.addProperty(ROC.name, name);
        return this;
    }

    public Manifest setDescription(String description) {
        rde.removeAll(ROC.description);
        rde.addProperty(ROC.description, description);
        return this;
    }

    /**
     * Sets the root data entity's datePublished. RO-Crate requires a single
     * ISO 8601 string with at least day precision, e.g. "2026-07-10".
     */
    public Manifest setDatePublished(String isoDate) {
        rde.removeAll(ROC.datePublished);
        rde.addProperty(ROC.datePublished, isoDate);
        return this;
    }

    /**
     * Sets the root data entity's license — an IRI becomes an entity
     * reference, any other text a plain literal.
     */
    public Manifest setLicense(String license) {
        rde.removeAll(ROC.license);
        if (isAbsoluteIRI(license)) {
            rde.addProperty(ROC.license, manifest.createResource(license));
        } else {
            rde.addProperty(ROC.license, license);
        }
        return this;
    }

    /**
     * Adds the datePublished the RO-Crate specification requires on the root
     * data entity if none was set; invoked when the crate is written.
     */
    public Manifest ensureDatePublished() {
        if (!rde.hasProperty(ROC.datePublished)) {
            rde.addProperty(ROC.datePublished,
                DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
        }
        return this;
    }

    private static boolean isAbsoluteIRI(String value) {
        try {
            return new URI(value).isAbsolute();
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    public Manifest addKeyword(String key) {
        if (isAbsoluteIRI(key)) {
            manifest.add(rde, ROC.keywords, manifest.createResource(key));
        } else {
            manifest.add(rde, ROC.keywords, key);
        }
        return this;
    }

    public Manifest addPublisher(String publisher) {
        if (isAbsoluteIRI(publisher)) {
            manifest.add(rde, ROC.publisher, manifest.createResource(publisher));
        } else {
            manifest.add(rde, ROC.publisher, publisher);
        }
        return this;
    }

    public Manifest addCreator(String author) {
        if (isAbsoluteIRI(author)) {
            manifest.add(rde, ROC.author, manifest.createResource(author));
        } else {
            manifest.add(rde, ROC.author, author);
        }
        return this;
    }

    public Resource addFile(String name, boolean track) {
        return addFile(rde, name, track);
    }

    private static String childIRI(Resource parent, String name) {
        String parentIRI = parent.getURI();
        return parentIRI.endsWith("/") ? parentIRI + name : parentIRI + "/" + name;
    }

    /**
     * Percent-encodes an entry name for use in an entity @id (RO-Crate
     * requires URI-path-encoded identifiers, e.g. "almost 50%.png" becomes
     * "almost%2050%25.png"). Forward slashes separate path segments;
     * backslashes are normalized to forward slashes.
     */
    private static String encodePath(String name) {
        String forward = name.replace('\\', '/');
        try {
            return new URI(null, null, forward, null).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("cannot encode entry name: "+name, ex);
        }
    }

    /**
     * Describes a directory entity: typed Dataset with an @id ending in "/",
     * linked from the parent via hasPart.
     */
    public Resource addFolder(Resource parent, String name) {
        return addFolder(parent, name, null);
    }

    public Resource addFolder(Resource parent, String name, Resource extraType) {
        Resource folder = manifest.createResource(childIRI(parent, encodePath(name)) + "/");
        folder.addProperty(RDF.type, ROC.Dataset);
        if (extraType != null && !ROC.Dataset.equals(extraType)) {
            folder.addProperty(RDF.type, extraType);
        }
        parent.addProperty(ROC.hasPart, folder);
        return folder;
    }

    /**
     * Describes a file entity. With track=true the entity is typed File and
     * linked from the parent via hasPart, as RO-Crate requires for described
     * files. With track=false only a resource handle is returned and nothing
     * is added to the graph — untracked entries must not be described further,
     * since described File entities have to be reachable from the root.
     */
    public Resource addFile(Resource parent, String name, boolean track) {
        Resource file = manifest.createResource(childIRI(parent, encodePath(name)));
        if (track) {
            file.addProperty(RDF.type, ROC.MediaObject);
            parent.addProperty(ROC.hasPart, file);
        }
        return file;
    }

    public String toTurtle(Model meta) {
        Dataset ds = DatasetFactory.createGeneral();
        ds.getDefaultModel().add(meta);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFWriter.create()
            .source(ds)
            .base(rde.getURI())
            .set(RIOT.symTurtleOmitBase, true)
            .format(RDFFormat.TRIG_PRETTY)
            .output(baos);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Serializes the model as RO-Crate 1.3 JSON-LD: flattened node objects in
     * @graph, compacted against the official context (referenced by URL, with
     * any addPrefix/addTerm extensions in the array form), and entity ids
     * relativized to the crate root.
     */
    public String toJSONLD(Model meta) {
        Dataset dsx = DatasetFactory.create();
        dsx.getDefaultModel().add(meta);
        DatasetGraph dsg = dsx.asDatasetGraph();
        JsonLdOptions options = new JsonLdOptions();
        options.setOrdered(false);
        options.setUseNativeTypes(true);
        options.setDocumentLoader(ROCrateContexts.LOADER);
        JsonArray expanded;
        try {
            expanded = JenaToTitanium.convert(dsg, options);
        } catch (JsonLdError ex) {
            throw new ROCrateException("JSON-LD conversion failed", ex);
        }
        JsonObjectBuilder contextHolder = Json.createObjectBuilder();
        if (extraTerms.isEmpty()) {
            contextHolder.add(Keywords.CONTEXT, ROCrateContexts.CONTEXT_1_3);
        } else {
            JsonObjectBuilder extras = Json.createObjectBuilder();
            extraTerms.forEach(extras::add);
            contextHolder.add(Keywords.CONTEXT,
                Json.createArrayBuilder().add(ROCrateContexts.CONTEXT_1_3).add(extras));
        }
        JsonObject compacted;
        try {
            compacted = JsonLd.compact(JsonDocument.of(expanded), JsonDocument.of(contextHolder.build()))
                .options(options)
                .compactArrays(true)
                .get();
        } catch (JsonLdError ex) {
            throw new ROCrateException("JSON-LD compaction failed", ex);
        }
        JsonWriterFactory factory = Json.createWriterFactory(configIndented);
        try (
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JsonWriter writer = factory.createWriter(os)
        ) {
            writer.write(compacted);
            String pre = new String(os.toByteArray(), StandardCharsets.UTF_8);
            return pre
                .replace("\""+base+"\"", "\"./\"")
                .replace("\""+base, "\"");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
