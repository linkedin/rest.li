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

/**
 * $Id: $
 */

package com.linkedin.restli.restspec;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.restli.common.RestConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class RestSpecCodec
{
  private static final String ACTIONS_SET_LEGACY_KEY = "actions-set";
  private static final String ACTIONS_SET_KEY = "actionsSet";
  private static final String TYPE_KEY = "type";
  private static final String PARAMETERS_KEY = "parameters";
  private static final String METADATA_KEY = "metadata";
  private static final String RETURNS_KEY = "returns";

  private static final String COLLECTION_KEY = "collection";
  private static final String ASSOCIATION_KEY = "association";
  private static final String SIMPLE_KEY = "simple";
  private static final String METHODS_KEY = "methods";
  private static final String SUPPORTS_KEY = "supports";

  private final JacksonDataCodec _dataCodec = new JacksonDataCodec();
  private final JacksonDataTemplateCodec _templateCodec = new JacksonDataTemplateCodec();

  /**
   * Initialize a default RestSpecCodec.
   */
  public RestSpecCodec()
  {
    _templateCodec.setPrettyPrinter(new DefaultPrettyPrinter());
  }

  /**
   * Reads a ResourceSchema from the given input stream, fixes it if necessary, and returns it.
   *
   * @param inputStream inputStream to read the ResourceSchema from
   * @return a ResourceSchema
   * @throws IOException
   */
  public ResourceSchema readResourceSchema(InputStream inputStream)
          throws IOException
  {
    final DataMap data = _dataCodec.readMap(inputStream);
    fixupLegacyRestspec(data);
    return new ResourceSchema(data);
  }

  /**
   * Write the given ResourceSchema to the OutputStream.
   *
   * @param schema a ResourceSchema
   * @param outputStream an outputStream
   * @throws IOException
   */
  public void writeResourceSchema(ResourceSchema schema, OutputStream outputStream)
          throws IOException
  {
    _templateCodec.writeDataTemplate(schema, outputStream, true);
  }

  /**
   * Generate a DataSchema from a JSON representation and a DataSchemaResolver.
   * 
   * @param typeText a String JSON representation of a DataSchema
   * @param schemaResolver the schemaResolver to use to resolve the typeText
   * @return a DataSchema
   */
  public static DataSchema textToSchema(String typeText, DataSchemaResolver schemaResolver)
  {
    typeText = typeText.trim();
    if (!typeText.startsWith("{") && !typeText.startsWith("\""))
    {
      //construct a valid JSON string to hand to the parser
      typeText = "\"" + typeText + "\"";
    }

    return DataTemplateUtil.parseSchema(typeText, schemaResolver);
  }

  private void fixupLegacyRestspec(DataMap data) throws IOException
  {
    if (data.containsKey(ACTIONS_SET_LEGACY_KEY))
    {
      Object actionsSet = data.remove(ACTIONS_SET_LEGACY_KEY);
      data.put(ACTIONS_SET_KEY, actionsSet);
    }

    serializeTypeFields(data, PathSpec.emptyPath());

    final DataMap methodsContainer;
    if (data.containsKey(COLLECTION_KEY))
    {
      methodsContainer = data.getDataMap(COLLECTION_KEY);
    }
    else if (data.containsKey(ASSOCIATION_KEY))
    {
      methodsContainer = data.getDataMap(ASSOCIATION_KEY);
    }
    else if (data.containsKey(SIMPLE_KEY))
    {
      methodsContainer = data.getDataMap(SIMPLE_KEY);
    }
    else
    {
      return;
    }

    if (methodsContainer.containsKey(METHODS_KEY))
    {
      return;
    }

    final DataList methods = new DataList();

    for (Object methodName: methodsContainer.getDataList(SUPPORTS_KEY))
    {
      final RestMethodSchema newMethod = new RestMethodSchema();
      newMethod.setMethod((String) methodName);
      newMethod.setParameters(new ParameterSchemaArray());

      methods.add(newMethod.data());
    }

    methodsContainer.put(METHODS_KEY, methods);
  }

  private boolean isPegasusTypeField(PathSpec path)
  {
    if (path.toString().endsWith(PARAMETERS_KEY + "/" + TYPE_KEY) ||
        path.toString().endsWith(METADATA_KEY + "/" + TYPE_KEY) ||
        path.toString().endsWith("/" + RETURNS_KEY))
    {
      return true;
    }
    return false;
  }

  private void serializeTypeFields(DataMap data, PathSpec path) throws IOException
  {
    for (Map.Entry<String, Object> entry : data.entrySet())
    {
      final PathSpec currentElement = new PathSpec(path.getPathComponents(), entry.getKey());
      if (isPegasusTypeField(currentElement) &&
              entry.getValue() instanceof DataMap)
      {
        final String value = new String(_dataCodec.mapToBytes((DataMap)entry.getValue()), RestConstants.DEFAULT_CHARSET);
        data.put(entry.getKey(), value);
      }
      else if (entry.getValue() instanceof DataMap)
      {
        serializeTypeFields((DataMap) entry.getValue(), currentElement);
      }
      else if (entry.getValue() instanceof DataList)
      {
        for (Object o : (DataList)entry.getValue())
        {
          if (o instanceof DataMap)
          {
            serializeTypeFields((DataMap) o, currentElement);
          }
        }
      }
    }
  }
}
