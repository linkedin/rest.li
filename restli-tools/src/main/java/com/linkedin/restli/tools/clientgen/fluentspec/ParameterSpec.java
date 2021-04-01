/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen.fluentspec;

import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.restli.restspec.ParameterSchema;
import org.apache.commons.lang.ClassUtils;


public class ParameterSpec
{
  private final ParameterSchema _parameterSchema;
  private final BaseResourceSpec _root;
  private final ClassTemplateSpec _classTemplateSpec;
  private final String _declaredTypeRefClassName;


  public ParameterSpec(ParameterSchema parameterSchema, BaseResourceSpec root)
  {
    _parameterSchema = parameterSchema;
    _root = root;
    String parameterClassType = _parameterSchema.getType();
    _classTemplateSpec = _root.classToTemplateSpec(parameterClassType);
    _declaredTypeRefClassName = _root.getClassRefNameForSchema(parameterClassType);
  }

  public String getParamName()
  {
    return _parameterSchema.getName();
  }

  public ParameterSchema getSchema()
  {
    return _parameterSchema;
  }

  public String getParamNameCaps()
  {
    return RestLiToolsUtils.nameCapsCase(_parameterSchema.getName());
  }

  public ClassTemplateSpec getParamClass()
  {
    return _classTemplateSpec;
  }

  public boolean hasParamTypeRef()
  {
    return !ClassUtils.getShortClassName(getParamClassName()).equals(ClassUtils.getShortClassName(
        _declaredTypeRefClassName));
  }

  // TODO: add displayable name
  public String getParamTypeRefClassName()
  {
    return _declaredTypeRefClassName;
  }

  public String getParamClassName()
  {
    return SpecUtils.getClassName(getParamClass());
  }
}
