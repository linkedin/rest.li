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

package com.linkedin.data.it;

import com.linkedin.data.element.DataElement;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;


/**
 * Evaluate if the Data object's {@link DataSchema} is a {@link NamedDataSchema}
 * and the name of the {@link DataSchema} is the specified name.
 *
 * @author slim.
 */
public class DataSchemaNameEqualsPredicate implements Predicate
{
  public DataSchemaNameEqualsPredicate(String name)
  {
    _name = name;
  }

  @Override
  public boolean evaluate(DataElement element)
  {
    DataSchema schema = element.getSchema();
    return (schema != null && schema instanceof NamedDataSchema && ((NamedDataSchema) schema).getFullName().equals(_name));
  }

  private final String _name;
}
