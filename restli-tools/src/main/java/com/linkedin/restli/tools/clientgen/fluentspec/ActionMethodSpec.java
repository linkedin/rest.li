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
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.ClassUtils;


public class ActionMethodSpec extends MethodSpec
{
  private final ActionSchema _actionSchema;
  private ClassTemplateSpec _valueClass;
  private final boolean _isEntityAction; // Entity action will need KeyClass and idName from its resource spec
  private boolean _usingShortClassName = false;
  private Boolean _usingShortTypeRefClassName = null;
  private String _declaredValuedClassName;

  public ActionMethodSpec(ActionSchema actionSchema, BaseResourceSpec resourceSpec, boolean isEntityAction)
  {
    super(resourceSpec);
    _actionSchema = actionSchema;
    _isEntityAction = isEntityAction;
    String valueClassName = _actionSchema.getReturns();
    _valueClass = resourceSpec.classToTemplateSpec(valueClassName);
    _declaredValuedClassName = valueClassName == null? null: resourceSpec.getClassRefNameForSchema(valueClassName);
  }

  public String getName()
  {
    return _actionSchema.getName();
  }

  @Override
  public String getMethod()
  {
    return ResourceMethod.ACTION.name();
  }

  public ParameterSchemaArray getParameters()
  {
    return _actionSchema.getParameters() == null ? new ParameterSchemaArray() : _actionSchema.getParameters();
  }

  public boolean hasActionParams()
  {
    return (_actionSchema.getParameters() != null && !_actionSchema.getParameters().isEmpty());
  }

  public ClassTemplateSpec getValueClass()
  {
    if (_valueClass == null)
    {
      _valueClass = _resourceSpec.classToTemplateSpec(_actionSchema.getReturns());
    }
    return _valueClass;
  }

  public String getValueClassName()
  {
    if (getValueClass() == null)
    {
      return Void.class.getName();
    }

    return SpecUtils.getClassName(getValueClass());
  }

  public String getValueClassDisplayName()
  {
    return _usingShortClassName ? ClassUtils.getShortClassName(getValueClassName()):
        getValueClassName();
  }

  public boolean isEntityAction()
  {
    return _isEntityAction;
  }

  @Override
  public Set<ProjectionParameterSpec> getSupportedProjectionParams()
  {
    // Projection is not supported in Action sets, see
    // https://linkedin.github.io/rest.li/How-to-use-projections-in-Java
    // for details
    return Collections.emptySet();
  }

  public boolean hasReturns()
  {
    return getValueClass() != null;
  }

  public boolean isUsingShortClassName()
  {
    return _usingShortClassName;
  }

  public void setUsingShortClassName(boolean usingShortClassName)
  {
    this._usingShortClassName = usingShortClassName;
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

  public String getValueTypeRefClassName()
  {
    return _declaredValuedClassName;
  }

  public String getValuedTypeRefClassDisplayName()
  {
    if (_usingShortTypeRefClassName == null)
    {
      _usingShortTypeRefClassName = !SpecUtils.checkIfShortNameConflictAndUpdateMapping(_resourceSpec.getImportCheckConflict(),
          ClassUtils.getShortClassName(getValueTypeRefClassName()),
          getValueTypeRefClassName());
    }
    return _usingShortTypeRefClassName ? ClassUtils.getShortClassName(getValueTypeRefClassName()):
        getValueTypeRefClassName();
  }

  public void setUsingShortTypeRefClassName(Boolean usingShortTypeRefClassName) {
    _usingShortTypeRefClassName = usingShortTypeRefClassName;
  }
}
