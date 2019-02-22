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


import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.generator.SchemaSampleDataGenerator;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.docgen.examplegen.ExampleRequestResponse;
import com.linkedin.restli.docgen.examplegen.ExampleRequestResponseGenerator;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.BatchFinderSchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.RoutingException;
import org.apache.commons.lang.exception.ExceptionUtils;
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
   * @param templatingEngine templating engine used to render HTML page
   * @param schemaResolver resolver that resolves related {@link ResourceSchema}
   */
  public RestLiHTMLDocumentationRenderer(URI serverNodeUri,
                                         RestLiResourceRelationship relationships,
                                         TemplatingEngine templatingEngine,
                                         DataSchemaResolver schemaResolver)
  {
    _serverNodeUri = serverNodeUri;
    _docBaseUri = UriBuilder.fromUri(serverNodeUri).path("restli").path("docs").build();
    _relationships = relationships;
    _resourceSchemas = _relationships.getResourceSchemaCollection();
    _templatingEngine = templatingEngine;
    _schemaResolver = schemaResolver;
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
    final List<ResourceSchema> parentResources = _resourceSchemas.getParentResources(resourceSchema);
    ExampleRequestResponseGenerator generator = new ExampleRequestResponseGenerator(parentResources, resourceSchema, _schemaResolver);
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
    final List<ResourceMethodDocView> batchFinders = new ArrayList<ResourceMethodDocView>();
    final List<ResourceMethodDocView> actions = new ArrayList<ResourceMethodDocView>();

    final MethodGatheringResourceSchemaVisitor visitor = new MethodGatheringResourceSchemaVisitor(resourceName);
    ResourceSchemaCollection.visitResources(_resourceSchemas.getResources().values(), visitor);

    for (RecordTemplate methodSchema : visitor.getAllMethods())
    {
      final ExampleRequestResponse capture;
      if (methodSchema instanceof RestMethodSchema)
      {
        RestMethodSchema restMethodSchema = (RestMethodSchema)methodSchema;
        capture = generator.method(ResourceMethod.valueOf(restMethodSchema.getMethod().toUpperCase()));
      }
      else if (methodSchema instanceof FinderSchema)
      {
        FinderSchema finderMethodSchema = (FinderSchema)methodSchema;
        capture = generator.finder(finderMethodSchema.getName());
      }
      else if (methodSchema instanceof BatchFinderSchema)
      {
        BatchFinderSchema batchFinderSchema = (BatchFinderSchema)methodSchema;
        capture = generator.batchFinder(batchFinderSchema.getName());
      }
      else if (methodSchema instanceof ActionSchema)
      {
        ActionSchema actionMethodSchema = (ActionSchema)methodSchema;
        final ResourceLevel resourceLevel = (visitor.getCollectionActions().contains(methodSchema) ?
                                             ResourceLevel.COLLECTION :
                                             ResourceLevel.ENTITY);
        capture = generator.action(actionMethodSchema.getName(), resourceLevel);
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
                                                                      getDoc(methodSchema, resourceSchema.hasSimple()),
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
      else if (methodSchema instanceof BatchFinderSchema)
      {
        batchFinders.add(docView);
      }
      else if (methodSchema instanceof ActionSchema)
      {
        actions.add(docView);
      }
    }
    pageModel.put("restMethods", restMethods);
    pageModel.put("finders", finders);
    pageModel.put("batchFinders", batchFinders);
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

  public void setJsonFormatUri(URI jsonFormatUri)
  {
    _jsonFormatUri = URI.create(_serverNodeUri.toString() + jsonFormatUri.toString());
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
    else if (resourceSchema.hasSimple())
    {
      return "simple";
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
    pageModel.put("jsonFormatUri", _jsonFormatUri);
    return pageModel;
  }

  private String getDoc(Object method, boolean isSimpleResourceMethod)
  {
    String doc = null;
    if (method instanceof RestMethodSchema)
    {
      final RestMethodSchema restMethodSchema = (RestMethodSchema) method;
      doc = restMethodSchema.getDoc();
      if (doc == null || doc.trim().length() == 0) // if no javadoc is supplied, fallback to generic doc string
      {
        if (isSimpleResourceMethod)
        {
          doc = _restMethodDocsMapForSimpleResource.get(restMethodSchema.getMethod());
        }
        else
        {
          doc = _restMethodDocsMapForCollection.get(restMethodSchema.getMethod());
        }

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
  private static final Map<String, String> _restMethodDocsMapForCollection = new HashMap<String, String>();
  private static final Map<String, String> _restMethodDocsMapForSimpleResource = new HashMap<String, String>();
  private static final JacksonDataCodec _codec = new JacksonDataCodec();

  private final URI _serverNodeUri;
  private final URI _docBaseUri;
  private final RestLiResourceRelationship _relationships;
  private final ResourceSchemaCollection _resourceSchemas;
  private final TemplatingEngine _templatingEngine;
  private final DataSchemaResolver _schemaResolver;

  private final Map<Object, Map<String, ResourceSchema>> _relatedResourceCache =
      new HashMap<Object, Map<String, ResourceSchema>>();
  private final Map<Object, Map<String, NamedDataSchema>> _relatedSchemaCache =
      new HashMap<Object, Map<String, NamedDataSchema>>();

  private URI _jsonFormatUri;

  static
  {
    _restMethodDocsMapForCollection.put(ResourceMethod.BATCH_CREATE.toString(), "Creates multiple entities");
    _restMethodDocsMapForCollection.put(ResourceMethod.BATCH_DELETE.toString(), "Deletes multiple entities");
    _restMethodDocsMapForCollection.put(ResourceMethod.BATCH_GET.toString(),
                           "Retrievies multiple entity representations given their keys");
    _restMethodDocsMapForCollection.put(ResourceMethod.BATCH_PARTIAL_UPDATE.toString(),
                           "Partial update applied to multiple entities");
    _restMethodDocsMapForCollection.put(ResourceMethod.BATCH_UPDATE.toString(), "Replaces multiple entities");
    _restMethodDocsMapForCollection.put(ResourceMethod.CREATE.toString(), "Creates an entity");
    _restMethodDocsMapForCollection.put(ResourceMethod.DELETE.toString(), "Deletes an entity");
    _restMethodDocsMapForCollection.put(ResourceMethod.GET.toString(), "Gets a single entity given a key");
    _restMethodDocsMapForCollection.put(ResourceMethod.PARTIAL_UPDATE.toString(), "Updates parts of an entity given a key");
    _restMethodDocsMapForCollection.put(ResourceMethod.UPDATE.toString(), "Replaces an entity given a key");

    _restMethodDocsMapForSimpleResource.put(ResourceMethod.DELETE.toString(), "Deletes the entity");
    _restMethodDocsMapForSimpleResource.put(ResourceMethod.GET.toString(), "Gets the entity");
    _restMethodDocsMapForSimpleResource.put(ResourceMethod.UPDATE.toString(), "Replaces the entity");

    _codec.setPrettyPrinter(new DefaultPrettyPrinter());
  }
}
