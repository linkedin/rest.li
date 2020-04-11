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

package com.linkedin.restli.internal.tools;


import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.internal.tools.ArgumentFileProcessor;
import com.linkedin.restli.restspec.ParameterSchema;
import java.io.IOException;


public class RestLiToolsUtils
{
  public static String normalizeUnderscores(String name)
  {
    StringBuilder output = new StringBuilder();
    boolean capitalize = false;
    for (int i = 0; i < name.length(); ++i)
    {
      if (name.charAt(i) == '_')
      {
        capitalize = true;
        continue;
      }
      else if (capitalize)
      {
        output.append(Character.toUpperCase(name.charAt(i)));
        capitalize = false;
      }
      else
      {
        output.append(name.charAt(i));
      }
    }
    return output.toString();
  }

  public static StringBuilder normalizeCaps(String name)
  {
    StringBuilder output = new StringBuilder();
    boolean boundary = true;
    for (int i =0; i <name.length(); ++i)
    {
      boolean currentUpper = Character.isUpperCase(name.charAt(i));
      if (currentUpper)
      {
        if (i ==0)
        {
          boundary = true;
        }
        else if (i ==name.length()-1)
        {
          boundary = false;
        }
        else if (Character.isLowerCase(name.charAt(i -1)) || Character.isLowerCase(name.charAt(i +1)))
        {
          boundary = true;
        }
      }

      if (boundary)
      {
        output.append(Character.toUpperCase(name.charAt(i)));
      }
      else
      {
        output.append(Character.toLowerCase(name.charAt(i)));
      }

      boundary = false;
    }
    return output;
  }

  public static StringBuilder normalizeName(String name)
  {
    return normalizeCaps(name);
  }

  public static String nameCamelCase(String name)
  {
    StringBuilder builder = normalizeName(name);
    char firstLower = Character.toLowerCase(builder.charAt(0));
    builder.setCharAt(0, firstLower);
    return builder.toString();
  }

  public static String nameCapsCase(String name)
  {
    return normalizeName(name).toString();
  }

  public static boolean isParameterOptional(ParameterSchema param)
  {
    boolean optional = param.isOptional() == null ? false : param.isOptional();
    return optional || param.hasDefault();
  }

  /**
   * Reads and returns the resolver path from system property {@link AbstractGenerator#GENERATOR_RESOLVER_PATH}.
   * If the value points to an arg file, reads the contents of the file and returns it.
   */
  public static String getResolverPathFromSystemProperty() throws IOException
  {
    String resolverPath = System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    return readArgFromFileIfNeeded(resolverPath);
  }

  /**
   * If argValue points to an arg file, reads the contents of the file and returns it.
   */
  public static String readArgFromFileIfNeeded(String argValue) throws IOException
  {
    if (argValue != null && ArgumentFileProcessor.isArgFile(argValue))
    {
      // The arg value is an arg file, prefixed with '@' and containing the actual value
      String[] argFileContents = ArgumentFileProcessor.getContentsAsArray(argValue);
      argValue = argFileContents.length > 0 ? argFileContents[0] : null;
    }
    return argValue;
  }
}
