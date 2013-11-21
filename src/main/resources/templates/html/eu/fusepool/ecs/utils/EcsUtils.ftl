<@namespace ont="http://example.org/service-description#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dcterms="http://purl.org/dc/terms/" />

<html>
  <head>
    <title>ECS Utils</title>
    <link type="text/css" rel="stylesheet" href="styles/ecsutils.css" />
  </head>

  <body>
    <h1>A set of utilities for the ECS.</h1>
    <h2>Ecsutils service component</h2>
	<p>Test the ecsutils service component. The component sends back the following message: "Test EcsUtils service Ok."</p>
<p>curl -u user:password http://platform.fusepool.info/ecsutils/test</p>

<p>Upload RDF data via HTTP POST. After the triples have been uploaded the service sends back the message: "The contentgraph now contains <n> triples" where <n> is
 the total number of triples</p>
<p>curl -u user:password -X POST -H "Content-Type: text/turtle" -T &lt;RDF_file&gt; http://platform.fusepool.info/ecsutils/upload</p>

<p>Upload RDF data via HTTP GET. The service sends back the same message as for the HTTP POST upload</p>
<p>curl -u user:password -H "Content-Type: application/rdf+xml" http://platform.fusepool.info/ecsutils/upload?uri=http://example.org/foaf.rdf</p>

<p> Smush triples. When the smushing is completed the service sends back the message: "Smushing completed"</p>   
<p>curl -u user:password -X POST -H "Content-Type: text/turtle" -T &lt;file_owlsameas_statements&gt; http://platform.fusepool.info/ecsutils/smush</p>


<h2>Patent service component</h2>
<p>Enrich patent data. It creates a content item for each pmo:PatentPublication from title and abstract then enhance the content sending it to the default chain. 
The content is added to the patent by the sioc:content property. The sioc:content property value is used for indexing. The entities referred by enhancements are then related 
to the patent publications by the dc:subject property. Statements with the same predicate are created for  entities taken from the XML->RDF transformation. Users will be 
able to search the ECS by keyword, retrieve a ranked list of patent publications with all the entities that are related with those documents by the dc:subject predicate. 
The service sends back the following message: "Enriched <n> patents".</p>
<p>curl -u user:password http://platform.fusepool.info/patent/enrich</p>

<h2>Pubmed service component</h2>
<p>This service does the same as the patent service for PubMed articles (bibo:Document). The service sends back the following message: "Enriched <n> PubMed articles".</p>
<p>curl -u user:password http://platform.fusepool.info/pubmed/enrich</p>

<#include "/html/includes/footer.ftl">
  </body>
</html>

