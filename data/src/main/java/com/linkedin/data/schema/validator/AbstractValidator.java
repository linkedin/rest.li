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


import com.linkedin.data.DataMap;


/**
 * Abstract base class for a {@link Validator}.
 * <p>
 * Derived classes that can be instantiated by {@link DataSchemaAnnotationValidator}
 * must provide a constructor that takes a {@link DataMap} as argument. For more
 * details on this requirement, see {@link DataSchemaAnnotationValidator}.
 */
abstract public class AbstractValidator implements Validator
{
  private final DataMap _config;

  /**
   * Default constructor.
   * <p>
   * A {@link Validator} is most commonly instantiated by
   * {@link DataSchemaAnnotationValidator}. {@link DataSchemaAnnotationValidator}
   * expects the constructor to take a {@link DataMap} as its only argument.
   * Deprecating the default no-argument constructor
   * emphasizes that derived classes should provide a constructor
   * that takes a {@link DataMap} as an argument.
   * @deprecated Please use {@link #AbstractValidator(com.linkedin.data.DataMap)} with a {@code null} config instead
   */
  @Deprecated
  protected AbstractValidator()
  {
    _config = null;
  }

  protected AbstractValidator(DataMap config)
  {
    _config = config;
  }

  @Override
  public String toString()
  {
    return _config == null ? getClass().getSimpleName() : getClass().getSimpleName() + " : " + _config;
  }
}
