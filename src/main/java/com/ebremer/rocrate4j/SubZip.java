/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ebremer.rocrate4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.stream.IntStream;

/**
 *
 * @author erich
 */
public class SubZip implements SeekableByteChannel {
    //private final long start;
    //private final long end;
    private final MappedByteBuffer buffer;
    
    public SubZip(MappedByteBuffer buffer) { //, long start, long end) {
        //System.out.println("S/E --> "+start+" "+end+"   size : "+buffer.remaining());
        //System.out.println("REMAINING IN BUFFER : "+buffer.remaining());
        this.buffer = buffer;
        this.buffer.position(0);
       // this.start = start;
       // this.end = end;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        //System.out.println("READ : "+dst.remaining()+"   Capacity : "+buffer.position()+" "+buffer.capacity()+" "+buffer.remaining());
        if (buffer.remaining()==0) {
            //System.out.println("out of data");
            return -1;
        } else if (buffer.remaining()<(dst.remaining())) {
            //System.out.println("begin runt " +buffer.remaining()+" "+dst.remaining());
            int left = buffer.remaining();
            IntStream.range(buffer.position(), buffer.capacity()).forEach(i->{dst.put(buffer.get());});
            //System.out.println("RUNT "+left);
            return left;
        }
        //System.out.println("Plenty B " +buffer.position()+" "+buffer.capacity()+" "+dst.remaining()+" "+buffer.remaining());
        int left = dst.remaining();
        IntStream.range(dst.position(), dst.capacity()).forEach(i->{
            dst.put(buffer.get());
        });
        //System.out.println("Plenty E "+left);
        return left;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        buffer.position((int) newPosition);
        //System.out.println("position ("+newPosition+")");
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
    public long position() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public long size() throws IOException {
        //System.out.println("position() = "+buffer.capacity());
        return buffer.capacity();
    }
   
}
