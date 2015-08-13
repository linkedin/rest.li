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
import com.linkedin.restli.restspec.FinderSchema;


/**
 * Request builder metadata specification for {@link FinderSchema}.
 *
 * @author Min Chen
 */
public class FinderBuilderSpec extends RequestBuilderSpec
{
  private String _finderName;
  private ClassTemplateSpec _metadataType; // default is no custom metadata

  public FinderBuilderSpec()
  {
  }

  public FinderBuilderSpec(String packageName, String className, String baseClassName)
  {
    super(packageName, className, baseClassName);
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.FINDER;
  }

  public String getFinderName()
  {
    return _finderName;
  }

  public void setFinderName(String finderName)
  {
    this._finderName = finderName;
  }

  public ClassTemplateSpec getMetadataType()
  {
    return _metadataType;
  }

  public void setMetadataType(ClassTemplateSpec metadata)
  {
    _metadataType = metadata;
  }
}
