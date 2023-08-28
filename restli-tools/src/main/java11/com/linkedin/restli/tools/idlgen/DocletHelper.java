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

package com.linkedin.restli.tools.idlgen;

import com.sun.source.doctree.DocCommentTree;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Helper class that defines generic util methods related to {@link jdk.javadoc.doclet.Doclet}.
 *
 * @author Yan Zhou
 */
public class DocletHelper {
  /**
   * Get the canonical name of the inputTypeStr, which does not include any reference to its formal type parameter
   * when it comes to generic type. For example, the canonical name of the interface java.util.Set<E> is java.util.Set.
   *
   * @param inputTypeStr class/method/variable type str
   * @return canonical name of the inputTypeStr
   */
  public static String getCanonicalName(String inputTypeStr) {
    if (inputTypeStr == null) {
      return null;
    }
    Pattern pattern = Pattern.compile("<.*>");
    Matcher matcher = pattern.matcher(inputTypeStr);
    StringBuilder sb = new StringBuilder();
    int start = 0;
    while (matcher.find()) {
      sb.append(inputTypeStr.substring(start, matcher.start()));
      start = matcher.end();
    }
    sb.append(inputTypeStr.substring(start));
    return sb.toString();
  }

  /**
   * When {@link DocCommentTree} return Java Doc comment string, they wrap certain chars with commas. For example,
   * <p> will become ,<p>, This method serves to remove such redundant commas if any.
   *
   * @param inputCommentStr input Java Doc comment string generated by {@link DocCommentTree}
   * @return processed string with redundant commas removed
   */
  public static String processDocCommentStr(String inputCommentStr) {
    if (inputCommentStr == null) {
      return null;
    }
    Pattern pattern = Pattern.compile("(\\,)(<.*>|\\{@.*\\}|>|<)(\\,)?");
    Matcher matcher = pattern.matcher(inputCommentStr);
    StringBuilder sb = new StringBuilder();
    int start = 0;
    while (matcher.find()) {
      sb.append(inputCommentStr.substring(start, matcher.start()));
      int end = matcher.group(3) == null ? matcher.end() : matcher.end() - 1;
      sb.append(inputCommentStr.substring(matcher.start() + 1, end).replace(",", ""));
      start = matcher.end();
    }
    sb.append(inputCommentStr.substring(start));
    return sb.toString();
  }
}