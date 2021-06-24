package com.linkedin.restli.tools.snapshot.check;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Keren Jin
 */
public abstract class AbstractSnapshot
{
  public static final String MODELS_KEY = "models";
  public static final String SCHEMA_KEY = "schema";

  protected static final JacksonDataCodec _dataCodec = new JacksonDataCodec();
  protected Map<String, NamedDataSchema> _models;
  protected ResourceSchema _resourceSchema;
  private final DataSchemaResolver _dataSchemaResolver = new DefaultDataSchemaResolver(); // each Snapshot should have its own DataSchemaResolver.

  protected Map<String, NamedDataSchema> parseModels(DataList models) throws IOException
  {
    final Map<String, NamedDataSchema> parsedModels = new HashMap<>();

    for (Object modelObj : models)
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
      parsedModels.put(dataSchema.getFullName(), dataSchema);
    }

    return parsedModels;
  }

  protected ResourceSchema parseSchema(DataMap data)
  {
    return new ResourceSchema(data);
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
