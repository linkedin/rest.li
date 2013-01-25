/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.docgen;


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.generator.SchemaSampleDataGenerator;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.RoutingException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.impl.DefaultPrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Renders static HTML documentation for a set of rest.li resources. This class is
 * intentionally R2/HTTP-agnostic so that it can be used for both online and offline
 * documentation generation.
 *
 * @author dellamag, Keren Jin
 */
public class RestLiHTMLDocumentationRenderer implements RestLiDocumentationRenderer
{
  /**
   * @param serverNodeUri URI prefix of the rendered pages
   * @param relationships relationship of all the resources and data models to be rendered
   * @param rootResources root resources from its resource path. subresource models will be discovered
   * @param templatingEngine templating engine used to render HTML page
   * @param schemaResolver resolver that resolves related {@link ResourceSchema}
   */
  public RestLiHTMLDocumentationRenderer(URI serverNodeUri,
                                         RestLiResourceRelationship relationships,
                                         Map<String, ResourceModel> rootResources,
                                         TemplatingEngine templatingEngine,
                                         DataSchemaResolver schemaResolver)
  {
    _serverNodeUri = serverNodeUri;
    _docBaseUri = UriBuilder.fromUri(serverNodeUri).path("restli").path("docs").build();
    _relationships = relationships;
    _resourceSchemas = _relationships.getResourceSchemaCollection();
    _templatingEngine = templatingEngine;

    _restliExampleGenerator = new RestLiExampleGenerator(relationships.getResourceSchemaCollection(),
                                                         rootResources,
                                                         schemaResolver);
  }

  @Override
  public void renderHome(OutputStream out)
  {
    final Map<String, Object> pageModel = createPageModel();
    pageModel.put("resourceSchemas", _resourceSchemas.getResources());
    pageModel.put("dataModels", _relationships.getDataModels());

    _templatingEngine.render("main.vm", pageModel, out);
  }

  @Override
  public void renderResourceHome(OutputStream out)
  {
    final Map<String, Object> pageModel = createPageModel();
    pageModel.put("resourceSchemas", _resourceSchemas.getResources());
    _templatingEngine.render("rest.vm", pageModel, out);
  }

  @Override
  public void renderResource(String resourceName, OutputStream out)
  {
    final ResourceSchema resourceSchema = _resourceSchemas.getResource(resourceName);
    if (resourceSchema == null)
    {
      throw new RoutingException(String.format("Resource \"%s\" does not exist", resourceName), HttpStatus.S_404_NOT_FOUND.getCode()) ;
    }

    final Map<String, Object> pageModel = createPageModel();
    pageModel.put("resource", resourceSchema);
    pageModel.put("resourceName", resourceName);
    pageModel.put("resourceFullName", ResourceSchemaUtil.getFullName(resourceSchema));
    pageModel.put("resourceType", getResourceType(resourceSchema));
    pageModel.put("subResources", _resourceSchemas.getSubResources(resourceSchema));

    final List<ResourceMethodDocView> restMethods = new ArrayList<ResourceMethodDocView>();
    final List<ResourceMethodDocView> finders = new ArrayList<ResourceMethodDocView>();
    final List<ResourceMethodDocView> actions = new ArrayList<ResourceMethodDocView>();

    final MethodGatheringResourceSchemaVisitor visitor = new MethodGatheringResourceSchemaVisitor(resourceName);
    ResourceSchemaCollection.visitResources(_resourceSchemas.getResources().values(), visitor);

    final RestLiExampleGenerator.RequestGenerationSpec spec = new RestLiExampleGenerator.RequestGenerationSpec();
    for (RecordTemplate methodSchema : visitor.getAllMethods())
    {
      final RequestResponsePair capture;
      if (methodSchema instanceof RestMethodSchema)
      {
        capture = _restliExampleGenerator.generateRestMethodExample(resourceSchema, (RestMethodSchema) methodSchema, spec);
      }
      else if (methodSchema instanceof FinderSchema)
      {
        capture = _restliExampleGenerator.generateFinderExample(resourceSchema, (FinderSchema) methodSchema, spec);
      }
      else if (methodSchema instanceof ActionSchema)
      {
        final ResourceLevel resourceLevel = (visitor.getCollectionActions().contains(methodSchema) ?
                                             ResourceLevel.COLLECTION :
                                             ResourceLevel.ENTITY);
        capture = _restliExampleGenerator.generateActionExample(resourceSchema, (ActionSchema) methodSchema, resourceLevel, spec);
      }
      else
      {
        capture = null;
      }

      String requestEntity = null;
      String responseEntity = null;
      if (capture != null)
      {
        try
        {
          DataMap entityMap;

          if (capture.getRequest().getEntity().length() > 0)
          {
            entityMap = DataMapUtils.readMap(capture.getRequest());
            requestEntity = new String(_codec.mapToBytes(entityMap));
          }

          if (capture.getResponse() != null &&
              capture.getResponse().getEntity() != null &&
              capture.getResponse().getEntity().length() > 0)
          {
            entityMap = DataMapUtils.readMap(capture.getResponse());
            responseEntity = new String(_codec.mapToBytes(entityMap));
          }
        }
        catch (IOException e)
        {
          throw new RestLiInternalException(e);
        }
      }

      final ResourceMethodDocView docView = new ResourceMethodDocView(methodSchema,
                                                                      capture,
                                                                      getDoc(methodSchema),
                                                                      requestEntity,
                                                                      responseEntity);
      if (methodSchema instanceof RestMethodSchema)
      {
        restMethods.add(docView);
      }
      else if (methodSchema instanceof FinderSchema)
      {
        finders.add(docView);
      }
      else if (methodSchema instanceof ActionSchema)
      {
        actions.add(docView);
      }
    }
    pageModel.put("restMethods", restMethods);
    pageModel.put("finders", finders);
    pageModel.put("actions", actions);
    addRelated(resourceSchema, pageModel);

    _templatingEngine.render("resource.vm", pageModel, out);
  }

