package eu.fusepool.ecs.utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.Path;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
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
 * Extracts text from properties dcterms:title and dcterms:abstract of individuals of type bibo:Document. 
 * The extracted text is added as a value of the sioc.content property to the same individuals. 
 * The text can be used for indexing purposes.
 */
/*
@Component(immediate = false)
@Service(RdfDigester.class)
@Properties(value = {
	    @Property(name = Constants.SERVICE_RANKING, 
	    		intValue = PubMedTextExtractor.DEFAULT_SERVICE_RANKING)
})
*/
public class PubMedTextExtractor implements RdfDigester {
	
	public static final int DEFAULT_SERVICE_RANKING = 101;
	
	private static Logger log = LoggerFactory.getLogger(PubMedTextExtractor.class);
	
	/**
	 * Looks for sioc:content property in the input document graph in individual of type bibo:Document. If there's no such property
	 * it adds it. The value of the property is taken from the following properties:
	 * dcterms:title, dcterms:abstract 
	 */
	public void extractText(MGraph graph) {
		
		// select all the resources that are bibo:Document and do not have a sioc:content property 
		Set<GraphNode> articlesRefs = getNodes(graph);
		for (GraphNode docNode : articlesRefs) {
            log.info("Adding sioc:content property to node: " + docNode.getNode());
            addPropertyToNode(docNode);
        }
		
	}
	
	/*
     * Select all resources of type bibo:Document that do not have a sioc:content property and have at least one of
     * dcterms:title, dcterms:abstract
     */
    private Set<GraphNode> getNodes(MGraph graph) {
    	Set<GraphNode> result = new HashSet<GraphNode>();
        
        Iterator<Triple> idocument = graph.filter(null, RDF.type, BibliographicOntology.Document);
        while (idocument.hasNext()) {
            Triple triple = idocument.next();
            GraphNode node = new GraphNode(triple.getSubject(), graph);
            if (!node.getObjects(SIOC.content).hasNext()) {
                result.add(node);
            }
        }
        
        log.info(result.size() + " Document nodes found.");
        
        return result;
    }
    
    /**
     * Add a sioc:content property to a resource. The value is taken from dcterm:title and dcterms:abstract properties 
     */

    private void addPropertyToNode(GraphNode resourceNode) {
    
    	String textContent = "";
    	
        Iterator<Literal> titles = resourceNode.getLiterals(DCTERMS.title);
        while (titles.hasNext()) {
        	String title = titles.next().getLexicalForm() + "\n";
            textContent += title;
        }
        
        Iterator<Literal> abstracts = resourceNode.getLiterals(DCTERMS.abstract_);
        while (abstracts.hasNext()) {
        	String _abstract = abstracts.next().getLexicalForm() + "\n";
            textContent += _abstract;
        }
        
        if(!"".equals(textContent)) {

        	resourceNode.addPropertyValue(SIOC.content, textContent);

        	// Resources with this type have sioc:content and rdfs:label indexed by the ECS 
        	// when added to the content graph
        	resourceNode.addProperty(RDF.type, ECS.ContentItem);
        	
        	log.info("Added sioc:content - " + textContent);
        }
        else {
        	log.info("No text found in dcterms:title or dcterms:abstract to add to sioc:content");
        }

    }
	
	@Activate
    protected void activate(ComponentContext context) {
        log.info("The PubMedTextExtractor service is being activated");

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("The PubMedTextExtractor service is being deactivated");
    }

}
