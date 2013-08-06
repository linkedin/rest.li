/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.server.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiAnnotationReader;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;

/**
 * @author dellamag
 */
public class RestLiTestHelper
{
  @SuppressWarnings("unchecked")
  public static <M extends ResourceModel> M buildResourceModel(Class<?> rootResourceClass)
  {
    return (M)RestLiAnnotationReader.processResource(rootResourceClass);
  }

  public static Map<String, ResourceModel> buildResourceModels(Class<?>... resourceClasses)
  {
    Set<Class<?>> classes = new HashSet<Class<?>>(Arrays.asList(resourceClasses));
    return RestLiApiBuilder.buildResourceModels(classes);
  }

  public static String doubleQuote(String s)
  {
    return s.replace("'", "\"");
  }

  /**
   * URL encodes a string with "=" and/or "&" present in it.
   * @param stringToEncode the string to encode
   * @return the encoded string
   */
  public static String simpleURLEncode(String stringToEncode)
  {
    return stringToEncode.replace("=", "%3D").replace("&", "%26");
  }

}
