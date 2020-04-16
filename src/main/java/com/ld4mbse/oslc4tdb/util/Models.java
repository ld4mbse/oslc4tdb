package com.ld4mbse.oslc4tdb.util;


import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NsIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility functions related with {@link Model models}.
 * @author rherrera
 */
public class Models {
    /**
     * Logger of this class.
     */
    private static final Logger LOG;
    /**
     * A MIME types of supported RDF languages map.
     */
    private static final Map<String, Lang> RDF_MIME_TYPES;
    /**
     * The query template to execute on the
     * {@link #resolveResourcesOfType(org.apache.jena.rdf.model.Resource, org.apache.jena.rdf.model.Model)} method.
     */
    private static final String RESOLVE_RESOURCES_OF_TYPE_QUERY_TEMPLATE;
    /**
     * Static initialization.
     */
    static {
        List<Lang> all;
        RDFFormat serialization;
        RDF_MIME_TYPES = new HashMap<>();
        StringBuilder queryTemplateBuilder = new StringBuilder();
        LOG = LoggerFactory.getLogger(Models.class);
        all = new ArrayList<>(RDFLanguages.getRegisteredLanguages());
        for (Lang language : all) {
            serialization = RDFWriterRegistry.defaultSerialization(language);
            if (serialization != null && !language.getLabel().contains("null")) {
                RDF_MIME_TYPES.put(language.getContentType().toHeaderString(), language);
            }
        }
        queryTemplateBuilder.append("PREFIX rdf: <");
        queryTemplateBuilder.append(RDF.getURI());
        queryTemplateBuilder.append(">\nSELECT ?s ?p ?o WHERE '{' ?s ?p ?o ;");
        queryTemplateBuilder.append("\n rdf:type <{0}>\n'}'");
        RESOLVE_RESOURCES_OF_TYPE_QUERY_TEMPLATE = queryTemplateBuilder.toString();
    }
    /**
     * Determines whether an RDF Mime type is supported by this Jena app.
     * @param mime the Mime RDF language to test.
     * @return {@code true} if the RDF {@code mime} type is supported;
     * {@code false} otherwise.
     */
    public static boolean supportRDFMimeType(String mime) {
        return RDF_MIME_TYPES.containsKey(mime);
    }
    /**
     * Gets the set of RDF Mime types allowed by this Jena app.
     * @return the set of RDF Mime types allowed by this Jena app.
     */
    public static Set<String> getAllowedRDFMimeTypes() {
        return RDF_MIME_TYPES.keySet();
    }
    /**
     * Transforms a string value into a standardized path of this jena app.
     * On this app, paths must start with a forward slash and must not end
     * with one of them.
     * @param value the value to transform into a path.
     * @return the standardized path.
     */
    public static String path(String value) {
        if (!value.startsWith("/"))
            value = "/" + value;
        if (value.endsWith("/"))
            value = value.substring(0, value.length() - 1);
        return value;
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
     * Resolves all resources of a given RDF type in a source model. Unlike
     * using {@link Model#query(org.apache.jena.rdf.model.Selector)} with a
     * {@link SimpleSelector} that sets the predicate to {@code rdf:type} and
     * the object to the required type, this method will resolve all the other
     * properties that the matching resources have. The {@code SimpleSelector}
     * approach will get only the statement where the type of the resource is
     * establish, but not the rest. Use this method if you want to resolve all
     * the properties of all resources of a given type.
     * @param type the {@link Resource} type to search.
     * @param source the source model to search into.
     * @return tha matching resources with the given {@code type} with all
     * their properties resolved.
     */
    public static Model resolveResourcesOfType(Resource type, Model source) {
        Query query;
        RDFNode object;
        Property property;
        String queryString;
        QuerySolution statement;
        Resource subject, predicate;
        Model filtered = ModelFactory.createDefaultModel();
        queryString = MessageFormat.format(RESOLVE_RESOURCES_OF_TYPE_QUERY_TEMPLATE, type.getURI());
        LOG.debug("> {}/SPARQL\n\n{}\n", source.size(), queryString);
        query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, source)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                statement = results.nextSolution();
                subject = statement.getResource("s");
                predicate = statement.getResource("p");
                object = statement.get("o");
                property = ResourceFactory.createProperty(predicate.getURI());
                filtered.add(subject, property, object);
            }
        }
        importNamespacesPrefixes(source, filtered);
        LOG.debug("< SPARQL/{}", filtered.size());
        return filtered;
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
    /**
     * Parses an input stream into a model.
     * @param content the input stream.
     * @param mimeRDFserialization the RDF serialization format of the content.
     * @return the parsed RDF model.
     * @throws IllegalArgumentException if {@code format} does not correspond
     * with any RDF serialization language.
     */
    public static Model read(InputStream content, String mimeRDFserialization) {
        Lang language = RDF_MIME_TYPES.get(mimeRDFserialization);
        if (language == null) {
            mimeRDFserialization += " is not an RDF language";
            throw new IllegalArgumentException(mimeRDFserialization);
        }
        return read(content, null, language);
    }

    public static Model read(InputStream content, String base, String mimeRDFserialization) {
        Lang language = RDF_MIME_TYPES.get(mimeRDFserialization);
        if (language == null) {
            mimeRDFserialization += " is not an RDF language";
            throw new IllegalArgumentException(mimeRDFserialization);
        }
        return read(content, base, language);
    }

    /**
     * Serializes a model into a target output stream.
     * @param model the model to serialize.
     * @param target the target output stream.
     * @param lang the RDF serialization format.
     */
    public static void write(Model model, OutputStream target, Lang lang) {
        RDFDataMgr.write(target, model, lang);
    }
}