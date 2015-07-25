package com.linkedin.data.schema.validation;

/**
 * Enum for how unrecognized fields should be handled during validation.
 */
public enum UnrecognizedFieldMode {

  /**
   * Ignore unrecognized fields during validation.
   */
  IGNORE,

  /**
   * If an unrecognized is present, then validation fails.
   */
  DISALLOW
}
