package com.ld4mbse.oslc4tdb.model;

import com.ld4mbse.oslc4tdb.services.TDBManager;
import com.ld4mbse.oslc4tdb.util.Warehouses;
import com.ld4mbse.oslc4tdb.util.OslcShaclAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;

/**
 * The OSLC manager.
 * @author rherrera
 */
@ApplicationScoped
public class OSLCManager {
    /**
     * The base URI to create OSLC resources.
     */
    private String baseURI;
    /**
     * The OSLC warehouses representations.
     */
    private Map<String, OSLCModel> oslcWarehouses;
    /**
     * Initializes the TDB directory.
     */
    @PostConstruct
    protected void init() {
        try {
            TDBManager watcher = CDI.current().select(TDBManager.class).get();
            Context module = (Context)new InitialContext().lookup("java:comp/env");
            baseURI = module.lookup("oslc.baseURI").toString();
            oslcWarehouses = new HashMap<>();
            watcher.addObserver((Observable o, Object warehouse) -> {
                oslcWarehouses.remove(warehouse.toString());
            });
        } catch (NamingException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private OSLCModel getOSLCModel(String warehouse) {
        Dataset warehouseData;
        OSLCModel warehouseOSLCModel = oslcWarehouses.get(warehouse);
        if (warehouseOSLCModel == null) {
            warehouseData = Warehouses.get(warehouse);
            warehouseOSLCModel = OslcShaclAdapter.getOSLCModel(baseURI, warehouse, warehouseData);
            oslcWarehouses.put(warehouse, warehouseOSLCModel);
            TDBFactory.release(warehouseData);
        }
        return warehouseOSLCModel;
    }
    /**
     * Gets the {@code ServiceProviderCatalog} of the given warehouse.
     * @param warehouse the given warehouse name; send the empty string to get
     * the master {@code ServiceProviderCatalog} of this application.
     * @return the {@code ServiceProviderCatalog} of given warehouse.
     */
    public Model getServiceProviderCatalog(String warehouse) {
        if (warehouse.isEmpty())
            return OSLCModel.getServiceProviderCatalog(
                    baseURI,
                    warehouse,
                    "Master Service Provider Catalog.",
                    "Encapsulates all ServiceProvider for all the RDF Stores.",
                    Warehouses.list()
            );
        else {
            warehouse = warehouse.endsWith("/") ? warehouse.substring(0, warehouse.length() - 1) : warehouse;
            return getOSLCModel(warehouse)
                    .expandResource(
                            3,
                            OSLCModel.PATHS.PREFIX,
                            warehouse,
                            OSLCModel.PATHS.SERVICE_PROVIDER_CATALOG
                    );
        }
    }
    /**
     * Gets a {@code ServiceProvider} definition by name.
     * @param warehouse the given warehouse name.
     * @param name the name of the {@code ServiceProvider} to retrieve.
     * @return the model definition of the service provider; and empty model if
     * it does not exists.
     */
    public Model getServiceProvider(String warehouse, String name) {
        return getOSLCModel(warehouse).describeResource(OSLCModel.PATHS.PREFIX, warehouse, OSLCModel.PATHS.SERVICE_PROVIDER, name);
    }
    /**
     * Gets a {@code ResourceShape} definition by name.
     * @param warehouse the given warehouse name.
     * @param name the name of the {@code ResourceShape} to retrieve.
     * @return the model definition of the resource shape; and empty model if
     * it does not exists.
     */
    public Model getResourceShape(String warehouse, String name) {
        return getOSLCModel(warehouse).describeResource(OSLCModel.PATHS.PREFIX, warehouse, OSLCModel.PATHS.RESOURCE_SHAPES, name);
    }
    /**
     * Gets the {@code AllowedValues} definition for a given property in a shape.
     * @param warehouse the given warehouse name.
     * @param property the name of the bounded property.
     * @param shape the {@code ResourceShape} where {@code property} is defined.
     * @return the model definition of the AllowedValues resource; and empty
     * model if it does not exists.
     */
    public Model getAllowedValues(String warehouse, String property, String shape) {
        return getOSLCModel(warehouse).describeResource(OSLCModel.PATHS.PREFIX, warehouse, OSLCModel.PATHS.RESOURCE_SHAPES, shape, OSLCModel.PATHS.VALUES, property);
    }
    /**
     * Resolves a resource type alias to the corresponding rdf:type.
     * @param warehouse the given warehouse name.
     * @param alias the simple resource type name.
     * @return the resource rdf:type if any; {@code null} otherwise.
     */
    public Resource getQualifiedResourceType(String warehouse, String alias) {
        Resource shape = getOSLCModel(warehouse).getResourceShapes().get(alias);
        return shape == null ? null : shape.getPropertyResourceValue(OSLCModel.PROPS.PATHS.DESCRIBES);
    }
    /**
     * Finds the {@link CreationFactory} resource for a given rdf:type and store.
     * @param warehouse the given warehouse name.
     * @param type the rdf:type URI.
     * @param store the title of {@code CreationFactory} {@code ServiceProvider}.
     * @return the corresponding {@code CreationFactory} resource if found;
     * {@code null} otherwise.
     */
    public Resource getCreationFactory(String warehouse, String type, String store) {
        return getOSLCModel(warehouse).getCreationFactory(type, store);
    }

    public String getBaseURI() {
        return baseURI;
    }

}