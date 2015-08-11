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

package com.linkedin.data.schema.validation;


import com.linkedin.data.it.Predicate;
import com.linkedin.data.it.Predicates;
import com.linkedin.util.ArgumentUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Options that control how a Data object is validated against a
 * {@link com.linkedin.data.schema.DataSchema}.
 *
 * <p>
 * The <i>required mode</i> indicates how required fields should be handled during validation,
 * see {@link RequiredMode} for a description of the various required modes.
 *
 * <p>
 * The <i>coercion mode</i> indicates whether and how primitive values should be coerced
 * from a value that does not conform to the schema to a value that conforms
 * to the schema's type, see {@link CoercionMode} for a description of the various
 * coercion modes.
 *
 * <p>
 * If fix-up occurs, the fixed-up value is returned by
 * {@link com.linkedin.data.schema.validation.ValidationResult#getFixed()}.
 * Fix-up can occur if the coercion mode is not set to {@link CoercionMode#OFF}
 * or the required mode is set to {@link RequiredMode#FIXUP_ABSENT_WITH_DEFAULT}.
 *
 * Unlike Avro, union to record schema resolution is not implemented.
 *
 * @author slim
 */
public final class ValidationOptions
{
  /**
   * Constructor.
   *
   * Sets coercion mode to {@link CoercionMode#NORMAL} and
   * required mode to {@link RequiredMode#CAN_BE_ABSENT_IF_HAS_DEFAULT}.
   */
  public ValidationOptions()
  {
    _coercionMode = CoercionMode.NORMAL;
    _requiredMode = RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT;
    _unrecognizedFieldMode = UnrecognizedFieldMode.IGNORE;
  }

  /**
   * Constructor.
   *
   * Sets coercion mode to {@link CoercionMode#NORMAL}
   *
   * @param requiredMode specifies the required mode.
   */
  public ValidationOptions(RequiredMode requiredMode)
  {
    _coercionMode = CoercionMode.NORMAL;
    _requiredMode = requiredMode;
    _unrecognizedFieldMode = UnrecognizedFieldMode.IGNORE;
  }

  /**
   * Constructor.
   *
   * @param requiredMode specifies the required mode.
   * @param coercionMode specifies the coercion mode.
   */
  public ValidationOptions(RequiredMode requiredMode, CoercionMode coercionMode)
  {
    _coercionMode = coercionMode;
    _requiredMode = requiredMode;
    _unrecognizedFieldMode = UnrecognizedFieldMode.IGNORE;
  }

  /**
   * Constructor.
   *
   * @param requiredMode specifies the required mode.
   * @param coercionMode specifies the coercion mode.
   * @param unrecognizedFieldMode specifies the unrecognized field mode.
   */
  public ValidationOptions(RequiredMode requiredMode, CoercionMode coercionMode, UnrecognizedFieldMode unrecognizedFieldMode)
  {
    _coercionMode = coercionMode;
    _requiredMode = requiredMode;
    _unrecognizedFieldMode = unrecognizedFieldMode;
  }

  /**
   * Set the coercion mode.
   *
   * @param coercionMode specifies the coercion mode.
   */
  public void setCoercionMode(CoercionMode coercionMode)
  {
    ArgumentUtil.notNull(coercionMode, "coercionMode");
    _coercionMode = coercionMode;
  }

  /**
   * Return the coercion mode.
   *
   * @return the coercion mode.
   */
  public CoercionMode getCoercionMode()
  {
    return _coercionMode;
  }

  /**
   * Return required mode that indicates how required fields should be handled during validation.
   *
   * @return the required mode.
   */
  public RequiredMode getRequiredMode()
  {
    return _requiredMode;
  }

  /**
   * Set the required mode that indicates how required fields should be handled during validation.
   *
   * @param requiredMode specifies the required mode.
   */
  public void setRequiredMode(RequiredMode requiredMode)
  {
    ArgumentUtil.notNull(requiredMode, "RequiredMode");
    _requiredMode = requiredMode;
  }

  /**
   * Returns how unrecognized fields are handled during validation.
   *
   * @return the unrecognized field mode.
   */
  public UnrecognizedFieldMode getUnrecognizedFieldMode()
  {
    return _unrecognizedFieldMode;
  }

  /**
   * Set how unrecognized fields are handled during validation.
   *
   * @param unrecognizedFieldMode provides unrecognized field mode.
   */
  public void setUnrecognizedFieldMode(UnrecognizedFieldMode unrecognizedFieldMode)
  {
    _unrecognizedFieldMode = unrecognizedFieldMode;
  }

  /**
   * Set a parameter intended to be passed to a {@link com.linkedin.data.schema.validator.Validator}.
   *
   * @param key to identify option.
   * @param parameter to pass.
   */
  public void setValidatorParameter(String key, Object parameter)
  {
    if (_validatorParameters == NO_VALIDATOR_PARAMETERS)
    {
      _validatorParameters = new HashMap<String, Object>();
    }
    _validatorParameters.put(key, parameter);
  }

  /**
   * Get a parameter intended to be passed to a {@link com.linkedin.data.schema.validator.Validator}.
   *
   * @param key to identify the option.
   * @return the value of the parameter previously associated with the key through
   *         {@link #setValidatorParameter(String, Object)} or null if key does not exists.
   */
  public Object getValidatorParameter(String key)
  {
    return _validatorParameters.get(key);
  }

  /**
   * Option to treat certain required fields as optional.
   * A required field whose corresponding data element satisfies the given {@link Predicate}
   * will be treated as optional.
   *
   * @param treatOptional
   */
  public void setTreatOptional(Predicate treatOptional)
  {
    _treatOptional = treatOptional;
  }

  /**
   * Return the predicate for treating certain fields as optional.
   *
   * @return predicate
   */
  public Predicate getTreatOptional()
  {
    return _treatOptional;
  }

  /**
   * Set Avro union mode.
   *
   * When set, data is validated as a Avro schema default value, which has a different data format
   * than other data.
   *
   * This mode should be used exclusively to validate Avro default values.
   *
   * For default values of unions in Avro, the discriminator is not present and the default
   * value always applies to the first member type of the union. E.g.:
   *
   * <pre>
   * { "type" : [ "int", null ], "default" : 5 }
   * </pre>
   *
   * This applies transitively even to default values of embedded unions.
   *
   * For additional details, see the comments about union default values here:
   * https://avro.apache.org/docs/1.7.7/spec.html#Unions
   *
   * @param value set to true to enable Avro union mode.
   */
  public void setAvroUnionMode(boolean value)
  {
    _avroUnionMode = value;
  }

  /**
   * Return whether Avro union mode is enabled.
   *
   * If Avro union mode is enabled, validate union default values according to Avro's rules.
   *
   * @return true if Avro union mode is enabled.
   * @see {@link #setAvroUnionMode(boolean)}
   */
  public boolean isAvroUnionMode()
  {
    return _avroUnionMode;
  }

  @Override
  public boolean equals(Object other)
  {
    if (other == null || other.getClass() != getClass())
    {
      return false;
    }
    ValidationOptions otherOptions = (ValidationOptions) other;
    return (otherOptions._coercionMode == _coercionMode
        && otherOptions._requiredMode == _requiredMode
        && otherOptions._unrecognizedFieldMode == _unrecognizedFieldMode
        && otherOptions._avroUnionMode == _avroUnionMode
        && otherOptions._validatorParameters.equals(_validatorParameters));
  }

  @Override
  public int hashCode()
  {
    int code = 17;
    code = code * 31 + (_requiredMode == null ? 0 : _requiredMode.hashCode());
    code = code * 31 + (_coercionMode == null ? 0 : _coercionMode.hashCode());
    code = code * 31 + (_unrecognizedFieldMode == null ? 0 : _unrecognizedFieldMode.hashCode());
    code = code * 31 + (_avroUnionMode ? 0 : 53);
    code = code * 31 + (_validatorParameters.hashCode());
    return code;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("RequiredMode=")
      .append(_requiredMode)
      .append(", FixupMode=")
      .append(_coercionMode)
      .append(", UnrecognizedFieldMode=")
      .append(_unrecognizedFieldMode)
      .append(", AvroUnionMode=")
      .append(_avroUnionMode);
    if (_validatorParameters != NO_VALIDATOR_PARAMETERS)
    {
      sb.append(", ValidatorOptions=")
        .append(_validatorParameters);
    }
    return sb.toString();
  }

  private CoercionMode _coercionMode;
  private RequiredMode _requiredMode;
  private UnrecognizedFieldMode _unrecognizedFieldMode;
  private boolean      _avroUnionMode = false;
  private Map<String,Object> _validatorParameters = NO_VALIDATOR_PARAMETERS;
  // Treat required fields as optional if the corresponding data element satisfies this predicate
  private Predicate _treatOptional = Predicates.alwaysFalse();

  private static final Map<String,Object> NO_VALIDATOR_PARAMETERS = Collections.emptyMap();
}
