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

package com.linkedin.data.template;

import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class DynamicRecordMetadata
{
  private final Map<String, FieldDef<?>> _fieldDefMap;
  private final RecordDataSchema _recordDataSchema;

  /**
   * Construct {@link DynamicRecordMetadata} that keeps track of a {@link DynamicRecordTemplate}'s
   * {@link RecordDataSchema} and the {@link FieldDef}s used to create it.
   *
   * @param name provides the name of the dynamic record.
   * @param fieldDefs provides the {@link FieldDef}s of dynamic record.
   */
  public DynamicRecordMetadata(String name, Collection<? extends FieldDef<?>> fieldDefs)
  {
    _fieldDefMap = new HashMap<>();
    _recordDataSchema = buildSchema(name, fieldDefs);

    for (FieldDef<?> fieldDef : fieldDefs)
    {
      _fieldDefMap.put(fieldDef.getName(), fieldDef);
    }
  }

  /**
   * Build the schema of a {@link DynamicRecordTemplate}.
   *
   * @param name the name of the record.
   * @param fieldDefs the fields of the record.
   * @throws IllegalArgumentException if the {@link com.linkedin.data.schema.RecordDataSchema.Field} of the fieldDefs
   *                                  are already set.
   */
  public static RecordDataSchema buildSchema(String name, Collection<? extends FieldDef<?>> fieldDefs)
  {
    StringBuilder errorMessageBuilder = new StringBuilder();
    RecordDataSchema schema = new RecordDataSchema(new Name(name, errorMessageBuilder), RecordDataSchema.RecordType.RECORD);

    List<RecordDataSchema.Field> fields = new ArrayList<>(fieldDefs.size());
    for (FieldDef<?> fieldDef: fieldDefs)
    {
      RecordDataSchema.Field paramField = fieldDef.getField();
      if (paramField.getRecord() != null)
      {
        throw new IllegalArgumentException("Attempt to assign field "+ fieldDef.getName() + " to record " + schema.getName() + "failed: " +
                                                   "Record of field is already set to " + paramField.getRecord().getName() );
      }
      paramField.setRecord(schema);
      fields.add(paramField);
    }

    schema.setFields(fields, errorMessageBuilder);
    return schema;
  }

  public RecordDataSchema getRecordDataSchema()
  {
    return _recordDataSchema;
  }

  public FieldDef<?> getFieldDef(String name)
  {
    return _fieldDefMap.get(name);
  }

}
