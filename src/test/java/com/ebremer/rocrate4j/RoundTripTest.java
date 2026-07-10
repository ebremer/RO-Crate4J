package com.ebremer.rocrate4j;

import com.ebremer.rocrate4j.writers.FolderWriter;
import com.ebremer.rocrate4j.writers.ZipWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFFormat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Write a crate, read it back, and check that what comes out is what went in.
 */
public class RoundTripTest {

    @TempDir
    Path tmp;

    private static final byte[] PAYLOAD = "hello subchannel world 0123456789".getBytes(StandardCharsets.UTF_8);

    static byte[] readAll(SeekableByteChannel ch) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer bb = ByteBuffer.allocate(1024);
        while (ch.read(bb) != -1) {
            bb.flip();
            out.write(bb.array(), 0, bb.limit());
            bb.clear();
        }
        return out.toByteArray();
    }

    private void populate(ROCrate.ROCrateBuilder builder) {
        Resource fileEnt = builder.add(builder.getRDE(), "test.bin", PAYLOAD, CompressionMethod.DEFLATE, true);
        fileEnt.addProperty(ROC.about, builder.getRDE());
        builder.getManifest()
            .setName("crate é 中文")
            .addKeyword("Whole Slide Imaging")
            .addKeyword("pathology")
            .addKeyword("https://example.com/keywords/segmentation")
            .addCreator("http://orcid.org/0000-0003-0223-1059")
            .addPublisher("https://ror.org/05qghxh33");
    }

    private void verify(ROCrateReader reader) throws IOException {
        Model m = reader.getManifest();
        Resource root = m.listSubjectsWithProperty(ROC.name, "crate é 中文").nextResource();

        List<RDFNode> keywords = m.listObjectsOfProperty(root, ROC.keywords).toList();
        assertEquals(2, keywords.stream().filter(RDFNode::isLiteral).count(), "plain keywords stay literals");
        assertEquals(1, keywords.stream().filter(RDFNode::isResource).count(), "IRI keywords become resources");

        // both the metadata descriptor and the file entity point at the root
        List<Statement> abouts = m.listStatements(null, ROC.about, (RDFNode) null).toList();
        assertEquals(2, abouts.size());
        abouts.forEach(st -> assertEquals(root, st.getObject(), "object-position root references rewritten to root IRI"));

        Resource part = m.listObjectsOfProperty(root, ROC.hasPart).next().asResource();
        assertFalse(part.getURI().contains("//test"), "no double slash in file IRIs");

        assertTrue(root.hasProperty(ROC.datePublished), "datePublished auto-set at build time");
        assertEquals(root, reader.getRootDataEntity(), "root discovery via the metadata descriptor");

        try (SeekableByteChannel ch = reader.getSeekableByteChannel("test.bin")) {
            assertArrayEquals(PAYLOAD, readAll(ch), "retrieval by root-relative entry name");
        }
        try (SeekableByteChannel ch = reader.getSeekableByteChannel(part.getURI())) {
            assertArrayEquals(PAYLOAD, readAll(ch), "retrieval by manifest IRI");
        }
    }

    @Test
    public void folderCrateRoundTrip() throws IOException {
        File dir = tmp.resolve("crateA").toFile();
        try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new FolderWriter(dir))) {
            populate(builder);
        }
        try (ROCrateReader reader = new ROCrateReader(dir.toURI())) {
            assertTrue(reader.hasManifest());
            verify(reader);
        }
    }

    @Test
    public void zipCrateRoundTrip() throws IOException {
        File zip = tmp.resolve("crateA.zip").toFile();
        try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new ZipWriter(zip))) {
            populate(builder);
        }
        try (ROCrateReader reader = new ROCrateReader(zip.toURI())) {
            assertTrue(reader.hasManifest());
            verify(reader);
        }
        assertTrue(zip.delete(), "no file handles may remain after close");
    }

    @Test
    public void autoDatePublishedIsIso8601() throws IOException {
        File dir = tmp.resolve("crateDate").toFile();
        try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new FolderWriter(dir))) {
            builder.getManifest().setName("dated");
        }
        try (ROCrateReader reader = new ROCrateReader(dir.toURI())) {
            Resource root = reader.getRootDataEntity();
            String lex = root.getProperty(ROC.datePublished).getLiteral().getLexicalForm();
            Instant.parse(lex); // throws if not ISO 8601
        }
    }

    @Test
    public void explicitDatePublishedIsNotOverwritten() throws IOException {
        File dir = tmp.resolve("crateDate2").toFile();
        try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new FolderWriter(dir))) {
            builder.getManifest().setName("dated2").setDatePublished("2020-01-01");
        }
        try (ROCrateReader reader = new ROCrateReader(dir.toURI())) {
            List<Statement> dates = reader.getRootDataEntity().listProperties(ROC.datePublished).toList();
            assertEquals(1, dates.size());
            assertEquals("2020-01-01", dates.get(0).getLiteral().getLexicalForm());
        }
    }

    @Test
    public void licenseSetterDistinguishesIriFromText() {
        Manifest man = new Manifest();
        man.setLicense("https://creativecommons.org/licenses/by/4.0/");
        assertTrue(man.getRDE().getProperty(ROC.license).getObject().isResource());
        man.setLicense("All rights reserved");
        List<Statement> licenses = man.getRDE().listProperties(ROC.license).toList();
        assertEquals(1, licenses.size(), "setLicense replaces the previous value");
        assertTrue(licenses.get(0).getObject().isLiteral());
    }

    @Test
    public void extensionPrefixAndTermRoundTrip() throws IOException {
        File dir = tmp.resolve("crateExt").toFile();
        Property tileSizeX = ResourceFactory.createProperty("https://halcyon.is/ns/tileSizeX");
        try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new FolderWriter(dir))) {
            builder.getManifest()
                .addPrefix("hal", "https://halcyon.is/ns/")
                .addTerm("tileSizeX", "hal:tileSizeX")
                .setName("ext");
            builder.getRDE().addLiteral(tileSizeX, 512);
        }

        String json = Files.readString(dir.toPath().resolve(ROCrate.MANIFEST));
        assertTrue(json.contains(ROCrateContexts.CONTEXT_1_3), "official context referenced by URL");
        assertTrue(json.contains("\"hal\""), "extension prefix present in @context");
        assertTrue(json.contains("tileSizeX"), "extension term present");

        try (ROCrateReader reader = new ROCrateReader(dir.toURI())) {
            Resource root = reader.getRootDataEntity();
            assertTrue(reader.getManifest().contains(root, tileSizeX), "extension term expands to the registered IRI");
        }
    }

    @Test
    public void eachManifestGetsItsOwnBase() {
        assertNotEquals(new Manifest().getRDE().getURI(), new Manifest().getRDE().getURI());
    }

    @Test
    public void unsupportedManifestFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Manifest().getManifest(RDFFormat.NTRIPLES));
    }

    @Test
    public void missingManifestThrows() throws IOException {
        Path empty = Files.createDirectories(tmp.resolve("empty"));
        assertThrows(FileNotFoundException.class, () -> new ROCrateReader(empty.toUri()));
    }

    @Test
    public void unsupportedSchemesThrow() {
        assertThrows(IllegalArgumentException.class, () -> new ROCrateReader(URI.create("ftp://example.com/x.zip")));
        assertThrows(UnsupportedOperationException.class, () -> new ROCrateReader(URI.create("https://example.com/x.zip")));
    }

    @Test
    public void zipWriterFailsLoudlyOnUnwritablePath() throws IOException {
        File blocker = tmp.resolve("blocker.txt").toFile();
        Files.writeString(blocker.toPath(), "x");
        assertThrows(UncheckedIOException.class, () -> new ZipWriter(new File(blocker, "x.zip")));
    }

    @Test
    public void missingEntriesFailLoudly() throws IOException {
        File dir = tmp.resolve("crateMissing").toFile();
        try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new FolderWriter(dir))) {
            builder.getManifest().setName("missing");
        }
        try (ROCrateReader reader = new ROCrateReader(dir.toURI())) {
            assertThrows(UncheckedIOException.class, () -> reader.getInputStream("nope.bin"));
            assertThrows(UncheckedIOException.class, () -> reader.getSeekableByteChannel("nope.bin"));
        }
    }
}
