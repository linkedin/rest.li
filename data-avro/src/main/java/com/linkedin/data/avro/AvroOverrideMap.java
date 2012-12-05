package com.linkedin.data.avro;


import com.linkedin.data.schema.DataSchema;
import java.io.IOException;
import java.util.IdentityHashMap;


/**
 * Maintains map of {@link com.linkedin.data.schema.RecordDataSchema} to {@link AvroOverride}.
 */
class AvroOverrideMap
{
  private static final AvroOverride NO_AVRO_OVERRIDE = new AvroOverride(null, null, null, null);

  protected final IdentityHashMap<DataSchema, AvroOverride> _dataSchemaToAvroOverrideMap = new IdentityHashMap<DataSchema, AvroOverride>();
  protected final AvroOverrideFactory _avroOverrideFactory;

  AvroOverrideMap(AvroOverrideFactory avroOverrideFactory)
  {
    _avroOverrideFactory = avroOverrideFactory;
  }

  AvroOverride getAvroOverride(DataSchema schema)
  {
    AvroOverride avroOverride;
    if (schema.getType() == DataSchema.Type.TYPEREF)
    {
      schema = schema.getDereferencedDataSchema();
    }
    if (schema.getType() != DataSchema.Type.RECORD)
    {
      avroOverride = null;
    }
    else
    {
      avroOverride = _dataSchemaToAvroOverrideMap.get(schema);
      if (avroOverride == NO_AVRO_OVERRIDE)
      {
        avroOverride = null;
      }
      else if (avroOverride == null)
      {
        avroOverride = _avroOverrideFactory.createFromDataSchema(schema);
        _dataSchemaToAvroOverrideMap.put(schema, avroOverride == null ? NO_AVRO_OVERRIDE : avroOverride);
      }
    }
    if (avroOverride != null)
    {
      avroOverride.incrementAccessCount();
    }
    return avroOverride;
  }
}
