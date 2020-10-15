package com.ld4mbse.oslc4tdb.util;


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NsIterator;
//import org.apache.jena.rdf.model.Property;
//import org.apache.jena.rdf.model.RDFNode;
//import org.apache.jena.rdf.model.Resource;
//import org.apache.jena.rdf.model.ResourceFactory;
//import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
//import org.apache.jena.vocabulary.RDF;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.InputStream;
//import java.io.OutputStream;
//import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility functions related with {@link Model models}.
 * @author rherrera
 */
public class Models {

    /**
     * A MIME types of supported RDF languages map.
     */
    private static final Map<String, Lang> RDF_MIME_TYPES;

    /**
     * Static initialization.
     */
    static {
        List<Lang> all;
        RDFFormat serialization;
        RDF_MIME_TYPES = new HashMap<>();
        all = new ArrayList<>(RDFLanguages.getRegisteredLanguages());
        for (Lang language : all) {
            serialization = RDFWriterRegistry.defaultSerialization(language);
            if (serialization != null && !language.getLabel().contains("null")) {
                RDF_MIME_TYPES.put(language.getContentType().toHeaderString(), language);
            }
        }
    }
    /**
     * Gets the URN for a given store name.
     * @param store the store name.
     * @return the corresponding URN for the given name.
     */
    public static String getStoreURN(String store) {
        return "urn:" + store;
    }
    /**
     * Imports the namespace prefixes used in a target model from a source model.
     * @param source the source namespaces model.
     * @param target the target of the import functionality.
     */
    public static void importNamespacesPrefixes(Model source, Model target) {
        String prefix, namespace;
        NsIterator namespaces = target.listNameSpaces();
        while(namespaces.hasNext()) {
            namespace = namespaces.next();
            prefix = source.getNsURIPrefix(namespace);
            if (prefix != null) target.setNsPrefix(prefix, namespace);
        }
    }
    /**
     * Parses an input stream into a model.
     * @param content the input stream.
     * @param base the base URL to resolve relative paths.
     * @param language the RDF serialization format on the input stream.
     * @return the parsed RDF model.
     */
    public static Model read(InputStream content, String base, Lang language) {
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, content, base, language);
        return model;
    }

    public static Model read(InputStream content, String base, String mimeRDFserialization) {
        Lang language = RDF_MIME_TYPES.get(mimeRDFserialization);
        if (language == null) {
            mimeRDFserialization += " is not an RDF language";
            throw new IllegalArgumentException(mimeRDFserialization);
        }
        return read(content, base, language);
    }

}