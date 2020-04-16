package com.ld4mbse.oslc4tdb.util;

import com.ld4mbse.oslc4tdb.model.OSLCModel;
import com.ld4mbse.oslc4tdb.model.SHACLModel;
import java.net.URI;
import java.util.*;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog;
import org.eclipse.lyo.oslc4j.core.model.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to create an OSLC discovery model given a SHACL model. This
 * class takes a {@link Dataset} as input to create the entire OSLC discovery
 * model; creates a {@link ServiceProvider} per named graph and the
 * {@link ServiceProviderCatalog}, {@link ResourceShape}'s and other OSLC
 * elements are created from a SHACL model which is expected to be in the
 * default model.
 * @author rherrera
 */
public class OslcShaclAdapter {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(OslcShaclAdapter.class);
    /**
     * Creates the CONSTRUCT query to create the oslc:ServiceProviderCatalog
     * wrapper for a warehouse OSLC adapter. This query will attempt to extract
     * the following oslc:ServiceProviderCatalog data from the target model:
     * dcterms:title, dcterms:description and oslc:domain. In order to perform
     * the extraction, the target model must have a resource identified by
     * <urn:ServiceProviderCatalog> with these properties.
     * @param catalogURI the oslc:ServiceProviderCatalog URI.
     * @param catalogTitle the oslc:ServiceProviderCatalog default title.
     * @return the corresponding query.
     */
    public static String getCatalogConstructQuery(String catalogURI,
                                                  String catalogTitle) {
        StringBuilder sb = new StringBuilder();
        Queries.prefix(OslcConstants.OSLC_CORE_NAMESPACE_PREFIX, OslcConstants.OSLC_CORE_DOMAIN, sb);
        Queries.prefix(OslcConstants.DCTERMS_NAMESPACE_PREFIX, OslcConstants.DCTERMS_NAMESPACE, sb);
        Queries.prefix(OslcConstants.RDF_NAMESPACE_PREFIX, OslcConstants.RDF_NAMESPACE, sb);
        sb.append("CONSTRUCT {<");
        sb.append(catalogURI);
        sb.append("> rdf:type oslc:ServiceProviderCatalog; dcterms:title ");
        sb.append("?ultimateTitle; dcterms:description ?description; ");
        sb.append("oslc:domain ?domain } WHERE { ");
        sb.append("OPTIONAL {<urn:ServiceProviderCatalog> dcterms:title ?title} ");
        sb.append("OPTIONAL {<urn:ServiceProviderCatalog> dcterms:description ?description} ");
        sb.append("OPTIONAL {<urn:ServiceProviderCatalog> oslc:domain ?domain} ");
        sb.append("BIND (COALESCE(?title, \"");
        sb.append(catalogTitle);
        sb.append("\") AS ?ultimateTitle)}");
        return sb.toString();
    }
    /**
     * Gets the simple id of a resource.
     * @param uri the resource URL.
     * @return the last path part after the last slash without extension if any.
     */
    public static String getSimpleId(String uri) {
        int index = uri.lastIndexOf("#");
        if (index < 0) index = uri.lastIndexOf("/");
        if (index < 0) index = uri.lastIndexOf(":");
        uri = uri.substring(index + 1);
        index = uri.indexOf(">");
        if (index > 0) uri = uri.substring(0, index);
        index = uri.indexOf(".");
        if (index > 0) uri = uri.substring(0, index);
        return uri;
    }
    /**
     * Maps the oslc:occur property having zero as sh:minCount.
     * @param srcMetaProperty the SHACL meta-property.
     * @param dstMetaProperty the OSLC meta-property.
     * @param pathProperty the sh:path property value.
     */
    private static void mapZeroOccurProperty(Resource srcMetaProperty,
            Resource dstMetaProperty, Resource pathProperty) {
        int maxCount;
        Model target = dstMetaProperty.getModel();
        Statement statement = srcMetaProperty.getProperty(SHACLModel.PATHS.MAX_COUNT);
        if (statement == null)
            target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.OCCURS, OSLCModel.VALUES.OCCURS.ZERO_OR_MANY);
        else {
            maxCount = statement.getObject().asLiteral().getInt();
            if (maxCount == 1)
                target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.OCCURS, OSLCModel.VALUES.OCCURS.ZERO_OR_ONE);
            else
                LOG.warn("[-] oslc:occurs [{}] No matching value for sh:maxCount = {}", pathProperty, maxCount);
        }
    }
    /**
     * Builds a {@link ResourceShape} for a sh:NodeShape definition.
     * @param id the {@link ResourceShape} single identifier.
     * @param describes the URL of the resource type describing.
     * @param shNodeShape the source sh:NodeShape resource.
     * @param target the target output model.
     * @param baseURI the base URI to create OSLC elements.
     * @param warehouse the warehouse name.
     * @return the resulting {@code ResourceShape} resource.
     */
    public static Resource getResourceShape(String id, String describes,
                                             Resource shNodeShape,
                                             Model target,
                                             String baseURI,
                                             String warehouse) {
        Statement statement;
        int minCount, maxCount;
        String property, propertyName;
        Iterator<RDFNode> allowedValues;
        Resource pathProperty, resource;
        Resource srcMetaProperty, dstMetaProperty;
        LOG.info("@shape/{} [{}]", id, describes);
        ValueType literalValueType = null;
        StmtIterator shapeProperties = shNodeShape.listProperties(SHACLModel.PATHS.PROPERTY);
        Resource resourceShape = target.createResource(Requests.buildURI(baseURI, OSLCModel.PATHS.PREFIX, warehouse, OSLCModel.PATHS.RESOURCE_SHAPES, id));
        target.add(resourceShape, OSLCModel.PROPS.PATHS.DESCRIBES, target.createResource(describes));
        target.add(resourceShape, DCTerms.title, id + " resource shape");
        target.add(resourceShape, RDF.type, OSLCModel.PROPS.RESOURCE_SHAPE.TYPE);
        while(shapeProperties.hasNext()) {
            srcMetaProperty = shapeProperties.next().getObject().asResource();
            LOG.info("@srcMetaProperty/{}", srcMetaProperty);
            pathProperty = srcMetaProperty.getPropertyResourceValue(SHACLModel.PATHS.PATH);
            LOG.info("@pathProperty/{}", pathProperty);
            if (pathProperty == null)
                throw new IllegalStateException("Missing sh.path property on sh:property: " + srcMetaProperty);
            dstMetaProperty = target.createResource();
            //oslc:propertyDefinition
            target.add(resourceShape, OSLCModel.PROPS.PROPERTY.PATH, dstMetaProperty);
            target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.PROPERTY_DEFINITION, pathProperty);
            target.add(dstMetaProperty, RDF.type, OSLCModel.PROPS.PROPERTY.TYPE);
            //oslc:name
            statement = srcMetaProperty.getProperty(SHACLModel.PATHS.NAME);
            if (statement == null)
                propertyName = getSimpleId(pathProperty.getURI());
            else
                propertyName = statement.getObject().asLiteral().getString();
            target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.NAME, propertyName);
            //oslc:description
            statement = srcMetaProperty.getProperty(SHACLModel.PATHS.DESCRIPTION);
            if (statement != null) {
                property = statement.getObject().asLiteral().getString();
                target.add(dstMetaProperty, DCTerms.description, property);
            }
            //oslc:occurs
            statement = srcMetaProperty.getProperty(SHACLModel.PATHS.MIN_COUNT);
            if (statement == null)
                mapZeroOccurProperty(srcMetaProperty, dstMetaProperty, pathProperty);
            else {
                minCount = statement.getObject().asLiteral().getInt();
                switch (minCount) {
                    case 0:
                        mapZeroOccurProperty(srcMetaProperty, dstMetaProperty, pathProperty);
                        break;
                    case 1:
                        statement = srcMetaProperty.getProperty(SHACLModel.PATHS.MAX_COUNT);
                        if (statement == null)
                            target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.OCCURS, OSLCModel.VALUES.OCCURS.ONE_OR_MANY);
                        else {
                            maxCount = statement.getObject().asLiteral().getInt();
                            if (maxCount == 1)
                                target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.OCCURS, OSLCModel.VALUES.OCCURS.EXACTLY_ONE);
                            else
                                LOG.warn("[-] oslc:occurs [{}] No matching value for sh:maxCount = {}", pathProperty, maxCount);
                        }   break;
                    default:
                        LOG.warn("[-] oslc:occurs [{}] No matching value for sh:minCount = {}", pathProperty, minCount);
                        break;
                }
            }
            //oslc:valueType
            statement = srcMetaProperty.getProperty(SHACLModel.PATHS.NODE_KIND);
            if (statement == null) {
                statement = srcMetaProperty.getProperty(SHACLModel.PATHS.DATA_TYPE);
                if (statement == null)
                    LOG.warn("[-] oslc:valueType [{}] No sh:nodeKind nor sh:datatype restrictions found", pathProperty);
                else
                    throw new IllegalStateException("sh:nodeKind property must be present and equal to sh:Literal on <" + pathProperty + "> when property sh:datatype is used");
                    // LOG.warn("[-] oslc:valueType [{}] sh:nodeKind property must be present and equal to sh:Literal on <{}> when property sh:datatype is used", pathProperty, pathProperty);
            } else {
                resource = statement.getObject().asResource();
                statement = srcMetaProperty.getProperty(SHACLModel.PATHS.DATA_TYPE);
                if (statement == null) {
                    if (SHACLModel.TYPES.NODE_KIND.LITERAL.equals(resource))
                        throw new IllegalStateException("sh:datatype property must be present on <" + pathProperty + "> when property sh:nodeKind is used and equal to sh:Literal");
                    else if (SHACLModel.TYPES.NODE_KIND.BLANK_NODE.equals(resource))
                        target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.VALUE_TYPE, OSLCModel.VALUES.VALUE_TYPES.LOCAL_RESOURCE);
                    else if (SHACLModel.TYPES.NODE_KIND.IRI.equals(resource))
                        target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.VALUE_TYPE, OSLCModel.VALUES.VALUE_TYPES.RESOURCE);
                    else if (SHACLModel.TYPES.NODE_KIND.BLANK_NODE_OR_IRI.equals(resource))
                        target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.VALUE_TYPE, OSLCModel.VALUES.VALUE_TYPES.ANY_RESOURCE);
                    else
                        throw new IllegalStateException("sh:nodeKind value <" + resource + "> on <" + pathProperty + "> is not supported, it is not compatible with any value of oslc:valueType");
                } else if (!SHACLModel.TYPES.NODE_KIND.LITERAL.equals(resource))
                    throw new IllegalStateException("sh:nodeKind property must be equal to sh:Literal on <" + pathProperty + "> when property sh:datatype is used");
                else {
                    property = statement.getObject().asResource().getURI();
                    literalValueType = ValueType.fromURI(URI.create(property));
                    if (literalValueType == null)
                        throw new IllegalStateException("sh:datatype value <" + property + "> on <" + pathProperty + "> is not supported, it is not compatible with any value of oslc:valueType");
                    target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.VALUE_TYPE, ResourceFactory.createResource(property));
                }
            }
            //oslc:range
            statement = srcMetaProperty.getProperty(SHACLModel.PATHS.CLASS);
            if (statement != null) {
                /*
                NOTE: sh:class seems to have a bug on https://github.com/labra/shaclex :
                 - when node value is IRI validations never succeed, on the other hand,
                 - if they are blank nodes, they always occur.
                */
                LOG.warn("[-] oslc:range [{}] sh:class is currently not supported by dependency lib", pathProperty);
            }
            //oslc:maxSize
            statement = srcMetaProperty.getProperty(SHACLModel.PATHS.MAX_LENGTH);
            if (statement != null) {
                if (ValueType.String == literalValueType)
                    target.add(dstMetaProperty, OSLCModel.PROPS.PATHS.MAX_SIZE, statement.getObject());
                else
                    throw new IllegalStateException("sh:maxLenght is not allowed on <" + pathProperty + ">, only allowed in combination with sh:datatype = xsd:string");
            }
            //oslc:allowedValues
            statement = srcMetaProperty.getProperty(SHACLModel.PATHS.IN);
            if (statement != null) {
                resource = target.createResource(Requests.buildURI(baseURI, OSLCModel.PATHS.PREFIX, warehouse, OSLCModel.PATHS.RESOURCE_SHAPES, id, OSLCModel.PATHS.VALUES, propertyName));
                target.add(dstMetaProperty, OSLCModel.PROPS.ALLOWED_VALUES.PATH, resource);
                target.add(resource, RDF.type, OSLCModel.PROPS.ALLOWED_VALUES.TYPE);
                allowedValues = statement.getObject().as(RDFList.class).asJavaList().iterator();
                while(allowedValues.hasNext()) {
                    target.add(resource, OSLCModel.PROPS.PATHS.ALLOWED_VALUE, allowedValues.next());
                }
            }
        }
        return resourceShape;
    }

    public static OSLCModel getOSLCModel(String baseURI,
                                         String name,
                                         Dataset dataset) {
        Resource provider, service, factory;

        Map<String, Resource> resourceShapes = new HashMap<>();

        Resource catalog = Resources.buildResource(baseURI, OSLCModel.PATHS.PREFIX, name, OSLCModel.PATHS.SERVICE_PROVIDER_CATALOG);
        Model workingModel = Queries.construct(getCatalogConstructQuery(catalog.getURI(), name), dataset);

        addShapeResource(dataset, catalog, workingModel, baseURI, name, resourceShapes);

        provider = workingModel.createResource(Requests.buildURI(baseURI, OSLCModel.PATHS.PREFIX, name, OSLCModel.PATHS.SERVICE_PROVIDER_CATALOG));
        workingModel.add(provider, RDF.type, OSLCModel.PROPS.SERVICE_PROVIDER.TYPE);
        workingModel.add(provider, DCTerms.title, name);
        workingModel.add(provider, OSLCModel.PROPS.PATHS.DETAILS, Resources.buildResource(baseURI, OSLCModel.PATHS.PREFIX, name, OSLCModel.PATHS.SERVICE_PROVIDER, name, "about"));
        workingModel.add(catalog, OSLCModel.PROPS.SERVICE_PROVIDER.PATH, provider);

        service = workingModel.createResource();
        workingModel.add(service, RDF.type, OSLCModel.PROPS.SERVICE.TYPE);

        factory = workingModel.createResource();
        workingModel.add(factory, RDF.type, OSLCModel.PROPS.CREATION_FACTORY.TYPE);
        workingModel.add(factory, DCTerms.title, "Creation Factory for " + name + " graphs.");
        workingModel.add(factory, OSLCModel.PROPS.PATHS.CREATION, Resources.buildResource(baseURI, OSLCModel.PATHS.PREFIX, "stores", name));
        workingModel.add(service, OSLCModel.PROPS.CREATION_FACTORY.PATH, factory);
        workingModel.add(provider, OSLCModel.PROPS.SERVICE.PATH, service);

        return new OSLCModel(baseURI, workingModel, resourceShapes);
    }

    public static void addShapeResource(Dataset dataset,
                                        Resource catalog,
                                        Model model,
                                        String baseURI,
                                        String name,
                                        Map<String, Resource> resourceShapes) {

        Resource provider, service, factory, queryCapability;
        Resource shaclShape, shaclTarget, resourceShape;
        ResIterator shaclShapes;
        String simpleId;
        Statement statement;

        Iterator<String> shaclGraphs = dataset.listNames();
        while (shaclGraphs.hasNext()) {
            String shaclGraph = shaclGraphs.next();

            Model shacl = dataset.getNamedModel(shaclGraph);

            if (shaclGraph.contains("-shacl")) {
                shaclGraph = getSimpleId(shaclGraph.replace("-shacl", ""));

                Set<String> uniqueTypes = new HashSet<>();
                Set<String> uniqueShapes = new HashSet<>();

                uniqueTypes.clear();
                uniqueShapes.clear();

                LOG.info("@serviceProvider/{}", shaclGraph);
                provider = model.createResource(Requests.buildURI(baseURI, OSLCModel.PATHS.PREFIX, name, OSLCModel.PATHS.SERVICE_PROVIDER, shaclGraph));
                model.add(provider, RDF.type, OSLCModel.PROPS.SERVICE_PROVIDER.TYPE);
                model.add(provider, DCTerms.title, shaclGraph);
                model.add(provider, OSLCModel.PROPS.PATHS.DETAILS, Resources.buildResource(baseURI, OSLCModel.PATHS.PREFIX, name, OSLCModel.PATHS.SERVICE_PROVIDER, shaclGraph, "about"));
                model.add(catalog, OSLCModel.PROPS.SERVICE_PROVIDER.PATH, provider);

                shaclShapes = shacl.listResourcesWithProperty(RDF.type, SHACLModel.TYPES.NODE_SHAPE);

                while(shaclShapes.hasNext()) {
                    service = model.createResource();
                    model.add(service, RDF.type, OSLCModel.PROPS.SERVICE.TYPE);

                    shaclShape = shaclShapes.next();
                    LOG.info("@shaclShape/{}", shaclShape);

                    statement = shaclShape.getProperty(SHACLModel.PATHS.TARGET_CLASS);
                    if (statement == null)
                        shaclTarget = shaclShape;
                    else
                        shaclTarget = statement.getObject().asResource();

                    statement = shaclShape.getProperty(RDFS.label);
                    if (statement == null)
                        simpleId = getSimpleId(shaclTarget.getURI());
                    else
                        simpleId = statement.getObject().asLiteral().getString();

                    if (!uniqueShapes.add(simpleId))
                        throw new IllegalStateException("Simple shape name '" + simpleId + "' is repeated on SHACL models (" + shaclShape.getURI() + "), use rdfs:label to create an alias.");

                    if (!uniqueTypes.add(shaclTarget.getURI()))
                        throw new IllegalStateException("sh:targetClass '" + shaclTarget.getURI() + "' already defined, only one sh:NodeShape per sh:targetClass is allowed.");

                    factory = model.createResource();
                    LOG.info("@@creationFactory/{}", simpleId);

                    model.add(factory, RDF.type, OSLCModel.PROPS.CREATION_FACTORY.TYPE);
                    model.add(factory, DCTerms.title, "Creation Factory for " + simpleId + " Resources.");
                    model.add(factory, OSLCModel.PROPS.PATHS.CREATION, Resources.buildResource(baseURI, OSLCModel.PATHS.PREFIX, name, shaclGraph, simpleId));
                    model.add(factory, OSLCModel.PROPS.PATHS.RESOURCE_TYPE, model.createResource(shaclTarget.getURI()));

                    resourceShape = resourceShapes.get(simpleId);
                    if (resourceShape == null) {
                        resourceShape = getResourceShape(simpleId, shaclTarget.getURI(), shaclShape, model, baseURI, name);
                        resourceShapes.put(simpleId, resourceShape);
                    }

                    model.add(factory, OSLCModel.PROPS.RESOURCE_SHAPE.PATH, resourceShape);
                    model.add(service, OSLCModel.PROPS.CREATION_FACTORY.PATH, factory);

                    queryCapability = model.createResource();
                    LOG.info("@@queryCapability/{}", simpleId);

                    model.add(queryCapability, RDF.type, OSLCModel.PROPS.QUERY_CAPABILITY.TYPE);
                    model.add(queryCapability, DCTerms.title, "Query Capability for " + simpleId + " Resources.");
                    model.add(queryCapability, OSLCModel.PROPS.PATHS.QUERY_BASE, Resources.buildResource(baseURI, OSLCModel.PATHS.PREFIX, name, shaclGraph, simpleId));
                    model.add(queryCapability, OSLCModel.PROPS.PATHS.RESOURCE_TYPE, model.createResource(shaclTarget.getURI()));

                    model.add(service, OSLCModel.PROPS.QUERY_CAPABILITY.PATH, queryCapability);
                    model.add(provider, OSLCModel.PROPS.SERVICE.PATH, service);

                }

                service = model.createResource();
                model.add(service, RDF.type, OSLCModel.PROPS.SERVICE.TYPE);

                factory = model.createResource();
                LOG.info("@@creationFactory/{}", "bulk loader");

                model.add(factory, RDF.type, OSLCModel.PROPS.CREATION_FACTORY.TYPE);
                model.add(factory, DCTerms.title, "Bulk Loader Creation Factory");
                model.add(factory, OSLCModel.PROPS.PATHS.CREATION, Resources.buildResource(baseURI, OSLCModel.PATHS.PREFIX, name, shaclGraph, "stores"));
                model.add(service, OSLCModel.PROPS.CREATION_FACTORY.PATH, factory);
                model.add(provider, OSLCModel.PROPS.SERVICE.PATH, service);
            }
        }
    }

}