package com.ld4mbse.oslc4tdb.services;

import com.ld4mbse.oslc4tdb.tdb.query.QueryCriteria;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * The RDF manager definition.
 * @author rherrera
 */
public interface RDFManager {

    /**
     * Gets the model URIs for the stores created
     * in the application.
     * @return a model with the stores as member.
     */
    Model getModels(String url);

    /**
     * Gets the model URIs whose value matches a pattern.
     * @param warehouse the name of the warehouse we want to search in.
     * @param pattern   the URI pattern to match. If {@code null} or empty,
     *                  all models' URIs will be returned.
     * @param url       the URL to use for creating the resources ID.
     * @return a model with the matching information.
     */
    Model getModels(String warehouse, String pattern, String url);

    /**
     * Determines whether a named model exists into the dataset.
     * @param warehouse the name of the warehouse we want to search in.
     * @param uri the model URI.
     * @return {@code true} if a model with the given uri exists into the
     *         dataset in the warehouse indicated; {@code false} otherwise.
     */
    boolean containsModel(String warehouse, String uri);

    /**
     * Create a model withma pair of graphs,
     * one containing the SHACL definition
     * and one for the user resources.
     * @param warehouse the name of the warehouse for which we want the graphs
     * @param shacl the model with the shacl definition
     * @param uri the identifier for the models
     */
    void setSHACLModel(String warehouse, Model shacl, String uri);

    void addModel(String warehouse, Model model, String uri);

    /**
     * Retrieves a model under a given URI.
     * @param warehouse the name of the warehouse we want to search in.
     * @param model the model identifier. If {@code null} then the default
     *              model on de {@link Dataset} will be retrieved.
     * @return the stored model.
     */
    Model getModel(String warehouse, String model);

    /**
     * Retrieves a filtered model under a given URI.
     * @param warehouse the name of the warehouse we want to search in.
     * @param uri the model identifier. If {@code null} then the default
     *            model on de {@link Dataset} will be retrieved/query.
     * @param where filtering criteria.
     * @param select projection criteria.
     * @return the filtered model.
     */
    Model getModel(String warehouse, String uri, String where, String select);

    /**
     * Removes a model under a given URI.
     * @param warehouse the name of the warehouse we want to delete from.
     * @param uri the model identifier. If {@code null} then the default
     *            model on de {@link Dataset} will be removed, be careful...
     */
    void removeModel(String warehouse, String uri);

    /**
     * Gets a resource on a given model.
     * @param warehouse the name of the warehouse we want to search in.
     * @param uri the resource identifier.
     * @param model optional, the graph name to search into.
     * @return the associated model to the resource model.
     */
    Model getResource(String warehouse, String uri, String model);

    void setResource(String warehouse, Resource resource, String model);

    void removeResource(String warehouse, Resource resource, String model);

    Model search(String warehouse, QueryCriteria criteria, String store, String base);

}