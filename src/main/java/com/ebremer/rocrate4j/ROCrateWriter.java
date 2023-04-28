package com.ebremer.rocrate4j;

import static com.ebremer.rocrate4j.ROCrate.MANIFEST;
import com.ebremer.rocrate4j.writers.Writer;
import net.lingala.zip4j.model.enums.CompressionMethod;

/**
 *
 * @author erich
 */
public class ROCrateWriter {
    private final Writer destination;
    private Manifest manifest;
    
    public ROCrateWriter(Writer destination) {
        this.destination = destination;
        manifest = new Manifest();
    }
    
    public void SetManifest(Manifest manifest) {
        this.manifest = manifest;
    }
    
    public void close() {
        destination.Add(MANIFEST, manifest.getManifestBytes(), CompressionMethod.DEFLATE);
        destination.close();
    }
    
    public void Add(String name, byte[] bytes, CompressionMethod method) {
        destination.Add(name, bytes, method);
    }
    
    public Manifest GetManifest() {
        return manifest;
    }

    public Writer GetWriter() {
        return destination;
    }
}
