package com.ld4mbse.oslc4tdb.util;

import com.ld4mbse.oslc4tdb.model.Environment;

import java.io.*;
import java.util.concurrent.TimeUnit;

import com.ld4mbse.oslc4tdb.rest.exception.IllegalStoreException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.StoreConnection;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.base.block.FileMode;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.sys.SystemTDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Warehouses {

    private static final Logger LOG = LoggerFactory.getLogger(Warehouses.class);

    public static String[] list() {
        File location = new File(Environment.TDB_LOCATION);
        String[] stores = location.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });
        return stores;
    }

    public static Dataset get(String name) {
        if (exist(name))
            return TDBFactory.createDataset(Environment.TDB_LOCATION + File.separator + name);
        throw new IllegalStoreException("The " + name + " warehouse does not exists in this server.");
    }

    public static boolean exist(String name) {
        String location = Environment.TDB_LOCATION + File.separator + name;
        File store = new File(location);
        return store.isDirectory();
    }

    public static void delete(String name) throws RuntimeIOException, InterruptedException, IOException {
        File location;
        if (!exist(name))
            throw new IllegalStateException(name + " warehouse does not not exist");

        location = new File(Environment.TDB_LOCATION + File.separator + name);

        Location dsLocation = Location.create(location.getAbsolutePath());
        StoreConnection.expel(dsLocation, true);
        TDB.closedown();

        if (SystemUtils.IS_OS_WINDOWS) {
            System.gc();
            SystemTDB.setFileMode(FileMode.direct);
            TimeUnit.SECONDS.sleep(15);
            System.gc();

            if (location.exists()) {

                File[] children = location.listFiles();
                for (int i = 0; children != null && i < children.length; i++) {

                    if (children[i].exists()) {
                        LOG.debug("File {}", children[i].getAbsolutePath());
                        String command="CMD /C DEL /F " + children[i].getAbsolutePath();
                        try {
                            Process process = Runtime.getRuntime().exec(command);
                            BufferedReader reader=new BufferedReader( new InputStreamReader(process.getInputStream()));
                            String s;
                            while ((s = reader.readLine()) != null){
                                LOG.debug("The inout stream is {}", s);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (location.exists()) {
            FileUtils.deleteDirectory(location);
        }
    }

    public static boolean isLocked(String catalog) {
        String location = Environment.TDB_LOCATION + File.separator + catalog;

        File catalogs = new File(location);
        File[] locked = catalogs.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".lock");
            }
        });

        return locked != null && locked.length > 0;

    }

}
