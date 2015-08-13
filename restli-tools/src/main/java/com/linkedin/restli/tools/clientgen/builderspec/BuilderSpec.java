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


/**
 * Base class for request builder metadata specification, including root request builder and individual request builder.
 *
 * @author Min Chen
 */
public abstract class BuilderSpec
{
  protected String _namespace;
  protected String _className;
  protected String _baseClassName;

  public BuilderSpec()
  {
  }

  public BuilderSpec(String packageName, String className, String baseClassName)
  {
    _namespace = packageName;
    _className = className;
    _baseClassName = baseClassName;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public void setNamespace(String packageName)
  {
    _namespace = packageName;
  }

  public String getClassName()
  {
    return _className;
  }

  public void setClassName(String className)
  {
    _className = className;
  }

  public String getBaseClassName()
  {
    return _baseClassName;
  }

  public void setBaseClassName(String baseClassName)
  {
    _baseClassName = baseClassName;
  }
}
