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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.ClassUtils;


public class ActionMethodSpec
{
  private final ActionSchema _actionSchema;
  private ClassTemplateSpec _valueClass;
  private final BaseResourceSpec _resourceSpec;
  private final boolean _isEntityAction; // Entity action will need KeyClass and idName from its resource spec
  private boolean _usingShortClassName = false;
  private Boolean _usingShortTypeRefClassName = null;
  private String _declaredValuedClassName;
  private List<ParameterSpec> _methodParameters;
  private List<ParameterSpec> _optionalParameters;
  private List<ParameterSpec> _requiredParameters;

  public ActionMethodSpec(ActionSchema actionSchema, BaseResourceSpec resourceSpec, boolean isEntityAction)
  {
    _actionSchema = actionSchema;
    _resourceSpec = resourceSpec;
    _isEntityAction = isEntityAction;
    String valueClassName = _actionSchema.getReturns();
    _valueClass = _resourceSpec.classToTemplateSpec(valueClassName);
    _declaredValuedClassName = valueClassName == null? null: _resourceSpec.getClassRefNameForSchema(valueClassName);

  }

  public String getName()
  {
    return _actionSchema.getName();
  }

  public List<ParameterSpec> getParameters()
  {
    if (_methodParameters == null)
    {
      if (_actionSchema.getParameters() == null)
      {
        _methodParameters = Collections.emptyList();
      }
      else
      {
        _methodParameters = new ArrayList<>(_actionSchema.getParameters().size());
        _methodParameters.addAll(getRequiredParameters());
        _methodParameters.addAll(getOptionalParameters());
      }
    }
    return _methodParameters;
  }

  public boolean hasActionParams()
  {
    return (_actionSchema.getParameters() != null && !_actionSchema.getParameters().isEmpty());
  }

  public List<ParameterSpec> getRequiredParameters()
  {
    if (_requiredParameters == null)
    {
      _requiredParameters =  new LinkedList<>();
      if (_actionSchema.getParameters() != null)
      {
        _actionSchema.getParameters().forEach(param ->
          {
            if(!param.hasOptional() && !param.hasDefault())
            {
              _requiredParameters.add(new ParameterSpec(param, _resourceSpec));
            };
          });
      }
    }
    return _requiredParameters;
  }

  public List<ParameterSpec> getOptionalParameters()
  {
    if (_optionalParameters == null)
    {
      _optionalParameters=  new LinkedList<>();
      if (_actionSchema.getParameters() != null)
      {
        _actionSchema.getParameters().forEach(param ->
          {
            if(param.hasOptional() || param.hasDefault())
            {
              _optionalParameters.add(new ParameterSpec(param, _resourceSpec));
            };
          });
      }
    }
    return _optionalParameters;
  }

  public boolean hasRequiredParams()
  {
    return getRequiredParameters().size() > 0;
  }

  public boolean hasOptionalParams()
  {
    return getOptionalParameters().size() > 0;
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

  public BaseResourceSpec getResourceSpec()
  {
    return _resourceSpec;
  }

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