  @Override
  public void renderDataModelHome(OutputStream out)
  {
    final Map<String, Object> pageModel = createPageModel();
    pageModel.put("dataModels", _relationships.getDataModels());
    _templatingEngine.render("data.vm", pageModel, out);
  }

  @Override
  public void renderDataModel(String dataModelName, OutputStream out)
  {
    final NamedDataSchema schema = _relationships.getDataModels().get(dataModelName);
    if (schema == null)
    {
      throw new RoutingException(String.format("Data model named '%s' does not exist", dataModelName), 404) ;
    }

    final Map<String, Object> pageModel = createPageModel();
    pageModel.put("dataModel", schema);

    final DataMap example = SchemaSampleDataGenerator.buildRecordData(schema, new SchemaSampleDataGenerator.DataGenerationOptions());
    try
    {
      pageModel.put("example", new String(_codec.mapToBytes(example)));
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
    addRelated(schema, pageModel);

    _templatingEngine.render("dataModel.vm", pageModel, out);
  }

  @Override
  public boolean handleException(RuntimeException e, OutputStream out)
  {
    final Map<String, Object> pageModel = createPageModel();
    pageModel.put("exception", e);
    pageModel.put("stacktrace", ExceptionUtils.getStackTrace(e));
    _templatingEngine.render("exception.vm", pageModel, out);

    return true;
  }

  @Override
  public String getMIMEType()
  {
    return "text/html";
  }

  private static String getResourceType(ResourceSchema resourceSchema)
  {
    if (resourceSchema.hasCollection())
    {
      return "collection";
    }
    else if (resourceSchema.hasAssociation())
    {
      return "association";
    }
    else if (resourceSchema.hasActionsSet())
    {
      return "actionSet";
    }
    return null;
  }

  private Map<String, Object> createPageModel()
  {
    final Map<String, Object> pageModel = new HashMap<String, Object>();
    pageModel.put("serverNodeUri", _serverNodeUri);
    pageModel.put("docBaseUri", _docBaseUri);
    return pageModel;
  }

  private String getDoc(Object method)
  {
    String doc = null;
    if (method instanceof RestMethodSchema)
    {
      RestMethodSchema restMethodSchema = ((RestMethodSchema) method);
      doc = restMethodSchema.getDoc();
      if (doc == null || doc.trim().length() == 0) // if no javadoc is supplied, fallback to generic doc string
      {
        doc = _restMethodDocsMap.get(restMethodSchema.getMethod());
        if (doc == null)
        {
          log.warn(String.format("No doc string for REST method %s", doc));
        }
      }
    }

    return doc;
  }

  private void addRelated(Object parent, Map<String, Object> pageModel)
  {
    final Node<?> node = _relationships.getRelationships(parent);
    Map<String, ResourceSchema> relatedResources;
    Map<String, NamedDataSchema> relatedSchemas;

    synchronized (this)
    {
      relatedResources = _relatedResourceCache.get(parent);
      if (relatedResources == null)
      {
        relatedResources = new HashMap<String, ResourceSchema>();
        final Iterator<Node<ResourceSchema>> resourcesItr = node.getAdjacency(ResourceSchema.class).iterator();
        while (resourcesItr.hasNext())
        {
          final ResourceSchema currResource = (ResourceSchema) resourcesItr.next().getObject();
          relatedResources.put(currResource.getName(), currResource);
        }
        _relatedResourceCache.put(parent, relatedResources);
      }

      relatedSchemas = _relatedSchemaCache.get(parent);
      if (relatedSchemas == null)
      {
        relatedSchemas = new HashMap<String, NamedDataSchema>();
        final Iterator<Node<NamedDataSchema>> schemaItr = node.getAdjacency(NamedDataSchema.class).iterator();
        while (schemaItr.hasNext())
        {
          final NamedDataSchema currResource = (NamedDataSchema) schemaItr.next().getObject();
          relatedSchemas.put(currResource.getFullName(), currResource);
        }
        _relatedSchemaCache.put(parent, relatedSchemas);
      }
    }

    pageModel.put("relatedResources", relatedResources);
    pageModel.put("relatedSchemas", relatedSchemas);
  }

  private static final Logger log = LoggerFactory.getLogger(RestLiServer.class);
  private static final Map<String, String> _restMethodDocsMap = new HashMap<String, String>();
  private static final JacksonDataCodec _codec = new JacksonDataCodec();

  private final URI _serverNodeUri;
  private final URI _docBaseUri;
  private final RestLiResourceRelationship _relationships;
  private final ResourceSchemaCollection _resourceSchemas;
  private final TemplatingEngine _templatingEngine;

  private final RestLiExampleGenerator _restliExampleGenerator;

  private final Map<Object, Map<String, ResourceSchema>> _relatedResourceCache =
      new HashMap<Object, Map<String, ResourceSchema>>();
  private final Map<Object, Map<String, NamedDataSchema>> _relatedSchemaCache =
      new HashMap<Object, Map<String, NamedDataSchema>>();

  static
  {
    _restMethodDocsMap.put(ResourceMethod.BATCH_CREATE.toString(), "Creates multiple entities");
    _restMethodDocsMap.put(ResourceMethod.BATCH_DELETE.toString(), "Deletes multiple entities");
    _restMethodDocsMap.put(ResourceMethod.BATCH_GET.toString(), "Retrievies multiple entity representations given their keys");
    _restMethodDocsMap.put(ResourceMethod.BATCH_PARTIAL_UPDATE.toString(), "Partial update applied to multiple entities");
    _restMethodDocsMap.put(ResourceMethod.BATCH_UPDATE.toString(), "Replaces multiple entities");
    _restMethodDocsMap.put(ResourceMethod.CREATE.toString(), "Creates an entity");
    _restMethodDocsMap.put(ResourceMethod.DELETE.toString(), "Deletes an entity");
    _restMethodDocsMap.put(ResourceMethod.GET.toString(), "Gets a single entity given a key");
    _restMethodDocsMap.put(ResourceMethod.PARTIAL_UPDATE.toString(), "Updates parts of an entity given a key");
    _restMethodDocsMap.put(ResourceMethod.UPDATE.toString(), "Replaces an entity given a key");

    _codec.setPrettyPrinter(new DefaultPrettyPrinter());
  }
}
