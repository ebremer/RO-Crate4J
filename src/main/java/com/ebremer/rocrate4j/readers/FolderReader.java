package com.ebremer.rocrate4j.readers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public SeekableByteChannel Retrieve(String name) {
        try {
            String wow = base.toString();
            System.out.println(base.toString()+"  "+name);
            URI uri = new URI(name);
            
            //FileInputStream fis = new FileInputStream(Path.of(wow, name).toFile());
            FileInputStream fis = new FileInputStream(new File(uri.getPath()));
            return fis.getChannel();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FolderReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(FolderReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static void main(String[] args) throws IOException {
        File file = new File("D:\\nlms2\\halcyon\\x");
        FolderReader zip = new FolderReader(file);
        System.out.println(zip.Retrieve("halcyon/dictionary").size());
    }

    @Override
    public InputStream getInputStream(String name) {
        try {
            return new FileInputStream(Path.of(base.toString(), name).toFile());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FolderReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
