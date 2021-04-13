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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.text.StringEscapeUtils;


/**
 * Common utility functions for building fluent apis.
 */
public class SpecUtils {
  static final String FIELDS_MASK_METHOD_NAME = "Mask";
  static final String METADATA_MASK_METHOD_NAME = "MetadataMask";
  static final String JAVA_LANG_PREFIX = "java.lang";
  static final Set<String> PRIMITIVE_CLASS_NAMES = new HashSet<String>(Arrays.asList(
      Integer.class.getSimpleName(),
      Double.class.getSimpleName(),
      Boolean.class.getSimpleName(),
      String.class.getSimpleName(),
      Long.class.getSimpleName(),
      Float.class.getSimpleName()
      ));

  private SpecUtils()
  {
  }

  /**
   * For checking if two class names are from same class
   *
   * This method can be used tell, for example, if declared class name is a TypeRef to the value class
   *
   * @param valueClassName the class which has been used a value
   * @param declaredClassNameToCheck the declared class which might be different than the value class
   * @return true if two names are pointing to same class, false otherwise
   */
  public static boolean checkIsSameClass(String valueClassName, String declaredClassNameToCheck)
  {
    if (PRIMITIVE_CLASS_NAMES.contains(valueClassName) || PRIMITIVE_CLASS_NAMES.contains(declaredClassNameToCheck))
    {
      return ClassUtils.getShortClassName(valueClassName).equals(ClassUtils.getShortClassName(declaredClassNameToCheck));
    }
    else
    {
      return valueClassName.equals(declaredClassNameToCheck);
    }

  }

  public static String getClassName(ClassTemplateSpec classTemplateSpec) {
    if (classTemplateSpec instanceof PrimitiveTemplateSpec)
    {
      switch (classTemplateSpec.getSchema().getType())
      {
        case INT:
          return Integer.class.getName();
        case DOUBLE:
          return Double.class.getName();
        case BOOLEAN:
          return Boolean.class.getName();
        case STRING:
          return String.class.getName();
        case LONG:
          return Long.class.getName();
        case FLOAT:
          return Float.class.getName();
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

  public static String nameCamelCase(String name)
  {
    return RestLiToolsUtils.nameCamelCase(name);
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

  /**
   * Check whether the shortName being tested conflicts with what has been imported
   * if not, will update the mapping used for look-up
   *
   * @param shortNameMapping a mapping of short name to binding name in imports
   * @param shortName the shortName to be checked
   * @param bindingName the binding name that the shortName being checked corresponds to
   * @return true if the shortName cannot be used due to conflicts,
   *         false otherwise, and will update the mapping
   */
  public static boolean checkIfShortNameConflictAndUpdateMapping(Map<String, String> shortNameMapping, String shortName, String bindingName)
  {
    // Always shortcut java native primitive class check
    if (bindingName.startsWith(SpecUtils.JAVA_LANG_PREFIX))
    {
      return false;
    }

    if (shortNameMapping.containsKey(shortName))
    {
      return !((shortNameMapping.get(shortName)!= null) && shortNameMapping.get(shortName).equals(bindingName));
    }
    else
    {
      shortNameMapping.put(shortName, bindingName);
      return false;
    }
  }
}
