package com.ebremer.rocrate4j.writers;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import net.lingala.zip4j.model.enums.CompressionMethod;

/**
 * Destination for crate content. Entry names are paths relative to the crate
 * root, e.g. "ro-crate-metadata.json" or "data/graph.arrow".
 *
 * @author erich
 */
public abstract class Writer {
    abstract public void add(String name, InputStream is, CompressionMethod method);

    abstract public void add(String name, File file, CompressionMethod method);

    abstract public void add(String name, byte[] buffer, CompressionMethod method);

    /**
     * Opens an entry for streaming. Closing the returned stream finishes the
     * entry (it does not close the crate).
     */
    abstract public OutputStream getOutputStream(String name, CompressionMethod method);

    abstract public void close();
}
