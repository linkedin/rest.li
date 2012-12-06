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
    _fieldDefMap = new HashMap<String, FieldDef<?>>();
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

    List<RecordDataSchema.Field> fields = new ArrayList<RecordDataSchema.Field>(fieldDefs.size());
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
