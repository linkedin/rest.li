package com.linkedin.data.avro;

import com.linkedin.data.schema.RecordDataSchema;
import java.util.IdentityHashMap;
import java.util.Map;


/**
 * Builder class for creating an instance of type {@link FieldOverridesProvider}. The builder creates an instance of
 * an implementation of {@link FieldOverridesProvider} that is defined inline.
 *
 * @author Arun Ponniah Sethuramalingam
 */
class FieldOverridesBuilder {
  private Map<RecordDataSchema.Field, FieldOverride> _defaultValueOverrides;
  private Map<RecordDataSchema.Field, RecordDataSchema.Field> _schemaOverrides;

  FieldOverridesBuilder defaultValueOverrides(IdentityHashMap<RecordDataSchema.Field, FieldOverride> defaultValueOverrides)
  {
    _defaultValueOverrides = defaultValueOverrides;
    return this;
  }

  FieldOverridesBuilder schemaOverrides(IdentityHashMap<RecordDataSchema.Field, RecordDataSchema.Field> schemaOverrides)
  {
    _schemaOverrides = schemaOverrides;
    return this;
  }

  FieldOverridesProvider build()
  {
    return new FieldOverridesProvider() {
      @Override
      public FieldOverride getDefaultValueOverride(RecordDataSchema.Field field)
      {
        return (_defaultValueOverrides == null) ? null : _defaultValueOverrides.get(field);
      }

      @Override
      public RecordDataSchema.Field getSchemaOverride(RecordDataSchema.Field field)
      {
        return (_schemaOverrides == null) ? null : _schemaOverrides.get(field);
      }
    };
  }
}
