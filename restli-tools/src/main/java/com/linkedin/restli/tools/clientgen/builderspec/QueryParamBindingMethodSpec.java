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

package com.linkedin.restli.tools.clientgen.builderspec;


import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;


/**
 * Query parameter binding method specification for individual request builder.
 *
 * @author Min Chen
 */
public class QueryParamBindingMethodSpec extends RequestBuilderMethodSpec
{
  public static final String ARG_NAME = "value";

  private String _paramName;
  private ClassTemplateSpec _argType;
  private boolean _isOptional;
  private boolean _needAddParamMethod;
  private String _doc; // parameter comments

  public QueryParamBindingMethodSpec()
  {
    super();
  }

  public QueryParamBindingMethodSpec(String methodName)
  {
    super(methodName);
  }

  public String getParamName()
  {
    return _paramName;
  }

  public void setParamName(String paramName)
  {
    _paramName = paramName;
  }

  public ClassTemplateSpec getArgType()
  {
    return _argType;
  }

  public void setArgType(ClassTemplateSpec argType)
  {
    _argType = argType;
  }

  public boolean isOptional()
  {
    return _isOptional;
  }

  public void setOptional(boolean isOptional)
  {
    _isOptional = isOptional;
  }

  public boolean isNeedAddParamMethod()
  {
    return _needAddParamMethod;
  }

  public void setNeedAddParamMethod(boolean isAdd)
  {
    _needAddParamMethod = isAdd;
  }

  public String getDoc()
  {
    return _doc;
  }

  public void setDoc(String doc)
  {
    _doc = doc;
  }
}
