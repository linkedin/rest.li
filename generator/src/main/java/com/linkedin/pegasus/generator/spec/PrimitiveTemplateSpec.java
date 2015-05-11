/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.spec;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.PrimitiveDataSchema;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Keren Jin
 */
public class PrimitiveTemplateSpec extends ClassTemplateSpec
{
  private static Map<DataSchema.Type, PrimitiveTemplateSpec> _schemaTypeToAst = new HashMap<DataSchema.Type, PrimitiveTemplateSpec>();

  static
  {
    for (DataSchema.Type type : DataSchema.Type.values())
    {
      final PrimitiveDataSchema schema = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchema(type);
      if (schema != null)
      {
        _schemaTypeToAst.put(type, new PrimitiveTemplateSpec(schema));
      }
    }
  }

  private PrimitiveTemplateSpec(PrimitiveDataSchema schema)
  {
    setSchema(schema);
  }

  public static PrimitiveTemplateSpec getInstance(DataSchema.Type schemaType)
  {
    return _schemaTypeToAst.get(schemaType);
  }

  @Override
  public PrimitiveDataSchema getSchema()
  {
    return (PrimitiveDataSchema) super.getSchema();
  }
}
