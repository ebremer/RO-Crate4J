package com.ebremer.rocrate4j.readers;

import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 *
 * @author erich
 */
public abstract class Reader {
    abstract public void close();

    /**
     * Returns a seekable channel over an entry. The caller owns the returned
     * channel and must close it.
     * @param name entry path relative to the crate root, e.g. "ro-crate-metadata.json" or "data/graph.arrow"
     */
    abstract public SeekableByteChannel retrieve(String name);

    /**
     * Returns a stream over an entry.
     * @param name entry path relative to the crate root
     */
    abstract public InputStream getInputStream(String name);

    abstract public boolean hasManifest();
}
