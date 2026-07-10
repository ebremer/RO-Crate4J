package com.ebremer.rocrate4j;

import com.ebremer.rocrate4j.writers.FolderWriter;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.jena.rdf.model.Resource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Asserts the shape RO-Crate 1.3 requires of the written metadata document:
 * official context by reference, flat @graph, exact metadata descriptor,
 * root data entity requirements, File/Dataset typing, encoded @ids.
 */
public class ConformanceTest {

    @TempDir
    Path tmp;

    private static final byte[] BYTES = "conformance".getBytes(StandardCharsets.UTF_8);
    private static final String UGLY_NAME = "almost 50%.bin";
    private static final String UGLY_ID = "sub/almost%2050%25.bin";

    private File writeCrate() throws IOException {
        File dir = tmp.resolve("crate").toFile();
        try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new FolderWriter(dir))) {
            builder.getManifest()
                .setName("conformance")
                .setLicense("https://creativecommons.org/licenses/by/4.0/");
            builder.add(builder.getRDE(), "test.bin", BYTES, CompressionMethod.DEFLATE, true);
            Resource sub = builder.addFolder(builder.getRDE(), "sub");
            builder.add(sub, UGLY_NAME, BYTES, CompressionMethod.DEFLATE, true);
        }
        return dir;
    }

    private static Map<String, JsonObject> graphById(File dir) throws IOException {
        try (JsonReader jr = Json.createReader(new FileInputStream(new File(dir, ROCrate.MANIFEST)))) {
            JsonObject doc = jr.readObject();
            assertEquals(JsonValue.ValueType.STRING, doc.get("@context").getValueType(),
                "context is the official URL by reference when no extensions are registered");
            assertEquals(ROCrateContexts.CONTEXT_1_3, doc.getString("@context"));
            JsonArray graph = doc.getJsonArray("@graph");
            assertNotNull(graph, "flattened form: entities are direct children of @graph");
            Map<String, JsonObject> byId = new HashMap<>();
            graph.forEach(v -> {
                JsonObject entity = v.asJsonObject();
                assertTrue(entity.containsKey("@id"), "every entity has an @id");
                assertTrue(entity.containsKey("@type"), "every entity has a @type");
                byId.put(entity.getString("@id"), entity);
            });
            return byId;
        }
    }

    private static void assertTypeContains(JsonObject entity, String type) {
        JsonValue t = entity.get("@type");
        boolean match = switch (t.getValueType()) {
            case STRING -> type.equals(((JsonString) t).getString());
            case ARRAY -> t.asJsonArray().stream()
                .anyMatch(v -> type.equals(((JsonString) v).getString()));
            default -> false;
        };
        assertTrue(match, entity.getString("@id")+" @type should contain "+type+" but was "+t);
    }

    @Test
    public void writtenCrateHasRoCrate13Shape() throws IOException {
        Map<String, JsonObject> byId = graphById(writeCrate());

        JsonObject descriptor = byId.get(ROCrate.MANIFEST);
        assertNotNull(descriptor, "metadata descriptor @id is exactly ro-crate-metadata.json");
        assertTypeContains(descriptor, "CreativeWork");
        assertEquals("./", descriptor.getJsonObject("about").getString("@id"));
        assertEquals("https://w3id.org/ro/crate/1.3", descriptor.getJsonObject("conformsTo").getString("@id"),
            "descriptor declares conformance to RO-Crate 1.3");

        JsonObject root = byId.get("./");
        assertNotNull(root, "root data entity @id is ./");
        assertTypeContains(root, "Dataset");
        assertTrue(root.containsKey("name"));
        assertTrue(root.containsKey("datePublished"), "datePublished is a MUST on the root");
        assertEquals(JsonValue.ValueType.STRING, root.get("datePublished").getValueType(),
            "datePublished is a plain ISO 8601 string");
        assertEquals("https://creativecommons.org/licenses/by/4.0/",
            root.getJsonObject("license").getString("@id"), "license is an entity reference");

        JsonObject file = byId.get("test.bin");
        assertNotNull(file, "file @id is the root-relative path");
        assertTypeContains(file, "File");

        JsonObject sub = byId.get("sub/");
        assertNotNull(sub, "directory @id ends with /");
        assertTypeContains(sub, "Dataset");

        JsonObject ugly = byId.get(UGLY_ID);
        assertNotNull(ugly, "names are percent-encoded in @id");
        assertTypeContains(ugly, "File");

        // references are {"@id": ...} objects, and hasPart reaches every file
        JsonArray rootParts = root.getJsonArray("hasPart");
        assertEquals(2, rootParts.size());
        rootParts.forEach(v -> {
            JsonObject refob = v.asJsonObject();
            assertEquals(1, refob.size(), "references carry only @id");
            assertTrue(refob.containsKey("@id"));
        });
        assertEquals(UGLY_ID, sub.getJsonObject("hasPart").getString("@id"),
            "single-element arrays are unpacked; nested file reachable via the directory");
    }

    @Test
    public void rootDiscoveryAndSpecVersion() throws IOException {
        File dir = writeCrate();
        try (ROCrateReader reader = new ROCrateReader(dir.toURI())) {
            Resource root = reader.getRootDataEntity();
            assertNotNull(root, "root found via descriptor about");
            assertTrue(root.hasProperty(ROC.name));
            assertTrue(reader.getConformsTo().contains("https://w3id.org/ro/crate/1.3"));
        }
    }

    @Test
    public void percentEncodedNamesRoundTrip() throws IOException {
        File dir = writeCrate();
        assertTrue(new File(dir, "sub/"+UGLY_NAME).isFile(), "payload stored under the raw name");
        try (ROCrateReader reader = new ROCrateReader(dir.toURI())) {
            try (SeekableByteChannel ch = reader.getSeekableByteChannel("sub/"+UGLY_NAME)) {
                assertArrayEquals(BYTES, RoundTripTest.readAll(ch), "retrieval by raw relative path");
            }
            Resource root = reader.getRootDataEntity();
            String uglyIRI = root.getModel()
                .listObjectsOfProperty(ROC.hasPart)
                .filterKeep(n -> n.isResource() && n.asResource().getURI().endsWith(".bin") && n.asResource().getURI().contains("%25"))
                .next().asResource().getURI();
            try (SeekableByteChannel ch = reader.getSeekableByteChannel(uglyIRI)) {
                assertArrayEquals(BYTES, RoundTripTest.readAll(ch), "retrieval by percent-encoded manifest IRI");
            }
        }
    }
}
