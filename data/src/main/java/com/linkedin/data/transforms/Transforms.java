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

package com.linkedin.data.transforms;

/**
 * Convenience methods for commonly used {@link Transform}s.
 * 
 * @author "Joe Betz<jbetz@linkedin.com>"
 * 
 */
public class Transforms
{
  public static <T> Transform<T, T> identity()
  {
    return new IdentityTransform<T>();
  }

  public static <T> Transform<Object, T> constantValue(T constant)
  {
    return new ConstantValueTransform<T>(constant);
  }
}