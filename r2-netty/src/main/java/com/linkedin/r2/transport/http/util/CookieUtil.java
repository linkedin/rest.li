/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.util;

import java.util.List;


/**
 * @author Nizar Mankulangara (nmankulangara@linkedin.com)
 */
public class CookieUtil {
  public static final String DELIMITER = ";";

  /**
   * Encodes list of cookies in to an RFC 6265 compliant single line cookie header value
   */
  public static String clientEncode(List<String> cookiesStr) {
    if (cookiesStr.isEmpty()) {
      return null;
    }

    return String.join(DELIMITER, cookiesStr);
  }
}
