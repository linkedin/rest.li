/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.spec;


import com.linkedin.data.schema.NamedDataSchema;


/**
 * @author Keren Jin
 */
public class CustomInfoSpec
{
  private final NamedDataSchema _sourceSchema;
  private final NamedDataSchema _customSchema;
  private final ClassTemplateSpec _customClass;
  private final ClassTemplateSpec _coercerClass;

  public CustomInfoSpec(NamedDataSchema sourceSchema, NamedDataSchema customSchema, ClassTemplateSpec customClass, ClassTemplateSpec coercerClass)
  {
    _sourceSchema = sourceSchema;
    _customSchema = customSchema;
    _customClass = customClass;
    _coercerClass = coercerClass;
  }

  public NamedDataSchema getSourceSchema()
  {
    return _sourceSchema;
  }

  public NamedDataSchema getCustomSchema()
  {
    return _customSchema;
  }

  public ClassTemplateSpec getCustomClass()
  {
    return _customClass;
  }

  public ClassTemplateSpec getCoercerClass()
  {
    return _coercerClass;
  }

  public String toString()
  {
    return "sourceSchema=" + _sourceSchema.getFullName() + ", customSchema=" + _customSchema.getFullName() +
        ", customClass=" + _customClass + (_coercerClass != null ? (", coercerClass=" + _coercerClass) : "");
  }
}
