/*
 * Erich Bremer
 * RO-Crate
 */
package com.ebremer.rocrate4j.writers;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;

/**
 * Writes a crate as a zip archive.
 *
 * @author erich
 */
public class ZipWriter extends Writer {
    private final ZipOutputStream zos;
    private final byte[] buff = new byte[4096];

    public ZipWriter(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static ZipParameters parameters(String name, CompressionMethod method, long length) {
        ZipParameters params = new ZipParameters();
        params.setCompressionMethod(method);
        if (method == CompressionMethod.STORE) {
            params.setEntrySize(length);
        }
        params.setFileNameInZip(name);
        return params;
    }

    @Override
    public void add(String name, byte[] buffer, CompressionMethod method) {
        add(name, new ByteArrayInputStream(buffer), method, buffer.length);
    }

    @Override
    public void add(String name, InputStream is, CompressionMethod method) {
        add(name, is, method, 0);
    }

    @Override
    public void add(String name, File file, CompressionMethod method) {
        try (FileInputStream fis = new FileInputStream(file)) {
            add(name, fis, method, file.length());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void add(String name, InputStream is, CompressionMethod method, long length) {
        long c = 0;
        try {
            zos.putNextEntry(parameters(name, method, length));
            int readLen;
            while ((readLen = is.read(buff)) != -1) {
                zos.write(buff, 0, readLen);
                c = c + readLen;
            }
            FileHeader fh = zos.closeEntry();
            fh.setUncompressedSize(c);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public OutputStream getOutputStream(String name, CompressionMethod method) {
        try {
            zos.putNextEntry(parameters(name, method, 0));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new OutputStream() {
            private long count = 0;
            private boolean closed = false;

            @Override
            public void write(int b) throws IOException {
                zos.write(b);
                count++;
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                zos.write(b, off, len);
                count += len;
            }

            @Override
            public void close() throws IOException {
                if (!closed) {
                    closed = true;
                    zos.closeEntry().setUncompressedSize(count);
                }
            }
        };
    }

    @Override
    public void close() {
        try {
            zos.close();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
