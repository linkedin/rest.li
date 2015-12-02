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

package com.linkedin.data.schema;

import com.linkedin.data.schema.validation.ValidationOptions;
import java.util.HashMap;
import java.util.Map;

public class SchemaParserFactory implements DataSchemaParserFactory
{
  /**
   * Create a new parser that will use the specified resolver and validation options.
   *
   * @param resolver to be provided to the parser.
   * @return a new parser.
   */
  public SchemaParser create(DataSchemaResolver resolver)
  {
    SchemaParser parser = new SchemaParser(resolver);
    if (_validationOptions != null)
    {
      parser.setValidationOptions(_validationOptions);
    }
    return parser;
  }

  protected SchemaParserFactory(ValidationOptions validationOptions)
  {
    _validationOptions = validationOptions;
  }

  static public final SchemaParserFactory instance()
  {
    return instance(null);
  }

  static public final SchemaParserFactory instance(ValidationOptions validationOptions)
  {
    if (factoryMap.containsKey(validationOptions))
    {
      return factoryMap.get(validationOptions);
    }
    else
    {
      SchemaParserFactory factory = new SchemaParserFactory(validationOptions);
      factoryMap.put(validationOptions, factory);
      return factory;
    }
  }

  static private final Map<ValidationOptions, SchemaParserFactory> factoryMap =
      new HashMap<ValidationOptions, SchemaParserFactory>();
  private final ValidationOptions _validationOptions;
}
