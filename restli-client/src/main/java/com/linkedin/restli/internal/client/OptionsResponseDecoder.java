/*
   Copyright (c) 2013 LinkedIn Corp.

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
package com.linkedin.restli.internal.client;


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.common.OptionsResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.restspec.ResourceSchema;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Converts a raw RestResponse into an OptionsResponse.
 *
 * @author jbetz
 */
public class OptionsResponseDecoder extends RestResponseDecoder<OptionsResponse>
{
  public static final String RESOURCES = "resources";
  public static final String MODELS = "models";

  private static final JacksonDataCodec CODEC = new JacksonDataCodec();

  @Override
  protected OptionsResponse wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
    throws IOException
  {
    if (dataMap == null)
    {
      return null;
    }

    DataMap resources = dataMap.getDataMap(RESOURCES);
    if(resources == null) resources = new DataMap();

    HashMap<String, ResourceSchema> resourceMap = new HashMap<String, ResourceSchema>(resources.size());
    for(Map.Entry<String, Object> entry: resources.entrySet())
    {
      resourceMap.put(entry.getKey(), new ResourceSchema((DataMap)entry.getValue()));
    }

    DataMap schemas = dataMap.getDataMap(MODELS);
    if(schemas == null) schemas = new DataMap();

    HashMap<String, DataSchema> dataSchemaMap = new HashMap<String, DataSchema>(schemas.size());
    for(Map.Entry<String, Object> entry: schemas.entrySet())
    {
      String schemaText = CODEC.mapToString((DataMap)entry.getValue());
      dataSchemaMap.put(entry.getKey(), DataTemplateUtil.parseSchema(schemaText));
    }

    return new OptionsResponse(resourceMap, dataSchemaMap);
  }

  @Override
  public Class<?> getEntityClass()
  {
    return OptionsResponse.class;
  }
}
