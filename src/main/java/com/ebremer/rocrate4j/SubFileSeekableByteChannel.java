package com.ebremer.rocrate4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;

/**
 *
 * @author erich
 */
/**
 *
 * @author erich
 */
public class SubFileSeekableByteChannel implements SeekableByteChannel {
    private final long offset;
    private final long size;
    private final FileChannel fc;
    
    public SubFileSeekableByteChannel(FileInputStream fis, long offset, long size) throws IOException {
        this.fc = fis.getChannel();
        this.fc.position(0);
        this.offset = offset;
        this.size = size;
    }
    
    private long remaining() throws IOException {
        return offset+size-fc.position();
    }

    @Override
    public long position() throws IOException {
        return fc.position()-offset;
    } 
    
    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (remaining()==0) {
            return -1;
        } else if (remaining()<(dst.remaining())) {
            long left = remaining();
            fc.read(dst);
            return (int) left;
        }
        int left = dst.remaining();
        fc.read(dst);
        return left;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        fc.position(newPosition+offset);
        return this;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public long size() throws IOException {
        return size;
    }
}
