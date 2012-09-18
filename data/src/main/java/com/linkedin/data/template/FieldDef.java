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

package com.linkedin.data.template;


/**
 * A dynamic record template field definition.
 *
 * @see DynamicRecordTemplate
 * @author Eran Leshem
 */
public class FieldDef<T>
{
  private final String _name;
  private final Class<T> _type;

  public FieldDef(String name, Class<T> type)
  {
    _name = name;
    _type = type;
  }

  public String getName()
  {
    return _name;
  }

  public Class<?> getType()
  {
    return _type;
  }

  @Override
  public String toString()
  {
    return "FieldDef{" + "_name='" + _name + '\'' + ", _type=" + _type.getName() + '}';
  }
}
