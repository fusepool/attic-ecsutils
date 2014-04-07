ECS Utils
===========
This component provides digesters for the Data Life Cycle service.
 
This component provides also utility commands for 
- upload of RDF data into the ECS (content graph)
- enrichment of patent documents and PubMed articles by addition of sioc:content property for indexing and 
  dc.subject properties between documents and entities extracted from transformation of XML data into RDF or
  extracted by NLP components (enhancements)
- smushing equivalent uris by choosing one uri as target uri and replacing all the aliases that appear as subject or
  object in triples with it.

To compile the bundle run

    mvn install

To deploy the engine to a stanbol instance running on localhost port 8080 run

    mvn org.apache.sling:maven-sling-plugin:install
    
    
The REST API provided by this bundle are described below.

Ecsutils service component
==========================
Test the ecsutils service component. The component sends back the following message: "Test EcsUtils service Ok."

    curl -u user:password http://platform.fusepool.info/ecsutils/test

Upload RDF data via HTTP POST. After the triples have been uploaded the service sends back the message: "The contentgraph 
now contains <n> triples" where <n> is the total number of triples.

    curl -u user:password -X POST -H "Content-Type: text/turtle" -T <RDF data file> http://platform.fusepool.info/ecsutils/upload

Upload RDF data via HTTP GET. The service sends back the same message as for the HTTP POST upload

    curl -u user:password -H "Content-Type: application/rdf+xml" http://platform.fusepool.info/ecsutils/upload?uri=http://example.org/foaf.rdf

Smush triple. When the smushing is completed the service sends back the message: "Smushing completed"   

    curl -u user:password -X POST -H "Content-Type: text/turtle" -T <owlsameas_statements_file> http://platform.fusepool.info/ecsutils/smush


Patent service component
========================
Enrich patent data. It creates a content item for each pmo:PatentPublication from title and abstract then enhance the 
content sending it to the default chain. The content is added to the patent by the sioc:content property. The sioc:content 
property value is used for indexing. The entities referred by enhancements are then related to the patent publications by
the dc:subject property. Statements with the same predicate are created for  entities taken from the XML->RDF transformation. 
Users will be able to search the ECS by keyword, retrieve a ranked list of patent publications with all the entities that 
are related with those documents by the dc:subject predicate. 
The service sends back the following message: "Enriched <n> patents".

    curl -u user:password http://platform.fusepool.info/patent/enrich

Pubmed service component
========================
This service does the same as the patent service for PubMed articles (bibo:Document). The service sends back the following message: "Enriched <n> PubMed articles".

    curl -u user:password http://platform.fusepool.info/pubmed/enrich
    


