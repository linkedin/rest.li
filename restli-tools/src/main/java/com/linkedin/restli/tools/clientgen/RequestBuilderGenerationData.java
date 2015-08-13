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

package com.linkedin.restli.tools.clientgen;


import com.linkedin.restli.tools.clientgen.builderspec.BuilderSpec;


/**
 * Class to represent mapping between a request builder specification with its generator template information. Currently
 * this is not used in Java request builder generator since we haven't shifted to use template to generate, but have been
 * used in Swift request builder generator.
 *
 * @author Min Chen
 */
public class RequestBuilderGenerationData
{
  private BuilderSpec _builder;
  private String _template;

  public RequestBuilderGenerationData(BuilderSpec spec)
  {
    _builder = spec;
  }

  public RequestBuilderGenerationData(BuilderSpec spec, String template)
  {
    _builder = spec;
    _template = template;
  }

  public String getTemplateName()
  {
    return _template;
  }

  public void setTemplateName(String template)
  {
    _template = template;
  }

  public BuilderSpec getBuilderSpec()
  {
    return _builder;
  }
}