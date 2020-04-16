package com.ld4mbse.oslc4tdb.tdb.validation;

import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import javax.ws.rs.core.MediaType;
import javax.xml.datatype.DatatypeConfigurationException;

import com.ld4mbse.oslc4tdb.util.Queries;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.lyo.oslc4j.core.exception.OslcCoreApplicationException;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.validation.impl.ValidatorImpl;
import org.eclipse.lyo.validation.model.ResourceModel;
import org.eclipse.lyo.validation.model.ValidationResultModel;
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

    private static String getTypeValidationQuery(String resource, String type) {
        StringBuilder qry = new StringBuilder();
        Queries.prefix("sh", SHACL_NS, qry);
        Queries.prefix(OslcConstants.RDF_NAMESPACE_PREFIX, OslcConstants.RDF_NAMESPACE, qry);
        qry.append("CONSTRUCT {<urn:shape> rdf:type sh:NodeShape; ");
        qry.append("sh:targetNode <");
        qry.append(resource);
        qry.append(">; sh:closed ?closed; ");
        qry.append("sh:property ?dstPropDef . ?dstPropDef ?prp ?val} ");
        qry.append("WHERE {?shape rdf:type sh:NodeShape; sh:targetClass <");
        qry.append(type);
        qry.append(">; sh:property _:srcPropDef BIND(BNODE() AS ?dstPropDef). ");
        qry.append("_:srcPropDef ?prp ?val . OPTIONAL {?shape sh:closed ?closed} } ");
        return qry.toString();
    }

    private static Model getShapeForTypeInstance(String resource, String type, Model shapes) {
        Query query = QueryFactory.create(getTypeValidationQuery(resource, type));
        try(QueryExecution qe = QueryExecutionFactory.create(query, shapes)) {
            return qe.execConstruct();
        }
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

    /**
     * Validates a resource against a single instance-type bounded constraints
     * model. This method assumes each rdf:type is validated at once.
     * @param resource the resource to validate.
     * @param typeConstrainsts single instance-type bounded constraints model.
     * @throws ValidationException if {@code resource} have not passed all
     * validations for the given type.
     */
    private void validate(Resource resource, Model typeConstrainsts) {
        StringBuilder sb;
        ValidationResultModel result;
        Iterator<ResourceModel> iterator;
        try {
            result = innerValidator.validate(resource.getModel(), typeConstrainsts);
            if (result.getInvalidResourceCount() > 0) {
                sb = new StringBuilder("[");
                iterator = result.getInvalidResources().iterator();
                sb.append(iterator.next().getResult().toJsonString2spaces());
                while(iterator.hasNext()) {
                    sb.append(',');
                    sb.append(iterator.next().getResult().toJsonString2spaces());
                }
                sb.append(']');
                throw new ValidationException(sb.toString(), MediaType.APPLICATION_JSON);
            }
        } catch(ReflectiveOperationException | DatatypeConfigurationException |
                OslcCoreApplicationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Validates a resource against all the constraints defined for each of its
     * types. Constraints for a particular resource type are validated at once.
     * @param resource the resource to validate.
     * @param types all resource's types.
     * @param shapes the complete constraints rules.
     * @throws ValidationException if working on closed-world assumption and
     * there are no rules defined for a particular resource type or
     * {@code resource} have not passed all validations for a given type.
     */
    private void validate(Resource resource, List<String> types, Model shapes) {
        Model typeInstanceShape;
        for(String type : types) {
            typeInstanceShape = getShapeForTypeInstance(resource.getURI(), type, shapes);
            LOG.info("Validating Resource : {} with shape: {}", resource, typeInstanceShape.getGraph().toString());
            if (typeInstanceShape.size() > 0)
                validate(resource, typeInstanceShape);
            else if (!openWorldAssumption)
                throw new ValidationException("Unknow resource type: " + type);
        }
    }

    @Override
    public final void validate(Resource resource) {
        List<String> types = Queries.types(resource);
        if (fetchingValidationModels)
            throw new FetchingRulesException();
        LOG.info("Validating Resource : {} with types: {} and constraints: {}", resource, types, shaclConstraints.getGraph().toString());
        validate(resource, types, shaclConstraints);
    }

    @Override
    public void validate(Model model) {
        ResIterator list = model.listSubjects();
        while (list.hasNext()) {
            Resource resource = list.next();
            LOG.info("Validating Resource : {}", resource);
            validate(resource);
            // validate the about property
        }
    }

}