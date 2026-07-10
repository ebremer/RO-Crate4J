package com.ebremer.rocrate4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * Read-only view of the byte range [offset, offset+size) of an underlying
 * file. Takes ownership of the supplied FileInputStream; close() closes it.
 *
 * @author erich
 */
public class SubFileSeekableByteChannel implements SeekableByteChannel {
    private final long offset;
    private final long size;
    private final FileChannel fc;

    public SubFileSeekableByteChannel(FileInputStream fis, long offset, long size) throws IOException {
        this.fc = fis.getChannel();
        this.offset = offset;
        this.size = size;
        this.fc.position(offset);
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
        long remaining = remaining();
        if (remaining <= 0) {
            return -1;
        }
        if (remaining < dst.remaining()) {
            int oldLimit = dst.limit();
            dst.limit(dst.position() + (int) remaining);
            try {
                return fc.read(dst);
            } finally {
                dst.limit(oldLimit);
            }
        }
        return fc.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (newPosition < 0) {
            throw new IllegalArgumentException("negative position: "+newPosition);
        }
        fc.position(newPosition+offset);
        return this;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return fc.isOpen();
    }

    @Override
    public void close() throws IOException {
        fc.close();
    }

    @Override
    public long size() throws IOException {
        return size;
    }
}
