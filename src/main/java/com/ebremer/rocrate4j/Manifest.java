/*
 * Erich Bremer
 * RO-Crate
 */
package com.ebremer.rocrate4j;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.serialization.RdfToJsonld;
import com.apicatalog.rdf.RdfDataset;
import static com.ebremer.rocrate4j.ROCrate.MANIFEST;
import static com.ebremer.rocrate4j.ROCrate.ROCSpec11;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.JenaTitanium;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;

/**
 *
 * @author erich
 */
public class Manifest {
    private final Model manifest;
    private final Resource rde;  //Root Data Entity
    private final Resource manifestIRI;
    
    public Manifest() {
        manifest = ModelFactory.createDefaultModel();
        rde = manifest.createResource("http://"+UUID.randomUUID().toString()+".com");
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
    
    public byte[] getManifestBytes() {
        return toJSONLD().getBytes();
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
            URL url = new URL(key);
            manifest.add(rde, SchemaDO.keywords, manifest.createResource(key));
        } catch (MalformedURLException ex) {
            // not a URI so add as String
        }
        manifest.add(rde, SchemaDO.keywords, key);
        return this;
    }

    public Manifest AddPublisher(String publisher) {
        try {
            URL url = new URL(publisher);
            manifest.add(rde, SchemaDO.publisher, manifest.createResource(publisher));
        } catch (MalformedURLException ex) {
            // not a URI so add as String
        }
        manifest.add(rde, SchemaDO.publisher, publisher);
        return this;
    }
    
    public Manifest AddCreator(String author) {
        try {
            URL url = new URL(author);
            manifest.add(rde, SchemaDO.author, manifest.createResource(author));
        } catch (MalformedURLException ex) {
            // not a URI so add as String
        }
        manifest.add(rde, SchemaDO.author, author);
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
        file
            .addProperty(RDF.type, SchemaDO.MediaObject);
        if (track) {
            parent.addProperty(SchemaDO.hasPart, file);
        }
        return file;
    }
    
