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


import com.linkedin.data.Data;
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
import java.util.Iterator;
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
  public void renderResourceHome(OutputStream out)
  {
    final DataMap outputMap = createEmptyOutput();

    try
    {
      for (ResourceSchema resourceSchema:
           new HashSet<ResourceSchema>(_relationships.getResourceSchemaCollection().getResources().values()))
      {
        renderResource(resourceSchema, outputMap);
      }

      _codec.writeMap(outputMap, out);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public void renderResource(String resourceName, OutputStream out)
  {
    final ResourceSchema resourceSchema = _relationships.getResourceSchemaCollection().getResource(resourceName);
    if (resourceSchema == null)
    {
      throw new RoutingException(String.format("Resource named '%s' does not exist", resourceName), 404) ;
    }

    final DataMap outputMap = createEmptyOutput();

    try
    {
      renderResource(resourceSchema, outputMap);
      _codec.writeMap(outputMap, out);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public void renderDataModelHome(OutputStream out)
  {
    final DataMap outputMap = createEmptyOutput();

    try
    {
      for (NamedDataSchema schema: new HashSet<NamedDataSchema>(_relationships.getDataModels().values()))
      {
        renderDataModel(schema, outputMap);
      }

      _codec.writeMap(outputMap, out);
    }
    catch (IOException e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public void renderDataModel(String dataModelName, OutputStream out)
  {
    final NamedDataSchema schema = _relationships.getDataModels().get(dataModelName);
    if (schema == null)
    {
      throw new RoutingException(String.format("Data model named '%s' does not exist", dataModelName), 404) ;
    }

    final DataMap outputMap = createEmptyOutput();

    try
    {
      renderDataModel(schema, outputMap);
      _codec.writeMap(outputMap, out);
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

  private void addRelatedModels(ResourceSchema resourceSchema, DataMap models) throws IOException
  {
    Map<String, DataMap> relatedSchemas;
    synchronized (this)
    {
      relatedSchemas = _relatedSchemaCache.get(resourceSchema);
      if (relatedSchemas == null)
      {
        relatedSchemas = new HashMap<String, DataMap>();
        final Node<?> node = _relationships.getRelationships(resourceSchema);
        final Iterator<Node<NamedDataSchema>> schemaItr = node.getAdjacency(NamedDataSchema.class).iterator();
        while (schemaItr.hasNext())
        {
          final NamedDataSchema currResource = (NamedDataSchema) schemaItr.next().getObject();
          relatedSchemas.put(currResource.getFullName(), _codec.bytesToMap(currResource.toString().getBytes(Data.UTF_8_CHARSET)));
        }
        _relatedSchemaCache.put(resourceSchema, relatedSchemas);
      }
    }

    models.putAll(relatedSchemas);
  }

  private void renderResource(ResourceSchema resourceSchema, DataMap outputMap) throws IOException
  {
    final DataMap resources = outputMap.getDataMap("resources");
    final DataMap models = outputMap.getDataMap("models");

    resources.put(ResourceSchemaUtil.getFullName(resourceSchema), resourceSchema.data());
    addRelatedModels(resourceSchema, models);

    final List<ResourceSchema> subresources = _relationships.getResourceSchemaCollection().getSubResources(resourceSchema);
    if (subresources != null)
    {
      for (ResourceSchema subresource: subresources)
      {
        resources.put(ResourceSchemaUtil.getFullName(subresource), subresource.data());
        addRelatedModels(subresource, models);
      }
    }
  }

  private void renderDataModel(NamedDataSchema schema, DataMap outputMap) throws IOException
  {
    final DataMap models = outputMap.getDataMap("models");
    final DataMap schemaData = _codec.bytesToMap(schema.toString().getBytes(Data.UTF_8_CHARSET));
    models.put(schema.getFullName(), schemaData);
  }

  private final RestLiResourceRelationship _relationships;
  private final JacksonDataCodec _codec = new JacksonDataCodec();
  private final Map<ResourceSchema, Map<String, DataMap>> _relatedSchemaCache =
      new HashMap<ResourceSchema, Map<String, DataMap>>();
}
