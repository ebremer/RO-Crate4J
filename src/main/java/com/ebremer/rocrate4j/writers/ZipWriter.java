/*
 * Erich Bremer
 * RO-Crate
 */
package com.ebremer.rocrate4j.writers;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;

/**
 *
 * @author erich
 */
public class ZipWriter extends Writer {
    private OutputStream fos;
    private ZipOutputStream zos;
    private byte[] buff = new byte[4096];
    
    public ZipWriter(File file)  {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            fos = new BufferedOutputStream(new FileOutputStream(file));
            zos = new ZipOutputStream(fos);           
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ZipWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ZipWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void Add(String name, byte[] buffer, CompressionMethod method) {
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
        Add(name, bis, method, 0);
    }
    
    @Override
    public void Add(String name, InputStream is, CompressionMethod method) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public OutputStream GetOutputStream(String name, CompressionMethod method) {
        ZipParameters params = new ZipParameters();
        params.setCompressionMethod(method);
        if (method==CompressionMethod.STORE) {
            params.setEntrySize(0);
        }
        params.setCompressionMethod(method);
        params.setFileNameInZip(name);
        try {
            zos.putNextEntry(params);
        } catch (IOException ex) {
            Logger.getLogger(ZipWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return zos;
    }
    
    private void Add(String name, InputStream is, CompressionMethod method, long length) {
        ZipParameters params = new ZipParameters();
        params.setCompressionMethod(method);
        if (method==CompressionMethod.STORE) {
            params.setEntrySize(length);
        }
        params.setFileNameInZip(name);
        long c = 0;
        try {
            zos.putNextEntry(params);
            int readLen;
            while ((readLen = is.read(buff)) != -1) {
                zos.write(buff, 0, readLen);
                c = c + readLen;
            }
            FileHeader fh = zos.closeEntry();
            fh.setUncompressedSize(c);
        } catch (IOException ex) {
            Logger.getLogger(ZipWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void close() {
        try {
            zos.close();
        } catch (IOException ex) {
            Logger.getLogger(ZipWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void Add(String name, File file, CompressionMethod method) {
        try {
            FileInputStream fis = new FileInputStream(file);
            Add(name, fis, method, file.length());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ZipWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
