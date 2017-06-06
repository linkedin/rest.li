package com.linkedin.data.avro;

import com.linkedin.data.schema.RecordDataSchema;


/**
 * Interface for passing around field overrides between different steps in the Pegasus to Avro schema conversion.
 *
 * @author Arun Ponniah Sethuramalingam
 */
interface FieldOverridesProvider {
  /**
   * Returns a default value {@link FieldOverride} if there exists one for the requested field.
   */
  FieldOverride getDefaultValueOverride(RecordDataSchema.Field field);

  /**
   * Returns a schema {@link FieldOverride} if there exists one for the requested field.
   */
  FieldOverride getSchemaOverride(RecordDataSchema.Field field);
}
