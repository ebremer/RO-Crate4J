package com.ebremer.rocrate4j.readers;

import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;

/**
 *
 * @author erich
 */
public abstract class Reader {
    abstract public void close();
    abstract public SeekableByteChannel Retrieve(String name);
    abstract public InputStream getInputStream(String name);
    abstract public boolean hasManifest();
}
