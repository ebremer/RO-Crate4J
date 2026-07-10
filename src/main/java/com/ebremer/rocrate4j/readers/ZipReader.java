/*
 * Erich Bremer
 * RO-Crate
 */
package com.ebremer.rocrate4j.readers;

import com.ebremer.rocrate4j.ROCrate;
import com.ebremer.rocrate4j.SubFileSeekableByteChannel;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;

/**
 *
 * @author erich
 */
public class ZipReader extends Reader {
    private final FileInputStream fis;
    private final ZipFile zip;
    private final HashMap<String, ZipArchiveEntry> inventory;
    private final File file;

    public ZipReader(File file) throws IOException {
        this.file = file;
        fis = new FileInputStream(file);
        zip = new ZipFile(fis.getChannel());
        inventory = new HashMap<>();
        zip.getEntries().asIterator().forEachRemaining(e->{
            inventory.put(e.getName(), e);
        });
    }
    
    @Override
    public void close() {
        try {
            zip.close();
            fis.close();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public SeekableByteChannel retrieve(String name) {
        ZipArchiveEntry zae = inventory.get(name);
        if (zae == null) {
            throw new IllegalArgumentException("no such entry in "+file+": "+name);
        }
        if (zae.getMethod() == ZipMethod.STORED.getCode()) {
            try {
                return new SubFileSeekableByteChannel(new FileInputStream(file), zae.getDataOffset(), zae.getSize());
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        } else {
            try {
                return new LazySeekableInMemoryByteChannel(zip.getInputStream(zae));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    @Override
    public InputStream getInputStream(String name) {
        ZipArchiveEntry zae = inventory.get(name);
        if (zae == null) {
            throw new IllegalArgumentException("no such entry in "+file+": "+name);
        }
        try {
            return zip.getInputStream(zae);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public boolean hasManifest() {
        return inventory.containsKey(ROCrate.MANIFEST);
    }
}
