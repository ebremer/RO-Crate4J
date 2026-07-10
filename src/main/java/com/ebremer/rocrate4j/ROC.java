package com.ebremer.rocrate4j;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * The schema.org terms this library uses, on the http://schema.org/
 * namespace that the RO-Crate context is built on. (Jena's SchemaDO
 * vocabulary uses https://schema.org/, which RO-Crate tooling does not
 * recognize.)
 *
 * @author erich
 */
public final class ROC {
    public static final String NS = "http://schema.org/";

    public static final Resource Dataset = resource("Dataset");
    public static final Resource CreativeWork = resource("CreativeWork");
    /** RO-Crate's "File" is an alias for schema.org MediaObject. */
    public static final Resource MediaObject = resource("MediaObject");

    public static final Property name = property("name");
    public static final Property description = property("description");
    public static final Property datePublished = property("datePublished");
    public static final Property license = property("license");
    public static final Property about = property("about");
    public static final Property hasPart = property("hasPart");
    public static final Property keywords = property("keywords");
    public static final Property author = property("author");
    public static final Property publisher = property("publisher");
    public static final Property contentSize = property("contentSize");
    public static final Property encodingFormat = property("encodingFormat");

    private ROC() {
    }

    private static Resource resource(String local) {
        return ResourceFactory.createResource(NS+local);
    }

    private static Property property(String local) {
        return ResourceFactory.createProperty(NS+local);
    }
}
