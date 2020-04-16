package com.ld4mbse.oslc4tdb.model;

/**
 * Environment properties.
 * @author rherrera
 */
public interface Environment {
    /**
     * The path of the TDB store.
     */
    String TDB_LOCATION = "${tdb.location}";
    /**
     * The naming of the TDB factory resource.
     */
    String TDB_NAMING_FACTORY = "${tdb.naming.factory}";
    /**
     * The relative path for Graph servlet.
     */
    String GRAPH_PATH = "${path.graphs}";
    /**
     * The relative path for Resource servlet.
     */
    String REST_PATH = "${path.rest}";
    /**
     * The configuration model attribute name.
     */
    String CONFIG_MODEL = "RDFSTORE_CONFIG";
    /**
     * The header value for ID's requests
     */
    String ID_HEADER = "Slug";
}