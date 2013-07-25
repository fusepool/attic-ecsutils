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

import org.apache.clerezza.platform.graphprovider.content.ContentGraphProvider;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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
@Path("EcsUtils")
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
     * Test service component
     *
     */
    @GET
    @Path("test")
    @Produces("text/plain")
    public String testService(@Context final UriInfo uriInfo,
            @QueryParam("uri") final URL uri) throws Exception {
        AccessController.checkPermission(new AllPermission());
        String userInfo = uriInfo.getBaseUri().getUserInfo();
        
        return "Test EcsUtils service Ok. uri parameter: " + uri.toString();
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
    
    @POST
    @Path("smush")
    public String sameAsSmush(TripleCollection sameAsTriples) {
        SameAsSmusher.smush(getContentGraph(), sameAsTriples);
        return "fine";
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
