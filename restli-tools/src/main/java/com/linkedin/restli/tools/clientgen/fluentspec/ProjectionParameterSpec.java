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


public class ProjectionParameterSpec extends ParameterSpec
{
  private final ClassTemplateSpec _classTemplateSpec;
  private final String _parameterName;
  private final String _methodName;

  public ProjectionParameterSpec(String name, String methodName, ClassTemplateSpec classTemplateSpec, BaseResourceSpec root)
  {
    super(null, root);
    _parameterName = name;
    _methodName = methodName == null ? _parameterName : methodName;
    _classTemplateSpec = classTemplateSpec;
  }

  public String getParamName()
  {
    return _parameterName;
  }

  public String getMethodName()
  {
    return _methodName;
  }

  public String getParamNameCaps()
  {
    return RestLiToolsUtils.nameCapsCase(_parameterName);
  }

  public ClassTemplateSpec getParamClass()
  {
    return _classTemplateSpec;
  }

  public String getParamClassName()
  {
    return SpecUtils.getClassName(getParamClass());
  }
}
