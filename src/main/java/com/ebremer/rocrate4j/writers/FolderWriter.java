package com.ebremer.rocrate4j.writers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.lingala.zip4j.model.enums.CompressionMethod;

/**
 * Writes a crate as a plain directory tree; the compression method is
 * ignored.
 *
 * @author erich
 */
public class FolderWriter extends Writer {
    private final String base;

    public FolderWriter(File file) {
        base = file.toString();
    }

    private Path prepare(String name) {
        Path dump = Path.of(base, name);
        try {
            Files.createDirectories(dump.getParent());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return dump;
    }

    @Override
    public void add(String name, byte[] buffer, CompressionMethod method) {
        Path dump = prepare(name);
        try (FileOutputStream fos = new FileOutputStream(dump.toFile())) {
            fos.write(buffer);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void add(String name, InputStream is, CompressionMethod method) {
        Path dump = prepare(name);
        try {
            Files.copy(is, dump, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void add(String name, File file, CompressionMethod method) {
        try (FileInputStream fis = new FileInputStream(file)) {
            add(name, fis, method);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public OutputStream getOutputStream(String name, CompressionMethod method) {
        Path dump = prepare(name);
        try {
            return new FileOutputStream(dump.toFile());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void close() {
        //nothing to do......
    }
}
