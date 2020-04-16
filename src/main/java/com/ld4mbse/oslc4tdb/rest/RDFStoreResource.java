package com.ld4mbse.oslc4tdb.rest;

import com.ld4mbse.oslc4tdb.model.Environment;
import com.ld4mbse.oslc4tdb.rest.exception.IllegalStoreException;
import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.util.Resources;
import com.ld4mbse.oslc4tdb.util.Warehouses;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.LOCATION;

@Path("rdfstores")
public class RDFStoreResource extends RDFResource {

    private static final Logger LOG = LoggerFactory.getLogger(RDFStoreResource.class);

    @Inject
    private RDFManager manager;

    public RDFStoreResource() { }

    public RDFStoreResource(RDFManager manager) {
        this.manager = manager;
    }

    @GET
    public Response getStores() {
        Lang language;
        Model model;
        OutputStream output;
        boolean isHumanClient = request.getHeader(ACCEPT).contains(MediaType.TEXT_HTML);

        try {
            LOG.debug("Searching TBD Store directories");
            LOG.debug(request.getContextPath());
            String storeURL = request.getRequestURL().toString().replace(request.getPathInfo(), "");
            model = manager.getModels(storeURL);

            if (isHumanClient) {
                renderDataStore("", model);
            } else {
                if (model == null || model.isEmpty()) {
                    LOG.debug("There is not any RDF Store.");
                    return Response.status(Response.Status.NO_CONTENT)
                            .entity("There is not any RDF Store.")
                            .build();
                }

                String accept = request.getHeader(org.apache.http.HttpHeaders.ACCEPT);
                if ((language = getAcceptableLanguage()) == null) {
                    LOG.info("The Content-Type {} is not compatible.", accept);
                    return Response.status(Response.Status.NOT_ACCEPTABLE)
                            .type(MediaType.TEXT_PLAIN)
                            .entity("Incompatible ContentType: " + accept)
                            .build();
                }

                response.setContentType(language.getContentType().toHeaderString());
                response.setStatus(HttpServletResponse.SC_OK);
                response.addHeader(HttpHeaders.ETAG, Resources.getETag(model.toString()));

                output = response.getOutputStream();
                RDFDataMgr.write(output, model, language);
                output.flush();
            }
        } catch(RuntimeException | IOException e) {
            LOG.error("Could not get resource at " + request.getRequestURL().toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Could not get resource at " + request.getRequestURL().toString())
                    .build();
        } catch (ServletException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Could not get resource at " + request.getRequestURL().toString())
                    .build();
        }
        return Response.ok().build();
    }

    @POST
    public Response addStore(@HeaderParam("Slug") String store) {

        Lang inputLanguage;
        StringBuilder location;

        String contentType = request.getContentType();
        if (contentType == null || contentType.isEmpty() || MediaType.TEXT_PLAIN.equals(contentType)) {
            contentType = WebContent.ctTurtle.getContentType();
        }

        inputLanguage = RDFLanguages.contentTypeToLang(contentType);
        if (inputLanguage == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Incompatible ContentType: " + contentType)
                    .build();
        }

        if (store == null || store.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(contentType)
                    .entity("Missing the Slug for naming the RDF Store.")
                    .build();
        } else {

            try {
                location = new StringBuilder(Environment.TDB_LOCATION);
                location.append(File.separator);
                location.append(store);

                File directory = new File(location.toString());
                if (directory.exists()) {
                    LOG.info("Store alredy exists in {} directory", location.toString());
                    return Response.status(Response.Status.CONFLICT)
                            .type(contentType)
                            .entity("There is a store with the same name.")
                            .build();
                } else if (directory.mkdirs()) {
                    LOG.info("{} directory created", location.toString());
                    String catalog = request.getRequestURL().toString();
                    catalog = catalog.replace("rdfstores", store);
                    catalog += "/catalog";
                    return Response.status(Response.Status.CREATED)
                            .type(contentType)
                            .entity("The RDF store " + store + " was created successfuly.")
                            .header(LOCATION, catalog)
                            .build();
                } else {
                    LOG.error(location.toString() + " directory cannot be created");
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .type(contentType)
                            .entity("The RDF Store can not be created.")
                            .build();
                }
            } catch (RiotException re) {
                LOG.error("Content-Type doesn't mismatched with the body content.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("Content-Type doesn't mismatched with the body content.")
                        .build();
            } catch (Exception e) {
                LOG.error("Can't create the RDF Store", e);
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(contentType)
                        .entity("Invalid syntax: " + e.getMessage())
                        .build();
            }
        }
    }

    @DELETE
    @Path("{store}")
    public Response deleteStore(@PathParam("store") String store) {
        String contetType = request.getContentType();
        if (contetType == null || contetType.isEmpty())
            contetType = WebContent.contentTypeTextPlain;
        if (store == null || store.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).type(contetType).entity("Missing the Slug with the name of the RDF Store for deleting.").build();
        } else {
            if (!Warehouses.exist(store)) {
                LOG.info("The store {} does not exists in the store.", store);
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("The store " + store + " does not exists in the store.").build();
            }
            if (!Warehouses.isLocked(store)) {
                try {
                    Warehouses.delete(store);
                    LOG.info("The RDF Store has been deleted.");
                    return Response.status(Response.Status.OK).entity("The RDF Store has been deleted.").build();
                } catch (RuntimeIOException | InterruptedException | IOException e) {
                    LOG.error("An error has ocurred: " + e);
                    return Response.status(Response.Status.CONFLICT).type(contetType).entity("The RDF Store can't be deleted.").build();
                }
            } else {
                LOG.info("The RDF Store you want to delete is being using.");
                return Response.status(Response.Status.CONFLICT).type(contetType).entity("The RDF Store you want to delete is being using.").build();
            }
        }
    }

}
