package com.ebremer.rocrate4j;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdEmbed;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.processor.FromRdfProcessor;
import com.apicatalog.rdf.RdfDataset;
import static com.ebremer.rocrate4j.ROCrate.MANIFEST;
import static com.ebremer.rocrate4j.ROCrate.ROCSpec11;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.JenaTitanium;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.jena.vocabulary.VOID;
import org.apache.jena.vocabulary.XSD;

/**
 *
 * @author erich
 */
public class Manifest {
    private final Model manifest;
    private final Resource rde;
    private final Resource manifestIRI;
    private static final Map<String, ?> configIndented = Map.of(JsonGenerator.PRETTY_PRINTING, true);
    private static final Map<String, ?> configFlat = Map.of();
    public static final String BASE = "http://"+UUID.randomUUID().toString()+".com/";
    
    public Manifest() {
        manifest = ModelFactory.createDefaultModel();
        SetDefaultNameSpaces(manifest);
        rde = manifest.createResource(BASE);
        manifestIRI = manifest.createResource(rde.toString()+"/"+MANIFEST);
        rde.addProperty(RDF.type, SchemaDO.Dataset);
        manifestIRI
            .addProperty(RDF.type, SchemaDO.CreativeWork)
            .addProperty(DCTerms.conformsTo, ROCSpec11)
            .addProperty(SchemaDO.about, rde);
    }
    
    public Model getManifestModel() {
        return manifest;
    }
    
    public Resource getRDE() {
        return rde;
    }
    
    public final void SetDefaultNameSpaces(Model m) {
        m.setNsPrefix("so", SchemaDO.NS);
        m.setNsPrefix("oa", OA.NS);
        m.setNsPrefix("bg", "https://www.ebremer.com/beakgraph/ns/");
        m.setNsPrefix("xmls", XSD.NS);
        m.setNsPrefix("rdfs", RDFS.uri);
        m.setNsPrefix("void", VOID.NS);
        m.setNsPrefix("dcterms", DCTerms.NS);
        m.setNsPrefix("hal", "https://www.ebremer.com/halcyon/ns/");
        m.setNsPrefix("exif", "http://www.w3.org/2003/12/exif/ns#");
    }

    public String getManifest() {
        return getManifest(RDFFormat.JSONLD11);
    }
    
    public String getManifest(RDFFormat lang) {
        if (lang.equals(RDFFormat.JSONLD11)) {
            return toJSONLD(manifest);
        } if (lang.equals(RDFFormat.TRIG_PRETTY)) {
            return toTurtle();
        }
        return null;
    }

    public Manifest SetDescription(String description) {
        manifest.add(rde, SchemaDO.description, description);
        return this;
    }
    
    public Manifest setName(String name) {
        rde.removeAll(SchemaDO.name);
        rde.addProperty(SchemaDO.name, name);
        return this;
    }
        
    public Manifest setDescription(String description) {
        rde.removeAll(SchemaDO.description);
        rde.addProperty(SchemaDO.description, description);
        return this;
    }
    
    public Manifest AddKeyword(String key) {
        try {
            URL url = URI.create(key).toURL();
            manifest.add(rde, SchemaDO.keywords, manifest.createResource(key));
        } catch (MalformedURLException ex) {
            // not a URI so add as String
            manifest.add(rde, SchemaDO.keywords, key);
        }
        return this;
    }

    public Manifest AddPublisher(String publisher) {
        try {
            URL url = URI.create(publisher).toURL();
            manifest.add(rde, SchemaDO.publisher, manifest.createResource(publisher));
        } catch (MalformedURLException ex) {
            // not a URI so add as String
            manifest.add(rde, SchemaDO.publisher, publisher);
        }
        return this;
    }
    
    public Manifest AddCreator(String author) {
        try {
            URL url = URI.create(author).toURL();
            manifest.add(rde, SchemaDO.author, manifest.createResource(author));
        } catch (MalformedURLException ex) {
            // not a URI so add as String
            manifest.add(rde, SchemaDO.author, author);
        }
        return this;
    }
    
    public Resource addFile(String name, boolean track) {
        return addFile(rde,name,track);
    }

    public Resource addFolder(Resource parent, String name, Resource type) {
        Resource folder = manifest.createResource(parent.getURI()+"/"+name);
        folder.addProperty(RDF.type, type);
        parent.addProperty(SchemaDO.hasPart, folder);
        return folder;
    }    
    
    public Resource addFile(Resource parent, String name, boolean track) {
        Resource file = manifest.createResource(parent.getURI()+"/"+name);
        //file.addProperty(RDF.type, SchemaDO.MediaObject);
        if (track) {
            parent.addProperty(SchemaDO.hasPart, file);
        }
        return file;
    }
    
