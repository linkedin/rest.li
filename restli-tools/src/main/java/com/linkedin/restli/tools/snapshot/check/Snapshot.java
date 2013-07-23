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

package com.linkedin.restli.tools.snapshot.check;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class Snapshot
{
  public static final String MODELS_KEY = "models";
  public static final String SCHEMA_KEY = "schema";

  private static JacksonDataCodec _dataCodec = new JacksonDataCodec();
  private DataSchemaResolver _dataSchemaResolver = new DefaultDataSchemaResolver(); // each Snapshot should have its own DataSchemaResolver.
  private Map<String, NamedDataSchema> _models = new HashMap<String, NamedDataSchema>();
  private ResourceSchema _resourceSchema;

  /**
   * Create a Snapshot based on the given {@link InputStream}
   * @param inputStream an input stream that represents a {@link DataMap} with two fields: "models", which
   * @throws IOException if the inputStream cannot be parsed as a {@link DataMap} or {@link String}.
   */
  public Snapshot(InputStream inputStream) throws IOException
  {
    DataMap dataMap = _dataCodec.readMap(inputStream);
    DataList models = dataMap.getDataList(MODELS_KEY);
    for(Object modelObj : models)
    {
      NamedDataSchema dataSchema;
      if (modelObj instanceof DataMap)
      {
        DataMap model = (DataMap)modelObj;
        dataSchema = (NamedDataSchema) RestSpecCodec.textToSchema(_dataCodec.mapToString(model), _dataSchemaResolver);
      }
      else if (modelObj instanceof String)
      {
        String str = (String)modelObj;
        dataSchema = (NamedDataSchema) RestSpecCodec.textToSchema(str, _dataSchemaResolver);
      }
      else
      {
        throw new IOException("Found " + modelObj.getClass() + " in models list; Models must be strings or DataMaps.");
      }
      _models.put(dataSchema.getFullName(), dataSchema);
    }
    _resourceSchema =  new ResourceSchema(dataMap.getDataMap(SCHEMA_KEY));
  }

  /**
   * @return a map containing all of the NamedDataSchemas in this Snapshot, keyed by fully qualified schema name.
   */
  public Map<String, NamedDataSchema> getModels()
  {
    return _models;
  }

  /**
   * @return the {@link ResourceSchema} of this snapshot
   */
  public ResourceSchema getResourceSchema()
  {
    return _resourceSchema;
  }

}
