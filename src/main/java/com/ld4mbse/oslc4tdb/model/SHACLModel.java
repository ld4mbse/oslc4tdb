package com.ld4mbse.oslc4tdb.model;

import es.weso.rdf.nodes.IRI;
import es.weso.shacl.SHACLPrefixes;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Encapsulates SHACL vocabulary.
 * @author rherrera
 */
public class SHACLModel {
    /**
     * Extracts the URL from a SHACL IRI definition.
     * @param definition the SHACL IRI definition.
     * @return the contained URL.
     */
    private static String extractURL(IRI definition) {
        String content = definition.toString();
        return content.substring(1, content.length()-1);
    }
    /**
     * Creates a property from a SHACL definition.
     * @param definition the SHACL definition.
     * @return the Jena property.
     */
    public static Property createProperty(IRI definition) {
        String content = extractURL(definition);
        return ResourceFactory.createProperty(content);
    }
    /**
     * Creates a resource from a SHACL definition.
     * @param definition the SHACL definition.
     * @return the Jena resource.
     */
    public static Resource createResource(IRI definition) {
        String content = extractURL(definition);
        return ResourceFactory.createResource(content);
    }
    /**
     * Known property paths.
     */
    public static interface PATHS {
        /**
         * The sh:targetClass property path.
         */
        Property TARGET_CLASS = createProperty(SHACLPrefixes.sh_targetClass());
        /**
         * The sh:property property path.
         */
        Property PROPERTY = createProperty(SHACLPrefixes.sh_property());
        /**
         * The sh:name property path.
         */
        Property NAME = createProperty(SHACLPrefixes.sh_name());
        /**
         * The sh:description property path.
         */
        Property DESCRIPTION = createProperty(SHACLPrefixes.sh_description());
        /**
         * The sh:path property path.
         */
        Property PATH = createProperty(SHACLPrefixes.sh_path());
        /**
         * The sh:minCount property path.
         */
        Property MIN_COUNT = createProperty(SHACLPrefixes.sh_minCount());
        /**
         * The sh:maxCount property path.
         */
        Property MAX_COUNT = createProperty(SHACLPrefixes.sh_maxCount());
        /**
         * The sh:nodeKind property path.
         */
        Property NODE_KIND = createProperty(SHACLPrefixes.sh_nodeKind());
        /**
         * The sh:datatype property path.
         */
        Property DATA_TYPE = createProperty(SHACLPrefixes.sh_datatype());
        /**
         * The sh:class property path.
         */
        Property CLASS = createProperty(SHACLPrefixes.sh_class());
        /**
         * The sh:maxLenght property path.
         */
        Property MAX_LENGTH = createProperty(SHACLPrefixes.sh_maxLength());
        /**
         * The sh:in property path.
         */
        Property IN = createProperty(SHACLPrefixes.sh_in());
    }
    /**
     * Known types.
     */
    public static interface TYPES {
        /**
         * The sh:NodeShape type.
         */
        Resource NODE_SHAPE = createResource(SHACLPrefixes.sh_NodeShape());
        /**
         * sh:NodeKind types.
         */
        public static interface NODE_KIND {
            /**
             * The sh:BlankNode node kind.
             */
            Resource BLANK_NODE = createResource(SHACLPrefixes.sh_BlankNode());
            /**
             * The sh:BlankNodeOrIRI node kind.
             */
            Resource BLANK_NODE_OR_IRI = createResource(SHACLPrefixes.sh_BlankNodeOrIRI());
            /**
             * The sh:IRI node kind.
             */
            Resource IRI = createResource(SHACLPrefixes.sh_IRI());
            /**
             * The sh:Literal node kind.
             */
            Resource LITERAL = createResource(SHACLPrefixes.sh_Literal());
        }
    }

}
