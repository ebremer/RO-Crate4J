/*
 * Erich Bremer
 * RO-Crate
 */
package com.ebremer.rocrate4j.readers;

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
    
    public ZipReader(File file) throws IOException {
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
            zip.close();
        } catch (IOException ex) {
            Logger.getLogger(ZipReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public SeekableByteChannel Retrieve(String name) {
        String t = name.substring(len);
        System.out.println("Retrieve --> "+t+"   "+name);
        ZipArchiveEntry zae = inventory.get(t);
        try {
            return new SubFileSeekableByteChannel(fis, zae.getDataOffset(), zae.getSize());
        } catch (IOException ex) {
            return null;
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
    
    public static void main(String[] args) throws IOException {
        File file = new File("D:\\nlms2\\halcyon\\x.zip");
        ZipReader zip = new ZipReader(file);
    }
}
