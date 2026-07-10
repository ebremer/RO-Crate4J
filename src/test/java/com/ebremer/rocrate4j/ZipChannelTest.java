package com.ebremer.rocrate4j;

import com.ebremer.rocrate4j.readers.LazySeekableInMemoryByteChannel;
import com.ebremer.rocrate4j.writers.ZipWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.jena.rdf.model.Resource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Channel-level behavior against a hand-built zip containing one STORED and
 * one DEFLATED entry, independent of this library's own writer.
 */
public class ZipChannelTest {

    @TempDir
    Path tmp;

    private static final byte[] STORED = new byte[300];
    private static final byte[] PACKED = "deflate me deflate me deflate me deflate me".getBytes(StandardCharsets.UTF_8);

    static {
        for (int i = 0; i < STORED.length; i++) {
            STORED[i] = (byte) (i % 251);
        }
    }

    private File buildZip() throws IOException {
        File zf = tmp.resolve("channels.zip").toFile();
        byte[] manifest = "{\"@id\":\"./\",\"@type\":\"http://schema.org/Dataset\"}".getBytes(StandardCharsets.UTF_8);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zf))) {
            zos.putNextEntry(new ZipEntry(ROCrate.MANIFEST));
            zos.write(manifest);
            zos.closeEntry();
            ZipEntry stored = new ZipEntry("stored.bin");
            stored.setMethod(ZipEntry.STORED);
            stored.setSize(STORED.length);
            CRC32 crc = new CRC32();
            crc.update(STORED);
            stored.setCrc(crc.getValue());
            zos.putNextEntry(stored);
            zos.write(STORED);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("packed.bin"));
            zos.write(PACKED);
            zos.closeEntry();
        }
        return zf;
    }

    @Test
    public void storedEntryUsesBoundedSubFileChannel() throws IOException {
        File zf = buildZip();
        try (ROCrateReader reader = new ROCrateReader(zf.toURI())) {
            try (SeekableByteChannel ch = reader.getSeekableByteChannel("stored.bin")) {
                assertInstanceOf(SubFileSeekableByteChannel.class, ch);
                assertTrue(ch.isOpen());
                assertEquals(STORED.length, ch.size());
                assertEquals(0, ch.position(), "fresh channel starts at the entry's first byte");

                ByteBuffer oversized = ByteBuffer.allocate(STORED.length + 100);
                assertEquals(STORED.length, ch.read(oversized), "read stops at the entry boundary");
                assertEquals(-1, ch.read(ByteBuffer.allocate(8)), "EOF after the entry");
                oversized.flip();
                byte[] got = new byte[oversized.remaining()];
                oversized.get(got);
                assertArrayEquals(STORED, got, "exact entry bytes, not zip headers or neighbors");

                ch.position(5);
                ByteBuffer window = ByteBuffer.allocate(10);
                ch.read(window);
                assertEquals(STORED[5], window.get(0), "seeking is relative to the entry");
                assertEquals(15, ch.position());
            }
        }
    }

    @Test
    public void deflatedEntryUsesLazyInMemoryChannel() throws IOException {
        File zf = buildZip();
        try (ROCrateReader reader = new ROCrateReader(zf.toURI())) {
            try (SeekableByteChannel ch = reader.getSeekableByteChannel("packed.bin")) {
                assertInstanceOf(LazySeekableInMemoryByteChannel.class, ch);
                assertArrayEquals(PACKED, RoundTripTest.readAll(ch), "lazy channel works without priming");
            }
        }
    }

    @Test
    public void channelsCloseAndReleaseTheZip() throws IOException {
        File zf = buildZip();
        SeekableByteChannel ch;
        try (ROCrateReader reader = new ROCrateReader(zf.toURI())) {
            ch = reader.getSeekableByteChannel("stored.bin");
            ch.read(ByteBuffer.allocate(16));
            ch.close();
            assertFalse(ch.isOpen());
            assertThrows(IllegalArgumentException.class, () -> reader.getSeekableByteChannel("missing.bin"));
        }
        assertTrue(zf.delete(), "all handles released; file deletable on Windows");
    }

    @Test
    public void iriBasedRetrievalOnZipCrate() throws IOException {
        File zf = buildZip();
        try (ROCrateReader reader = new ROCrateReader(zf.toURI())) {
            Resource root = reader.getManifest().listSubjects().nextResource();
            try (SeekableByteChannel ch = reader.getSeekableByteChannel(root.getURI() + "/stored.bin")) {
                assertArrayEquals(STORED, RoundTripTest.readAll(ch));
            }
        }
    }

    @Test
    public void writerOutputStreamClosesEntryNotArchive() throws IOException {
        File zf = tmp.resolve("streamed.zip").toFile();
        ZipWriter writer = new ZipWriter(zf);
        try (OutputStream os = writer.getOutputStream("a.bin", CompressionMethod.DEFLATE)) {
            os.write(new byte[]{1, 2, 3});
        }
        writer.add("b.bin", new byte[]{4, 5}, CompressionMethod.DEFLATE);
        writer.close();
        try (ZipFile zip = ZipFile.builder().setFile(zf).get()) {
            assertArrayEquals(new byte[]{1, 2, 3}, zip.getInputStream(zip.getEntry("a.bin")).readAllBytes());
            assertArrayEquals(new byte[]{4, 5}, zip.getInputStream(zip.getEntry("b.bin")).readAllBytes());
        }
    }
}
