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


import java.io.File;


/**
 * Location is provided by an input string.
 *
 * @author slim
 */
public class StringDataSchemaLocation implements DataSchemaLocation
{
  private final String _string;

  public StringDataSchemaLocation(String string)
  {
    _string = string;
  }

  @Override
  public int hashCode()
  {
    return _string.hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    return this == o ||
           (o instanceof StringDataSchemaLocation &&
            _string.equals(((StringDataSchemaLocation) o)._string));
  }

  @Override
  public String toString()
  {
    return _string;
  }

  @Override
  public File getSourceFile()
  {
    return null;
  }
}