package com.ebremer.rocrate4j;

import static com.ebremer.rocrate4j.ROCrate.MANIFEST;
import static com.ebremer.rocrate4j.ROCrate.MANIFESTTTL;
import com.ebremer.rocrate4j.writers.Writer;
import java.io.File;
import java.nio.charset.StandardCharsets;
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

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    public void close() {
        manifest.ensureDatePublished();
        destination.add(MANIFEST, manifest.getManifest().getBytes(StandardCharsets.UTF_8), CompressionMethod.DEFLATE);
        destination.add(MANIFESTTTL, manifest.getManifest(RDFFormat.TRIG_PRETTY).getBytes(StandardCharsets.UTF_8), CompressionMethod.DEFLATE);
        destination.close();
    }

    public void add(String name, byte[] bytes, CompressionMethod method) {
        destination.add(name, bytes, method);
    }

    public void add(String name, File file, CompressionMethod method) {
        destination.add(name, file, method);
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Writer getWriter() {
        return destination;
    }
}
