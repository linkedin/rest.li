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
  private ClassTemplateSpec _classTemplateSpec;
  // a boolean flag to turn on whether show className as short name
  // Note: need to explicitly turn this flag on during imports checking
  private Boolean _usingShortClassName;

  public ParameterSpec(ParameterSchema parameterSchema, BaseResourceSpec root)
  {
    _parameterSchema = parameterSchema;
    _root = root;
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
    if (_classTemplateSpec == null)
    {
      _classTemplateSpec = _root.classToTemplateSpec(_parameterSchema.getType());
    }
    return _classTemplateSpec;
  }

  public String getParamClassName()
  {
    return SpecUtils.getClassName(getParamClass());
  }

  public String getParamClassDisplayName()
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
    return _usingShortClassName ? ClassUtils.getShortClassName(getParamClassName()):
        getParamClassName();
  }

  public boolean isUsingShortClassName()
  {
    return _usingShortClassName;
  }

  public void setUsingShortClassName(boolean useShortName)
  {
    this._usingShortClassName = useShortName;
  }

}
