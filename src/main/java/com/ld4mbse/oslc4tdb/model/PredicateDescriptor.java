package com.ld4mbse.oslc4tdb.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Encapsulates the path and type of a {@link Property}.
 * @author rherrera
 */
public class PredicateDescriptor {
    /**
     * The property type. E.g: rdf:Type.
     */
    public final Resource TYPE;
    /**
     * The property path. E.g: the OSLC propertyDefinition.
     */
    public final Property PATH;
    /**
     * Constructs an instance specifying the namespace and a name.
     * @param ns the namespace for the property.
     * @param name the property simple name.
     */
    public PredicateDescriptor(String ns, String name) {
        String firstLetter = name.substring(0, 1);
        PATH = ResourceFactory.createProperty(ns, name);
        name = firstLetter.toUpperCase() + name.substring(1);
        TYPE = ResourceFactory.createResource(ns + name);
    }
}