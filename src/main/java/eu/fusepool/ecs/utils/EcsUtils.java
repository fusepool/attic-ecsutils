package eu.fusepool.ecs.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.jaxrs.utils.TrailingSlash;
import org.apache.clerezza.platform.graphprovider.content.ContentGraphProvider;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.viewable.RdfViewable;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* Add dcterm:subject and sioc:content properties to pmo:PatentPublication. The
* content is created from patent's title and abstract.
*/
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("ecsutils")
public class EcsUtils {
	
	/**
     * Using slf4j for normal logging
     */
    private static final Logger log = LoggerFactory
            .getLogger(EcsUtils.class);
    
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
    @Activate
    protected void activate(ComponentContext context) {
        log.info("The EcsUtils service is being activated");

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("The EcsUtils service is being deactivated");
    }
    
    /**
     * This method return an RdfViewable, this is an RDF serviceUri with associated
     * presentational information.
     */
    @GET
    public RdfViewable serviceEntry(@Context final UriInfo uriInfo, 
            @HeaderParam("user-agent") String userAgent) throws Exception {
        //this maks sure we are nt invoked with a trailing slash which would affect
        //relative resolution of links (e.g. css)
        TrailingSlash.enforcePresent(uriInfo);
        final String resourcePath = uriInfo.getAbsolutePath().toString();
        //The URI at which this service was accessed accessed, this will be the 
        //central serviceUri in the response
        final UriRef serviceUri = new UriRef(resourcePath);
        //the in memory graph to which the triples for the response are added
        final MGraph responseGraph = new IndexedMGraph();
        //This GraphNode represents the service within our result graph
        final GraphNode node = new GraphNode(serviceUri, responseGraph);
        //The triples will be added to the first graph of the union
        //i.e. to the in-memory responseGraph
        //node.addProperty(RDF.type, Ontology.MultiEnhancer);
        node.addProperty(RDFS.comment, new PlainLiteralImpl("A set of utilities for the ECS"));
        //What we return is the GraphNode we created with a template path
        return new RdfViewable("EcsUtils", node, EcsUtils.class);
        //return "OK";
    }
    
    /**
     * Test service component
     *
     */
    @GET
    @Path("test")
    @Produces("text/plain")
    public String testService(@Context final UriInfo uriInfo,
            @QueryParam("query") final String query) throws Exception {
        AccessController.checkPermission(new AllPermission());
        String uriInfoStr = uriInfo.getRequestUri().toString();
        
        String response = "no query. Add a query param for a response message /test?query=<your query>";
        
        if(!(query == null)) {
        	response = "parrot's response: " + query;
        }
        
        return "Test EcsUtils service Ok. Request URI: " + uriInfoStr + ". Response: " + response;
    }
    
    /**
     * Load RDF data from a URI (schemes: "file://" or "http://")
     *
     */
    @GET
    @Path("upload")
    @Produces("text/plain")
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

    /**
     * Load RDF data sent by HTTP POST
     */
    @POST
    @Path("upload")
    @Produces("text/plain")
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
    
    /**
     * Select a (random) target uri from a set of equivalent uris and replace those aliases with it in all the triples in the content graph
     *  in which these aliases are subjects or objects.
     * @param sameAsTriples
     * @return
     */
    @POST
    @Path("smush")
    @Produces("text/plain")
    public String sameAsSmush(TripleCollection sameAsTriples) {
    	String responseMessage = "";
    	if(sameAsTriples == null || sameAsTriples.isEmpty()) {
    		responseMessage = "Error! You must upload an RDF file containing owl:sameAs statements.";
    		
    	}
    	else {
    		SameAsSmusher.smush(getContentGraph(), sameAsTriples);
    		responseMessage = "Smushing completed";
    	}
        return responseMessage;
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
