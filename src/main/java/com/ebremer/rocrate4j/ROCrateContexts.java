package com.ebremer.rocrate4j;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.SchemeRouter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * Bundled copies of the official RO-Crate JSON-LD contexts, so crates can be
 * written and read without fetching w3id.org at runtime. Documents other than
 * the bundled contexts fall through to titanium's default loader.
 *
 * @author erich
 */
public final class ROCrateContexts {
    public static final String CONTEXT_1_1 = "https://w3id.org/ro/crate/1.1/context";
    public static final String CONTEXT_1_2 = "https://w3id.org/ro/crate/1.2/context";
    public static final String CONTEXT_1_3 = "https://w3id.org/ro/crate/1.3/context";

    private static final Map<String, String> BUNDLED = Map.of(
        CONTEXT_1_1, "/ro-crate-context-1.1.jsonld",
        CONTEXT_1_2, "/ro-crate-context-1.2.jsonld",
        CONTEXT_1_3, "/ro-crate-context-1.3.jsonld"
    );

    public static final DocumentLoader LOADER = ROCrateContexts::load;

    private ROCrateContexts() {
    }

    private static Document load(URI url, DocumentLoaderOptions options) throws JsonLdError {
        String resource = BUNDLED.get(url.toString());
        if (resource == null) {
            return SchemeRouter.defaultInstance().loadDocument(url, options);
        }
        try (InputStream is = ROCrateContexts.class.getResourceAsStream(resource)) {
            Document document = JsonDocument.of(is);
            document.setDocumentUrl(url);
            return document;
        } catch (IOException ex) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_REMOTE_CONTEXT_FAILED, ex);
        }
    }
}
