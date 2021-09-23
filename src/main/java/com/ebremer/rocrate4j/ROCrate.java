/*
 * Erich Bremer
 * RO-Crate
 */
package com.ebremer.rocrate4j;

import com.ebremer.rocrate4j.writers.Writer;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author erich
 */
public class ROCrate {
    public static String MANIFEST = "ro-crate-metadata.json";
    public static Resource ROCSpec11 = ResourceFactory.createProperty("https://w3id.org/ro/crate/1.1");
    
    private ROCrate(ROCrateBuilder builder) {
        ROCrateWriter rocwriter = builder.rocwriter;
        rocwriter.close();
    }
    
    public static class ROCrateBuilder {
        private final Writer destination;
        private ROCrateWriter rocwriter;
        private final Manifest manifest;
        
        public ROCrate build() {
            return new ROCrate(this);
        }
           
        public ROCrateBuilder(Writer destination) {
            this.destination = destination;
            this.rocwriter = new ROCrateWriter(destination);
            this.manifest = this.rocwriter.GetManifest();
        }
        
        public ROCrateWriter getROCrateWriter() {
            return rocwriter;
        }
        
        public Writer getDestination() {
            return destination;
        }
        
        public Manifest getManifest() {
            return manifest;
        }
        
        public Resource getRDE() {
            return manifest.getRDE();
        }
                
        public Resource Add(Resource parent, String base, String name, byte[] bytes, CompressionMethod method, boolean track) {
            rocwriter.Add(base+"/"+name, bytes, method);
            return manifest.addFile(parent, name, track);
        }
        
        public Resource Add(Resource parent, String base, String name, CompressionMethod method, boolean track) {
            return manifest.addFile(parent, name, track);
        }
        
        public Resource AddFolder(Resource parent, String name, Resource type) {
            return manifest.addFolder(parent, name, type);
        }
    }
}
