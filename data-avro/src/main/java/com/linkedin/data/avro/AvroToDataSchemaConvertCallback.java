package com.linkedin.data.avro;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.linkedin.data.schema.DataSchemaConstants.NULL_DATA_SCHEMA;

/**
 * Implementation of {@link com.linkedin.data.schema.DataSchemaTraverse.Callback} for translating Avro union fields to
 * Pegasus optional fields, if that is appropriate.
 */
class AvroToDataSchemaConvertCallback implements DataSchemaTraverse.Callback
{
  static final AvroToDataSchemaConvertCallback INSTANCE = new AvroToDataSchemaConvertCallback();

  private AvroToDataSchemaConvertCallback()
  {
  }

  @Override
  public void callback(List<String> path, DataSchema schema)
  {
    if (schema.getType() != DataSchema.Type.RECORD)
    {
      return;
    }
    RecordDataSchema recordSchema = (RecordDataSchema) schema;
    for (RecordDataSchema.Field field : recordSchema.getFields())
    {
      DataSchema fieldSchema = field.getType();
      // check if union
      boolean isUnion = fieldSchema.getDereferencedType() == DataSchema.Type.UNION;
      field.setOptional(false);
      if (isUnion) {
        UnionDataSchema unionSchema = (UnionDataSchema) fieldSchema;
        // check if union with null
        if (unionSchema.contains(NULL_DATA_SCHEMA.getUnionMemberKey()))
        {
          List<UnionDataSchema.Member> nonNullMembers = unionSchema.getMembers().stream()
              .filter(member -> member.getType().getType() != NULL_DATA_SCHEMA.getType())
              .collect(Collectors.toCollection(ArrayList::new));

          if (nonNullMembers.size() == 1)
          {
            field.setType(nonNullMembers.get(0).getType());
          }
          else
          {
            StringBuilder errorMessages = null; // not expecting errors
            unionSchema.setMembers(nonNullMembers, errorMessages);
          }
          // set to optional
          field.setOptional(true);
        }
      }
    }
  }
}
