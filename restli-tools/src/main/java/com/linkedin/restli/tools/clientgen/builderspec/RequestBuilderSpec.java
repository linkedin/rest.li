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
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.ResourceSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Base class for individual resource method request builder metadata specification.
 *
 * @author Min Chen
 */
public abstract class RequestBuilderSpec extends BuilderSpec
{
  protected List<String> _pathKeys;
  protected Map<String, String> _keyPathTypes;
  protected ClassTemplateSpec _keyClass;
  protected ClassTemplateSpec _valueClass;
  protected List<PathKeyBindingMethodSpec> _pathKeyMethods = new ArrayList<PathKeyBindingMethodSpec>();
  private List<QueryParamBindingMethodSpec> _queryParamMethods = new ArrayList<QueryParamBindingMethodSpec>();

  public RequestBuilderSpec()
  {
  }

  public RequestBuilderSpec(String packageName, String className, String baseClassName, ResourceSchema resourceSchema)
  {
    super(packageName, className, baseClassName, resourceSchema);
  }

  public List<String> getPathKeys()
  {
    return _pathKeys;
  }

  public void setPathKeys(List<String> pathKeys)
  {
    _pathKeys = pathKeys;
  }

  public Map<String, String> getKeyPathTypes()
  {
    return _keyPathTypes;
  }

  public void setKeyPathTypes(Map<String, String> keyPathTypes)
  {
    _keyPathTypes = keyPathTypes;
  }

  public abstract ResourceMethod getResourceMethod();

  public ClassTemplateSpec getKeyClass()
  {
    return _keyClass;
  }

  public void setKeyClass(ClassTemplateSpec keyClass)
  {
    this._keyClass = keyClass;
  }

  public ClassTemplateSpec getValueClass()
  {
    return _valueClass;
  }

  public void setValueClass(ClassTemplateSpec valueClass)
  {
    this._valueClass = valueClass;
  }

  public List<PathKeyBindingMethodSpec> getPathKeyMethods()
  {
    return _pathKeyMethods;
  }

  public void setPathKeyMethods(List<PathKeyBindingMethodSpec> pathKeyMethods)
  {
    _pathKeyMethods = pathKeyMethods;
  }

  public void addPathKeyMethod(PathKeyBindingMethodSpec pathKeyMethod)
  {
    _pathKeyMethods.add(pathKeyMethod);
  }

  public List<QueryParamBindingMethodSpec> getQueryParamMethods()
  {
    return _queryParamMethods;
  }

  public void setQueryParamMethods(List<QueryParamBindingMethodSpec> queryParamMethods)
  {
    this._queryParamMethods = queryParamMethods;
  }

  public void addQueryParamMethod(QueryParamBindingMethodSpec queryParamMethod)
  {
    _queryParamMethods.add(queryParamMethod);
  }
}
