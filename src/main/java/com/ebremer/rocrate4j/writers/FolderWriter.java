package com.ebremer.rocrate4j.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.lingala.zip4j.model.enums.CompressionMethod;

/**
 *
 * @author erich
 */
public class FolderWriter extends Writer {
    private final String base;
    
    public FolderWriter(File file) {
        base = file.toString();
    }

    @Override
    public void Add(String name, byte[] buffer, CompressionMethod method) {
        File dump = Path.of(base,name).toFile();
        if (!dump.getParentFile().exists()) {
            dump.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(dump)) {
            fos.write(buffer);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FolderWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FolderWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() {
        //nothing to do......
    }

    @Override
    public void Add(String name, InputStream is, CompressionMethod method) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OutputStream GetOutputStream(String name, CompressionMethod method) {
        File dump = Path.of(base,name).toFile();
        if (!dump.getParentFile().exists()) {
            dump.getParentFile().mkdirs();
        }
        try {
            return new FileOutputStream(dump);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FolderWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
