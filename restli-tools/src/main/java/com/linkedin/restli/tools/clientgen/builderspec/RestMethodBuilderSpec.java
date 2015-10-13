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


import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;


/**
 * Request builder metadata specification for {@link RestMethodSchema}.
 *
 * @author Min Chen
 */
public class RestMethodBuilderSpec extends RequestBuilderSpec
{
  private ResourceMethod _method;

  public RestMethodBuilderSpec(String method)
  {
    _method = ResourceMethod.fromString(method);
  }

  public RestMethodBuilderSpec(String packageName, String className, String baseClassName, ResourceSchema resource, String method)
  {
    super(packageName, className, baseClassName, resource);
    _method = ResourceMethod.fromString(method);
  }

  public ResourceMethod getResourceMethod()
  {
    return _method;
  }
}
