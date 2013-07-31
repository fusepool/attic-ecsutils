package eu.fusepool.ecs.utils;

import org.apache.clerezza.rdf.core.UriRef;


/**
 * Patent Ontology: http://www.patexpert.org/ontologies/pmo.owl#
 * This ontology's resources are not dereferencable.
 */
public class PatentOntology {
    
	public static final String PREFIX = "http://www.patexpert.org/ontologies/pmo.owl#";
    
	
	public static final UriRef applicant = new UriRef(PREFIX + "applicant");
	
	public static final UriRef inventor = new UriRef(PREFIX + "inventor");
	
	public static final UriRef PatentPublication = new UriRef(PREFIX + "PatentPublication");
	    
}
