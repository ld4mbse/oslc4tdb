package com.ld4mbse.oslc4tdb.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

/**
 * This class helps to encapsulates common SPARQL queries,
 * that could be used for applying queries to the store.
 */
public class Queries {

    /**
     * Configuring the PREFIXES definitions.
     * @param prefix to define for using on a query.
     * @param ns is the corresponding namespace on the prefix.
     * @param sb is the target query builder where we want to add a prefix.
     */
    public static void prefix(String prefix, String ns, StringBuilder sb) {
        sb.append("PREFIX ");
        sb.append(prefix);
        sb.append(":<");
        sb.append(ns);
        sb.append("> ");
    }

    /**
     * This method executes a query against a target model graph
     * for obtain the DESCRIBE information.
     * @param resource the resource URI to describe.
     * @param target the target model.
     * @return the description model.
     */
    public static Model describe(String resource, Model target) {
        Query query = QueryFactory.create("DESCRIBE <" + resource +">");
        try(QueryExecution qe = QueryExecutionFactory.create(query, target)) {
            return qe.execDescribe();
        }
    }

    /**
     * Executes a CONSTRUCT query against a target model graph.
     * @param constructionQuery the query to execute.
     * @param target the target model.
     * @return the constructed model.
     */
    public static Model construct(String constructionQuery, Model target) {
        Query query = QueryFactory.create(constructionQuery);
        try(QueryExecution qe = QueryExecutionFactory.create(query, target)) {
            return qe.execConstruct();
        }
    }

    /**
     * Executes a CONSTRUCT query against a target dataset.
     * @param constructionQuery the query to execute.
     * @param target the target dataset.
     * @return the constructed model.
     */
    public static Model construct(String constructionQuery, Dataset target) {
        Query query = QueryFactory.create(constructionQuery);
        try(QueryExecution qe = QueryExecutionFactory.create(query, target)) {
            return qe.execConstruct();
        }
    }

    /**
     * Gets all the rdf:type's of a resource to use on validations.
     * @param resource the target resource where we will extract the types.
     * @return all rdf:type's of {@code resource}; an empty list if no rdf:type
     * is found.
     */
    public static List<String> types(Resource resource) {
        List<String> types = new ArrayList<>();
        StmtIterator iterator = resource.listProperties(RDF.type);
        while(iterator.hasNext())
            types.add(iterator.next().getObject().asResource().getURI());
        return types;
    }

 }