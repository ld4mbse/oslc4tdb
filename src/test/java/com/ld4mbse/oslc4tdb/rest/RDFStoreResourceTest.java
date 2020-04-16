package com.ld4mbse.oslc4tdb.rest;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.ld4mbse.oslc4tdb.model.Environment;
import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.util.Warehouses;
import org.apache.commons.io.FileUtils;
import org.apache.jena.tdb.base.block.FileMode;
import org.apache.jena.tdb.sys.SystemTDB;
import org.jboss.resteasy.core.Dispatcher;
import org.junit.*;
import org.jboss.resteasy.mock.*;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RDFStoreResource}.
 * @author rherrera
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RDFStoreResourceTest extends BaseResourceTest {

    private static final Logger LOG = LoggerFactory.getLogger(RDFStoreResourceTest.class);

    private static File storeDirectory;
    private static String storeName;

    private RDFManager manager;
    private Dispatcher dispatcher;
    private MockHttpRequest request;
    private MockHttpResponse response;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    private InputStream stream;

    @Before
    public void init() throws IOException, InterruptedException {
        cleanFolder();

        storeName = "testStore";
        storeDirectory = new File(Environment.TDB_LOCATION + File.separator + storeName);
        if (!storeDirectory.exists()) {
            storeDirectory.mkdirs();
        }

        RDFStoreResource resource;
        Map<Class<?>, Object> ctx;
        manager = mock(RDFManager.class);
        resource = new RDFStoreResource(manager);
        ctx = ResteasyProviderFactory.getContextDataMap();
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(resource);
        servletRequest = mock(HttpServletRequest.class);
        servletResponse = mock(HttpServletResponse.class);
        response = new MockHttpResponse();
        ctx.put(HttpServletRequest.class, servletRequest);
        ctx.put(HttpServletResponse.class, servletResponse);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        stream = loader.getResourceAsStream("family.ttl");
    }

    @After
    public void clean() throws IOException, InterruptedException {
        SystemTDB.setFileMode(FileMode.direct);
        cleanFolder();
    }

    private void cleanFolder() throws IOException, InterruptedException {
        String[] stores = Warehouses.list();
        String[] fakeStores = {"store-without-shape", "store-with-shape", "nonexistent", "myStore", "testStore", ".DS_Store"};
        for (String dir : fakeStores) {
            LOG.info("Stores: " + Arrays.asList(stores));
            LOG.info("Dir: " + dir);
            if (Arrays.asList(stores).contains(dir)) {
                File folderStore = new File(Environment.TDB_LOCATION + File.separator + dir);
                TimeUnit.SECONDS.sleep(3);
                if (folderStore.exists() && folderStore.isDirectory()) {
                    FileUtils.deleteDirectory(folderStore);
                } else {
                    FileUtils.forceDelete(folderStore);
                }
            }
        }
    }

    @Test
    public void testSet_ListingEmptyRDFStore() throws URISyntaxException, UnsupportedEncodingException {

        when(servletRequest.getHeader(ACCEPT)).thenReturn(MediaType.APPLICATION_JSON);
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/oslc4tdb/oslc/rdfstores"));
        when(servletRequest.getPathInfo()).thenReturn("/rdfstores");

        request = MockHttpRequest.get("rdfstores");
        dispatcher.invoke(request, response);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals("There is not any RDF Store.", response.getContentAsString());
    }

    @Test()
    public void testSet_ListingStoresWithWrongMethod() throws URISyntaxException {
        request = MockHttpRequest.post("rdfstores");
        dispatcher.invoke(request, response);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testSet_MissingSlugHeader() throws URISyntaxException, UnsupportedEncodingException {
        when(servletRequest.getContentType()).thenReturn(MediaType.TEXT_PLAIN);
        request = MockHttpRequest.post("rdfstores");
        request.content(stream);
        dispatcher.invoke(request, response);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Missing the Slug for naming the RDF Store.", response.getContentAsString());
    }

    @Test
    public void testSet_CreateStore() throws URISyntaxException, IOException {
        byte[] data = new byte[]{};

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/oslc4tdb/oslc/rdfstores"));

        request = MockHttpRequest.post("rdfstores");
        request.header("Slug", "myStore");
        request.content(data);
        dispatcher.invoke(request, response);
        assertEquals(CREATED.getStatusCode(), response.getStatus());
        assertEquals("The RDF store myStore was created successfuly.", response.getContentAsString());
    }

    @Test
    public void testSet_InvalidContentType() throws URISyntaxException, UnsupportedEncodingException {
        String responseBody = "Incompatible ContentType:";
        when(servletRequest.getContentType()).thenReturn(MediaType.TEXT_XML);
        when(servletRequest.getHeader("Slug")).thenReturn("myStore");

        request = MockHttpRequest.post("rdfstores");
        request.header("Slug", "myStore");
        request.content(stream);
        dispatcher.invoke(request, response);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.getContentAsString().startsWith(responseBody));
    }

    @Test
    public void testSet_DeleteRDFStore() throws URISyntaxException, UnsupportedEncodingException {

        request = MockHttpRequest.delete("rdfstores/" + storeName);
        dispatcher.invoke(request, response);
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals("The RDF Store has been deleted.", response.getContentAsString());

    }

}
