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
 * Path Key binding method specification for individual request builder.
 *
 * @author Min Chen
 */
public class PathKeyBindingMethodSpec extends RequestBuilderMethodSpec
{
  public static final String ARG_NAME = "key";

  private String _pathKey;
  private ClassTemplateSpec _argType;

  public PathKeyBindingMethodSpec()
  {
    super();
  }

  public PathKeyBindingMethodSpec(String methodName)
  {
    super(methodName);
  }

  public String getPathKey()
  {
    return _pathKey;
  }

  public void setPathKey(String pathKey)
  {
    _pathKey = pathKey;
  }

  public ClassTemplateSpec getArgType()
  {
    return _argType;
  }

  public void setArgType(ClassTemplateSpec argType)
  {
    _argType = argType;
  }
}