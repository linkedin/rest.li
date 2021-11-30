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
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.server.RoutingException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * Renders JSON representation of resources and data models. When rendering for resource, all subresources
 * and related data models accessible are also returned. When rendering for data model, only the model itself
 * is returned.
 *
 * @author Keren Jin
 */
public class RestLiJSONDocumentationRenderer implements RestLiDocumentationRenderer
{
  /**
   * @param relationships relationship of all the resources and data models to be rendered
   */
  public RestLiJSONDocumentationRenderer(RestLiResourceRelationship relationships)
  {
    _relationships = relationships;
  }

  @Override
  public void renderHome(OutputStream out)
  {
    renderResourceHome(out);
  }

  @Override
  public void renderHome(RenderContext renderContext)
  {
    renderResourceHome(renderContext);
  }

  @Override
  public void renderResourceHome(OutputStream out)
  {
    renderResourceHome(new RenderContext(out));
  }

  @Override
  public void renderResourceHome(RenderContext renderContext)
  {
    final DataMap outputMap = createEmptyOutput();

    try
    {
      for (ResourceSchema resourceSchema:
          new HashSet<>(_relationships.getResourceSchemaCollection().getResources().values()))
      {
        renderResource(resourceSchema, outputMap, renderContext.getHeaders());
      }

      _codec.writeMap(outputMap, renderContext.getOutputStream());
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public void renderResource(String resourceName, OutputStream out)
  {
    renderResource(resourceName, new RenderContext(out));
  }

  @Override
  public void renderResource(String resourceName, RenderContext renderContext)
  {
    final ResourceSchema resourceSchema = _relationships.getResourceSchemaCollection().getResource(resourceName);
    if (resourceSchema == null)
    {
      throw new RoutingException(String.format("Resource named '%s' does not exist", resourceName), 404) ;
    }

    final DataMap outputMap = createEmptyOutput();

    try
    {
      renderResource(resourceSchema, outputMap, renderContext.getHeaders());
      _codec.writeMap(outputMap, renderContext.getOutputStream());
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public void renderDataModelHome(OutputStream out)
  {
    renderDataModelHome(new RenderContext(out));
  }

  @Override
  public void renderDataModelHome(RenderContext renderContext)
  {
    final DataMap outputMap = createEmptyOutput();
    final DataMap models = outputMap.getDataMap("models");

    try
    {
      for (NamedDataSchema schema: new HashSet<>(_relationships.getDataModels().values()))
      {
        renderDataModel(schema, models, renderContext.getHeaders());
      }

      _codec.writeMap(outputMap, renderContext.getOutputStream());
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public void renderDataModel(String dataModelName, OutputStream out)
  {
    renderDataModel(dataModelName, new RenderContext(out));
  }

  @Override
  public void renderDataModel(String dataModelName, RenderContext renderContext)
  {
    final NamedDataSchema schema = _relationships.getDataModels().get(dataModelName);
    if (schema == null)
    {
      throw new RoutingException(String.format("Data model named '%s' does not exist", dataModelName), 404) ;
    }

    final DataMap outputMap = createEmptyOutput();
    final DataMap models = outputMap.getDataMap("models");
    try
    {
      renderDataModel(schema, models, renderContext.getHeaders());
      _codec.writeMap(outputMap, renderContext.getOutputStream());
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public boolean handleException(RuntimeException e, OutputStream out)
  {
    return false;
  }

  @Override
  public String getMIMEType()
  {
    return RestConstants.HEADER_VALUE_APPLICATION_JSON;
  }

  private DataMap createEmptyOutput()
  {
    final DataMap emptyOutputMap = new DataMap();
    emptyOutputMap.put("resources", new DataMap());
    emptyOutputMap.put("models", new DataMap());

    return emptyOutputMap;
  }

  private void addRelatedModels(ResourceSchema resourceSchema, DataMap models,
      Map<String, String> requestHeaders) throws IOException
  {
   DataMap relatedSchemas;
    synchronized (this)
    {
      relatedSchemas = _relatedSchemaCache.get(resourceSchema);
      if (relatedSchemas == null)
      {
        relatedSchemas = new DataMap();
        final Node<?> node = _relationships.getRelationships(resourceSchema);
        for (Node<NamedDataSchema> namedDataSchemaNode : node.getAdjacency(NamedDataSchema.class))
        {
          final NamedDataSchema currResource = (NamedDataSchema) namedDataSchemaNode.getObject();
          renderDataModel(currResource, relatedSchemas, requestHeaders);
        }
        _relatedSchemaCache.put(resourceSchema, relatedSchemas);
      }
    }

    models.putAll(relatedSchemas);
  }

  protected void renderResource(ResourceSchema resourceSchema, DataMap outputMap,
      Map<String, String> requestHeaders) throws IOException
  {
    final DataMap resources = outputMap.getDataMap("resources");
    final DataMap models = outputMap.getDataMap("models");

    resources.put(ResourceSchemaUtil.getFullName(resourceSchema), resourceSchema.data());
    addRelatedModels(resourceSchema, models, requestHeaders);

    final List<ResourceSchema> subResources = _relationships.getResourceSchemaCollection().getAllSubResources(
        resourceSchema);
    if (subResources != null)
    {
      for (ResourceSchema subresource: subResources)
      {
        resources.put(ResourceSchemaUtil.getFullName(subresource), subresource.data());
        addRelatedModels(subresource, models, requestHeaders);
      }
    }
  }

  /**
   * Render a data schema to be included in the documentation response.
   * @param schema Schema to render
   * @param outputMap Output map to render into. The full name of the schema should be used as the key.
   */
  protected void renderDataModel(NamedDataSchema schema, DataMap outputMap,
      Map<String, String> requestHeaders) throws IOException
  {
    final DataMap schemaData = _codec.stringToMap(schema.toString());
    outputMap.put(schema.getFullName(), schemaData);
  }

  protected final RestLiResourceRelationship _relationships;
  protected final JacksonDataCodec _codec = new JacksonDataCodec();
  private final Map<ResourceSchema, DataMap> _relatedSchemaCache = new HashMap<>();
}
