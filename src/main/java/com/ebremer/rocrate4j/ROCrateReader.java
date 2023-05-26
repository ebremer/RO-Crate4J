package com.ebremer.rocrate4j;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.rdf.RdfDataset;
import com.ebremer.rocrate4j.readers.FolderReader;
import com.ebremer.rocrate4j.readers.Reader;
import com.ebremer.rocrate4j.readers.ZipReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.JenaTitanium;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

/**
 *
 * @author erich
 */
public final class ROCrateReader implements AutoCloseable {
    private final Model manifest;
    private final Reader reader;
    private final String ref;
    
    public ROCrateReader(URI uri) throws IOException {
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
                throw new Error("HTTP(S) Support COMING SOON...");
            default:
                throw new Error("UNSUPPORTED "+uri.toString());
        }
        String xx = FixURI(uri.toString());
        if (!xx.startsWith("file://")) {
            xx = "file:///"+ xx.substring("file:/".length());
        }
        ref = xx;
        manifest = LoadManifest();
    }
    
    public String FixURI(String uri) {
        String hold = uri
                .replace("file:/C:/", "file:///C:/")
                .replace("file:/D:/", "file:///D:/");
        return hold;
    }
    
    @Override
    public void close() {
        reader.close();
    }

    public SeekableByteChannel getSeekableByteChannel(String name) {
        return reader.Retrieve(name);
    }
    
    public InputStream getInputStream(String name) {
        return reader.getInputStream(name);
    }
    
    public Model getManifest() {
        return manifest;
    }

    private Model LoadManifest() {
        Model m = null;
        InputStream inputStream = reader.getInputStream(ROCrate.MANIFEST);
        try {
            Document document = JsonDocument.of(inputStream);
            JsonLdOptions options = new JsonLdOptions();
            options.setBase(new URI(ref+"/"));
            options.setUseNativeTypes(true);
            options.setProcessingMode(JsonLdVersion.V1_1);
            options.setCompactToRelative(true);
            RdfDataset rdf = JsonLd.toRdf(document).options(options).get();
            DatasetGraph dsg = JenaTitanium.convert(rdf);
            Graph gg = dsg.getDefaultGraph();
            m = ModelFactory.createModelForGraph(gg);
        } catch (JsonLdError ex) {
            Logger.getLogger(ROCrateReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(ROCrateReader.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                ?old ?p ?o
            }
        """);
        pss.setIri("old", ref+"/");
        pss.setIri("new", ref);
        update.add(pss.toString());
        UpdateAction.execute(update, m);
        return m;
    }
}
