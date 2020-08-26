package com.ld4mbse.oslc4tdb.rest;

import com.ld4mbse.oslc4tdb.model.Environment;
import com.ld4mbse.oslc4tdb.model.OSLCManager;
import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.services.TDBManager;
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class RDFStoreResourceValidateSHACLTest extends BaseResourceTest {

    private static final Logger LOG = LoggerFactory.getLogger(RDFStoreResourceValidateSHACLTest.class);

    private MockHttpRequest request;
    private MockHttpResponse response;
    private static File storeDirectory;
    private static String storeName;

    private ClassLoader loader;
    private Dispatcher dispatcher;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    private InputStream streamSHACL;
    private InputStream malformed;
    private InputStream person;
    private RDFManager manager;
    private OSLCManager oslcManager;

    @BeforeClass
    public static void initClass() {
        storeName = "testStore";
        storeDirectory = new File(Environment.TDB_LOCATION + File.separator + storeName);
        if (!storeDirectory.exists()) {
            storeDirectory.mkdirs();
        }
    }

    @Before
    public void init() throws InterruptedException {
        Map<Class<?>, Object> ctx;

        manager = new TDBManager();
        oslcManager = mock(OSLCManager.class);
        RDFStoreResource tdbStoreResource = new RDFStoreResource(manager);
        StoreResource storeResource = new StoreResource(manager, oslcManager);

        loader = Thread.currentThread().getContextClassLoader();
        streamSHACL = loader.getResourceAsStream("family.ttl");
        malformed = loader.getResourceAsStream("malformed.rdf");
        person = loader.getResourceAsStream("person.rdf");

        response = new MockHttpResponse();
        servletRequest = mock(HttpServletRequest.class);
        servletResponse = mock(HttpServletResponse.class);

        ctx = ResteasyProviderFactory.getContextDataMap();
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(tdbStoreResource);
        dispatcher.getRegistry().addSingletonResource(storeResource);

        ctx.put(HttpServletRequest.class, servletRequest);
        ctx.put(HttpServletResponse.class, servletResponse);

        Model model = loadSHACLDefinition();


        storeDirectory = new File(Environment.TDB_LOCATION + File.separator + storeName);
        if (!storeDirectory.exists()) {
            storeDirectory.mkdirs();
        }
        TimeUnit.SECONDS.sleep(3);
        // manager.setModel(storeName, model, null);

    }

    @After
    public void clean() throws InterruptedException, IOException {
        SystemTDB.setFileMode(FileMode.direct);
        if (storeDirectory.exists()) {
            TimeUnit.SECONDS.sleep(3);
            FileUtils.deleteDirectory(storeDirectory);
        }
    }

    private Model loadSHACLDefinition() {
        String contentType = WebContent.ctTurtle.getContentType();
        Lang language = RDFLanguages.contentTypeToLang(contentType);
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model.getGraph(), streamSHACL, null, language);
        return model;
    }

    private String convertStream(InputStream inputStream) throws IOException {
        StringBuilder stringInput = new StringBuilder();
        String line;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringInput.append(line);
            }
        }

        return stringInput.toString();

    }

}
