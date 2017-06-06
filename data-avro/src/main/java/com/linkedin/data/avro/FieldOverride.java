package com.linkedin.data.avro;

import com.linkedin.data.Data;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;


/**
 * Simple POJO class to hold references to a {@link com.linkedin.data.schema.RecordDataSchema.Field}'s
 * {@link DataSchema schema} and {@link Object value}.
 *
 * @author Arun Ponniah Sethuramalingam
 */
class FieldOverride {
  final private DataSchema _schema;
  final private Object _value;

  FieldOverride(DataSchema schema, Object value)
  {
    _schema = schema;
    _value = value;
  }

  public DataSchema getSchema() {
    return _schema;
  }

  public Object getValue() {
    return _value;
  }

  public String toString()
  {
    return _schema + " " + _value;
  }

  static FieldOverride NULL_DEFAULT_VALUE =
      new FieldOverride(DataSchemaConstants.NULL_DATA_SCHEMA, Data.NULL);
}
