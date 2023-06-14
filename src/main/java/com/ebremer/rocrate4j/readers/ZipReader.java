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
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 *
 * @author erich
 */
public class ZipReader extends Reader {
    private final FileInputStream fis;
    private final ZipFile zip;
    private final HashMap<String, ZipArchiveEntry> inventory;
    private final int len;
    private final File file;
    
    public ZipReader(File file) throws IOException {
        this.file = file;
        len = file.toURI().toString().length()+3;
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
            fis.close();
            zip.close();
        } catch (IOException ex) {
            Logger.getLogger(ZipReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public SeekableByteChannel Retrieve(String name) {
        String t = name.substring(len);
        ZipArchiveEntry zae = inventory.get(t);
        if (zae.getCompressedSize()==zae.getSize()) {
            try {
                return new SubFileSeekableByteChannel(new FileInputStream(file), zae.getDataOffset(), zae.getSize());
            } catch (IOException ex) {
                return null;
            }
        } else {
            try {
                //byte[] buffer = zip.getInputStream(zae).readAllBytes();
                //return new SeekableInMemoryByteChannel(IOUtils.toByteArray(zip.getInputStream(zae)));
                return new LazySeekableInMemoryByteChannel(zip.getInputStream(zae));
            } catch (IOException ex) {
                Logger.getLogger(ZipReader.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    @Override
    public InputStream getInputStream(String name) {
        try {
            return zip.getInputStream(zip.getEntry(name));
        } catch (IOException ex) {
            Logger.getLogger(ZipReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public boolean hasManifest() {
        return inventory.containsKey(ROCrate.MANIFEST);
    }
}
