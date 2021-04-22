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

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import org.apache.commons.lang.ClassUtils;


public class ParameterSpec
{
  private final ParameterSchema _parameterSchema;
  private final BaseResourceSpec _root;
  private ClassTemplateSpec _classTemplateSpec;
  private ClassTemplateSpec _itemTemplateSpec;
  // a boolean flag to turn on whether show className as short name
  // Note: need to explicitly turn this flag on during imports checking
  private Boolean _usingShortClassName;
  private Boolean _usingShortItemClassName;
  private String _declaredTypeRefClassName;
  private Boolean _usingShortTypeRefClassName;


  public ParameterSpec(ParameterSchema parameterSchema, BaseResourceSpec root)
  {
    _parameterSchema = parameterSchema;
    _root = root;
    if(_parameterSchema != null) // Excluding projection parameter
    {
      String parameterClassType = _parameterSchema.getType();
      _classTemplateSpec = _root.classToTemplateSpec(parameterClassType);
      _declaredTypeRefClassName = _root.getClassRefNameForSchema(parameterClassType);

      final DataSchema typeSchema = RestSpecCodec.textToSchema(_parameterSchema.getType(), _root._schemaResolver);
      if (typeSchema instanceof ArrayDataSchema)
      {
        _itemTemplateSpec = _root.schemaToTemplateSpec(((ArrayDataSchema) typeSchema).getItems());
      }
    }
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
    return _declaredTypeRefClassName!= null &&
        !SpecUtils.checkIsSameClass(getParamClassName(), _declaredTypeRefClassName);
  }

  public String getParamTypeRefClassName()
  {
    return _declaredTypeRefClassName;
  }

  public String getParamTypeRefClassDisplayName()
  {
    if (_usingShortTypeRefClassName == null)
    {
      _usingShortTypeRefClassName = !SpecUtils.checkIfShortNameConflictAndUpdateMapping(_root.getImportCheckConflict(),
          ClassUtils.getShortClassName(getParamTypeRefClassName()),
          getParamTypeRefClassName());
    }
    return _usingShortTypeRefClassName ? ClassUtils.getShortClassName(getParamTypeRefClassName()):
        getParamTypeRefClassName();
  }

  public boolean isArray()
  {
    return _itemTemplateSpec != null;
  }

  public String getParamClassName()
  {
    return SpecUtils.getClassName(getParamClass());
  }

  public String getItemClassName()
  {
    return SpecUtils.getClassName(_itemTemplateSpec);
  }

  public String getParamClassDisplayName()
  {
    String className;
    if (isArray()) // Array parameter.
    {
      className = String.format("Iterable<%s>", getItemClassDisplayName());
    }
    else
    {
      className = getFieldClassDisplayName();
    }
    return className;
  }

  public String getFieldClassDisplayName()
  {
    if (_usingShortClassName == null)
    {
      // It seems the Marco sometimes resolves earlier than the the template
      // Unfortunately need to check the conflicts again here to figure out the correct display name,
      // even though BaseResourceSpec already did so during import resolution
      _usingShortClassName = !SpecUtils.checkIfShortNameConflictAndUpdateMapping(_root.getImportCheckConflict(),
                              ClassUtils.getShortClassName(getParamClassName()),
                                                           getParamClassName());
    }
    return _usingShortClassName ? ClassUtils.getShortClassName(getParamClassName())
        : getParamClassName();
  }

  public String getItemClassDisplayName()
  {
    if (_usingShortItemClassName == null)
    {
      _usingShortItemClassName = !SpecUtils.checkIfShortNameConflictAndUpdateMapping(_root.getImportCheckConflict(),
          ClassUtils.getShortClassName(getItemClassName()),
          getItemClassName());
    }
    return _usingShortItemClassName ? ClassUtils.getShortClassName(getItemClassName())
        : getItemClassName();
  }

  public boolean isUsingShortClassName()
  {
    return _usingShortClassName;
  }

  public void setUsingShortClassName(boolean useShortName)
  {
    this._usingShortClassName = useShortName;
  }

  public void setUsingShortTypeRefClassName(Boolean usingShortTypeRefClassName)
  {
    _usingShortTypeRefClassName = usingShortTypeRefClassName;
  }

  public void setUsingShortItemClassName(Boolean usingShortItemClassName)
  {
    _usingShortItemClassName = usingShortItemClassName;
  }
}
