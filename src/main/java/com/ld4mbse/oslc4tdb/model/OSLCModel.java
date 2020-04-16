package com.ld4mbse.oslc4tdb.model;

import com.ld4mbse.oslc4tdb.util.Queries;
import com.ld4mbse.oslc4tdb.util.Warehouses;
import com.ld4mbse.oslc4tdb.util.Resources;
import com.ld4mbse.oslc4tdb.util.Requests;

import java.util.*;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.Occurs;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.QueryCapability;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ld4mbse.oslc4tdb.util.OslcShaclAdapter.addShapeResource;

/**
 * Encapsulates the OSLC basic model.
 * @author rherrera
 */
public class OSLCModel {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(OSLCModel.class);
    /**
     * Builds the SPARQL query to expand a resource statements up to n level.
     * @param resource the origin resource URL.
     * @param level the maximum level to expand statements. Origin resource is
     * at zero level.
     * @return the corresponding query.
     */
    private static String getExpansionQuery(String resource, int level) {
        StringBuilder qry = new StringBuilder();
        StringBuilder braces = new StringBuilder();
        StringBuilder optionals = new StringBuilder();
        LOG.trace(" > getExpansionQuery? [{}] {}", level, resource);
        qry.append("CONSTRUCT {?rsc0 ?prd0 ?obj0");
        for(int i = 1; i <= level; i++) {
            qry.append(" . ?obj");
            qry.append(i - 1);
            qry.append(" ?prd");
            qry.append(i);
            qry.append(" ?obj");
            qry.append(i);
            optionals.append(" . OPTIONAL {?obj");
            optionals.append(i - 1);
            optionals.append(" ?prd");
            optionals.append(i);
            optionals.append(" ?obj");
            optionals.append(i);
            braces.append('}');
        }
        qry.append("} WHERE {?rsc0 ?prd0 ?obj0 . <");
        qry.append(resource);
        qry.append("> ?prd0 ?obj0");
        qry.append(optionals.toString());
        qry.append(braces.toString());
        qry.append('}');
        resource = qry.toString();
        LOG.trace("< getExpansionQuery[{}]", resource);
        return resource;
    }
    /**
     * Builds the SPARQL query to resolve the CreationFactory for an rdf:type.
     * @param type the rdf:type to resolve its CreationFactory.
     * @param store the ServiceProvider title to scope the search.
     * @return the corresponding query.
     */
    private static String getCreationFactoryQuery(String type, String store) {
        StringBuilder qry = new StringBuilder();
        LOG.trace("> getFactoryForTypeQuery? {} @ {}", type, store);
        Queries.prefix(OslcConstants.RDF_NAMESPACE_PREFIX, OslcConstants.RDF_NAMESPACE, qry);
        Queries.prefix(OslcConstants.DCTERMS_NAMESPACE_PREFIX, OslcConstants.DCTERMS_NAMESPACE, qry);
        Queries.prefix(OslcConstants.OSLC_CORE_NAMESPACE_PREFIX, OslcConstants.OSLC_CORE_DOMAIN, qry);
        qry.append("SELECT ?crF WHERE {?srv rdf:type oslc:ServiceProvider ; ");
        qry.append("dcterms:title \"");
        qry.append(store);
        qry.append("\" ; oslc:service [ oslc:creationFactory [ oslc:resourceType <");
        qry.append(type);
        qry.append("> ; oslc:creation ?crF ] ] }");
        type = qry.toString();
        LOG.trace("< getFactoryForTypeQuery[{}]", type);
        return type;
    }
    /**
     * Encapsulates all OSLC single property paths. Single property paths does
     * not require to have or know the Type counterpart of a property.
     */
    public static interface PATHS {
        /**
         * The path prefix for all OSLC model request.
         */
        String PREFIX = OslcConstants.OSLC_CORE_NAMESPACE_PREFIX;
        /**
         * The path part for getting the ServiceProviderCatalog.
         */
        String SERVICE_PROVIDER_CATALOG = "catalog";
        /**
         * The path part for getting a ServiceProvider.
         */
        String SERVICE_PROVIDER = OslcConstants.PATH_SERVICE_PROVIDER;
        /**
         * The path part for denoting artifacts/generic resources.
         */
        String RESOURCE = "resource";
        /**
         * The path part for denoting resources shapes.
         */
        String RESOURCE_SHAPES = "shape";
        /**
         * The path part for denoting allowed values resources.
         */
        String VALUES = "values";
    }
    /**
     * Encapsulates OSLC {@link PredicateDescriptor property descriptors}. A
     * property descriptor is compound for a TYPE and a PATH for each property.
     */
    public static interface PROPS {
        /**
         * Encapsulates all OSLC single property paths. Single property paths does
         * not require to have or know the Type counterpart of a property.
         */
        public static interface PATHS {
            /**
             * The oslc:domain property path.
             */
            Property DOMAIN = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "domain");
            /**
             * The oslc:propertyDefinition property path.
             */
            Property PROPERTY_DEFINITION = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "propertyDefinition");
            /**
             * The oslc:name property path.
             */
            Property NAME = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "name");
            /**
             * The oslc:describes property path.
             */
            Property DESCRIBES = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "describes");
            /**
             * The oslc:creation property path.
             */
            Property CREATION = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "creation");
            /**
             * The oslc:resourceType property path.
             */
            Property RESOURCE_TYPE = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "resourceType");
            /**
             * The oslc:occurs property path.
             */
            Property OCCURS = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "occurs");
            /**
             * The oslc:valueType property path.
             */
            Property VALUE_TYPE = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "valueType");
            /**
             * The oslc:maxSize property path.
             */
            Property MAX_SIZE = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "maxSize");
            /**
             * The oslc:allowedValue property path.
             */
            Property ALLOWED_VALUE = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "allowedValue");
            /**
             * The oslc:queryBase property path.
             */
            Property QUERY_BASE = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "queryBase");
            /**
             * The oslc:totalCount property path.
             */
            Property TOTAL_COUNT = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "totalCount");
            /**
             * The oslc:details property path.
             */
            Property DETAILS = ResourceFactory.createProperty(OslcConstants.OSLC_CORE_DOMAIN, "details");
            /**
             * The rdf:member property path.
             */
            Property MEMBER = ResourceFactory.createProperty(OslcConstants.RDFS_NAMESPACE, "member");
            /**
             * The rdf:member property path.
             */
            Property TYPE = ResourceFactory.createProperty(OslcConstants.RDF_NAMESPACE, "type");

        }
        /**
         * The ServiceProviderCatalog property descriptor.
         */
        PredicateDescriptor SERVICE_PROVIDER_CATALOG = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.PATH_SERVICE_PROVIDER_CATALOG);
        /**
         * The ServiceProvider property descriptor.
         */
        PredicateDescriptor SERVICE_PROVIDER = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.PATH_SERVICE_PROVIDER);
        /**
         * The Service property descriptor.
         */
        PredicateDescriptor SERVICE = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.PATH_SERVICE);
        /**
         * The CreationFactory property descriptor.
         */
        PredicateDescriptor CREATION_FACTORY = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.PATH_CREATION_FACTORY);
        /**
         * The ResourceShape property descriptor.
         */
        PredicateDescriptor RESOURCE_SHAPE = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.PATH_RESOURCE_SHAPE);
        /**
         * The Property property descriptor.
         */
        PredicateDescriptor PROPERTY = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, "property");
        /**
         * The AllowedValues property descriptor.
         */
        PredicateDescriptor ALLOWED_VALUES = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.PATH_ALLOWED_VALUES);
        /**
         * The QueryCapability property descriptor.
         */
        PredicateDescriptor QUERY_CAPABILITY = new PredicateDescriptor(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.PATH_QUERY_CAPABILITY);
    }
    /**
     * Known OSLC values.
     */
    public static interface VALUES {
        /**
         * oslc:occurs values.
         */
        public static interface OCCURS {
            /**
             * The {@link Occurs#ExactlyOne} resource.
             */
            Resource EXACTLY_ONE = ResourceFactory.createResource(Occurs.ExactlyOne.toString());
            /**
             * The {@link Occurs#OneOrMany} resource.
             */
            Resource ONE_OR_MANY = ResourceFactory.createResource(Occurs.OneOrMany.toString());
            /**
             * The {@link Occurs#ZeroOrMany} resource.
             */
            Resource ZERO_OR_MANY = ResourceFactory.createResource(Occurs.ZeroOrMany.toString());
            /**
             * The {@link Occurs#ZeroOrOne} resource.
             */
            Resource ZERO_OR_ONE = ResourceFactory.createResource(Occurs.ZeroOrOne.toString());
        }
        /**
         * oslc:valueType values.
         */
        public static interface VALUE_TYPES {
            /**
             * The oslc:Resource value type.
             */
            Resource RESOURCE = ResourceFactory.createResource(OslcConstants.OSLC_CORE_NAMESPACE + "Resource");
            /**
             * The oslc:LocalResource value type.
             */
            Resource LOCAL_RESOURCE = ResourceFactory.createResource(OslcConstants.OSLC_CORE_NAMESPACE + "LocalResource");
            /**
             * The oslc:AnyResource value type.
             */
            Resource ANY_RESOURCE = ResourceFactory.createResource(OslcConstants.OSLC_CORE_NAMESPACE + "AnyResource");

            /**
             * The oslc:ResponseType type.
             */
            Resource RESPONSE_TYPE = ResourceFactory.createResource(OslcConstants.OSLC_CORE_NAMESPACE + "ResponseInfo");

        }
    }
    /**
     * Executes a {@link QueryCapability} search.
     * @param query the CONSTRUCT query capability.
     * @param baseURL the base URL of the Query Resource representation.
     * @param target the search target dataset.
     * @return an OSLC 2.0 compliant Query Response container.
     */
    public static Model search(String query, String baseURL, Dataset target) {
        Literal total;
        long count = 0;
        LOG.info("\n\n{}\n\n", query);
        Model queryResults = Queries.construct(query, target);
        Resource responseInfo = queryResults.getResource(baseURL);
        StmtIterator iterator = responseInfo.listProperties(RDFS.member);
        while(iterator.hasNext()) {
            iterator.next();
            count++;
        }
        total = ResourceFactory.createTypedLiteral(String.valueOf(count), XSDDatatype.XSDinteger);
        queryResults.add(responseInfo, RDF.type, VALUES.VALUE_TYPES.RESPONSE_TYPE);
        queryResults.add(responseInfo, PROPS.PATHS.TOTAL_COUNT, total);
        return queryResults;
    }
    /**
     * Creates an OSLC {@link ServiceProviderCatalog} template model.
     * @param baseURI the base URI for identification.
     * @param name the catalog identifier, URL compatible.
     * @param title optional, catalog title.
     * @param description optional, the catalog description.
     * @param catalogs optional, other available catalogs.
     * @param domains optional, the catalog domains.
     * @return the model template.
     */
    public static Model getServiceProviderCatalog(String baseURI, String name,
                                                  String title, String description,
                                                  String[] catalogs, String... domains) {
        Resource serviceProviderCatalog;
        String serviceProviderURI;
        Resource serviceProvider;
        Resource service;
        Resource creationFactory;
        Resource queryCapability;

        Map<String, Resource> resourceShapes = new HashMap<>();

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix(OslcConstants.OSLC_CORE_NAMESPACE_PREFIX, OslcConstants.OSLC_CORE_NAMESPACE);
        model.setNsPrefix(OslcConstants.DCTERMS_NAMESPACE_PREFIX, DCTerms.NS);
        model.setNsPrefix(OslcConstants.RDFS_NAMESPACE_PREFIX, RDFS.uri);
        model.setNsPrefix(OslcConstants.RDF_NAMESPACE_PREFIX, RDF.uri);
        model.setNsPrefix("xsd", OslcConstants.XML_NAMESPACE);

        serviceProviderCatalog = model.createResource(Requests.buildURI(baseURI, PATHS.PREFIX, name, PATHS.SERVICE_PROVIDER_CATALOG));
        model.add(serviceProviderCatalog, RDF.type, PROPS.SERVICE_PROVIDER_CATALOG.TYPE);

        if (catalogs != null && catalogs.length > 0) {
            for (String catalog : catalogs) {

                LOG.info("@serviceProvider/{}", catalog);

                serviceProviderURI = Requests.buildURI(baseURI, PATHS.PREFIX, catalog, PATHS.SERVICE_PROVIDER_CATALOG);
                serviceProvider = getServiceResource(model, serviceProviderURI, PROPS.SERVICE_PROVIDER.TYPE, PROPS.PATHS.DETAILS, serviceProviderURI, "Service Provider for " + catalog + " store.", "Service for managing the Graph for " + catalog + " store.");
                service = getServiceResource(model, null, PROPS.SERVICE.TYPE, null, null, null, null);

                serviceProviderURI = Requests.buildURI(baseURI, PATHS.PREFIX, "stores", catalog);
                creationFactory = getServiceResource(model, null, PROPS.CREATION_FACTORY.TYPE, PROPS.PATHS.CREATION, serviceProviderURI,"Creation Factory for " + catalog + " store.", "Service for creating a new Graph for the RDF Store");
                queryCapability = getServiceResource(model, null, PROPS.QUERY_CAPABILITY.TYPE, PROPS.PATHS.QUERY_BASE, serviceProviderURI, "Query Capability for " + catalog + " store.", "Service for listing all the Graphs of the RDF Store");

                model.add(service, PROPS.QUERY_CAPABILITY.PATH, queryCapability);
                model.add(service, PROPS.CREATION_FACTORY.PATH, creationFactory);
                model.add(serviceProvider, PROPS.SERVICE.PATH, service);
                model.add(serviceProviderCatalog, PROPS.SERVICE_PROVIDER.PATH, serviceProvider);

                Dataset dataset = Warehouses.get(catalog);
                addShapeResource(dataset, serviceProviderCatalog, model, baseURI, catalog, resourceShapes);
            }
        }

        serviceProviderURI = Requests.buildURI(baseURI, PATHS.PREFIX, name, "rdfstores");
        serviceProvider = getServiceResource(model, serviceProviderURI, PROPS.SERVICE_PROVIDER.TYPE, PROPS.PATHS.DETAILS, serviceProviderURI, "RDFStore Service Provider", "Service for managing the RDF Store");
        service = getServiceResource(model, null, PROPS.SERVICE.TYPE,null, null,null, null);
        creationFactory = getServiceResource(model, null, PROPS.CREATION_FACTORY.TYPE, PROPS.PATHS.CREATION, Requests.buildURI(baseURI, OSLCModel.PATHS.PREFIX, "rdfstores"), "RDFStore Creation Factory","Service for creating a new RDF Store");
        queryCapability = getServiceResource(model, null, PROPS.QUERY_CAPABILITY.TYPE, PROPS.PATHS.QUERY_BASE, Requests.buildURI(baseURI, OSLCModel.PATHS.PREFIX, "rdfstores"),"RDFStore Query Capability","Service for listing all the RDF Stores");

        model.add(service, PROPS.QUERY_CAPABILITY.PATH, queryCapability);
        model.add(service, PROPS.CREATION_FACTORY.PATH, creationFactory);
        model.add(serviceProvider, PROPS.SERVICE.PATH, service);
        model.add(serviceProviderCatalog, PROPS.SERVICE_PROVIDER.PATH, serviceProvider);

        if (domains != null && domains.length > 0) {
            for (String domain : domains) {
                model.add(serviceProviderCatalog, PROPS.PATHS.DOMAIN, ResourceFactory.createResource(domain));
            }
        }

        if (description != null && !description.isEmpty()) {
            model.add(serviceProviderCatalog, DCTerms.description, description);
        }

        if (title != null && !title.isEmpty()) {
            model.add(serviceProviderCatalog, DCTerms.title, title);
        }

        return model;
    }

    private static Resource getServiceResource(Model model, String resourceURI, Resource type, Property property, String uri, String title, String description) {
        Resource serviceResource = model.createResource();

        if (resourceURI != null && !resourceURI.isEmpty()) {
            serviceResource = model.createResource(resourceURI);
        }

        model.add(serviceResource, RDF.type, type);

        if (property != null && property.isProperty()) {
            model.add(serviceResource, property, Resources.buildResource(uri));
        }

        if (description != null && !description.isEmpty()) {
            model.add(serviceResource, DCTerms.description, description);
        }

        if (title != null && !title.isEmpty()) {
            model.add(serviceResource, DCTerms.title, title);
        }

        return serviceResource;
    }

    /**
     * A simple resource type name (alias) to the corresponding
     * {@link ResourceShape} mapping.
     */
    private final Map<String, Resource> resourceShapes;
    /**
     * The base URI to build relative ones.
     */
    private final String baseURI;
    /**
     * The working model definition.
     */
    private final Model model;
    /**
     * Constructs an instance specifying the open-world-assumption flag, the
     * base URL and the initial model definition.
     * @param baseURI the base URI to build the relatives ones.
     * @param model the OSLC model definition.
     * @param resourceShapes all the resource shapes contained.
     */
    public OSLCModel(String baseURI, Model model, Map<String, Resource> resourceShapes) {
        this.resourceShapes = resourceShapes;
        this.model = model;
        this.baseURI = baseURI;
    }
    /**
     * Returns the base URI used by this model.
     * @return the base URI used by this model.
     */
    public String getBaseURI() {
        return baseURI;
    }
    /**
     * Gets the registered {@link ResourceShape}s mapping. {@code Key} is the
     * simple resource type name (alias) and {@code Value} is the
     * {@code ResourceShape} resource.
     * @return the registered {@code ResourceShape}s mapping.
     */
    public Map<String, Resource> getResourceShapes() {
        return Collections.unmodifiableMap(resourceShapes);
    }
    /**
     * Finds the {@link CreationFactory} resource for a given rdf:type and store.
     * @param type the rdf:type URI.
     * @param store the title of {@code CreationFactory} {@code ServiceProvider}.
     * @return the corresponding {@code CreationFactory} resource if found;
     * {@code null} otherwise.
     */
    public Resource getCreationFactory(String type, String store) {
        Query query;
        ResultSet results;
        QuerySolution solution;
        Model cachedWorkingModel;
        Resource creationFactory = null;
        cachedWorkingModel = model;
        query = QueryFactory.create(getCreationFactoryQuery(type, store));
        try(QueryExecution qe = QueryExecutionFactory.create(query, cachedWorkingModel)) {
            results = qe.execSelect();
            if (results.hasNext()) {
                solution = results.next();
                creationFactory = solution.getResource("crF");
                if (results.hasNext()) {
                    //"only one sh:NodeShape per sh:targetClass is allowed" validation prevents this scenario, just in case...
                    LOG.warn("More than one CreationFactory was found for <{}> in the same ServiceProvider '{}'", type, store);
                }
            }
        }
        return creationFactory;
    }
    /**
     * Expands a resource model to contain all referenced resources up to a
     * given expansion level.
     * @param expansionLevel the maximum level for expansion.
     * @param parts path parts to find the resource.
     * @return a filtered model containing the expansion of the resource if
     * it exists; an empty model otherwise.
    */
    public Model expandResource(int expansionLevel, String... parts) {
        String query;
        Model resourceModel, cachedWorkingModel;
        cachedWorkingModel = model;
        query = getExpansionQuery(Requests.getURI(baseURI, parts), expansionLevel);
        resourceModel = Queries.construct(query, cachedWorkingModel);
        resourceModel.setNsPrefixes(cachedWorkingModel.getNsPrefixMap());
        return resourceModel;
    }
    /**
     * Executes a DESCRIBE query over a resource URL. This command will expand
     * all (and only the) blank nodes contained from the root element.
     * @param parts path parts to find the resource.
     * @return a filtered model containing the description of the resource if
     * it exists; an empty model otherwise.
    */
    public Model describeResource(String... parts) {
        Model resourceModel, cachedWorkingModel;
        cachedWorkingModel = model;
        resourceModel = Queries.describe(Requests.getURI(baseURI, parts), cachedWorkingModel);
        resourceModel.setNsPrefixes(cachedWorkingModel.getNsPrefixMap());
        return resourceModel;
    }

}