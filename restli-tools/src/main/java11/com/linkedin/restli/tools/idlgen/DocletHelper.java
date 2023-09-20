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
import com.sun.source.doctree.DocTree;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
   * Return the string representation of a list of {@link DocTree}.
   *
   * @param docTreeList a list of {@link DocTree}
   * @return string representation of the docTreeList
   */
  public static String convertDocTreeListToStr(List<? extends DocTree> docTreeList) {
    List<String> docTreeStrList = docTreeList.stream().map(
        docTree -> {return docTree.toString();}
    ).collect(Collectors.toList());
    return String.join("", docTreeStrList);
  }
}