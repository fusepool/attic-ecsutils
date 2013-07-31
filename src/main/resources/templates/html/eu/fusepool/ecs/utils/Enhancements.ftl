<@namespace dct="http://purl.org/dc/terms/" />
<@namespace fise="http://fise.iks-project.eu/ontology/" />

<html>
  <head>
    <title>ECS Utils</title>
    <link type="text/css" rel="stylesheet" href="styles/multi-enhancer.css" />
  </head>

  <body>
    <h1>ECS Utils</h1>
    The uploaded file hast URI: <@ldpath path="."/><br/>
    <@ldpath path="^fise:extracted-from">
        <p>Annotation: <@ldpath path="."/><br/>
        <!-- unfortunately it doesn't seem to be possible to show all the properties and there values -->
        Created by: <@ldpath path="dct:creator"/><br/>
        <@ldpath path="rdf:type">
          Of type: <@ldpath path="."/><br/>
        </@ldpath>
        </p>
    </@ldpath>
    <#include "/html/includes/footer.ftl">
  </body>
</html>

