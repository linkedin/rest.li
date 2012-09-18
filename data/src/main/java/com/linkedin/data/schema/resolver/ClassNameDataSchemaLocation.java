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

package com.linkedin.data.schema.resolver;

import com.linkedin.data.schema.DataSchemaLocation;
import java.io.File;

/**
 * @author Keren Jin
 */
public class ClassNameDataSchemaLocation implements DataSchemaLocation
{
  public ClassNameDataSchemaLocation(String className)
  {
    _className = className;
  }

  @Override
  public File getSourceFile()
  {
    return null;
  }

  @Override
  public int hashCode()
  {
    return _className.hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    return o != null &&
        o instanceof ClassNameDataSchemaLocation &&
        _className.equals(((ClassNameDataSchemaLocation) o)._className);
  }

  @Override
  public String toString()
  {
    return _className;
  }

  private String _className;
}
