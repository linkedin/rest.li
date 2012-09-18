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
 * Always returns the value provided to the transformer on construction.
 * 
 * @author "Joe Betz<jbetz@linkedin.com>"
 *
 * @param <T> constant value type
 */
public class ConstantValueTransform<T> implements Transform<Object,T>
{
  private final T _constant;

  public ConstantValueTransform(T constant)
  {
    _constant = constant;
  }

  @Override
  public T apply(Object element)
  {
    return _constant;
  }
}
