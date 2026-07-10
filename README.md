# RO-Crate4J

A Java library for creating and reading [Research Object Crates (RO-Crate)](https://www.researchobject.org/ro-crate/),
built on [Apache Jena](https://jena.apache.org/). Crates can be written to (and read from) either a
plain directory or a zip archive, and payload files inside zip crates can be accessed through
`SeekableByteChannel`s without extracting the archive.

## Status

Writes **RO-Crate 1.3** metadata: the official JSON-LD context referenced by URL, flattened and
compacted `@graph`, the exact `ro-crate-metadata.json` descriptor with `conformsTo`
`https://w3id.org/ro/crate/1.3`, root data entity `./` with an automatic `datePublished`, `File`/`Dataset`
typing, and percent-encoded identifiers. Reads 1.1, 1.2, and 1.3 crates; the official contexts are
bundled, so neither writing nor reading needs network access. Remaining roadmap items (detached
crates, `ro-crate-preview.html`) are tracked in [REVIEW.md](REVIEW.md) §9.

## Requirements

- Java 25+
- Maven

## Usage

### Writing a crate

```java
try (ROCrate.ROCrateBuilder builder = new ROCrate.ROCrateBuilder(new ZipWriter(new File("mycrate.zip")))) {
    // new FolderWriter(new File("mycrate")) writes the same crate as a directory

    builder.getManifest()
        .setName("My dataset")
        .setDescription("Nuclear segmentation of TCGA cancer types")
        .setLicense("https://creativecommons.org/licenses/by/4.0/")
        .setDatePublished("2026-07-10")             // auto-set to now if omitted
        .addKeyword("pathology")                    // plain string -> literal
        .addKeyword("https://example.com/keyword")  // IRI -> entity reference
        .addCreator("https://orcid.org/0000-0003-0223-1059")
        .addPublisher("https://ror.org/05qghxh33");

    byte[] data = ...;
    builder.add(builder.getRDE(), "data.bin", data, CompressionMethod.DEFLATE, true);

    Resource results = builder.addFolder(builder.getRDE(), "results");
    builder.add(results, "table.csv", csvBytes, CompressionMethod.DEFLATE, true);
} // closing the builder writes ro-crate-metadata.json + ro-crate-metadata.ttl
```

### Reading a crate

```java
try (ROCrateReader reader = new ROCrateReader(new File("mycrate.zip").toURI())) {
    Model manifest = reader.getManifest();           // Jena model of the crate metadata
    Resource root = reader.getRootDataEntity();      // via the metadata descriptor
    List<String> spec = reader.getConformsTo();      // e.g. ["https://w3id.org/ro/crate/1.3"]

    // entries are addressed by root-relative path or by their IRI from the manifest
    try (SeekableByteChannel ch = reader.getSeekableByteChannel("data.bin")) {
        ...
    }
}
```

Zip entries that are STORED are served as bounded views over the archive file itself (no
extraction, seekable); DEFLATED entries are inflated into memory on first access.

### Extension vocabularies

Domain vocabularies are not baked into the library; register them per crate and they are added
to the `@context` array alongside the official context:

```java
builder.getManifest()
    .addPrefix("hal", "https://halcyon.is/ns/")    // Turtle + JSON-LD @context
    .addTerm("tileSizeX", "hal:tileSizeX");         // JSON-LD context term
```

## Building

```
mvn verify
```

## License

[Apache License 2.0](LICENSE)
