package com.ld4mbse.oslc4tdb.tdb.query;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A {@code oslc.select} property.
 * @param <E> type for nested properties.
 * @author rherrera
 */
public class Property<E extends Property> {
    /**
     * The wildcard property.
     */
    public static final String WILDCARD = "*";
    /**
     * The {@code rdf:type} property.
     */
    public static final String RDF_TYPE = "rdf:type";
    /**
     * The wildcard property alias name.
     */
    private static final String WILDCARD_ALIAS = "ALL";
    /**
     * The wildcard property object variable name.
     */
    private static final String OBJECT_VARIABLE_FOR_WILDCARD = WILDCARD_ALIAS + "Value";
    /**
     * Standardizes the predicate to use, in a sentence, for a property name
     * given the subject variable. The outcome can be a variable or the same
     * property got in.
     * @param subjectVariable the subject variable.
     * @param property the property name.
     * @return the standardized predicate expression to use in a sentence.
     */
    public static String predicate(String subjectVariable, String property) {
        return WILDCARD.equals(property) ? subjectVariable + WILDCARD_ALIAS : property;
    }
    /**
     * Standardizes the object variable to use, in a sentence, for a property
     * name given the subject variable.
     * @param subjectVariable the subject variable.
     * @param property the property name.
     * @return the standardized object variable expression to use in a sentence.
     */
    public static String objectVariable(String subjectVariable, String property) {
        return subjectVariable + (WILDCARD.equals(property) ?  OBJECT_VARIABLE_FOR_WILDCARD : property.replace(":", ""));
    }
    /**
     * The RDF property to this term.
     */
    protected final String property;
    /**
     * The nested properties of this property.
     */
    protected final List<E> properties;
    /**
     * Constructs an instance specifying the nested properties.
     * @param property the underlying property of this instance.
     * @param properties nested properties.
     */
    public Property(String property, List<E> properties) {
        this.property = Objects.requireNonNull(property, "property cannot be null");
        this.properties = properties;
    }
    /**
     * Constructs an instance specifying the RDF property.
     * @param property the RDF property.
     * @throws NullPointerException if {@code property} is {@code null}.
     */
    public Property(String property) {
        this(property, Collections.emptyList());
    }
    /**
     * Gets the standardized predicate to use, in a sentence, for this property
     * with a given subject variable. The outcome can be a variable or the same
     * property got in.
     * @param subjectVariable the subject variable.
     * @return the standardized predicate expression to use in a sentence.
     */
    public final String getPredicate(String subjectVariable) {
        return predicate(subjectVariable, property);
    }
    /**
     * Gets the standardized object variable to use, in a sentence, for this
     * property with a given subject variable.
     * @param subjectVariable the subject variable.
     * @return the standardized object variable expression to use in a sentence.
     */
    public final String getObjectVariable(String subjectVariable) {
        return objectVariable(subjectVariable, property);
    }
    /**
     * Formats this property to be part of a SPARQL query projection.
     * @param resource the SPARQL variable to which this term belongs to.
     * @param query the target query buffer.
     */
    public void formatProjection(String resource, StringBuilder query) {
        String objectVariable = getObjectVariable(resource);
        if (query.indexOf(objectVariable) < 0) {
            query.append(". ");
            query.append(resource);
            query.append(' ');
            query.append(getPredicate(resource));
            query.append(' ');
            query.append(objectVariable);
        }
        properties.forEach((nestedProperty) -> {
            nestedProperty.formatProjection(objectVariable, query);
        });
    }
    /**
     * Formats this property to be part of a SPARQL query selection.
     * @param resource the SPARQL variable to which this term belongs to.
     * @param query the target query buffer.
     */
    public void formatSelection(String resource, StringBuilder query) {
        int whereIndex = query.indexOf("WHERE");
        String objectVariable = getObjectVariable(resource);
        if (query.indexOf(objectVariable, whereIndex) < 0) {
            query.append(". OPTIONAL {");
            query.append(resource);
            query.append(' ');
            query.append(getPredicate(resource));
            query.append(' ');
            query.append(objectVariable);
            query.append('}');
        }
        properties.forEach((nestedProperty) -> {
            nestedProperty.formatSelection(objectVariable, query);
        });
    }
}