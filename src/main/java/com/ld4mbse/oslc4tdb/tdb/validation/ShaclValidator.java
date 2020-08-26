package com.ld4mbse.oslc4tdb.tdb.validation;

import java.util.Observable;

import com.ld4mbse.oslc4tdb.util.Queries;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.validation.impl.ValidatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This validator helps to check the structure of a resource on each request
 * allowing to save the information on the store only if the structure
 * is well formed appliying the structure defined on SHACL models.
 */
public class ShaclValidator implements Validator {

    private static final Logger LOG = LoggerFactory.getLogger(ShaclValidator.class);

    /**
     * @see #closeShapesConstruction().
     */
    private static final String CLOSE_SHAPE_CONSTRUCTION;

    /**
     * The SHACL Namespace.
     */
    public static final String SHACL_NS = "http://www.w3.org/ns/shacl#";

    /**
     * Builds the SPARQL CONSTRUCT query to close the sh:NodeShapes that
     * currently haven't set a value for the sh:closed property.
     * @return the corresponding SPARQL CONSTRUCT query.
     */
    private static String closeShapesConstruction() {
        StringBuilder qry = new StringBuilder();
        Queries.prefix("sh", SHACL_NS, qry);
        Queries.prefix("xsd", OslcConstants.XML_NAMESPACE, qry);
        Queries.prefix(OslcConstants.RDF_NAMESPACE_PREFIX, OslcConstants.RDF_NAMESPACE, qry);
        qry.append("CONSTRUCT { ?sub sh:closed \"true\"^^xsd:boolean . ");
        qry.append(" ?sub sh:property [ sh:path rdf:type ] } ");
        qry.append("WHERE { ?sub rdf:type sh:NodeShape . ");
        qry.append("FILTER (NOT EXISTS { ?sub sh:closed ?obj })} ");
        return qry.toString();
    }

    /**
     * Initialization of the SHAPES.
     */
    static {
        CLOSE_SHAPE_CONSTRUCTION = closeShapesConstruction();
    }
    /**
     * The SHACL inner validator.
     */
    private final org.eclipse.lyo.validation.Validator innerValidator;
    /**
     * Whether to validate resources in strict mode.
     */
    private final boolean openWorldAssumption;
    /**
     * Indicates whether validation models are being fetched.
     */
    private boolean fetchingValidationModels;
    /**
     * The SHACL constrains model.
     */
    private Model shaclConstraints;
    /**
     * Constructs an instance specifying the world assumption nature.
     * @param openWorldAssumption world assumption nature.
     */
    public ShaclValidator(boolean openWorldAssumption) {
        this.openWorldAssumption = openWorldAssumption;
        this.innerValidator = new ValidatorImpl();
    }

    /**
     * Updates the validation models.
     */
    @Override
    public void update(Observable o, Object arg) {
        Model source;
        if (arg instanceof Model) {
            source = (Model)arg;
            fetchingValidationModels = true;
            shaclConstraints = ModelFactory.createDefaultModel();
            shaclConstraints.add(source);
            if (!openWorldAssumption) {
                source = Queries.construct(CLOSE_SHAPE_CONSTRUCTION, source);
                shaclConstraints.add(source);
            }
            fetchingValidationModels = false;
            LOG.info("SHACL validation shapes updated");
        } else
            LOG.warn("Cannot update SHACL validation shapes with a {} argument", arg);
    }

}