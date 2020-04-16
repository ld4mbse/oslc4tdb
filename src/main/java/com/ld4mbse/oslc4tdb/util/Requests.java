package com.ld4mbse.oslc4tdb.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common functionalities to HTTP requests.
 * @author rherrera
 */
public class Requests {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Requests.class);

    /**
     * Helper class to sort an HTTP header values according to their quality.
     */
    public static class HeaderValueQuality {
        /**
         * A HTTP header value.
         */
        private final String headerValue;
        /**
         * The HTTP header value quality.
         */
        private final double quality;
        /**
         * Constructs an instance specifying the HTTP header value and quality.
         * @param headerValue the HTTP header value.
         * @param quality the HTTP header value quality.
         */
        private HeaderValueQuality(String headerValue, double quality) {
            this.headerValue = headerValue;
            this.quality = quality;
        }
        /**
         * Gets the HTTP header value wrapped on this instance.
         * @return the HTTP header value wrapped on this instance.
         */
        public String getHeaderValue() {
            return headerValue;
        }
        /**
         * Gets the quality of the HTTP header value wrapped on this instance.
         * @return the quality of the HTTP header value wrapped on this instance.
         */
        public double getQuality() {
            return quality;
        }
        @Override
        public String toString() {
            return headerValue + "=" + quality;
        }
    }

    /**
     * Gets the values of an HTTP header sorted in descending fashion according
     * to their quality parameter. Values closer to quality 1.0 comes first.
     * @param values all the values given for an HTTP header.
     * @return descending sorted list of the HTTP header values according to
     * their quality parameter; an empty list if {@code values} has no more
     * elements from beginning.
     * @throws NullPointerException if {@code values} is {@code null}.
     */

    public static List<HeaderValueQuality> getQualifiedHeaderValues(
            Enumeration<String> values) {
        String[] inlineValues, qualifiedHeader;
        List<HeaderValueQuality> headers = new ArrayList<>();
        while(values.hasMoreElements()) {
            inlineValues = values.nextElement().split(",");
            for(String inlineValue : inlineValues) {
                qualifiedHeader = inlineValue.split(" *; *q *= *");
                if (qualifiedHeader.length == 1)
                    headers.add(new HeaderValueQuality(qualifiedHeader[0],1.0));
                else {
                    try {
                        headers.add(new HeaderValueQuality(qualifiedHeader[0],
                                Double.parseDouble(qualifiedHeader[1])));
                    } catch(NumberFormatException ex) {
                        LOG.warn("Could not weigh HTTP header value " + inlineValue, ex);
                    }
                }
            }
        }
        if (headers.size() > 1) {
            Collections.sort(headers, (HeaderValueQuality v1, HeaderValueQuality v2) -> {
                if (v1.quality > v2.quality)
                    return -1;
                if (v1.quality < v2.quality)
                    return 1;
                return 0;
            });
        }
        return headers;
    }
    /**
     * Gets a URI for a resource within this domain (without encoding parts).
     * @param baseURI the base URI.
     * @param parts additional path parts to be included.
     * @return the final URI for a resource within this domain.
     */
    public static String getURI(String baseURI, String[] parts) {
        StringBuilder sb = new StringBuilder(baseURI);
        if (parts != null && parts.length > 0) {
            for (String part : parts) {
                if (part != null && !part.isEmpty()) {
                    sb.append('/');
                    sb.append(part);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Builds a URI for a resource.
     * @param baseURI the base URI.
     * @param parts additional path parts to be included.
     * @return the final URI for a resource within this domain.
     */
    public static String buildURI(String baseURI, String... parts) {
        StringBuilder sb = new StringBuilder(baseURI);
        if (parts != null && parts.length > 0) {
            for (String part : parts) {
                if (part != null && !part.isEmpty()) {
                    sb.append('/');
                    try {
                        sb.append(URLEncoder.encode(part, "UTF-8"));
                    } catch (UnsupportedEncodingException ex) {
                        LOG.warn("Could not encode URL path part as UTF-8: " + part, ex);
                        sb.append(part);
                    }
                }
            }
        }
        return sb.toString();
    }

}