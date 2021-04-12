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
import com.linkedin.restli.restspec.ActionSchema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.ClassUtils;


public class ActionMethodSpec
{
  private final ActionSchema _actionSchema;
  private final BaseResourceSpec _root;
  private ClassTemplateSpec _valueClass;
  private String _declaredValuedClassName;

  public ActionMethodSpec(ActionSchema actionSchema, BaseResourceSpec root)
  {
    _actionSchema = actionSchema;
    _root = root;
    String valueClassName = _actionSchema.getReturns();
    _valueClass = _root.classToTemplateSpec(valueClassName);
    _declaredValuedClassName = valueClassName == null? null: root.getClassRefNameForSchema(valueClassName);

  }

  public String getName()
  {
    return _actionSchema.getName();
  }

  public List<ParameterSpec> getParameters()
  {
    if (_actionSchema.getParameters() == null)
    {
      return Collections.emptyList();
    }
    List<ParameterSpec> params = new ArrayList<>(_actionSchema.getParameters().size());
    _actionSchema.getParameters().forEach(param -> params.add(new ParameterSpec(param, _root)));
    return params;
  }

  public ClassTemplateSpec getValueClass()
  {
    return _valueClass;
  }

  public String getValueClassName()
  {
    return SpecUtils.getClassName(getValueClass());
  }

  public boolean hasReturns()
  {
    return getValueClass() != null;
  }

  /**
   * Action methods with return TypeRef are defined as
   * <blockquote><pre>
   * {@code @Action}(name = "{@code <actionMethodName>}", returnTyperef={@code TypeRefToReturnType}.class)
   * public {@code <ReturnType>} {@code <actionMethodName>} (...) {}
   * </pre></blockquote>
   *
   * @return whether this action method's return type has a returnTypeRef
   */
  public boolean hasReturnTypeRef()
  {
    return hasReturns() && (_declaredValuedClassName != null) &&
        !SpecUtils.checkIsSameClass(getValueClassName(), _declaredValuedClassName);
  }

  // TODO: add to imports so can have a shortened display name
  public String getValueTypeRefClassName()
  {
    return _declaredValuedClassName;
  }
}
