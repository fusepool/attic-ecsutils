package eu.fusepool.ecs.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fusepool.datalifecycle.RdfDigester;
import eu.fusepool.ecs.ontologies.ECS;

/**
 * Extracts text from properties dcterms:title and dcterms:abstract of individuals of type pmo:PatentPublication. 
 * The extracted text is added as a value of the sioc.content property to the same individuals. 
 * The text can be used for indexing purposes.
 */
@Component(immediate = true)
@Service(RdfDigester.class)
@Properties(value = {
	    @Property(name = Constants.SERVICE_RANKING, 
	    		intValue = PatentTextExtractor.DEFAULT_SERVICE_RANKING)
})
public class PatentTextExtractor implements RdfDigester {
	
public static final int DEFAULT_SERVICE_RANKING = 101;
	
	private static Logger log = LoggerFactory.getLogger(PatentTextExtractor.class);
	
	/**
	 * Looks for sioc:content property in the input document graph in individual of type pmo:PatentPublication.
	 * If there's no such property it adds it. 
	 * The value of the property is taken from the following properties:
	 * dcterms:title, dcterms:abstract 
	 */
	public void extractText(MGraph graph) {
		
		// select all the resources that are bibo:Document and do not have a sioc:content property 
		Set<UriRef> patentRefs = getPatents(graph);
		for (UriRef patentRef : patentRefs) {
            log.info("Adding sioc:content property to patent: " + patentRef.getUnicodeString());
            addPropertyToNode(graph, patentRef);
        }
		
	}
	
	/**
     * Select all resources of type pmo:patentPublication that do not have a sioc:content 
     * property and have at least one of
     * dcterms:title, dcterms:abstract
     */
    private Set<UriRef> getPatents(MGraph graph) {
    	Set<UriRef> result = new HashSet<UriRef>();
        
        Iterator<Triple> idocument = graph.filter(null, RDF.type, PatentOntology.PatentPublication);
        while (idocument.hasNext()) {
            Triple triple = idocument.next();
            UriRef patentRef = (UriRef) triple.getSubject();
            GraphNode node = new GraphNode(patentRef, graph);
            if (!node.getObjects(SIOC.content).hasNext()) {
                result.add(patentRef);
            }
        }
        
        log.info(result.size() + " Document nodes found.");
        
        return result;
    }
    
    /**
     * Add a sioc:content property to a resource. 
     * The value is taken from dcterm:title and dcterms:abstract properties 
     */

    private void addPropertyToNode(MGraph graph, UriRef patentRef) {
    
    	String textContent = "";
    	
    	GraphNode node = new GraphNode(patentRef, graph);
    	
        Iterator<Literal> titles = node.getLiterals(DCTERMS.title);
        while (titles.hasNext()) {
        	String title = titles.next().getLexicalForm() + "\n";
            textContent += title;
        }
        
        Iterator<Literal> abstracts = node.getLiterals(DCTERMS.abstract_);
        while (abstracts.hasNext()) {
        	String _abstract = abstracts.next().getLexicalForm() + "\n";
            textContent += _abstract;
        }
        if(!"".equals(textContent)) {
        	node.addPropertyValue(SIOC.content, textContent);
        	
        	// Resources with this type have sioc:content and rdfs:label indexed by the ECS 
        	// when added to the content graph
            node.addProperty(RDF.type, ECS.ContentItem);
        }
        else {
        	log.info("No text found in dcterms:title or dcterms:abstract to add to sioc:content");
        }

        

    }
	
	@Activate
    protected void activate(ComponentContext context) {
        log.info("The PatentTextExtractor service is being activated");

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("The PatentTextExtractor service is being deactivated");
    }



}
