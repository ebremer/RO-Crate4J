package com.ebremer.rocrate4j.readers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

/**
 * SeekableByteChannel that defers draining the supplied InputStream into
 * memory until the first channel operation. The stream is closed once
 * drained, or on close() if never read.
 *
 * @author erich
 */
public class LazySeekableInMemoryByteChannel implements SeekableByteChannel {

    private final InputStream is;
    private boolean loaded;
    private boolean closed;
    private SeekableInMemoryByteChannel sbc;

    public LazySeekableInMemoryByteChannel(InputStream is) {
        this.is = is;
        loaded = false;
        closed = false;
    }

    private SeekableInMemoryByteChannel load() throws IOException {
        if (closed) {
            throw new ClosedChannelException();
        }
        if (!loaded) {
            try (InputStream in = is) {
                sbc = new SeekableInMemoryByteChannel(in.readAllBytes());
            }
            loaded = true;
        }
        return sbc;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return load().read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return load().write(src);
    }

    @Override
    public long position() throws IOException {
        return load().position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return load().position(newPosition);
    }

    @Override
    public long size() throws IOException {
        return load().size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return load().truncate(size);
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (loaded) {
            sbc.close();
        } else {
            is.close();
        }
    }
}
