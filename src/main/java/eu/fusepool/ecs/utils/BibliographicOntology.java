package eu.fusepool.ecs.utils;

import org.apache.clerezza.rdf.core.UriRef;


/**
 * Bibliographic Ontology 
 */
public class BibliographicOntology {
    /**
     * Resources of this type can be dereferenced and will return a description
     * of the resource of which the IRI is specified in the "iri" query parameter.
     * 
     */
	public static final String PREFIX = "http://purl.org/ontology/bibo/";
	
	public static final UriRef Document = new UriRef(PREFIX + "Document");
	
    
}
