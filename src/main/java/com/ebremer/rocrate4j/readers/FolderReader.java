package com.ebremer.rocrate4j.readers;

import com.ebremer.rocrate4j.ROCrate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 *
 * @author erich
 */
public class FolderReader extends Reader {
    private final File base;
    
    public FolderReader(File file) {
        base = file;
    }

    @Override
    public void close() {
        //nothing to do......
    }

    @Override
    public SeekableByteChannel retrieve(String name) {
        try {
            return FileChannel.open(Path.of(base.toString(), name), StandardOpenOption.READ);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public InputStream getInputStream(String name) {
        try {
            return new FileInputStream(Path.of(base.toString(), name).toFile());
        } catch (FileNotFoundException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public boolean hasManifest() {
        return Path.of(base.toString(),ROCrate.MANIFEST).toFile().exists();
    }
}
