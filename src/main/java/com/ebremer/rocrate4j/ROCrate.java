package com.ebremer.rocrate4j;

import com.ebremer.rocrate4j.writers.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author erich
 */
public final class ROCrate {
    public static final String MANIFEST = "ro-crate-metadata.json";
    public static final String MANIFESTTTL = "ro-crate-metadata.ttl";
    public static final Resource ROCSpec11 = ResourceFactory.createResource("https://w3id.org/ro/crate/1.1");
    public static final Resource ROCSpec12 = ResourceFactory.createResource("https://w3id.org/ro/crate/1.2");
    public static final Resource ROCSpec13 = ResourceFactory.createResource("https://w3id.org/ro/crate/1.3");

    private ROCrate() {
    }

    /**
     * Assembles an RO-Crate: construct with a Writer destination, describe
     * and add content, then close() (or use try-with-resources) to write the
     * metadata documents and finalize the destination.
     */
    public static class ROCrateBuilder implements AutoCloseable {
        private final Writer destination;
        private final ROCrateWriter rocwriter;
        private final Manifest manifest;

        public ROCrateBuilder(Writer destination) {
            this.destination = destination;
            this.rocwriter = new ROCrateWriter(destination);
            this.manifest = this.rocwriter.getManifest();
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

        /**
         * Stores bytes in the crate at the path derived from parent and name,
         * and returns the file's entity. With track=true the entity is
         * described (typed File, linked via hasPart); with track=false the
         * bytes are stored but the entity is not described.
         */
        public Resource add(Resource parent, String name, byte[] bytes, CompressionMethod method, boolean track) {
            rocwriter.add(entryPath(parent, name), bytes, method);
            return manifest.addFile(parent, name, track);
        }

        /** Describes a directory entity under parent; see Manifest.addFolder. */
        public Resource addFolder(Resource parent, String name) {
            return manifest.addFolder(parent, name);
        }

        public Resource addFolder(Resource parent, String name, Resource extraType) {
            return manifest.addFolder(parent, name, extraType);
        }

        /**
         * Archive path for an entry: the parent's crate-relative (decoded)
         * path plus the raw name. Keeps the written entry and the manifest
         * @id derived from one value.
         */
        private String entryPath(Resource parent, String name) {
            String root = manifest.getRDE().getURI();
            String parentIRI = parent.getURI();
            if (!parentIRI.startsWith(root)) {
                throw new IllegalArgumentException("parent is not part of this crate: "+parentIRI);
            }
            String rel = parentIRI.substring(root.length());
            if (rel.isEmpty()) {
                return name;
            }
            try {
                String decoded = new URI(rel).getPath();
                return (decoded != null ? decoded : rel) + name;
            } catch (URISyntaxException ex) {
                return rel + name;
            }
        }

        /** Writes the metadata documents into the destination and closes it. */
        @Override
        public void close() {
            rocwriter.close();
        }
    }
}
