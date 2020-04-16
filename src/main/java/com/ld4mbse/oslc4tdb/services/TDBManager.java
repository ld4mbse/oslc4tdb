package com.ld4mbse.oslc4tdb.services;

import com.ld4mbse.oslc4tdb.model.OSLCModel;
import com.ld4mbse.oslc4tdb.model.SHACLModel;
import com.ld4mbse.oslc4tdb.tdb.query.QueryCriteria;
import com.ld4mbse.oslc4tdb.tdb.validation.ShaclValidator;
import com.ld4mbse.oslc4tdb.util.Models;
import com.ld4mbse.oslc4tdb.util.Requests;
import com.ld4mbse.oslc4tdb.util.Warehouses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.context.ApplicationScoped;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.Lock;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ld4mbse.oslc4tdb.util.OslcShaclAdapter.getResourceShape;
import static com.ld4mbse.oslc4tdb.util.OslcShaclAdapter.getSimpleId;

/**
 * TDB implementation for {@link RDFManager}.
 * @author rherrera
 */
@ApplicationScoped
public class TDBManager extends Observable implements RDFManager {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TDBManager.class);
    /**
     * A regex for an ID.
     */
    private static final String ID_REGEX;
    /**
     * A regex for an URL.
     */
    private static final String URL_REGEX;
    /**
     * A regex for a PREFIXED:ID.
     */
    private static final String PREFIXED_ID_REGEX;
    /**
     * A regex for an IRI_ID.
     */
    private static final String IRI_ID_REGEX;
    /**
     * A regex for a VALUE.
     */
    private static final String VALUE_REGEX;
    /**
     * A regex for a WHERE TERM.
     */
    private static final String WHERE_TERM_REGEX;
    /**
     * A regex for the WHERE EXPRESSION.
     */
    private static final String WHERE_EXP_REGEX;
    /**
     * A regex for a SELECT TERM.
     */
    private static final String SELECT_TERM_REGEX;
    /**
     * A regex for the SELECT EXPRESSION.
     */
    private static final String SELECT_EXP_REGEX;

    private boolean updatingSHACL;

    /**
     * Initialization block.
     */
    static {
        ID_REGEX = "([a-zA-Z]\\w*)";
        URL_REGEX = "<https?://.+?>";
        PREFIXED_ID_REGEX = ID_REGEX + ":" +ID_REGEX;
        IRI_ID_REGEX = URL_REGEX + "|" + PREFIXED_ID_REGEX;
        SELECT_TERM_REGEX = "(" + IRI_ID_REGEX + ")";
        VALUE_REGEX = "(\".+?\"|\\d+|" + IRI_ID_REGEX  +")";
        WHERE_TERM_REGEX = SELECT_TERM_REGEX + "([=~])" + VALUE_REGEX;
        WHERE_EXP_REGEX = WHERE_TERM_REGEX + "( and " + WHERE_TERM_REGEX + ")*";
        SELECT_EXP_REGEX = SELECT_TERM_REGEX + "(," + SELECT_TERM_REGEX + ")*";
    }

    /**
     * Constructs a default instance.
     */
    public TDBManager() {
        super();
    }

    @Override
    public Model getModels(String url) {
        Resource resource;
        Model uris = ModelFactory.createDefaultModel();
        uris.setNsPrefix(OslcConstants.OSLC_CORE_NAMESPACE_PREFIX, OslcConstants.OSLC_CORE_NAMESPACE);
        uris.setNsPrefix(OslcConstants.DCTERMS_NAMESPACE_PREFIX, DCTerms.NS);
        uris.setNsPrefix(OslcConstants.RDFS_NAMESPACE_PREFIX, RDFS.uri);
        uris.setNsPrefix(OslcConstants.RDF_NAMESPACE_PREFIX, RDF.uri);
        uris.setNsPrefix("xsd", OslcConstants.XML_NAMESPACE);

        String[] directories = Warehouses.list();

        for (String store : directories) {
            resource = uris.createResource(Requests.buildURI(url, store, "catalog"));
            uris.add(resource, RDF.type, OSLCModel.PROPS.SERVICE_PROVIDER.TYPE);
        }

        return uris;
    }

    @Override
    public Model getModels(String warehouse, String pattern, String url) {
        Lock lock;
        Model uris;
        String name;
        Iterator<String> names;
        LOG.debug("> ? model match {}", pattern);
        Dataset dataset = Warehouses.get(warehouse);
        dataset.begin(ReadWrite.READ);
        lock = dataset.getLock();
        try {
            lock.enterCriticalSection(Lock.READ);
            uris = ModelFactory.createDefaultModel();
            names = dataset.listNames();
            while(names.hasNext()) {
                name = names.next();
                if (name.contains("-shacl")) {
                    name = name.substring(name.indexOf(":") + 1);
                    if (pattern == null || pattern.isEmpty() || name.matches(pattern)) {
                        uris.add(ResourceFactory.createResource(url + name.replace("-shacl", "")), RDF.type, RDFS.Resource);
                    }
                }
            }
            if (uris.size() > 0) {
                uris.setNsPrefix("rdfs", RDFS.getURI());
                uris.setNsPrefix("rdf", RDF.getURI());
            }
            dataset.commit();
            LOG.debug("< {} ", uris.size());
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            lock.leaveCriticalSection();

            TDBFactory.release(dataset);
            LOG.info("{} Dataset directory released", warehouse);
        }
        return uris;
    }

    @Override
    public boolean containsModel(String warehouse, String model) {
        Lock lock;
        boolean contains = true;
        LOG.debug("> EXISTS model @ {}", model);
        Dataset dataset = Warehouses.get(warehouse);
        LOG.info("{} Dataset directory connected", warehouse);
        dataset.begin(ReadWrite.READ);
        lock = dataset.getLock();
        try {
            lock.enterCriticalSection(Lock.READ);
            contains = dataset.containsNamedModel(model);
            dataset.commit();
            LOG.debug("< {} ", contains);
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            lock.leaveCriticalSection();

            TDBFactory.release(dataset);
            LOG.info("{} Dataset directory released", warehouse);

        }
        return contains;
    }

    public void setSHACLModel(String catalog, Model shacl, String uri) {

        Lock lock;
        LOG.debug("> + model @ {}", uri);
        Dataset dataset = Warehouses.get(catalog);
        LOG.info("{} Dataset directory connected", catalog);
        dataset.begin(ReadWrite.WRITE);
        lock = dataset.getLock();
        LOG.debug("> + updatingSHACL @ {}", updatingSHACL);

        Resource shaclShape, shaclTarget, resourceShape;
        ResIterator shaclShapes;
        String simpleId;
        Statement statement;

        try {
            lock.enterCriticalSection(Lock.WRITE);

            updatingSHACL = true;

            Map<String, Resource> resourceShapes = new HashMap<>();
            Set<String> uniqueTypes = new HashSet<>();
            Set<String> uniqueShapes = new HashSet<>();

            uniqueTypes.clear();
            uniqueShapes.clear();

            shaclShapes = shacl.listResourcesWithProperty(RDF.type, SHACLModel.TYPES.NODE_SHAPE);

            while(shaclShapes.hasNext()) {

                shaclShape = shaclShapes.next();
                LOG.info("@shaclShape/{}", shaclShape);

                statement = shaclShape.getProperty(SHACLModel.PATHS.TARGET_CLASS);
                if (statement == null)
                    shaclTarget = shaclShape;
                else
                    shaclTarget = statement.getObject().asResource();

                statement = shaclShape.getProperty(RDFS.label);
                if (statement == null)
                    simpleId = getSimpleId(shaclTarget.getURI());
                else
                    simpleId = statement.getObject().asLiteral().getString();

                if (!uniqueShapes.add(simpleId))
                    throw new IllegalStateException("Simple shape name '" + simpleId + "' is repeated on SHACL models (" + shaclShape.getURI() + "), use rdfs:label to create an alias.");

                if (!uniqueTypes.add(shaclTarget.getURI()))
                    throw new IllegalStateException("sh:targetClass '" + shaclTarget.getURI() + "' already defined, only one sh:NodeShape per sh:targetClass is allowed.");

                resourceShape = resourceShapes.get(simpleId);
                if (resourceShape == null) {
                    resourceShape = getResourceShape(simpleId, shaclTarget.getURI(), shaclShape, shacl, uri, catalog);
                    resourceShapes.put(simpleId, resourceShape);
                }
            }

            dataset.replaceNamedModel(uri + "-shacl", shacl);
            LOG.info("{} SHACL Definition created", uri + "-shacl");

            setChanged();
            notifyObservers(catalog);
            updatingSHACL = false;

            dataset.commit();
            LOG.debug("< [+] {} statements", shacl.size());
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            lock.leaveCriticalSection();

            TDBFactory.release(dataset);
            LOG.info("{} Dataset directory released", catalog);
        }
    }

    public void setModel(String catalog, Model model, String uri) {
        Lock lock;
        boolean mustUpdateOLSCmodel;
        LOG.debug("> + model @ {}", uri);
        Dataset dataset = Warehouses.get(catalog);
        LOG.info("{} Dataset directory connected", catalog);
        dataset.begin(ReadWrite.WRITE);
        lock = dataset.getLock();
        LOG.debug("> + updatingSHACL @ {}", updatingSHACL);
        try {
            lock.enterCriticalSection(Lock.WRITE);
            if (uri == null) {

                updatingSHACL = true;
                dataset.getDefaultModel().removeAll();
                dataset.getDefaultModel().add(model);
                setChanged();
                notifyObservers(catalog);
                updatingSHACL = false;

            } else {
                Model validationRules = dataset.getDefaultModel();
                ShaclValidator validator = new ShaclValidator(false);

                if (!updatingSHACL) {
                    validator.update(null, validationRules);
                    validator.validate(model);
                    mustUpdateOLSCmodel = !dataset.containsNamedModel(uri);
                    dataset.replaceNamedModel(uri, model);
                    if (mustUpdateOLSCmodel) {
                        setChanged();
                        notifyObservers(catalog);
                    }
                } else {
                    throw new IllegalStateException("SHACL Models are being updated, please try later.");
                }

            }
            dataset.commit();
            LOG.debug("< [+] {} statements", model.size());
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            lock.leaveCriticalSection();

            TDBFactory.release(dataset);
            LOG.info("{} Dataset directory released", catalog);
        }
    }

    public void addModel(String warehouse, Model model, String uri) {
        Lock lock;
        boolean mustUpdateOLSCmodel;
        LOG.debug("> + model @ {}", uri);
        Dataset dataset = Warehouses.get(warehouse);
        LOG.info("{} Dataset directory connected", warehouse);
        dataset.begin(ReadWrite.WRITE);
        lock = dataset.getLock();
        LOG.debug("> + updatingSHACL @ {}", updatingSHACL);
        try {
            lock.enterCriticalSection(Lock.WRITE);
            if (uri == null) {
                throw new IllegalStateException("URI does not specified.");
            } else {
                Model validationRules = dataset.getNamedModel(uri + "-shacl");
                ShaclValidator validator = new ShaclValidator(false);

                if (!updatingSHACL) {
                    validator.update(null, validationRules);
                    // validator.validate(model);
                    mustUpdateOLSCmodel = !dataset.containsNamedModel(uri);
                    dataset.addNamedModel(uri, model);
                    if (mustUpdateOLSCmodel) {
                        setChanged();
                        notifyObservers(warehouse);
                    }
                } else {
                    throw new IllegalStateException("SHACL Models are being updated, please try later.");
                }

            }
            dataset.commit();
            LOG.debug("< [+] {} statements", model.size());
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            lock.leaveCriticalSection();

            TDBFactory.release(dataset);
            LOG.info("{} Dataset directory released", warehouse);
        }

    }

    public Model getModel(String warehouse, String uri) {
        Model buffer, model = null;
        LOG.debug("> ? model @ {}", uri);
        Dataset dataset = Warehouses.get(warehouse);
        dataset.begin(ReadWrite.READ);
        try {
            if (uri == null)
                model = dataset.getDefaultModel();
            else
                model = dataset.getNamedModel(uri);
            model.enterCriticalSection(Lock.READ);
            buffer = ModelFactory.createDefaultModel();
            buffer.add(model);
            Models.importNamespacesPrefixes(model, buffer);
            dataset.commit();
            LOG.debug("< {} statements", buffer.size());
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            if (model != null) {
                model.leaveCriticalSection();
            }
            TDBFactory.release(dataset);
        }
        return buffer;
    }

    /**
     * Adds a prefix declaration for the building query.
     * @param prefix the prefix to add.
     * @param prefixes the prefixes statement collection.
     * @param src the source model.
     */
    private void declarePrefix(String prefix, StringBuilder prefixes, Model src) {
        String ns;
        if (prefix != null) {
            ns = src.getNsPrefixURI(prefix);
            if (ns == null)
                throw new IllegalArgumentException("Unknow prefix: " + prefix);
            if (prefixes.indexOf(ns) < 0) {
                prefixes.append("PREFIX ");
                prefixes.append(prefix);
                prefixes.append(": <");
                prefixes.append(ns);
                prefixes.append("> \n");
            }
        }
    }

    /**
     * Breaks the where parameter into a sparql query.
     * @param where the where clause.
     * @param source the source model.
     * @return a map of properties to be matched.
     */
    private String getQuery(String where, Model source) {
        int index;
        Pattern pattern;
        Matcher matcher;
        String fullTextVar;
        StringBuilder prefixes = new StringBuilder("");
        StringBuilder query = new StringBuilder("\nSELECT ?s ?p ?o WHERE {\n ?s ?p ?o");
        if (where != null) {
            if (where.matches(WHERE_EXP_REGEX)) {
                pattern = Pattern.compile(WHERE_TERM_REGEX);
                matcher = pattern.matcher(where);
                while(matcher.find()) {
                    query.append(" .\n ?s ");
                    query.append(matcher.group(1));
                    query.append(' ');
                    declarePrefix(matcher.group(2), prefixes, source);
                    if (matcher.group(4).equals("=")) {
                        query.append(matcher.group(5));
                        declarePrefix(matcher.group(6), prefixes, source);
                    } else {
                        fullTextVar = matcher.group(3);
                        if (fullTextVar == null) {
                            fullTextVar = matcher.group(1);
                            index = fullTextVar.indexOf("#");
                            if (index < 0) index = fullTextVar.lastIndexOf("/");
                            fullTextVar = fullTextVar.substring(index + 1, fullTextVar.length() - 1);
                        }
                        query.append('?');
                        query.append(fullTextVar);
                        query.append(" ;\n\tFILTER regex(?");
                        query.append(fullTextVar);
                        query.append(", ");
                        query.append(matcher.group(5));
                        query.append(", \"i\")");
                    }
                    LOG.trace("[+] CND {}", matcher.group(0));
                }
            } else
                throw new IllegalArgumentException("Invalid where syntax, must match: " + WHERE_EXP_REGEX);
        }
        query.append("\n}");
        prefixes.append(query);
        where = prefixes.toString();
        return where;
    }

    /**
     * Gets the collection of properties to be projected.
     * @param select the select clause.
     * @param source the source model.
     * @return the collection of properties to be projected.
     */
    private Set<String> getProjection(String select, Model source) {
        Pattern pattern;
        Matcher matcher;
        String prefix, ns, uri;
        Set<String> projection = new HashSet<>();
        if (select != null) {
            if (select.matches(SELECT_EXP_REGEX)) {
                pattern = Pattern.compile(SELECT_TERM_REGEX);
                matcher = pattern.matcher(select);
                while(matcher.find()) {
                    prefix = matcher.group(2);
                    if (prefix == null) {
                        uri = matcher.group(1);
                        projection.add(uri.substring(1, uri.length()-1));
                    } else {
                        ns = source.getNsPrefixURI(prefix);
                        if (ns == null)
                            throw new IllegalArgumentException("Unknow prefix: " + prefix);
                        projection.add(ns + matcher.group(3));
                    }
                    LOG.trace("[+] PRJ {}", matcher.group(0));
                }
            } else
                throw new IllegalArgumentException("Invalid select syntax, must match: " + SELECT_EXP_REGEX);
        }
        return projection;
    }

    public Model getModel(String warehouse, String uri, String where, String select) {
        Query query;
        RDFNode object;
        Property property;
        String queryString;
        Set<String> projection;
        QuerySolution statement;
        Resource subject, predicate;
        Model filtered, source = getModel(warehouse, uri);
        filtered = ModelFactory.createDefaultModel();
        LOG.debug("> WHERE [{}]", where);
        LOG.debug("> SELECT [{}]", select);
        queryString = getQuery(where, source);
        projection = getProjection(select, source);
        LOG.debug("> {}/SPARQL\n\n{}\n", source.size(), queryString);
        query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, source)) {
            ResultSet results = qexec.execSelect() ;
            while (results.hasNext()) {
                statement = results.nextSolution();
                subject = statement.getResource("s");
                predicate = statement.getResource("p");
                if (projection.isEmpty() || projection.contains(predicate.getURI())) {
                    object = statement.get("o");
                    property = ResourceFactory.createProperty(predicate.getURI());
                    filtered.add(subject, property, object);
                }
            }
            Models.importNamespacesPrefixes(source, filtered);
            LOG.debug("< SPARQL/{}", results.getRowNumber());
        }
        return filtered;
    }

    public Model getResource(String warehouse, String uri, String model) {
        Resource finding;
        Model buffer, source = null;
        LOG.debug("> ? {} @ {}", uri, model);
        Dataset dataset = Warehouses.get(warehouse);
        dataset.begin(ReadWrite.READ);
        try {
            if (model == null)
                source = dataset.getDefaultModel();
            else
                source = dataset.getNamedModel(Models.getStoreURN(model));
            source.enterCriticalSection(Lock.READ);
            buffer = ModelFactory.createDefaultModel();
            finding = ResourceFactory.createResource(uri);
            buffer.add(source.query(new SimpleSelector(finding, null, (String)null)));
            Models.importNamespacesPrefixes(source, buffer);
            dataset.commit();
            LOG.debug("< {} statements", buffer.size());
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            if (source != null) {
                source.leaveCriticalSection();
            }
            TDBFactory.release(dataset);
        }
        return buffer;
    }

    public void removeModel(String warehouse, String uri) {
        Lock lock;
        LOG.debug("> + model @ {}", uri);
        Dataset dataset = Warehouses.get(warehouse);
        dataset.begin(ReadWrite.WRITE);
        lock = dataset.getLock();
        try {
            lock.enterCriticalSection(Lock.WRITE);
            if (uri == null)
                dataset.getDefaultModel().removeAll();
            else
                dataset.removeNamedModel(uri);
            setChanged();
            notifyObservers(warehouse);
            dataset.commit();
            LOG.debug("< 0 statements");
        } catch(Exception ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            lock.leaveCriticalSection();
            TDBFactory.release(dataset);
        }
    }

    @Override
    public Model getResource(String warehouse, String store, String model, String id) {
        Model graph = getModel(warehouse, model);
        Model resource = graph.query(new SimpleSelector());
        Models.importNamespacesPrefixes(graph, resource);
        return resource;
    }

    public Model getResources(String warehouse, String model) {
        Model resources, graph = getModel(warehouse, model);
        resources = graph.query(new SimpleSelector(null, RDF.type, (String)null));
        Models.importNamespacesPrefixes(graph, resources);
        return resources;
    }

    public Map<String, String> getNSPrefixes(String warehouse, String uri) {
        Model model = getModel(warehouse, uri);
        return model.getNsPrefixMap();
    }

    @Override
    public void setResource(String warehouse, Resource resource, String model) {
        Model target = null;

        Dataset dataset = Warehouses.get(warehouse);
        ShaclValidator validator = new ShaclValidator(false);
        Model validationContext = ModelFactory.createDefaultModel();
        model = Models.getStoreURN(model);
        LOG.debug("> + {} @ {}", resource.getURI(), model);
        dataset.begin(ReadWrite.WRITE);
        try {
            target = dataset.getNamedModel(model);
            validationContext.add(target);
            validationContext.removeAll(resource, null, null);
            validationContext.add(resource.listProperties());
            resource = validationContext.getResource(resource.getURI());
            target.enterCriticalSection(Lock.WRITE);

            Model val = dataset.getNamedModel(model + "-shacl");

            validator.update(null, val);
            // validator.validate(resource);
            target.removeAll(resource, null, null);
            target.add(resource.listProperties());
            dataset.commit();
            LOG.debug("< {} saved @ {}", resource.getURI(), model);
        } catch(RuntimeException ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            if (target != null) {
                target.leaveCriticalSection();
            }
            TDBFactory.release(dataset);
        }
    }

    @Override
    public Model search(String warehouse, QueryCriteria criteria, String store, String base) {
        Model buffer;
        Dataset dataset = Warehouses.get(warehouse);
        String query = criteria.getSparqlQuery(base, store);
        dataset.begin(ReadWrite.READ);
        try {
            dataset.getLock().enterCriticalSection(Lock.READ);
            buffer = OSLCModel.search(query, base, dataset);
            dataset.getLock().leaveCriticalSection();
            dataset.commit();
            LOG.debug("< {} statements", buffer.size());
        } catch(RuntimeException ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            TDBFactory.release(dataset);
        }
        return buffer;
    }

    @Override
    public void removeResource(String warehouse, Resource resource, String model) {
        Model target = null;
        Dataset dataset = Warehouses.get(warehouse);
        model = Models.getStoreURN(model);
        LOG.debug("> - {} @ {}", resource.getURI(), model);
        dataset.begin(ReadWrite.WRITE);
        try {
            target = dataset.getNamedModel(model);
            target.enterCriticalSection(Lock.WRITE);
            target.removeAll(resource, null, null);
            target.removeAll(null, null, resource);
            dataset.commit();
            LOG.debug("< {} removed @ {}", resource.getURI(), model);
        } catch(RuntimeException ex) {
            dataset.abort();
            throw ex;
        } finally {
            dataset.end();
            if (target != null) {
                target.leaveCriticalSection();
            }
            TDBFactory.release(dataset);
        }
    }

}