    public String toTurtle() {
        Dataset ds = DatasetFactory.createGeneral();
        ds.getDefaultModel().add(manifest);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFWriter.create()
            .source(ds)
            .base(rde.getURI())
            .set(RIOT.symTurtleOmitBase, true)
            .format(RDFFormat.TRIG_PRETTY)
            .output(baos);
        return new String(baos.toByteArray());
    }
    
    private static Map<String, ?> config(boolean indented) {
        return indented ? configIndented : configFlat;
    }
    
    public String toJSONLD(Model meta) {
      //  File xz = new File("source.ttl");
        Dataset dsx = DatasetFactory.create();
        dsx.getDefaultModel().add(meta);
        
        dsx.getPrefixMapping().removeNsPrefix("dcterms");
        dsx.getPrefixMapping().setNsPrefix("dct", DCTerms.NS);
        dsx.getPrefixMapping().setNsPrefix("rdf", RDF.uri);
        dsx.getPrefixMapping().setNsPrefix("asWKT", "geo:asWKT");
        dsx.getPrefixMapping().setNsPrefix("Feature", "geo:Feature");
        dsx.getPrefixMapping().setNsPrefix("geometry", "geo:hasGeometry");
        dsx.getPrefixMapping().setNsPrefix("FeatureCollection", "geo:FeatureCollection");
        dsx.getPrefixMapping().setNsPrefix("hasProbability", "hal:hasProbability");
        dsx.getPrefixMapping().setNsPrefix("Dataset", "so:Dataset");
        dsx.getPrefixMapping().setNsPrefix("hasPart", "so:hasPart");
        dsx.getPrefixMapping().setNsPrefix("ImageObject", "so:ImageObject");
        dsx.getPrefixMapping().setNsPrefix("object", "so:object");
        dsx.getPrefixMapping().setNsPrefix("name", "so:name");
        dsx.getPrefixMapping().setNsPrefix("publisher", "so:publisher");
        
        dsx.getPrefixMapping().setNsPrefix("ScholarlyArticle", "so:ScholarlyArticle");
        dsx.getPrefixMapping().setNsPrefix("contributor", "so:contributor");
        
        dsx.getPrefixMapping().setNsPrefix("result", "so:result");
        dsx.getPrefixMapping().setNsPrefix("keywords", "so:keywords");
        dsx.getPrefixMapping().setNsPrefix("datePublished", "so:datePublished");
        dsx.getPrefixMapping().setNsPrefix("description", "so:description");
        dsx.getPrefixMapping().setNsPrefix("instrument", "so:instrument");
        dsx.getPrefixMapping().setNsPrefix("CreateAction", "so:CreateAction");
        dsx.getPrefixMapping().setNsPrefix("height", "exif:height");
        dsx.getPrefixMapping().setNsPrefix("width", "exif:width");
        dsx.getPrefixMapping().setNsPrefix("source", "dc:source");
        dsx.getPrefixMapping().setNsPrefix("member", "rdfs:member");
       
        dsx.getPrefixMapping().setNsPrefix("type", "rdf:type");

        
        dsx.getPrefixMapping().setNsPrefix("Dataset", "void:Dataset");
        dsx.getPrefixMapping().setNsPrefix("classPartition", "void:classPartition");
        dsx.getPrefixMapping().setNsPrefix("class", "void:class");
        dsx.getPrefixMapping().setNsPrefix("entities", "void:entities");
        dsx.getPrefixMapping().setNsPrefix("distinctObjects", "void:distinctObjects");
        dsx.getPrefixMapping().setNsPrefix("distinctSubjects", "void:distinctSubjects");
        dsx.getPrefixMapping().setNsPrefix("properties", "void:properties");
        dsx.getPrefixMapping().setNsPrefix("property", "void:property");
        dsx.getPrefixMapping().setNsPrefix("propertyPartition", "void:propertyPartition");
        dsx.getPrefixMapping().setNsPrefix("triples", "void:triples");
        dsx.getPrefixMapping().setNsPrefix("subset", "void:subset");
        
        dsx.getPrefixMapping().setNsPrefix("BeakGraph", "hal:BeakGraph");
        dsx.getPrefixMapping().setNsPrefix("Segmentation", "hal:Segmentation");
        dsx.getPrefixMapping().setNsPrefix("hasProbability", "hal:hasProbability");
        dsx.getPrefixMapping().setNsPrefix("classification", "hal:classification");
        dsx.getPrefixMapping().setNsPrefix("tileSizeX", "hal:tileSizeX");
        dsx.getPrefixMapping().setNsPrefix("tileSizeY", "hal:tileSizeY");
        
        
        DatasetGraph dsg = dsx.asDatasetGraph();
        RdfDataset ds = JenaTitanium.convert(dsg);
        Document doc = RdfDocument.of(ds);
        JsonLdOptions options = new JsonLdOptions();
            options.setOrdered(false);
            options.setUseNativeTypes(true);
            options.setOmitGraph(true);
            options.setExplicit(true);
            options.setRequiredAll(false);
            options.setEmbed(JsonLdEmbed.ALWAYS);
        JsonArray ja = null;
        try {
            ja = FromRdfProcessor.fromRdf(doc, options);
        } catch (JsonLdError ex) {
            Logger.getLogger(Manifest.class.getName()).log(Level.SEVERE, null, ex);
        }
        jakarta.json.JsonObject writeRdf = Json.createObjectBuilder()
                .add(Keywords.GRAPH, ja)
                .build();
        JsonObjectBuilder cxt = Json.createObjectBuilder();
        JsonObjectBuilder neocontext = Json.createObjectBuilder();
          dsg.prefixes().forEach((k, v) -> {
            if ( ! k.isEmpty() )
                neocontext.add(k, v);
        });
        //neocontext.add("id","@id");
        //neocontext.add("type","@type");
        neocontext.add("hasPart", Json.createObjectBuilder().add(Keywords.ID, "so:hasPart").add(Keywords.TYPE, Keywords.ID));
        neocontext.add("class", Json.createObjectBuilder().add(Keywords.ID, "void:class").add(Keywords.TYPE, Keywords.ID));
        neocontext.add("property", Json.createObjectBuilder().add(Keywords.ID, "void:property").add(Keywords.TYPE, Keywords.ID));
        neocontext.add("datePublished", Json.createObjectBuilder().add(Keywords.ID, "so:datePublished").add(Keywords.TYPE, XSD.dateTime.getURI()));
        neocontext.add("date", Json.createObjectBuilder().add(Keywords.ID, "dct:date").add(Keywords.TYPE, XSD.dateTime.getURI()));
        neocontext.add("creator", Json.createObjectBuilder().add(Keywords.ID, "dct:creator").add(Keywords.TYPE, Keywords.ID));
        neocontext.add("publisher", Json.createObjectBuilder().add(Keywords.ID, "so:publisher").add(Keywords.TYPE, Keywords.ID));
        neocontext.add("dct:publisher", Json.createObjectBuilder().add(Keywords.ID, "dct:publisher").add(Keywords.TYPE, Keywords.ID));
        neocontext.add("hasClassification", Json.createObjectBuilder().add(Keywords.ID, "hal:hasClassification").add(Keywords.TYPE, Keywords.ID));
        cxt.add(Keywords.CONTEXT, neocontext);
        cxt.add(Keywords.EMBED, Keywords.ALWAYS);
        cxt.add(Keywords.EXPLICIT, false);
        cxt.add(Keywords.OMIT_DEFAULT, true);
        cxt.add(Keywords.REQUIRE_ALL, false);
        cxt.add(Keywords.TYPE, Json.createArrayBuilder().add("so:Dataset"));
        jakarta.json.JsonObject context = cxt.build();   
        Document contextDoc = JsonDocument.of(context);
        FramingApi api = JsonLd.frame(JsonDocument.of(writeRdf), contextDoc);
        JsonStructure x = null;
        try {
            x = api
                    .options(options)                    
                    .omitGraph(true)
                    .mode(JsonLdVersion.V1_1)
                    //  .options(options)
                    .get();
        } catch (JsonLdError ex) {
            Logger.getLogger(Manifest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Map<String,?> config = config(true);
        JsonWriterFactory factory = Json.createWriterFactory(config);
        try (
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JsonWriter writer = factory.createWriter(os)
        ) {
            writer.write(x);
            String pre =  new String(os.toByteArray(), StandardCharsets.UTF_8);
            pre = pre.replaceAll(BASE, ".");
            return pre;
        } catch (IOException ex) {
            Logger.getLogger(Manifest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        Manifest man = new Manifest();
        Model m = ModelFactory.createDefaultModel();
        File x = new File("source.ttl");
        RDFParser.create()
            .base(BASE)
            .source(new FileInputStream(x))
            .lang(Lang.TURTLE)
            .errorHandler(ErrorHandlerFactory.errorHandlerWarn)
            .parse(m);
        m.setNsPrefix("", "file:///D:/projects/RO-Crate4J/");
        
        System.out.println(man.toJSONLD(m));
    }
}
