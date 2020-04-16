package com.ld4mbse.oslc4tdb.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

/**
 * Encapsulates the information of one resource in a way that is easy to show
 * in a user interface (Java Server Page).
 * @author rherrera
 */
public class ResourceDescriptor {
    /**
     * Gets an instance from a container {@link Model}.
     * @param model the container model of the resource.
     * @return the corresponding {@code ResourceDescriptor} to that in model.
     * @throws IllegalArgumentException if the model contains more than one resource.
     * @throws NoSuchElementException if model is empty.
     * @throws URISyntaxException should not happen.
     */
    public static ResourceDescriptor getInstante(Model model, Model shacl)
            throws URISyntaxException {
        Property predicate;
        Statement statement;
        URI currentUri, rscUri = null;
        Resource resource, type = null;
        StmtIterator statements = model.listStatements();
        List<Map.Entry<Property, RDFNode>> properties = new ArrayList<>();
        List<Map.Entry<String, String>> propertiesLabels = new ArrayList<>();
        while(statements.hasNext()) {
            statement = statements.next();
            resource = statement.getSubject();
            currentUri = new URI(resource.getURI());
            if (rscUri == null)
                rscUri = currentUri;
            else if (!rscUri.equals(currentUri))
                throw new IllegalArgumentException("model cannot have more than one resource");
            predicate = statement.getPredicate();
            if (predicate.getURI().equals(RDF.type.getURI()))
                type = statement.getObject().asResource();
            else {
                //properties.add(new AbstractMap.SimpleEntry(predicate, statement.getObject()));
                String labelProperty = "";

                Statement finalStatement = statement;
                SimpleSelector selectorLabel = new SimpleSelector(null, null, (RDFNode) null) {
                    public boolean selects(Statement st) {
                        return st.getObject().toString().equals(finalStatement.getPredicate().toString());
                    }
                };

                StmtIterator iterator = shacl.listStatements(selectorLabel);
                while (iterator.hasNext()) {
                    Statement st = iterator.nextStatement();
                    StmtIterator iterator1 = st.getSubject().listProperties();
                    while (iterator1.hasNext()) {
                        Statement st2 = iterator1.nextStatement();
                        if (st2.getPredicate().toString().contains("label")) {
                            labelProperty = st2.getObject().toString();
                            break;
                        }
                    }

                    if (!labelProperty.isEmpty()) {
                        break;
                    }
                }

                properties.add(new AbstractMap.SimpleEntry(predicate, statement.getObject()));

                if (!labelProperty.isEmpty()) {
                    propertiesLabels.add(new AbstractMap.SimpleEntry(predicate.getURI(), labelProperty));
                }
            }

        }
        return new ResourceDescriptor(rscUri, type, model, properties, propertiesLabels);
    }

    /**
     * The resource's URI.
     */
    private final URI about;
    /**
     * The resource's type.
     */
    private final Resource type;
    /**
     * The container model.
     */
    private final Model model;
    /**
     * The resource's properties.
     */
    private final List<Map.Entry<Property, RDFNode>> properties;

    private final List<Map.Entry<String, String>> propertiesLabels;
    /**
     * Constructs an instance specifying all properties.
     * @param about the resource's URI.
     * @param type the resource's type.
     * @param model the container model.
     * @param properties the resource's properties.
     */
    private ResourceDescriptor(URI about, Resource type, Model model,
                               List<Map.Entry<Property, RDFNode>> properties,
                               List<Map.Entry<String, String>> propertiesLabels) {
        this.properties = properties;
        this.propertiesLabels = propertiesLabels;
        this.model = model;
        this.about = about;
        this.type = type;
    }
    /**
     * Gets the resource's URI.
     * @return the resource's URI.
     */
    public URI getAbout() {
        return about;
    }
    /**
     * Gets the resource's type.
     * @return the resource's type.
     */
    public Resource getType() {
        return type;
    }
    /**
     * Determines whether this resource has an {@code rdf:type} value defined.
     * @return {@code true} if there is a statement with {@code rdf:type} as
     * predicate; {@code false} otherwise.
     */
    public boolean isTypeDefined() {
        return type != null;
    }
    /**
     * Gets the resource's properties.
     * @return the resource's properties.
     */
    public List<Map.Entry<Property, RDFNode>> getProperties() {
        return properties;
    }
    /**
     * Gets the prefixes known by the resource's model.
     * @return the prefixes known by the resource's model.
     */
    public Map<String, String> getPrefixes() {
        return model.getNsPrefixMap();
    }
    /**
     * Gets a prefixed name of a resource/property.
     * @param resource the resource to get its prefixed named.
     * @return the prefixed name of the resource/property.
     */
    public String getPrefixedResource(Resource resource) {
        String ns = resource.getNameSpace();
        String prefix = model.getNsURIPrefix(ns);
        if (prefix == null)
            return resource.getLocalName();
        return prefix + ":" + resource.getLocalName();
    }

    /**
     * Get the label for each propertie in the
     * webview.
     * @param resource The resource with the property and its label.
     * @return String with the label for the property.
     */
    public String getResourceLabel(Resource resource) {
        String ns = resource.getNameSpace();
        String local = resource.getLocalName();
        String value = ns + local;

        String label = "";

        for (Map.Entry<String, String> element : propertiesLabels) {
            if (element.getKey().equals(value)) {
                label = element.getValue();
                break;
            }
        }

        if (label.isEmpty()) {
            label = getPrefixedResource(resource);
        }

        return label;
    }
    /**
     * Gets the resource's type with prefix.
     * @return the resource's type with prefix.
     */
    public String getPrefixedType() {
        String prefixedType = null;
        if (isTypeDefined())
            prefixedType = getPrefixedResource(type);
        return prefixedType;
    }
}