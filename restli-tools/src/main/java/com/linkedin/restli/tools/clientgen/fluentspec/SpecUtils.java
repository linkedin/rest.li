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

import com.linkedin.data.ByteString;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.pegasus.generator.spec.PrimitiveTemplateSpec;
import com.linkedin.pegasus.generator.spec.TyperefTemplateSpec;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;


/**
 * Common utility functions for building fluent apis.
 */
public class SpecUtils {
  private SpecUtils()
  {
  }

  public static String getClassName(ClassTemplateSpec classTemplateSpec) {
    if (classTemplateSpec instanceof PrimitiveTemplateSpec)
    {
      switch (classTemplateSpec.getSchema().getType())
      {
        case INT:
          return Integer.class.getSimpleName();
        case DOUBLE:
          return Double.class.getSimpleName();
        case BOOLEAN:
          return Boolean.class.getSimpleName();
        case STRING:
          return String.class.getSimpleName();
        case LONG:
          return Long.class.getSimpleName();
        case FLOAT:
          return Float.class.getSimpleName();
        case BYTES:
          return ByteString.class.getName();

        default:
          throw new RuntimeException("Not supported primitive: " + classTemplateSpec);
      }
    }
    else if (classTemplateSpec instanceof TyperefTemplateSpec)
    {
      return ((TyperefTemplateSpec) classTemplateSpec).getCustomInfo().getCustomClass().getFullName();
    }
    else
    {
      return classTemplateSpec.getFullName();
    }
  }

  public static String nameCapsCase(String name)
  {
    return RestLiToolsUtils.nameCapsCase(name);
  }

  public static String restMethodToClassPrefix(String name)
  {
    ResourceMethod method = ResourceMethod.fromString(name);
    switch (method)
    {
      case GET:
        return "Get";
      case BATCH_GET:
        return "BatchGet";
      case CREATE:
        return "Create";
      case BATCH_CREATE:
        return "BatchCreate";
      case PARTIAL_UPDATE:
        return "PartialUpdate";
      case UPDATE:
        return "Update";
      case BATCH_UPDATE:
        return "BatchUpdate";
      case DELETE:
        return "Delete";
      case BATCH_PARTIAL_UPDATE:
        return "BatchPartialUpdate";
      case BATCH_DELETE:
        return "BatchDelete";
      case GET_ALL:
        return "GetAll";
      default:
        return name;
    }
  }

  public static String getResourcePath(String rawPath)
  {
    if (rawPath.charAt(0) == '/')
    {
      return rawPath.substring(1);
    }
    return rawPath;
  }

  public static List<String> escapeJavaDocString(String doc)
  {
    String[] lines = doc.split("\n");
    List<String> docLines = new ArrayList<>(lines.length);
    for (String line : lines)
    {
      docLines.add(StringEscapeUtils.escapeHtml4(line));
    }
    return docLines;
  }
}
