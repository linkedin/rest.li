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


import com.linkedin.pegasus.generator.CodeUtil;


/**
 * Method specification for Root request builder methods.
 *
 * @author Min Chen
 */
public class RootBuilderMethodSpec
{
  private String _name;
  private String _doc;
  private RequestBuilderSpec _return;
  private RootBuilderSpec _rootBuilder;

  public RootBuilderMethodSpec(String name, String doc, RequestBuilderSpec aReturn, RootBuilderSpec rootBuilder)
  {
    _name = name;
    _doc = doc;
    _return = aReturn;
    _rootBuilder = rootBuilder;
  }

  public String getName()
  {
    return _name;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getDoc()
  {
    return _doc;
  }

  public void setDoc(String doc)
  {
    _doc = doc;
  }

  public RequestBuilderSpec getReturn()
  {
    return _return;
  }

  public void setReturn(RequestBuilderSpec aReturn)
  {
    _return = aReturn;
  }

  public RootBuilderSpec getRootBuilder()
  {
    return _rootBuilder;
  }

  public void setRootBuilder(RootBuilderSpec rootBuilder)
  {
    _rootBuilder = rootBuilder;
  }

  public String getUniqueName()
  {
    return _rootBuilder.getResourceName() + CodeUtil.capitalize(_name);
  }
}
