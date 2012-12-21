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
