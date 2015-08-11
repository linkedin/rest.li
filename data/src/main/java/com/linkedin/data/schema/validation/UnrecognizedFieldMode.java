package com.linkedin.data.schema.validation;


/**
 * Enum for how unrecognized fields should be handled during validation.
 * @author jpbetz
 */
public enum UnrecognizedFieldMode
{
  /**
   * Ignore unrecognized fields during validation.
   */
  IGNORE,

  /**
   * Remove unrecognized fields from data during validation.
   */
  TRIM,

  /**
   * If an unrecognized is present, then validation fails.
   */
  DISALLOW
}
