package com.ld4mbse.oslc4tdb.tdb.validation;

import java.util.Observer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * Interface that define the methods for
 * the resource shape validator.
 */
public interface Validator extends Observer {

    /**
     * This method helps to validate a specific resource.
     * @param resource the resource we want to validate.
     * @throws ValidationException when resource doesn't complet the structure validation.
     * @throws FetchingRulesException when other request is updating the resource constraints.
     */
    void validate(Resource resource);

    /**
     * This method helps to validate a graph with all its resources.
     * @param model the model representing the graph.
     */
    void validate(Model model);
}
