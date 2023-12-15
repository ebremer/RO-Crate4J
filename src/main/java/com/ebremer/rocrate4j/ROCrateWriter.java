package com.ebremer.rocrate4j;

import static com.ebremer.rocrate4j.ROCrate.MANIFEST;
import static com.ebremer.rocrate4j.ROCrate.MANIFESTTTL;
import com.ebremer.rocrate4j.writers.Writer;
import java.io.File;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.jena.riot.RDFFormat;

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
        //System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        //RDFDataMgr.write(System.out, manifest.getManifestModel(), Lang.TURTLE);
        //System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        destination.Add(MANIFEST, manifest.getManifest().getBytes(), CompressionMethod.DEFLATE);
        destination.Add(MANIFESTTTL, manifest.getManifest(RDFFormat.TRIG_PRETTY).getBytes(), CompressionMethod.DEFLATE);
        destination.close();
    }
    
    public void Add(String name, byte[] bytes, CompressionMethod method) {
        destination.Add(name, bytes, method);
    }
    
    public void Add(String name, File file, CompressionMethod method) {
        destination.Add(name, file, method);
    }
    
    public Manifest GetManifest() {
        return manifest;
    }

    public Writer GetWriter() {
        return destination;
    }
}
