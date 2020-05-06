package com.ld4mbse.oslc4tdb.rest;

import com.ld4mbse.oslc4tdb.model.Environment;
import com.ld4mbse.oslc4tdb.model.OSLCManager;
import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.tdb.validation.ValidationException;
import com.ld4mbse.oslc4tdb.util.Models;
import com.ld4mbse.oslc4tdb.util.Requests;
import com.ld4mbse.oslc4tdb.util.Warehouses;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("stores/{warehouse}")
public class StoreResource extends RDFResource {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StoreResource.class);

    @Inject
    private RDFManager manager;

    @Inject
    private OSLCManager oslcManager;

    public StoreResource() {
    }

    public StoreResource(RDFManager manager, OSLCManager oslcManager) {
        this.manager = manager;
        this.oslcManager = oslcManager;
    }

    @GET
    public Response getStores(@PathParam("warehouse") String warehouse,
                              @QueryParam("pattern") String pattern,
                              @HeaderParam(ACCEPT) String accept) {
        Model model;
        try {
            StringBuffer url = request.getRequestURL();
            if (url.charAt(url.length() - 1) != '/') url.append('/');
            model = manager.getModels(warehouse, pattern, url.toString());
            return dispatchResource(warehouse, model, "", false);
        } catch (IllegalArgumentException ex) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    @GET
    @Path("{store}")
    public Response getStore(@PathParam("warehouse") String warehouse,
                             @PathParam("store") String store,
                             @QueryParam("where") String where,
                             @QueryParam("select") String select,
                             @HeaderParam(ACCEPT) String accept) {
        try {
            Model model;
            if (where == null && select == null)
                model = manager.getModel(warehouse, Models.getStoreURN(store));
            else
                model = manager.getModel(warehouse, Models.getStoreURN(store), where, select);
            return dispatchResource(store, model, "",false);
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    @POST
    public Response addGraph(@PathParam("warehouse") String warehouse,
                             @HeaderParam(Environment.ID_HEADER) String slug,
                             String body) {

        Model shacl;
        Lang inputLanguage;
        StringBuffer requestURL = request.getRequestURL();

        String contentType = request.getContentType();
        if (contentType == null || contentType.isEmpty() || MediaType.TEXT_PLAIN.equals(contentType)) {
            contentType = WebContent.contentTypeTurtle;
        }

        LOG.info("POST {}", requestURL.toString());
        LOG.info("Content-Type: {}", contentType);
        LOG.info("Slug: {}", slug);
        inputLanguage = RDFLanguages.contentTypeToLang(contentType);
        if (slug == null) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Missing " + Environment.ID_HEADER + " header")
                    .build();
        } else if (request.getContentLength() <= 0 || body.isEmpty()) {
            LOG.info("Missing request body.", warehouse);
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Missing request body")
                    .build();
        } else if (inputLanguage == null) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Incompatible ContentType: " + contentType)
                    .build();
        } else {
            try {
                requestURL.append('/');
                requestURL.append(slug);

                if (manager.containsModel(warehouse, Models.getStoreURN(slug + "-shacl"))) {
                    return Response.status(CONFLICT)
                            .type(TEXT_PLAIN)
                            .entity("Resource '" + requestURL.toString() + "' already exists.")
                            .build();
                } else {

                    LOG.trace("Reading model from request body in {}.", contentType);
                    InputStream inputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));

                    String url = Requests.buildURI(oslcManager.getBaseURI(), OslcConstants.OSLC_CORE_NAMESPACE_PREFIX, warehouse, slug) + "/";

                    shacl = Models.read(inputStream, url, contentType);
                    LOG.trace("{} statements read.", shacl.size());
                    LOG.trace("Appending target graph URI {} to resources.", slug);

                    LOG.trace("Storing final model.");
                    manager.setSHACLModel(warehouse, shacl, Models.getStoreURN(slug));

                    response.addHeader(LOCATION, requestURL.toString());
                    response.setStatus(CREATED.getStatusCode());
                    response.flushBuffer();

                    return Response.status(CREATED)
                            .type(contentType)
                            .entity("The resource was created.")
                            .header(LOCATION, requestURL.toString())
                            .build();

                }
            } catch (IllegalStateException ex) {
                LOG.trace("An exception has ocurred: ", ex);
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("SHACL schema malformed: " + ex.getMessage())
                        .build();
            } catch (RiotException ex) {
                LOG.trace("An exception has ocurred: ", ex);
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Invalid syntax: " + ex.getMessage())
                        .build();
            } catch(ValidationException e) {
                LOG.trace("An exception has ocurred: ", e);
                return Response.status(BAD_REQUEST)
                        .type(e.getMessageContentType())
                        .entity(e.getMessage())
                        .build();
            } catch (IOException e) {
                LOG.error("Could not create resource at " + requestURL.toString(), e);
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("An Internal error has ocurred.")
                        .build();
            }

        }
    }

    @POST
    @Path("{store}")
    public Response bulkLoader(@PathParam("warehouse") String warehouse,
                               @PathParam("store") String store,
                               String body) {

        Model model;
        Lang inputLanguage;
        StringBuffer requestURL = request.getRequestURL();

        String contentType = request.getContentType();
        if (contentType == null || contentType.isEmpty() || MediaType.TEXT_PLAIN.equals(contentType)) {
            contentType = WebContent.contentTypeRDFXML;
        }

        LOG.info("POST {}", requestURL.toString());
        LOG.info("Content-Type: {}", contentType);

        inputLanguage = RDFLanguages.contentTypeToLang(contentType);
        if (request.getContentLength() <= 0 || body.isEmpty()) {
            LOG.info("Missing request body.", warehouse);
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Missing request body")
                    .build();
        } else if (inputLanguage == null) {
            return Response.status(BAD_REQUEST)
                    .type(TEXT_PLAIN)
                    .entity("Incompatible ContentType: " + contentType)
                    .build();
        } else {
            try {

                LOG.trace("Reading model from request body in {}.", contentType);
                InputStream inputStream = new ByteArrayInputStream(body.getBytes(Charset.forName("UTF-8")));

                String url = Requests.buildURI(oslcManager.getBaseURI() , OslcConstants.OSLC_CORE_NAMESPACE_PREFIX, warehouse, store) + "/";

                model = Models.read(inputStream, url, contentType);
                LOG.trace("{} statements read.", model.size());

                Model modelStored = manager.getModel(warehouse, Models.getStoreURN(store));
                List<Resource> resourceStored = modelStored.listResourcesWithProperty(RDF.type).toList();
                List<Resource> resourceToStore = model.listResourcesWithProperty(RDF.type).toList();

                if (Collections.disjoint(resourceToStore, resourceStored)) {
                    LOG.trace("Storing {} new elements.", model.size());
                    LOG.info("< [+] {}/{}", store, model.size());
                    manager.addModel(warehouse, model, Models.getStoreURN(store));

                    return Response.status(CREATED)
                            .type(contentType)
                            .entity("The resource was created.")
                            .header(LOCATION, requestURL.toString().replace("/stores", "") + "/stores")
                            .build();

                } else {

                    List<Resource> toStoreButExist = new ArrayList<>(resourceToStore);
                    resourceToStore.removeAll(resourceStored);
                    toStoreButExist.removeAll(resourceToStore);

                    if (resourceToStore.size() == 0) {
                        LOG.trace("There are not changes to apply.");
                        LOG.info("< [+] {}/{}", store, model.size());

                        return Response.status(NOT_MODIFIED)
                                .type(TEXT_PLAIN)
                                .entity("The resources have not been modified.")
                                .build();
                    } else {

                        ResIterator iterator = model.listResourcesWithProperty(RDF.type);
                        while (iterator.hasNext()) {
                            Resource r = iterator.next();
                            if (toStoreButExist.contains(r)) {
                                // remove statements where resource is subject
                                model.removeAll(r, null, (RDFNode) null);
                                // remove statements where resource is object
                                model.removeAll(null, null, r);
                            }
                        }

                        LOG.trace("Storing {} new elements.", model.size());
                        LOG.info("< [+] {}/{}", store, model.size());
                        manager.addModel(warehouse, model, Models.getStoreURN(store));

                        Model modelResponse = ModelFactory.createDefaultModel();
                        modelResponse.setNsPrefix(OslcConstants.OSLC_CORE_NAMESPACE_PREFIX, OslcConstants.OSLC_CORE_NAMESPACE);
                        modelResponse.setNsPrefix(OslcConstants.DCTERMS_NAMESPACE_PREFIX, DCTerms.NS);
                        modelResponse.setNsPrefix(OslcConstants.RDFS_NAMESPACE_PREFIX, RDFS.uri);
                        modelResponse.setNsPrefix(OslcConstants.RDF_NAMESPACE_PREFIX, RDF.uri);
                        modelResponse.setNsPrefix("xsd", OslcConstants.XML_NAMESPACE);
                        modelResponse.setNsPrefixes(model.getNsPrefixMap());

                        Resource container = modelResponse.createResource();
                        modelResponse.add(container, DCTerms.title, "Resources");
                        modelResponse.add(container, DCTerms.description, "Resources that alredy exists in the store");

                        Resource member = modelResponse.createResource();
                        for (Resource res : toStoreButExist) {
                            modelResponse.add(member, RDF.type, res);
                        }
                        modelResponse.add(container, RDF.type, member);

                        response.setContentType(inputLanguage.getContentType().toHeaderString());
                        response.addHeader(LOCATION, requestURL.toString().replace("/stores", "") + "/stores");
                        response.setStatus(ACCEPTED.getStatusCode());
                        OutputStream output = response.getOutputStream();
                        RDFDataMgr.write(output, modelResponse.getGraph(), inputLanguage);
                        output.flush();

                        return Response.status(ACCEPTED)
                                .type(contentType)
                                .entity("The resources have not been modified.")
                                .header(LOCATION, requestURL.toString().replace("/stores", "/") + "/stores")
                                .build();
                    }

                }

            } catch (RiotException ex) {
                LOG.trace("An exception has ocurred: ", ex);
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("Invalid syntax: " + ex.getMessage())
                        .build();
            } catch(ValidationException e) {
                LOG.trace("An exception has ocurred: ", e);
                return Response.status(BAD_REQUEST)
                        .type(e.getMessageContentType())
                        .entity(e.getMessage())
                        .build();
            } catch (IOException e) {
                LOG.error("Could not create resource at " + requestURL.toString(), e);
                return Response.status(BAD_REQUEST)
                        .type(TEXT_PLAIN)
                        .entity("An Internal error has ocurred.")
                        .build();
            }

        }
    }

    @PUT
    @Path("{store}")
    public Response updateStore(@PathParam("warehouse") String warehouse,
                                @PathParam("store") String store,
                                String body) {

        Lang inputLanguage;
        Model model;
        InputStream inputStream;

        String contentType = request.getContentType();
        if (contentType == null || contentType.isEmpty() || MediaType.TEXT_PLAIN.equals(contentType)) {
            contentType = WebContent.ctTurtle.getContentType();
        }

        if (!Warehouses.exist(warehouse)) {
            LOG.info("The store {} does not exists in the store.", store);
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN).entity("The store " + store + " does not exists in the store.").build();
        }

        if (request.getContentLength() <= 0 || body.isEmpty()) {
            LOG.info("Missing request body.", store);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Missing request body").build();
        }

        inputLanguage = RDFLanguages.contentTypeToLang(contentType);
        if (inputLanguage == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Incompatible ContentType: " + contentType)
                    .build();
        }

        try {
            inputStream = new ByteArrayInputStream(body.getBytes(Charset.forName("UTF-8")));
            model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model.getGraph(), inputStream, null, inputLanguage);
            manager.setSHACLModel(warehouse, model, Models.getStoreURN(store));

            return Response.status(Response.Status.OK).type(MediaType.TEXT_PLAIN).entity("The Resource Shape was updated successfully.").build();

        } catch (RiotException re) {
            LOG.error("Content-Type doesn't mismatched with the body content.");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Content-Type doesn't mismatched with the body content.").build();
        }
    }

    @DELETE
    @Path("{store}")
    public Response delete(@PathParam("warehouse") String warehouse,
                           @PathParam("store") String store) {
        String contetType = request.getContentType();

        if (store == null || store.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(contetType)
                    .entity("Missing the name of the RDF Store for deleting.")
                    .build();
        } else {
            if (manager.containsModel(warehouse, Models.getStoreURN(store + "-shacl"))) {
                manager.removeModel(warehouse, Models.getStoreURN(store));
                manager.removeModel(warehouse, Models.getStoreURN(store + "-shacl"));
            }
        }

        return Response.ok().build();
    }

}