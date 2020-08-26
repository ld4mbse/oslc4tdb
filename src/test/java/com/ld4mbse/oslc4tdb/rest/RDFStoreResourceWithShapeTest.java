package com.ld4mbse.oslc4tdb.rest;

import com.ld4mbse.oslc4tdb.model.Environment;
import com.ld4mbse.oslc4tdb.services.RDFManager;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.WebContent;
import org.apache.jena.tdb.base.block.FileMode;
import org.apache.jena.tdb.sys.SystemTDB;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RDFStoreResourceWithShapeTest extends BaseResourceTest {

    private static final Logger LOG = LoggerFactory.getLogger(RDFStoreResourceWithShapeTest.class);

    private static File storeDirectory;
    private static String storeName;
    private ClassLoader loader;
    private InputStream shacl;
    private InputStream shaclUpdate;

    private RDFManager manager;
    private MockHttpRequest request;
    private MockHttpResponse response;
    private Dispatcher dispatcher;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    private Model model;
    private Lang language;

    @Before
    public void init() {

        storeName = "testStore";
        storeDirectory = new File(Environment.TDB_LOCATION + File.separator + storeName);
        if (!storeDirectory.exists()) {
            storeDirectory.mkdirs();
        }

        Map<Class<?>, Object> ctx;
        RDFStoreResource resource;

        String contentType = WebContent.ctTurtle.getContentType();

        manager = mock(RDFManager.class);
        resource = new RDFStoreResource(manager);

        /*
         * Loading the SHACL definition to validate the graphs
         */
        loader = Thread.currentThread().getContextClassLoader();
        shacl = loader.getResourceAsStream("family.ttl");
        shaclUpdate = loader.getResourceAsStream("family_update.ttl");
        language = RDFLanguages.contentTypeToLang(contentType);
        model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model.getGraph(), shacl, null, language);
        // manager.setModel(storeName, model, null);

        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(resource);
        servletRequest = mock(HttpServletRequest.class);
        servletResponse = mock(HttpServletResponse.class);

        ctx = ResteasyProviderFactory.getContextDataMap();
        ctx.put(HttpServletRequest.class, servletRequest);
        ctx.put(HttpServletResponse.class, servletResponse);

        response = new MockHttpResponse();
    }

    @After
    public void clean() throws IOException, InterruptedException {
        SystemTDB.setFileMode(FileMode.direct);
        if (storeDirectory.exists()) {
            TimeUnit.SECONDS.sleep(3);
            FileUtils.deleteDirectory(storeDirectory);
        }
    }

    private String convertStream(StubServletOutputStream stream) throws IOException {

        StringBuilder stringResponse = new StringBuilder();
        String line = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(stream.baos.toByteArray())))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringResponse.append(line);
            }
        }

        return stringResponse.toString();

    }

    @Test
    public void testSet_ListingRDFStore() throws URISyntaxException, IOException {
        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/oslc4tdb/oslc/" + storeName));
        when(servletRequest.getPathInfo()).thenReturn("/rdfstores");
        when(servletResponse.getOutputStream()).thenReturn(servletOutputStream);
        when(servletRequest.getHeader(ACCEPT)).thenReturn(MediaType.TEXT_PLAIN);

        when(manager.getModels("http://localhost:8080/oslc4tdb/oslc/" + storeName)).thenReturn(model);

        request = MockHttpRequest.get("rdfstores");
        dispatcher.invoke(request, response);

        String stringResponse = convertStream(servletOutputStream);

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertTrue(stringResponse.startsWith("<rdf:RDF"));

    }

//    @Test
//    public void testSet_RetrieveRDFStore() throws URISyntaxException, IOException {
//        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
//
//        when(servletRequest.getHeader(ACCEPT)).thenReturn(MediaType.TEXT_PLAIN);
//        when(manager.getModel(storeName, null)).thenReturn(model);
//        when(servletResponse.getOutputStream()).thenReturn(servletOutputStream);
//
//        request = MockHttpRequest.get("oslc/" + storeName + "/stores");
//        dispatcher.invoke(request, response);
//
//        String stringResponse = convertStream(servletOutputStream);
//
//        assertEquals(OK.getStatusCode(), response.getStatus());
//        assertTrue(stringResponse.startsWith("<rdf:RDF"));
//
//    }

//    @Test
//    public void testSet_RetrieveSHACLDefinition() throws IOException, URISyntaxException {
//
//        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
//
//        when(servletRequest.getHeader(ACCEPT)).thenReturn(MediaType.TEXT_PLAIN);
//        when(manager.getModel(storeName, null)).thenReturn(model);
//        when(servletResponse.getOutputStream()).thenReturn(servletOutputStream);
//
//        request = MockHttpRequest.get("rdfstores/" + storeName);
//        request.header("Accept", "text/turtle");
//        dispatcher.invoke(request, response);
//
//        String stringResponse = convertStream(servletOutputStream);
//
//        assertEquals(OK.getStatusCode(), response.getStatus());
//        assertTrue(stringResponse.toString().startsWith("<rdf:RDF"));
//
//    }

//    @Test
//    public void testSet_UpdateSHACLDocument() throws URISyntaxException {
//
//        when(servletRequest.getContentLength()).thenReturn(1);
//
//        request = MockHttpRequest.put("rdfstores/" + storeName);
//        request.content(shaclUpdate);
//        dispatcher.invoke(request, response);
//        assertEquals(OK.getStatusCode(), response.getStatus());
//
//    }

    @Test
    public void testSet_DeleteRDFStore() throws URISyntaxException, UnsupportedEncodingException {

        request = MockHttpRequest.delete("rdfstores/" + storeName);
        dispatcher.invoke(request, response);
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals("The RDF Store has been deleted.", response.getContentAsString());

    }

}


