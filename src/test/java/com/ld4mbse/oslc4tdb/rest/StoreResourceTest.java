package com.ld4mbse.oslc4tdb.rest;

import com.ld4mbse.oslc4tdb.services.RDFManager;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link StoreResource}.
 * @author rherrera
 */
public class StoreResourceTest extends BaseResourceTest {

    private RDFManager manager;
    private static Dispatcher dispatcher;
    private MockHttpRequest request;
    private MockHttpResponse response;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    @BeforeClass
    public static void BeforeClass() {
        dispatcher = MockDispatcherFactory.createDispatcher();
    }


    @Before
    public void init() {
        StoreResource resource;
        Map<Class<?>, Object> ctx;
        manager = mock(RDFManager.class);
        resource = new StoreResource();
        ctx = ResteasyProviderFactory.getContextDataMap();
        dispatcher.getRegistry().addSingletonResource(resource);
        servletRequest = mock(HttpServletRequest.class);
        servletResponse = mock(HttpServletResponse.class);
        response = new MockHttpResponse();
        ctx.put(HttpServletRequest.class, servletRequest);
        ctx.put(HttpServletResponse.class, servletResponse);
    }

    /*
    @Test
    public void testSet_MissingContentType() throws URISyntaxException {
        request = MockHttpRequest.post("stores");
        dispatcher.invoke(request, response);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        try {
            assertEquals("Missing Content-Type", response.getContentAsString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSet_MissingSlugHeader() throws URISyntaxException {
        when(servletRequest.getContentType()).thenReturn(MediaType.TEXT_PLAIN);
        request = MockHttpRequest.post(GraphResource.PATH);
        dispatcher.invoke(request, response);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertEquals("Missing Slug header", response.getContentAsString());
    }

    @Test
    public void testSet_InvalidContentType() throws URISyntaxException {
        String responseBody = "Invalid MediaType. Use ";
        when(servletRequest.getContentType()).thenReturn(MediaType.TEXT_PLAIN);
        when(servletRequest.getHeader("Slug")).thenReturn("myLoad");
        request = MockHttpRequest.post(GraphResource.PATH);
        dispatcher.invoke(request, response);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.getContentAsString().startsWith(responseBody));
    }

    @Test
    public void testSet_InvalidRDFSyntax()
            throws URISyntaxException, IOException {
        String responseBody = "Invalid syntax: ";
        ServletInputStream inputStream = getServletInputStream("Hello World");
        when(servletRequest.getContentType()).thenReturn(WebContent.contentTypeTurtle);
        when(servletRequest.getHeader("Slug")).thenReturn("myLoad");
        when(servletRequest.getInputStream()).thenReturn(inputStream);
        request = MockHttpRequest.post(GraphResource.PATH);
        dispatcher.invoke(request, response);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        System.out.println(response.getContentAsString());
        assertTrue(response.getContentAsString().startsWith(responseBody));
    }

    private void testSet(String mediaType, Lang language)
            throws URISyntaxException, IOException {
        String location, slug = "myLoad";
        String serialization = getModelSerialization(language);
        ServletInputStream inputStream = getServletInputStream(serialization);
        when(servletRequest.getHeader("Slug")).thenReturn(slug);
        when(servletRequest.getContentType()).thenReturn(mediaType);
        when(servletRequest.getInputStream()).thenReturn(inputStream);
        request = MockHttpRequest.post(GraphResource.PATH);
        dispatcher.invoke(request, response);
        location = response.getOutputHeaders().getFirst("Location").toString();
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        assertEquals("/" + GraphResource.PATH + "/" + slug, location);
    }

    @Test
    public void testSet_turle() throws URISyntaxException, IOException {
        testSet(WebContent.contentTypeTurtle, Lang.TURTLE);
    }

    @Test
    public void testSet_jsonld() throws URISyntaxException, IOException {
        testSet(WebContent.contentTypeJSONLD, Lang.JSONLD);
    }

    @Test
    public void testSet_rdfxml() throws URISyntaxException, IOException {
        testSet(WebContent.contentTypeRDFXML, Lang.RDFXML);
    }

    @Test
    public void testGet_MissingModel() throws URISyntaxException {
        request = MockHttpRequest.get(GraphResource.PATH + "/myLoad");
        dispatcher.invoke(request, response);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testGet_EmptyModel() throws URISyntaxException {
        Model model = ModelFactory.createDefaultModel();
        when(manager.getModel(ArgumentMatchers.<URI>any())).thenReturn(model);
        request = MockHttpRequest.get(GraphResource.PATH + "/myLoad");
        dispatcher.invoke(request, response);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
    }

    private void testGet(String mediaType, Lang language)
            throws URISyntaxException, IOException {
        Model model = getModel();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ServletOutputStream sos = getServletOutputStream(output);
        when(manager.getModel(ArgumentMatchers.<URI>any())).thenReturn(model);
        when(servletRequest.getHeader(HttpHeaders.ACCEPT)).thenReturn(mediaType);
        when(servletResponse.getOutputStream()).thenReturn(sos);
        request = MockHttpRequest.get(GraphResource.PATH + "/myLoad");
        request.accept(mediaType);
        dispatcher.invoke(request, response);
        assertEquals(getModelSerialization(language), output.toString("UTF8"));
    }

    @Test
    public void testGet_turtle() throws URISyntaxException, IOException {
        testGet(WebContent.contentTypeTurtle, Lang.TURTLE);
    }

    @Test
    public void testGet_jsonld() throws URISyntaxException, IOException {
        testGet(WebContent.contentTypeJSONLD, Lang.JSONLD);
    }

    @Test
    public void testGet_rdfxml() throws URISyntaxException, IOException {
        testGet(WebContent.contentTypeRDFXML, Lang.RDFXML);
    }
*/
}