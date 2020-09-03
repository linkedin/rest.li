/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.common.validation;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.DataSchemaAnnotationValidator;
import com.linkedin.data.schema.validator.Validator;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.ResourceMethod;
import java.lang.annotation.Annotation;
import java.util.Collections;


/**
 * Extension of {@link RestLiDataValidator} to allow for validation against a provided data schema.
 *
 * @author Evan Williams
 */
public class RestLiDataSchemaDataValidator extends RestLiDataValidator {
  private final DataSchema _validatingSchema;
  private final DataValidator _inputDataValidator;
  private final DataSchemaAnnotationValidator _outputSchemaValidator;

  /**
   * Constructor.
   *
   * @param annotations annotations on the resource class
   * @param resourceMethod resource method type
   * @param validatingSchema data schema to validate against
   * @throws IllegalArgumentException if validating schema is null
   */
  public RestLiDataSchemaDataValidator(Annotation[] annotations,
      ResourceMethod resourceMethod,
      DataSchema validatingSchema)
  {
    super(annotations, null, resourceMethod, Collections.emptyMap());

    if (validatingSchema == null)
    {
      throw new IllegalArgumentException("validating schema is null");
    }

    _validatingSchema = validatingSchema;
    _inputDataValidator = new DataValidator(_validatingSchema);
    _outputSchemaValidator = new DataSchemaAnnotationValidator(_validatingSchema);
  }

  /**
   * Validate Rest.li output data (single entity) against this validator's validating schema.
   * @param dataTemplate data to validate
   * @return validation result
   */
  @Override
  public ValidationResult validateOutput(RecordTemplate dataTemplate)
  {
    return super.validateOutputAgainstSchema(dataTemplate, _validatingSchema);
  }

  /**
   * Validator to use to validate the output.
   * The validator is instantiated in the constructor, so directly returns that if input is equal to _validatingSchema.
   * @param validatingSchema schema to validate against
   * @return validator
   */
  @Override
  protected Validator getValidatorForOutput(DataSchema validatingSchema) {
    if (_validatingSchema.equals(validatingSchema))
    {
      return _outputSchemaValidator;
    }
    else
    {
      return super.getValidatorForOutput(validatingSchema);
    }
  }

  @Override
  protected Validator getValidatorForInput(DataSchema validatingSchema) {
    if (_validatingSchema.equals(validatingSchema))
    {
      return _inputDataValidator;
    }
    else
    {
      return super.getValidatorForInput(validatingSchema);
    }
  }

  /**
   * @throws UnsupportedOperationException to prevent validation by projection
   */
  @Override
  public ValidationResult validateOutput(RecordTemplate dataTemplate, MaskTree projectionMask)
  {
    throw new UnsupportedOperationException("Cannot validate by projection if validating against a data schema");
  }
}
