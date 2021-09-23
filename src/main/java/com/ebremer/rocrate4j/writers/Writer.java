package com.ebremer.rocrate4j.writers;

import java.io.InputStream;
import java.io.OutputStream;
import net.lingala.zip4j.model.enums.CompressionMethod;

/**
 *
 * @author erich
 */
public abstract class Writer {
    abstract public void Add(String name, InputStream is, CompressionMethod method);
    abstract public OutputStream GetOutputStream(String name, CompressionMethod method);
    abstract public void Add(String name, byte[] buffer, CompressionMethod method);
    abstract public void close();
}