    public String toJSONLD() {
        try {
            System.out.println("RDE  : "+rde);
            //RDFDataMgr.write(System.out, manifest, Lang.TURTLE);
            //System.out.println("======================================");
            Dataset ds = DatasetFactory.createGeneral();
            ds.getDefaultModel().add(manifest);
            RdfDataset rds = JenaTitanium.convert(ds.asDatasetGraph());
            RdfToJsonld rtj = RdfToJsonld.with(rds);
            JsonArray ja = rtj.build();
            JsonLdOptions options = new JsonLdOptions();
            options.setBase(new URI(rde+"/"));
            options.setUseNativeTypes(true);
            options.setProcessingMode(JsonLdVersion.V1_1);
            options.setCompactToRelative(true);
            JsonWriterFactory writerFactory = Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonWriter out = writerFactory.createWriter(baos);
            Document contextDocument = JsonDocument.of(new ByteArrayInputStream("""
        {
            "iiif": "http://iiif.io/api/image/2#",
            "xsd": "http://www.w3.org/2001/XMLSchema#",
            "exif": "http://www.w3.org/2003/12/exif/ns#",
            "dc": "http://purl.org/dc/elements/1.1/",
            "dcterms": "http://purl.org/dc/terms/",
            "doap": "http://usefulinc.com/ns/doap#",
            "svcs": "http://rdfs.org/sioc/services#",
            "foaf": "http://xmlns.com/foaf/0.1/",
            "sc": "http://iiif.io/api/presentation/2#",
            "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
            "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "so": "https://schema.org/",
            "csvw": "https://www.w3.org/ns/csvw/",
            "hal": "https://www.ebremer.com/halcyon/ns/",
            "bg": "https://www.ebremer.com/beakgraph/ns/",
            "void": "http://rdfs.org/ns/void#",                                                                                                                                                                                                                                
            "baseUriRedirect": {"@id": "iiif:baseUriRedirectFeature"},
            "cors": {"@id": "iiif:corsFeature"},
            "regionByPct": {"@id": "iiif:regionByPctFeature"},
            "regionByPx": {"@id": "iiif:regionByPxFeature"},
            "regionSquare": {"@id": "iiif:regionSquareFeature"},
            "rotationArbitrary": {"@id": "iiif:arbitraryRotationFeature"},
            "rotationBy90s": {"@id": "iiif:rotationBy90sFeature"},
            "mirroring": {"@id": "iiif:mirroringFeature"},
            "sizeAboveFull": {"@id": "iiif:sizeAboveFullFeature"},
            "sizeByForcedWh": {"@id": "iiif:sizeByForcedWHFeature"},
            "sizeByH": {"@id": "iiif:sizeByHFeature"},
            "sizeByPct": {"@id": "iiif:sizeByPctFeature"},
            "sizeByW": {"@id": "iiif:sizeByWFeature"},
            "sizeByWh": {"@id": "iiif:sizeByWHFeature"},
            "sizeByWhListed": {"@id": "iiif:sizeByWHListedFeature"},
            "sizeByConfinedWh": {"@id": "iiif:sizeByConfinedWHFeature"},
            "sizeByDistortedWh": {"@id": "iiif:sizeByDistortedWHFeature"},
            "profileLinkHeader": {"@id": "iiif:profileLinkHeaderFeature"},
            "canonicalLinkHeader": {"@id": "iiif:canonicalLinkHeaderFeature"},
            "jsonldMediaType": {"@id": "iiif:jsonLdMediaTypeFeature"},
            "height": {"@id": "exif:height"},
            "width": {"@id": "exif:width"},
            "xResolution": {"@id": "exif:xResolution"},
            "yResolution": {"@id": "exif:yResolution"},            
            "resolutionUnit": {"@id": "exif:resolutionUnit"},
            "scaleFactors": {"@id": "iiif:scaleFactor", "@container": "@set"},
            "formats": {"@id": "iiif:format"},
            "qualities": {"@id": "iiif:quality"},
            "sizes": {"@id": "iiif:hasSize","@type": "@id"},
            "tiles": {"@id": "iiif:hasTile","@type": "@id"},
            "maxWidth": {"@id": "iiif:maxWidth"},
            "maxHeight": {"@id": "iiif:maxHeight"},
            "maxArea": {"@id": "iiif:maxArea"},
            "profile": {"@id": "doap:implements","@type": "@id"},  
            "supports": {"@id": "iiif:supports", "@type": "@vocab"},
            "service": {"@type": "@id", "@id": "svcs:has_service"},
            "license": {"@type": "@id", "@id": "dcterms:rights"},
            "logo": {"@type": "@id", "@id": "foaf:logo"},
            "attribution": {"@id": "sc:attributionLabel"},
            "label": {"@id": "rdfs:label"},
            "value": {"@id": "rdf:value"}
        }
    """.getBytes()));
            //            "protocol": {"@id": "dcterms:conformsTo", "@type": "@id"},
            JsonObject jo = JsonLd.compact(JsonDocument.of(ja), contextDocument).options(options).get();
            out.writeObject(jo);
            String hold = new String(baos.toByteArray());
            //System.out.println("BEFORE\n"+hold+"\n========================================================");
            hold = hold.replaceAll(rde+"/", "./");
            //System.out.println(hold);
            //System.out.println("=============================================================================");
            return hold;
        } catch (JsonLdError ex) {
            Logger.getLogger(ROCrate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ROCrate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ROCrate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(ROCrate.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static void main(String[] args) {
        Manifest manifest = new Manifest();
        manifest
            .AddCreator("http://orcid.org/0000-0003-0223-1059")
            .AddKeyword("Whole Slide Imaging")
            .AddKeyword("pathology")
            .AddKeyword("nuclear segmentation")
            .AddPublisher("https://ror.org/05qghxh33")
            .AddPublisher("https://ror.org/01882y777");
        System.out.println(manifest.toJSONLD());
    }
}
