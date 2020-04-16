package com.ld4mbse.oslc4tdb.util;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class Resources {

    /**
     * Method to generate the ETag value for a graph,
     * this value will be send to the client to validate
     * the version of the element in a call for updating the graph.
     *
     * @param object the string with the graph value.
     * @return the ETag value.
     */

    public static String getETag(String object) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = object.getBytes("UTF-8");
            return new BigInteger(1, md.digest(bytes)).toString(16);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("Could not compute ETag for " + object, ex);
        }
    }

    public static Lang getLanguage(String accept) {
        Lang language;
        if (accept == null || "*/*".equals(accept))
            language = Lang.TURTLE;
        else
            language = RDFLanguages.contentTypeToLang(accept);
        return language;
    }

    /**
     * Builds a {@link Resource} within this domain.
     * @param baseURI the base URI.
     * @param parts additional path parts to be included.
     * @return the corresponding {@code Resource}.
     */
    public static Resource buildResource(String baseURI, String... parts) {
        return ResourceFactory.createResource(Requests.buildURI(baseURI, parts));
    }

}
