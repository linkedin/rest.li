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

package com.linkedin.pegasus.generator;

import com.linkedin.pegasus.generator.spec.ArrayTemplateSpec;
import com.linkedin.pegasus.generator.spec.EnumTemplateSpec;
import com.linkedin.pegasus.generator.spec.FixedTemplateSpec;
import com.linkedin.pegasus.generator.spec.MapTemplateSpec;
import com.linkedin.pegasus.generator.spec.RecordTemplateSpec;
import com.linkedin.pegasus.generator.spec.TyperefTemplateSpec;
import com.linkedin.pegasus.generator.spec.UnionTemplateSpec;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;

/**
 * This class is intended for testing subclassing JavaDataTemplateGenerator.
 */
public class CustomJavaDataTemplateGenerator extends JavaDataTemplateGenerator
{

  public CustomJavaDataTemplateGenerator(String defaultPackage)
  {
    super(defaultPackage);
  }

  @Override
  protected JClass getRecordClass()
  {
    return super.getRecordClass();
  }

  @Override
  protected JClass getWrappingArrayClass()
  {
    return super.getWrappingArrayClass();
  }

  @Override
  protected JClass getWrappingMapClass()
  {
    return super.getWrappingMapClass();
  }

  @Override
  protected void generateArray(JDefinedClass arrayClass,
                               ArrayTemplateSpec arrayDataTemplateSpec) throws JClassAlreadyExistsException
  {
    super.generateArray(arrayClass, arrayDataTemplateSpec);
  }

  @Override
  protected void generateEnum(JDefinedClass enumClass, EnumTemplateSpec enumSpec)
  {
    super.generateEnum(enumClass, enumSpec);
  }

  @Override
  protected void generateFixed(JDefinedClass fixedClass, FixedTemplateSpec fixedSpec)
  {
    super.generateFixed(fixedClass, fixedSpec);
  }

  @Override
  protected void generateMap(JDefinedClass mapClass, MapTemplateSpec mapSpec) throws JClassAlreadyExistsException
  {
    super.generateMap(mapClass, mapSpec);
  }

  @Override
  protected void generateRecord(JDefinedClass templateClass, RecordTemplateSpec recordSpec) throws JClassAlreadyExistsException
  {
    super.generateRecord(templateClass, recordSpec);
  }

  @Override
  protected void generateTyperef(JDefinedClass typerefClass,
                                 TyperefTemplateSpec typerefSpec)
  {
    super.generateTyperef(typerefClass, typerefSpec);
  }

  @Override
  protected void generateUnion(JDefinedClass unionClass, UnionTemplateSpec unionSpec) throws JClassAlreadyExistsException
  {
    super.generateUnion(unionClass, unionSpec);
  }
}
