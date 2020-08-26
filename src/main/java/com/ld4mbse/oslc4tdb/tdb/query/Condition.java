package com.ld4mbse.oslc4tdb.tdb.query;

import java.util.List;

/**
 * A single condition in a {@code oslc.where} clause.
 * @author rherrera
 */
public class Condition extends Property<Condition> {
    /**
     * The operator of this condition.
     */
    private final String operator;
    /**
     * The values of this condition.
     */
    private final String[] values;
    /**
     * Determines whether this terms is scoped or not.
     */
    private final boolean scoped;
    /**
     * Constructs an instance specifying the RDF property and nested terms.
     * @param property the RDF property.
     * @param nestedTerms the nested terms.
     */
    public Condition(String property, List<Condition> nestedTerms) {
        super(property, nestedTerms);
        this.operator = null;
        this.values = null;
        this.scoped = true;
    }
    /**
     * Constructs an instance specifying the RDF property, the operator and
     * the values for this condition.
     * @param property the RDF property.
     * @param operator the operator of this condition.
     * @param values the values of this condition.
     */
    public Condition(String property, String operator, String... values) {
        super(property);
        this.scoped = false;
        this.values = values;
        this.operator = operator;
    }
    /**
     * Determines whether this condition has nested terms or not.
     * @return {@code true} if this condition has nested terms; {@code false}
     * otherwise.
     */
    public boolean isScoped() {
        return scoped;
    }
    /**
     * Gets the operator of this condition.
     * @return the operator of this condition.
     */
    public String getOperator() {
        return operator;
    }
    /**
     * Gets the values of this condition.
     * @return the values of this condition.
     */
    public String[] getValues() {
        return values;
    }

}