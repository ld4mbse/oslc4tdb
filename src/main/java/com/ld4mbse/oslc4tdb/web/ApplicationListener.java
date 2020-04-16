package com.ld4mbse.oslc4tdb.web;

import com.ld4mbse.oslc4tdb.model.Environment;
import java.io.File;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application verifier and resources releaser.
 * @author rherrera
 */
public class ApplicationListener implements ServletContextListener {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationListener.class);
    /**
     * Prepares a directory for use. If the directory does not exists, this
     * method will attempt to create it, otherwise it will check whether it has
     * permission for read and write operations.
     * @param path the directory path.
     * @throws IllegalStateException if the directory cannot be created or it
     * does not have permission for reading or writing.
     */
    private File prepareDirectory(String path) {
        File directory = new File(path);
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                LOG.error(path = path + " is not a directory");
                throw new IllegalStateException(path);
            }
            if (!directory.canRead()) {
                LOG.error(path = "Cannot read on " + path);
                throw new IllegalStateException(path);
            }
            if (!directory.canWrite()) {
                LOG.error(path = "Cannot write on " + path);
                throw new IllegalStateException(path);
            }
            LOG.info("{} directory ready", path);
        } else if (directory.mkdirs()) {
            LOG.info("{} directory created", path);
        } else {
            LOG.error(path = path + " directory cannot be created");
            throw new IllegalStateException(path);
        }
        return directory;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Model config = ModelFactory.createDefaultModel();
        prepareDirectory(Environment.TDB_LOCATION);
        context.setAttribute(Environment.CONFIG_MODEL, config);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}