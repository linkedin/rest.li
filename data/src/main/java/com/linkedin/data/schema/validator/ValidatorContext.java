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

package com.linkedin.data.schema.validator;

import com.linkedin.data.element.DataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.validation.ValidationOptions;


/**
 * Input to the {@link Validator#validate} method.
 *
 * A {@link ValidatorContext} is used by the validator
 * framework to provide the current value (and related
 * information) to be validated and by the
 * {@link Validator#validate(ValidatorContext)} method
 * to provide messages and validation status to the
 * framework.
 */
public interface ValidatorContext
{
  /**
   * The {@link DataElement} providing the value to be validated.
   *
   * @return the {@link DataElement} providing the value to be validated.
   */
  DataElement dataElement();

  /**
   * The schema to validate against.
   * This is important when REST.li projections are present, because not all fields in the original schema could be present.
   * The default is null for backwards compatibility.
   *
   * @return the {@link DataSchema} after the REST.li projection has been applied
   */
  default DataSchema dataSchema() {
    return null;
  }

  /**
   * Add a {@link Message} to the result.
   *
   * @param message to add.
   */
  void addResult(Message message);

  /**
   * See {@link com.linkedin.data.schema.validation.ValidationResult#hasFix()}
   */
  void setHasFix(boolean value);

  /**
   * See {@link com.linkedin.data.schema.validation.ValidationResult#hasFixupReadOnlyError()}
   */
  void setHasFixupReadOnlyError(boolean value);

  /**
   * The {@link ValidationOptions} used to validate.
   *
   * @return the {@link ValidationOptions} used to validate.
   */
  ValidationOptions validationOptions();
}
