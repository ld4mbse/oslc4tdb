package com.ld4mbse.oslc4tdb.tdb.query;

import com.ld4mbse.oslc4tdb.util.Models;
import com.ld4mbse.oslc4tdb.util.Queries;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.QueryCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates criteria to execute a {@link QueryCapability} search.
 * Contains the logic to parse {@code oslc.where}, {@code oslc.select} and
 * {@code oslc.prefix} sintaxes.
 * @author rherrera
 */
public class QueryCriteria {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(QueryCriteria.class);
    /**
     * Regex to denote a regular letter.
     */
    private static final String LETTER = "A-Za-z";
    /**
     * Regex to denote a digit.
     */
    private static final String DIGIT = "0-9";
    /**
     * Regex to denote a decimal.
     * @see https://www.w3.org/TR/xmlschema-2/
     */
    private static final String DECIMAL = "[+-]?[" + DIGIT + "]+(\\.[" + DIGIT + "]+)?([eE][+-]?[" + DIGIT + "]+)?";
    /**
     * Regex to denote an alphanumeric character.
     */
    private static final String ALPHANUMERIC = LETTER + DIGIT;
    /**
     * @see https://www.w3.org/TR/rdf-sparql-query/#rLANGTAG
     */
    private static final String LANGTAG = "@[" + LETTER + "](-?[" + ALPHANUMERIC + "])*";
    /**
     * @see https://www.w3.org/TR/rdf-sparql-query/#rPN_PREFIX
     */
    private static final String PN_PREFIX = "[" + LETTER + "](\\.?[" + ALPHANUMERIC + "_-])*";
    /**
     * @see https://www.w3.org/TR/rdf-sparql-query/#rIRI_REF
     */
    private static final String IRI_REF = "<(([^<>\"{}\\|`\\\\])+)>";
    /**
     * @see http://open-services.net/bin/view/Main/OslcCoreSpecification#oslc_prefix
     */
    private static final String PREFIX_DEF = "(" + PN_PREFIX + ")=" + IRI_REF;
    /**
     * Valid {@code PrefixedName} regex.
     * @see http://www.w3.org/TR/rdf-sparql-query/#rPrefixedName
     */
    private static final String PREFIXED_NAME = PN_PREFIX + ":[" + ALPHANUMERIC + "_](\\.?[" + ALPHANUMERIC +"_-])*";
    /**
     * Valid {@code identifier_wc} regex.
     * @see http://open-services.net/bin/view/Main/OSLCCoreSpecQuery#oslc_where
     */
    private static final String IDENTIFIER = "\\*|" + PREFIXED_NAME;
    /**
     * Valid operators for comparison regex.
     * @see http://open-services.net/bin/view/Main/OSLCCoreSpecQuery#oslc_where
     */
    private static final String OPERATOR = "(([!<>]?=)|[<>])";
    /**
     * An escaped string regex.
     */
    private static final String STRING_ESC = "\".*?\"";
    /**
     * Valid VALUE regex.
     * @see http://open-services.net/bin/view/Main/OSLCCoreSpecQuery#oslc_where
     */
    private static final String VALUE = "(" + IRI_REF + "|true|false|" + DECIMAL + "|" + STRING_ESC + "(" + LANGTAG + "|\\^\\^" + PREFIXED_NAME + ")?)";
    /**
     * A single comparison expression regex.
     */
    private static final String COMPARISION = OPERATOR + VALUE;
    /**
     * A IN expression regex.
     */
    private static final String IN = " (in) \\[((.+?))\\]";
    /**
     * A SCOPED TERM expression regex.
     */
    private static final String SCOPED_TERM = "\\{((.+))\\}";
    /**
     * A single TERM regex.
     */
    private static final String TERM = "(" + IDENTIFIER + ")((" + COMPARISION + ")|(" + IN + ")|(" + SCOPED_TERM + "))";
    /**
     * A single PROPERTY regex.
     */
    private static final String PROPERTY = "(" + IDENTIFIER + ")((" + SCOPED_TERM + ")?)";
    /**
     * A single SORT_TERM regex.
     */
    private static final String SORT_TERM = "((([+-])(" + IDENTIFIER + "))|((" + IDENTIFIER + ")(" + SCOPED_TERM + ")))";
    /**
     * Creates an instance specifying the target resource type to retrieve.
     * @param type target resource type to retrieve.
     * @return the simplest {@code QueryCriteria} instance.
     */
    public static QueryCriteria type(Resource type) {
        return new QueryCriteria(type == null ? null : type.getURI());
    }
    /**
     * The target resource type to retrieve.
     */
    private final String type;
    /**
     * Additional prefixes to use in query.
     */
    private Map<String, String> prefixes;
    /**
     * Projection properties.
     */
    private List<Property> properties;
    /**
     * Conditional conditions.
     */
    private List<Condition> conditions;
    /**
     * Sort keys.
     */
    private List<SortKey> sortKeys;
    /**
     * Constructs an instance specifying the target resource type to retrieve.
     * @param type target resource type URL to retrieve.
     */
    private QueryCriteria(String type) {
        this.properties = Collections.EMPTY_LIST;
        this.sortKeys = Collections.EMPTY_LIST;
        this.prefixes = Collections.EMPTY_MAP;
        this.conditions = Collections.EMPTY_LIST;
        this.type = type;
    }
    /**
     * Sets the extra prefixes to use in the query.
     * @param prefixes the extra prefixes to use in the query.
     * @throws NullPointerException if {@code prefixes} is {@code null}.
     */
    public void setPrefixes(Map<String, String> prefixes) {
        this.prefixes = Objects.requireNonNull(prefixes, "prefixes cannot be null");
    }
    /**
     * Parses the {@code oslc.prefix} parameter to
     * {@link #setPrefixes(java.util.Map) set the prefixes} of this instance.
     * @param prefixParameter the {@code oslc.prefix} parameter.
     * @return this instance.
     * @throws IllegalArgumentException if {@code prefixParameter} is bad
     * formed.
     */
    public QueryCriteria prefixes(String prefixParameter) {
        Pattern pattern;
        Matcher matcher;
        Map<String, String> prfxs;
        if (prefixParameter == null || prefixParameter.isEmpty())
            prfxs = Collections.EMPTY_MAP;
        else if (!prefixParameter.matches(PREFIX_DEF + "(," + PREFIX_DEF + ")*"))
            throw new IllegalArgumentException("Bad formed oslc.prefix: " + prefixParameter);
        else {
            prfxs = new HashMap<>();
            pattern = Pattern.compile(",?" + PREFIX_DEF);
            matcher = pattern.matcher(prefixParameter);
            while(matcher.find()) {
                prfxs.put(matcher.group(3), matcher.group(1));
                LOG.trace("[+] PREFIX <{}>={}", matcher.group(3), matcher.group(1));
            }
        }
        setPrefixes(prfxs);
        return this;
    }
    /**
     * Sets the projection properties to use in the query.
     * @param properties the projection properties to use in the query.
     */
    public void setProperties(List<Property> properties) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
    }
    /**
     * Decomposes a {@code compound_term} into a list of conditions.
     * @param properties the properties term.
     * @return decomposed properties.
     */
    private List<Property> properties(String properties) {
        String property;
        Pattern pattern;
        Matcher matcher;
        List<Property> localProperties;
        if (properties == null || properties.isEmpty())
            localProperties = Collections.EMPTY_LIST;
        else if (!properties.matches(PROPERTY + "(," + PROPERTY + ")*"))
            throw new IllegalArgumentException("Bad formed properties: " + properties);
        else {
            localProperties = new ArrayList<>();
            pattern = Pattern.compile(",?" + PROPERTY);
            matcher = pattern.matcher(properties);
            while(matcher.find()) {
                property = matcher.group(1);
                if (matcher.group(6) == null)
                    localProperties.add(new Property(property));
                else
                    localProperties.add(new Property(property, properties(matcher.group(6))));
            }
        }
        return localProperties;
    }
    /**
     * Parses the {@code oslc.select} parameter to
     * {@link #setProperties(java.util.List) set the selection properties} of
     * this instance.
     * @param selectParameter the {@code oslc.where} parameter.
     * @return this instance.
     * @throws IllegalArgumentException if {@code selectParameter} is bad
     * formed.
     */
    public QueryCriteria select(String selectParameter) {
        setProperties(properties(selectParameter));
        return this;
    }
    /**
     * Gets the conditions to use in the query.
     * @param conditions the conditions to use in the query.
     */
    public void setConditions(List<Condition> conditions) {
        this.conditions = Objects.requireNonNull(conditions, "conditions cannot be null");
    }
    /**
     * Decomposes a {@code in_val} into a list of values.
     * @param inVal the compound {@code in_val}.
     * @return decomposed values.
     */
    private String[] in_val(String inVal) {
        List<String> values = new ArrayList<>();
        if (!inVal.matches(VALUE + "(," + VALUE + ")*"))
            throw new IllegalArgumentException("Bad formed in_val: " + inVal);
        Pattern pattern = Pattern.compile(",?" + VALUE);
        Matcher matcher = pattern.matcher(inVal);
        while(matcher.find())
            values.add(matcher.group(1));
        return values.toArray(new String[0]);
    }
    /**
     * Decomposes a {@code compound_term} into a list of conditions.
     * @param compoundTerm the compound term.
     * @return decomposed conditions.
     */
    private List<Condition> compound_term(String compoundTerm) {
        String property;
        Pattern pattern;
        Matcher matcher;
        List<Condition> localConditions;
        if (compoundTerm == null || compoundTerm.isEmpty())
            localConditions = Collections.EMPTY_LIST;
        else if (!compoundTerm.matches(TERM + "( and " + TERM + ")*"))
            throw new IllegalArgumentException("Bad formed compoundTerm: " + compoundTerm);
        else {
            localConditions = new ArrayList<>();
            pattern = Pattern.compile("( and )?" + TERM);
            matcher = pattern.matcher(compoundTerm);
            while(matcher.find()) {
                property = matcher.group(2);
                if (matcher.group(23) != null)
                    localConditions.add(new Condition(property, compound_term(matcher.group(23))));
                else if (matcher.group(19) != null)
                    localConditions.add(new Condition(property, matcher.group(19), in_val(matcher.group(20))));
                else
                    localConditions.add(new Condition(property, matcher.group(7), matcher.group(9)));
            }
        }
        return localConditions;
    }
    /**
     * Parses the {@code oslc.where} parameter to
     * {@link #setConditions(java.util.List) set the conditions} of this instance.
     * @param whereParameter the {@code oslc.where} parameter.
     * @return this instance.
     * @throws IllegalArgumentException if {@code whereParameter} is bad formed.
     */
    public QueryCriteria where(String whereParameter) {
        setConditions(compound_term(whereParameter));
        return this;
    }
    /**
     * Sets the sort keys to use in the query.
     * @param sortKeys the sort keys to use in the query.
     */
    public void setSortKeys(List<SortKey> sortKeys) {
        this.sortKeys = Objects.requireNonNull(sortKeys, "sortKeys cannot be null");
    }
    /**
     * Decomposes a {@code sort_terms} into a list of sort terms.
     * @param sortTerms the sort_terms term.
     * @return decomposed sort keys.
     */
    private List<SortKey> sort_terms(String sortTerms) {
        Pattern pattern;
        Matcher matcher;
        List<SortKey> localSortKeys;
        if (sortTerms == null || sortTerms.isEmpty())
            localSortKeys = Collections.EMPTY_LIST;
        else if (!sortTerms.matches(SORT_TERM + "(," + SORT_TERM + ")*"))
            throw new IllegalArgumentException("Bad formed sort_terms: " + sortTerms);
        else {
            localSortKeys = new ArrayList<>();
            pattern = Pattern.compile(",?" + SORT_TERM);
            matcher = pattern.matcher(sortTerms);
            while(matcher.find()) {
                if (matcher.group(12) == null)
                    localSortKeys.add(new SortKey(matcher.group(4), matcher.group(3)));
                else
                    localSortKeys.add(new SortKey(matcher.group(8), sort_terms(matcher.group(12))));
            }
        }
        return localSortKeys;
    }
    /**
     * Parses the {@code oslc.orderBy} parameter to
     * {@link #setSortKeys(java.util.List) set the sort keys} of this instance.
     * @param orderBy the {@code oslc.orderBy} parameter.
     * @return this instance.
     * @throws IllegalArgumentException if {@code orderBy} is bad formed.
     */
    public QueryCriteria orderBy(String orderBy) {
        setSortKeys(sort_terms(orderBy));
        return this;
    }
    /**
     * Gets the OSLC SPARQL query equivalent to this criteria.
     * @param base the URL for the OSLC queryBase resource.
     * @param graphs the target graphs.
     * @return the corresponding OSLC SPARQL query.
     */
    public String getSparqlQuery(String base, String... graphs) {
        String match = "?M";
        StringBuilder sb = new StringBuilder();
        Map<String, String> finalPrefixes = new HashMap<>(prefixes);
        finalPrefixes.put(OslcConstants.DCTERMS_NAMESPACE, OslcConstants.DCTERMS_NAMESPACE_PREFIX);
        finalPrefixes.put(OslcConstants.OSLC_CORE_DOMAIN, OslcConstants.OSLC_CORE_NAMESPACE_PREFIX);
        finalPrefixes.put(OslcConstants.RDFS_NAMESPACE, OslcConstants.RDFS_NAMESPACE_PREFIX);
        finalPrefixes.put(OslcConstants.RDF_NAMESPACE, OslcConstants.RDF_NAMESPACE_PREFIX);
        finalPrefixes.put(OslcConstants.XML_NAMESPACE, "xsd");
        finalPrefixes.forEach((namespace, prefix) -> {
            Queries.prefix(prefix, namespace, sb);
        });
        sb.append("CONSTRUCT {<");
        sb.append(base);
        sb.append("> ");
        sb.append(" rdfs:member ");
        sb.append(match);
        sb.append(" . ");
        sb.append(match);
        sb.append(" oslc:score ?oslcSC ");
        properties.forEach((property) -> {
            property.formatProjection(match, sb);
        });
        sb.append('}');
        for (String graph : graphs) {
            sb.append(" FROM <");
            sb.append(Models.getStoreURN(graph));
            sb.append('>');
        }
        sb.append(" WHERE { ");
        sb.append(match);
        sb.append(' ');
        if (type == null) {
            sb.append(Property.predicate(match, Property.WILDCARD));
            sb.append(' ');
            sb.append(Property.objectVariable(match, Property.WILDCARD));
        } else {
            sb.append(Property.RDF_TYPE);
            sb.append(" <");
            sb.append(type);
            sb.append('>');
        }

        for (Condition condition : conditions) {
            String[] conditionValues = condition.getValues();
            if(conditionValues.length > 0){
                String conditionValue = conditionValues[0];
                sb.append(" .");
                sb.append(match);
                sb.append(' ');
                sb.append(condition.getPredicate(""));
                sb.append(' ');
                sb.append(conditionValue);
            }
        }

        conditions.forEach((condition) -> {
            condition.formatSelection(match, sb);
        });
        if (sb.indexOf(Property.RDF_TYPE, sb.indexOf("WHERE")) < 0) {
            //if it is the Generic QueryCapability and the user didn't filter
            //by rdf:type, only those resource without a type should appear.
            sb.append(". FILTER(NOT EXISTS {");
            sb.append(match);
            sb.append(' ');
            sb.append(Property.predicate(match, Property.RDF_TYPE));
            sb.append(' ');
            sb.append(Property.objectVariable(match, Property.RDF_TYPE));
            sb.append("})");
        }
        properties.forEach((property) -> {
            property.formatSelection(match, sb);
        });
        sortKeys.forEach((sortKey) -> {
            sortKey.formatSelection(match, sb);
        });
        sb.append("} ORDER BY ?SC");
        sortKeys.forEach((sortKey) -> {
            sortKey.formatProjection(match, sb);
        });
        base = sb.toString();
        LOG.info("\n\n{}\n\n", base);
        return base;
    }
}