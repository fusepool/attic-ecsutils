package eu.fusepool.patentutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.apache.clerezza.jaxrs.utils.TrailingSlash;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.UnionMGraph;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fusepool.ecs.ontologies.ECS;
import org.apache.clerezza.platform.graphprovider.content.ContentGraphProvider;

/**
 * Add dcterm:subject and sioc:content properties to pmo:PatentPublication. The content is created from patent's title and abstract.
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("PatentInserter")
public class ResourceResolver {

	/**
	 * Using slf4j for normal logging
	 */
	private static final Logger log = LoggerFactory
			.getLogger(ResourceResolver.class);

	private static final double CONFIDENCE_THRESHOLD = 0.3;

	/**
	 * This service allows to get entities from configures sites
	 */
	@Reference
	private SiteManager siteManager;

	/**
	 * This service allows accessing and creating persistent triple collections
	 */
	@Reference
	private ContentGraphProvider contentGraphProvider;

	@Reference
	private Parser parser;

	@Reference
	private ContentItemFactory contentItemFactory;

	@Reference
	private EnhancementJobManager enhancementJobManager;

	@Reference
	private ChainManager chainManager;

	/**
	 * This is the name of the graph in which we "log" the requests
	 */
	private UriRef CONTENT_GRAPH_NAME = new UriRef(
			"urn:x-localinstance:/content.graph");

	@Activate
	protected void activate(ComponentContext context) {
		log.info("The example service is being activated");

	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		log.info("The example service is being deactivated");
	}

	/**
	 * Load RDF data from a URI (file://)
	 * 	
	 */
	@GET
	public String loadFromUri(@Context final UriInfo uriInfo,
			@QueryParam("uri") final URL uri) throws Exception {
		AccessController.checkPermission(new AllPermission());
		// final URL url = new URL(uri);
		HttpURLConnection urlConnection = (HttpURLConnection) uri
				.openConnection();
		urlConnection.addRequestProperty("Accept",
				"application/rdf+xml; q=.9, text/turte;q=1");
		final String mediaType = urlConnection.getContentType();
		final InputStream data = urlConnection.getInputStream();
		return loadFromStream(uriInfo, mediaType, data);
	}

	/*
	 * Load RDF data sent by HTTP POST
	 */
	@POST
	public String loadFromStream(@Context final UriInfo uriInfo,
			@HeaderParam("Content-Type") final String mediaType,
			final InputStream data) throws Exception {
		AccessController.checkPermission(new AllPermission());
		// final MGraph addition = new SimpleMGraph();
		parser.parse(getContentGraph(), data, mediaType);
		// GraphNode resourceNode = new GraphNode(new UriRef(uri.toString()),
		// addition);
		String textContent = "";
		
		return "The contentgraph now contains " + getContentGraph().size()
				+ " triples";
	}

	/*
	 * Filter all pmo:PatentPublication that do not have Add a sioc:content property to each PatentPublication 
	 */
	@GET
	@Path("enrich")
	@Produces("text/plain")
	public String enrichAll() throws IOException, EnhancementException {
		Set<GraphNode> unenrichedPatents = getUnenrichedPatents();
		for (GraphNode graphNode : unenrichedPatents) {
			log.info("enriching: " + graphNode.getNode());
			enrich(graphNode);
		}
		
		return "we gave " + unenrichedPatents.size() + " patents";
	}
	
	@GET
	@Path("delete")
	public String deleteAll() throws IOException, EnhancementException {
		
		getContentGraph().clear();
		
		return "deleted all triples in content graph";
	}

	/*
	 * Add a sioc:content property to a resource. The content is created from dc:title and dc:abstract properties 
	 */
	private void enrich(GraphNode resourceNode) throws IOException,
			EnhancementException {
		String textContent = "";
		Iterator<Literal> titles = resourceNode.getLiterals(DCTERMS.title);
		while (titles.hasNext()) {
			textContent += titles.next().getLexicalForm() + " ";
		}
		Iterator<Literal> abstracts = resourceNode
				.getLiterals(DCTERMS.abstract_);
		while (abstracts.hasNext()) {
			textContent += abstracts.next().getLexicalForm() + " ";
		}
		
		enhance(textContent, resourceNode);

		Set<Resource> applicantSet = new HashSet<Resource>();
		resourceNode.addPropertyValue(SIOC.content, textContent);
		
		Lock lock = resourceNode.readLock();
		lock.lock();
		try {
			final Iterator<Resource> applicants = resourceNode
					.getObjects(new UriRef(
							"http://www.patexpert.org/ontologies/pmo.owl#applicant"));
			while (applicants.hasNext()) {
				Resource applicant = applicants.next();
				applicantSet.add(applicant);
			}
		} finally {
			lock.unlock();
		}
		for (Resource applicant : applicantSet) {
			resourceNode.addProperty(DC.subject, applicant);
		}
		
		// Resources with this type have sioc:content and rdfs:label indexed by the ECS when added to the content graph
		resourceNode.addProperty(RDF.type, ECS.ContentItem);

	}

	/*
	 * Select all pmo:PatentPublication that do not have a sioc:content property
	 */
	private Set<GraphNode> getUnenrichedPatents() {
		Set<GraphNode> result = new HashSet<GraphNode>();
		LockableMGraph contentGraph = getContentGraph();
		Lock l = contentGraph.getLock().readLock();
		l.lock();
		try {
			UriRef patentPublication = new UriRef(
					"http://www.patexpert.org/ontologies/pmo.owl#PatentPublication");
			Iterator<Triple> ipatent = contentGraph.filter(null, RDF.type,
					patentPublication);
			while (ipatent.hasNext()) {
				Triple patentTriple = ipatent.next();
				GraphNode patentNode = new GraphNode(patentTriple.getSubject(),
						contentGraph);
				if (!patentNode.getObjects(SIOC.content).hasNext()) {
					result.add(patentNode);
				}
			}
		} finally {
			l.unlock();
		}
		return result;
	}

	/** 
	 * Add dc:subject properties to <code>node</code> pointing to entities 
	 * which are assumed to be related to <code>content</code>. 
	 * This method uses the enhancementJobManager to get 
	 * related entities using the default chain.
	 * 	 * 
	 */
	private void enhance(String content, GraphNode node) throws IOException,
			EnhancementException {
		final ContentSource contentSource = new ByteArraySource(
				content.getBytes(), "text/plain");
		final ContentItem contentItem = contentItemFactory.createContentItem(
				(UriRef) node.getNode(), contentSource);
		enhancementJobManager.enhanceContent(contentItem);
		// this contains the enhancement results
		final MGraph enhancementGraph = contentItem.getMetadata();
		addSubjects(node, enhancementGraph);
	}

	/* 
	 * Add dc:subject property to a pmo:PatentPublication with entities extracted by engines in the default chain
	 * 
	 */
	/**
	 * Given a node and a TripleCollection containing fise:Enhancements about this
	 * node this method adds dc:subject properties to node pointing to entities referenced
	 * by those enhancements if the enhancement confidence value is above a threshold.
	 * 
	 * @param node
	 * @param metadata
	 */
	private void addSubjects(GraphNode node, TripleCollection metadata) {
		final GraphNode enhancementType = new GraphNode(
				TechnicalClasses.ENHANCER_ENHANCEMENT, metadata);
		final Set<UriRef> entities = new HashSet<UriRef>();
		final Iterator<GraphNode> enhancements = enhancementType
				.getSubjectNodes(RDF.type);
		while (enhancements.hasNext()) {
			final GraphNode enhhancement = enhancements.next();
			// Add dc:subject to the patent for each referenced entity
			final Iterator<Resource> referencedEntities = enhhancement.getObjects(Properties.ENHANCER_ENTITY_REFERENCE);
			while (referencedEntities.hasNext()) {
				final UriRef entity = (UriRef) referencedEntities.next();
				GraphNode entityNode = new GraphNode(entity, metadata);
				Iterator<Literal> confidenceLevels = entityNode.getLiterals(TechnicalClasses.FNHANCER_CONFIDENCE_LEVEL);
				if (!confidenceLevels.hasNext()) {
					continue;
				}
				double confidenceLevel = LiteralFactory.getInstance().createObject(Double.class, (TypedLiteral)confidenceLevels.next());
				if (confidenceLevel >= CONFIDENCE_THRESHOLD) {
					node.addProperty(DC.subject, entity);			    	
					entities.add(entity);
				}
			}
			
			
		}
		for (UriRef uriRef : entities) {
			// We don't get the entity description directly from metadata
			// as the context there would include
			addResourceDescription(uriRef, (MGraph) node.getGraph());
		}
	}

	/* 
	 * Add a description of the entities found in the text by engines in the default chain
	 */
	private void addResourceDescription(UriRef iri, MGraph mGraph) {
		final Entity entity = siteManager.getEntity(iri.getUnicodeString());
		if (entity != null) {
			final RdfValueFactory valueFactory = new RdfValueFactory(mGraph);
			final Representation representation = entity.getRepresentation();
			if (representation != null) {
				valueFactory.toRdfRepresentation(representation);
			}
		}
	}

	/**
	 * This returns the existing MGraph for the log .
	 * 
	 * @return the MGraph to which the requests are logged
	 */
	private LockableMGraph getContentGraph() {
		return contentGraphProvider.getContentGraph();
	}

}
