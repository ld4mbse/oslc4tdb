package com.ld4mbse.oslc4tdb.util;

import com.ld4mbse.oslc4tdb.model.OSLCModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests cases for {@link OslcShaclAdapter}.
 * @author rherrera
 */
public class OslcShaclAdapterTest {
    /**
     * Temporal location for tests.
     */
    private static String location;
    /**
     * The base URI for OSLC components.
     */
    private static String baseURI;
    /**
     * The warehouse name.
     */
    private static String warehouseName;
    /**
     * The warehouse directory.
     */
    private static File warehouseDirectory;
    /**
     * The warehouse.
     */
    private Dataset warehouse;

    @BeforeClass
    public static void initClass() {
        warehouseName = "warehouse";
        baseURI = "http://example.com/context/rest/";
        location = System.getProperty("java.io.tmpdir");
        warehouseDirectory = new File(location + File.separator + warehouseName);
    }

    @Before
    public void init() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("family.ttl");
        warehouse = TDBFactory.createDataset(warehouseDirectory.getPath());
        RDFDataMgr.read(warehouse.getDefaultModel(), stream, Lang.TURTLE);
        warehouse.addNamedModel("urn:uno", warehouse.getDefaultModel());
        warehouse.addNamedModel("urn:dos", warehouse.getDefaultModel());
    }

    @After
    public void clean() throws IOException {
        TDBFactory.release(warehouse);
        if (warehouseDirectory.exists()) {
            FileUtils.deleteDirectory(warehouseDirectory);
        }
    }

    @Test
    public void testGetOSLCModel() {
        OSLCModel oslcModel = OslcShaclAdapter.getOSLCModel(baseURI, warehouseName, warehouse);
        RDFDataMgr.write(System.out, warehouse, Lang.NQUADS);
    }

}