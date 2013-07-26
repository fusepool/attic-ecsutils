ECS Utils
===========

This component provides utility commands for 
- upload of RDF data into the ECS (content graph)
- enrichment of patent documents and PubMed articles by addition of sioc:content property for indexing and 
  dc.subject properties between documents and entities extracted from transformation of XML data into RDF or
  extracted by NLP components (enhancements)
- smushing equivalent uris by choosing one uri as target uri and replacing all the aliases that appear as subject or
  object in triples with it.

To compile the engine run

    mvn install

To deploy the engine to a stanbol instance running on localhost port 8080 run

    mvn org.apache.sling:maven-sling-plugin:install

