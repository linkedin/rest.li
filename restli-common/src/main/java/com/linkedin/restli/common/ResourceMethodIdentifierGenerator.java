/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.restli.common;

/**
 * This class generates a unique identifier for each resource method. The identifier is based on the resource
 * baseUriTemplate (with key names removed), the resource method, and any action or finder if appropriate.
 *
 * The resourceMethodIdentifier is available from the Request, and ResourceMethodDescriptor APIs.
 *
 * @author dmessink
 */
public class ResourceMethodIdentifierGenerator
{
  private static final char RESOURCE_METHOD_SEPARATOR = ':';
  private static final char KEY_START_CHAR = '{';
  private static final char KEY_END_CHAR = '}';
  private static final char PATH_SEPARATOR_KEY = '/';

  private ResourceMethodIdentifierGenerator() {
  }

  /**
   * Builds the resource method identifier string for a method
   */
  public static String generate(String baseUriTemplate, ResourceMethod method, String methodName) {
    final StringBuilder builder = new StringBuilder();

    if (baseUriTemplate != null) {
      builder.append(baseUriTemplate);

      // Remove any path key names (example: album/{id}/photo -> album/{}/photo)
      int index = baseUriTemplate.indexOf(KEY_START_CHAR);

      if (index >= 0) {
        while (index < builder.length()) {
          if (builder.charAt(index) == KEY_START_CHAR) {
            final int startingIndex = index;

            while (++index < builder.length()) {
              if (builder.charAt(index) == KEY_END_CHAR) {
                builder.delete(startingIndex + 1, index);

                index = startingIndex + 2;
                break;
              }
            }
          } else {
            index++;
          }
        }
      }
    }

    return builder.append(RESOURCE_METHOD_SEPARATOR).append(OperationNameGenerator.generate(method, methodName)).toString();
  }

  /**
   * Removes any path key patterns from the resource method identifier
   * @param resourceMethodIdentifier the original resource method identifier
   * @return the resource method identifier with any path keys removed
   */
  public static String stripPathKeys(String resourceMethodIdentifier) {
    if (resourceMethodIdentifier == null || resourceMethodIdentifier.isEmpty()) {
      return resourceMethodIdentifier;
    }

    int index = resourceMethodIdentifier.indexOf(KEY_START_CHAR);

    if (index >= 0) {
      final StringBuilder builder = new StringBuilder(resourceMethodIdentifier);

      while (index < builder.length()) {
        if (builder.charAt(index) == KEY_START_CHAR) {
          final int startingIndex = index;

          while (++index < builder.length()) {
            if (builder.charAt(index) == KEY_END_CHAR) {
              // Remove any proceeding path separator
              if (startingIndex > 0 && builder.charAt(startingIndex - 1) == PATH_SEPARATOR_KEY) {
                builder.delete(startingIndex - 1, index + 1);
                index = startingIndex - 1;
              } else {
                builder.delete(startingIndex, index + 1);
                index = startingIndex;
              }

              break;
            }
          }
        } else if (builder.charAt(index) == RESOURCE_METHOD_SEPARATOR) {
          break;
        } else {
          index++;
        }
      }

      return builder.toString();
    }

    return resourceMethodIdentifier;
  }
}
