package com.ld4mbse.oslc4tdb.rest.oslc;

import com.ld4mbse.oslc4tdb.rest.RDFResource;
import com.ld4mbse.oslc4tdb.model.OSLCManager;
import com.ld4mbse.oslc4tdb.model.OSLCModel;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.tdb.query.QueryCriteria;
import com.ld4mbse.oslc4tdb.tdb.validation.FetchingRulesException;
import com.ld4mbse.oslc4tdb.tdb.validation.ValidationException;
import com.ld4mbse.oslc4tdb.util.Models;
import com.ld4mbse.oslc4tdb.util.Resources;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.*;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.*;

/**
 * Provides OSLC discovery capabilities.
 * @author rherrera
 */
@Path("/")
public class GenericResource extends RDFResource {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenericResource.class);

    @Inject
    protected OSLCManager oslcManager;

    @Inject
    protected RDFManager rdfManager;

    @GET
    @Path("{warehouse:(.*/)?}" + OSLCModel.PATHS.SERVICE_PROVIDER_CATALOG)
    public Response getServiceProviderCatalog(@PathParam("warehouse") String warehouse) {
        try {
            boolean masterCatalog = warehouse == null || warehouse.isEmpty();
            return dispatchResource(
                    warehouse,
                    oslcManager.getServiceProviderCatalog(warehouse),
                    masterCatalog ? "Master Catalog" : "Catalog",
                    true
            );
        } catch(RuntimeException e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("{warehouse}/" + OSLCModel.PATHS.SERVICE_PROVIDER + "/{provider}")
    public Response getServiceProvider(@PathParam("warehouse") String warehouse,
                                       @PathParam("provider") String provider) {
        try {
            return dispatchResource(provider, oslcManager.getServiceProvider(warehouse, provider), "Service Provider",true);
        } catch(RuntimeException e) {
            LOG.error(e.getMessage());
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("{warehouse}/" +OSLCModel.PATHS.RESOURCE_SHAPES + "/{shape}")
    public Response getResourceShape(@PathParam("warehouse") String warehouse, @PathParam("shape") String shape) {
        try {
            return dispatchResource(shape, oslcManager.getResourceShape(warehouse, shape), "",true);
        } catch(RuntimeException e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("{warehouse}/" + OSLCModel.PATHS.RESOURCE_SHAPES + "/{shape}/" + OSLCModel.PATHS.VALUES + "/{property}")
    public Response getAllowedValues(@PathParam("warehouse") String warehouse,
                                     @PathParam("shape") String shape,
                                     @PathParam("property") String property) {
        try {
            return dispatchResource(property, oslcManager.getAllowedValues(warehouse, property, shape), "",true);
        } catch(RuntimeException e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("{warehouse}/{store}/stores")
    public Response getStore(@PathParam("warehouse") String warehouse,
                             @PathParam("store") String store,
                             @QueryParam("where") String where,
                             @QueryParam("select") String select,
                             @HeaderParam(ACCEPT) String accept) {
        try {
            Model model;
            if (where == null && select == null)
                model = rdfManager.getModel(warehouse, Models.getStoreURN(store));
            else
                model = rdfManager.getModel(warehouse, Models.getStoreURN(store), where, select);
            return dispatchResource(store, model, "",false);
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    @GET
    @Path("{warehouse}/{store}/{type}")
    public Response searchResources(@PathParam("warehouse") String warehouse,
                                    @PathParam("store") String store,
                                    @PathParam("type") String type,
                                    @QueryParam("oslc.prefix") String prefixes,
                                    @QueryParam("oslc.where") String where,
                                    @QueryParam("oslc.select") String select,
                                    @QueryParam("oslc.orderBy") String orderBy) {
        Lang lang;
        Model resource;
        Resource oslcType;
        OutputStream output;
        QueryCriteria criteria;
        String queryString = request.getQueryString();
        String queryBase = request.getRequestURL().toString();
        LOG.info("GET {}[{}] @ store[{}]", type, queryString, store);

        try {
            oslcType = oslcManager.getQualifiedResourceType(warehouse, type);
            if (oslcType == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(TEXT_PLAIN)
                        .entity("The resource " + type + " does not exists in the store.")
                        .build();
            }
            criteria = QueryCriteria.type(oslcType).prefixes(prefixes);
            criteria.where(where).select(select).orderBy(orderBy);
            resource = rdfManager.search(warehouse, criteria, store, queryBase);
            if ((lang = getAcceptableLanguage()) == null) {
                return Response.status(Response.Status.NOT_ACCEPTABLE)
                        .type(TEXT_PLAIN)
                        .entity("Content not acceptable")
                        .build();
            }

            response.setContentType(lang.getContentType().toHeaderString());
            response.setStatus(OK.getStatusCode());
            output = response.getOutputStream();
            RDFDataMgr.write(output, resource, lang);
            output.flush();
            return Response.ok().build();
        } catch(IllegalArgumentException e) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        } catch(RuntimeException | IOException e) {
            LOG.error("Could not search resources at " + queryBase, e);
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("{warehouse}/{store}/{type}/{id}")
    public Response findResource(@PathParam("warehouse") String warehouse,
                                 @PathParam("store") String store,
                                 @PathParam("type") String type,
                                 @PathParam("id") String id) {
        Lang lang;
        Resource oslcType;
        Model resource;
        OutputStream output;
        StringBuffer query = request.getRequestURL();

        LOG.info("GET {} @ store[{}]", query, store);
        try {

            oslcType = oslcManager.getQualifiedResourceType(warehouse, type);
            if (oslcType == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(TEXT_PLAIN)
                        .entity("The resource " + type + " does not exists in the store.")
                        .build();
            }

            String queryString = request.getQueryString();
            if (queryString != null) {
                query.append('?');
                query.append(queryString);
            }

            resource = rdfManager.getResource(warehouse, query.toString(), store);

            if (resource == null || resource.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(TEXT_PLAIN)
                        .entity("Resource not found.")
                        .build();
            } else if ((lang = getAcceptableLanguage()) == null) {
                return Response.status(Response.Status.NOT_ACCEPTABLE)
                        .type(TEXT_PLAIN)
                        .entity("Content not acceptable")
                        .build();
            } else {
                response.setContentType(lang.getContentType().toHeaderString());
                response.setStatus(OK.getStatusCode());
                response.addHeader(ETAG, Resources.getETag(resource.toString()));
                output = response.getOutputStream();
                RDFDataMgr.write(output, resource, lang);
                output.flush();
            }
        } catch(RuntimeException | IOException e) {
            LOG.error("Could not get resource at " + query, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
        return Response.ok().build();
    }

    // Add Graph

    @POST
    @Path("{warehouse}/{store}/{type}")
    public Response insertResource(@PathParam("warehouse") String warehouse,
                                   @PathParam("store") String store,
                                   @PathParam("type") String type) {
        Model model;
        Resource resource;
        Lang inputLanguage;
        String factory, slug;
        String contentType = request.getContentType();

        String finalURL = request.getRequestURL().toString();
        if (contentType == null || contentType.isEmpty())
            contentType = WebContent.contentTypeRDFXML;
        LOG.info("PUT {} @ store[{}] as {}", finalURL, store, contentType);
        try {
            if (request.getContentLength() <= 0) {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Missing request body")
                        .build();
            }
            inputLanguage = RDFLanguages.contentTypeToLang(contentType);
            if (inputLanguage == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Incompatible ContentType: " + contentType)
                        .build();
            }
            slug = request.getHeader("Slug");
            finalURL += "/" + (slug == null || slug.isEmpty() ? String.valueOf(System.currentTimeMillis()) : slug);
            model = rdfManager.getResource(warehouse, finalURL, store);
            if (!model.isEmpty()) {
                return Response.status(Response.Status.CONFLICT)
                        .type(TEXT_PLAIN)
                        .entity("Another resource exists at " + finalURL)
                        .build();
            }
            model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model.getGraph(), request.getInputStream(), finalURL, inputLanguage);
            resource = model.getResource(finalURL);
            if (!pathTypeMatchesResource(warehouse, type, resource)) {
                factory = getCreationFactory(warehouse, resource, store);
                if (factory == null) {
                    return Response.status(BAD_REQUEST)
                            .type(TEXT_PLAIN)
                            .entity("Unknow resource type.")
                            .build();
                } else {
                    return Response.status(BAD_REQUEST)
                            .type(TEXT_PLAIN)
                            .entity("Wrong CreationFactory for resource.")
                            .header(LOCATION, factory)
                            .build();
                }
            }
            if (resource.listProperties().hasNext()) {
                rdfManager.setResource(warehouse, resource, store);
                response.addHeader(LOCATION, finalURL);
                response.setStatus(CREATED.getStatusCode());
                response.flushBuffer();
                LOG.info("{} created", finalURL);

                return Response.ok().build();

            } else if (slug == null) {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Request body does not include a resource with empty URL (e.g. rdf:about=\"\"). The empty URL denotes the resource to be created.")
                        .build();

            } else {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Request body does not include a resource which URL includes the Slug header value: " + finalURL)
                        .build();
            }
        } catch(RiotException e) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Invalid syntax: " + e.getMessage())
                    .build();
        } catch(FetchingRulesException e) {
            return Response.status(SERVICE_UNAVAILABLE)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        } catch(ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(e.getMessageContentType())
                    .entity(e.getMessage())
                    .build();
        } catch(RuntimeException | IOException e) {
            LOG.error("Could not create resource at " + finalURL, e);
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity("An Internal error has ocurred: " + e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("{warehouse}/{store}/{type}/{id}")
    public Response updateResource(@PathParam("warehouse") String warehouse,
                                   @PathParam("store") String store,
                                   @PathParam("type") String type) {
        Model model;
        Lang inputLanguage;
        org.apache.jena.rdf.model.Resource resource;
        String ifmatch, contentType = request.getContentType();
        String finalURL = request.getRequestURL().toString();
        // String store = warehouse;
        if (contentType == null || contentType.isEmpty())
            contentType = WebContent.contentTypeRDFXML;
        LOG.info("POST {} @ warehouse[{}] as {}", finalURL, store, contentType);
        try {
            if (request.getContentLength() <= 0) {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Missing request body")
                        .build();
            }
            inputLanguage = RDFLanguages.contentTypeToLang(contentType);
            if (inputLanguage == null) {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Incompatible ContentType: " + contentType)
                        .build();
            }
            model = rdfManager.getResource(warehouse, finalURL, store);
            if (model.isEmpty()) {
                return Response.status(NOT_FOUND)
                        .type(TEXT_PLAIN)
                        .entity("Resource not found.")
                        .build();
            }
            ifmatch = request.getHeader(IF_MATCH);
            if (ifmatch == null || ifmatch.isEmpty()) {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Missing header " + IF_MATCH)
                        .build();
            }
            if (!ifmatch.equals(Resources.getETag(model.toString()))) {
                return Response.status(PRECONDITION_FAILED)
                        .type(TEXT_PLAIN)
                        .entity("Precondition failed: " + ifmatch)
                        .build();
            }
            model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model.getGraph(), request.getInputStream(), finalURL, inputLanguage);
            resource = model.getResource(finalURL);
            if (!pathTypeMatchesResource(warehouse, type, resource)) {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Resources cannot change their rdf:type")
                        .build();
            }
            if (resource.listProperties().hasNext()) {
                rdfManager.setResource(warehouse, resource, store);
                response.addHeader(LOCATION, finalURL);
                response.setStatus(OK.getStatusCode());
                response.flushBuffer();
                LOG.info("{} updated", finalURL);

                return Response.status(OK)
                        .type(TEXT_PLAIN)
                        .entity("The resource was updated.")
                        .header(LOCATION, finalURL)
                        .build();
            } else {
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Request body does not include the resource with the URL to update" + finalURL)
                        .build();
            }

        } catch(RiotException e) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Invalid syntax: " + e.getMessage())
                    .build();
        } catch(FetchingRulesException e) {
            return Response.status(SERVICE_UNAVAILABLE)
                    .type(TEXT_PLAIN)
                    .entity("Fetching rules exception: " + e.getMessage())
                    .build();
        } catch(ValidationException e) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Validation exception: " + e.getMessage())
                    .build();
        } catch(RuntimeException | IOException e) {
            LOG.error("Could not create resource at " + finalURL, e);
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity("Internal server error: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("{warehouse}/{store}/{type}/{id}")
    public Response deleteResource(@PathParam("warehouse") String warehouse,
                                   @PathParam("store") String store,
                               @PathParam("type") String type,
                               @PathParam("id") String id) {
        Model model;
        org.apache.jena.rdf.model.Resource resource;
        String finalURL = request.getRequestURL().toString();
        // String store = warehouse;
        LOG.info("DELETE artifact @ {}", finalURL);
        try {
            model = rdfManager.getResource(warehouse, finalURL, store);
            if (model.isEmpty()) {
                return Response.status(NOT_FOUND)
                        .type(TEXT_PLAIN)
                        .entity("Resource not found.")
                        .build();
            }

            resource = model.getResource(finalURL);
            rdfManager.removeResource(warehouse, resource, store);
            response.setStatus(OK.getStatusCode());
            response.flushBuffer();
            LOG.info("{} deleted", finalURL);

            return Response.ok().build();

        } catch(RuntimeException | IOException e) {
            LOG.error("Could not remove resource at " + finalURL, e);
            return Response.status(INTERNAL_SERVER_ERROR)
                    .type(TEXT_PLAIN)
                    .entity(e.getMessage())
                    .build();
        }
    }

    private boolean pathTypeMatchesResource(String warehouse, String typeName,
                                            Resource resource){
        Resource type;
        boolean match = false;
        Resource pathResource = oslcManager.getQualifiedResourceType(warehouse, typeName);
        StmtIterator iterator = resource.listProperties(RDF.type);
        while (iterator.hasNext() && match == false) {
            type = iterator.next().getObject().asResource();
            match = type.equals(pathResource);
        }
        return match;
    }

    private String getCreationFactory(String warehouse, Resource resource,
                                      String store) {
        Resource type, factory = null;
        StmtIterator iterator = resource.listProperties(RDF.type);
        while (iterator.hasNext() && factory == null) {
            type = iterator.next().getObject().asResource();
            factory = oslcManager.getCreationFactory(warehouse, type.getURI(), store);
        }
        return factory == null ? null : factory.getURI();
    }

}