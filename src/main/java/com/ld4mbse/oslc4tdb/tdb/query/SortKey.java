package com.ld4mbse.oslc4tdb.tdb.query;

import java.util.List;

/**
 * A sorting key in a {@code oslc.orderBy} clause.
 * @author rherrera
 */
public class SortKey extends Property<SortKey> {
    /**
     * The sorting direction.
     */
    private final String direction;
    /**
     * Constructs an instance specifying the RDF property and nested terms.
     * @param property the RDF property.
     * @param nestedKeys the nested sort keys.
     */
    public SortKey(String property, List<SortKey> nestedKeys) {
        super(property, nestedKeys);
        this.direction = null;
    }
    /**
     * Constructs an instance specifying the RDF property, the operator and
     * the values for this term.
     * @param property the RDF property.
     * @param direction the operator of this term.
     */
    public SortKey(String property, String direction) {
        super(property);
        this.direction = direction;
    }

}