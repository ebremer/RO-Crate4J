package com.ebremer.rocrate4j.readers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

/**
 *
 * @author erich
 */
public class LazySeekableInMemoryByteChannel implements SeekableByteChannel {
    
    private final InputStream is;
    private boolean loaded;
    private SeekableInMemoryByteChannel sbc;
    
    public LazySeekableInMemoryByteChannel(InputStream is) {
        this.is = is;
        loaded = false;
    }
    
    public void MakeValid() {
        if (!loaded) {
            try {
                sbc = new SeekableInMemoryByteChannel(IOUtils.toByteArray(is));
                loaded = true;
            } catch (IOException ex) {
                Logger.getLogger(LazySeekableInMemoryByteChannel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return sbc.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return sbc.write(src);
    }

    @Override
    public long position() throws IOException {
        return sbc.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return sbc.position(newPosition);
    }

    @Override
    public long size() throws IOException {
        return sbc.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return sbc.truncate(size);
    }

    @Override
    public boolean isOpen() {
        return sbc.isOpen();
    }

    @Override
    public void close() throws IOException {
        sbc.close();
    }
}
