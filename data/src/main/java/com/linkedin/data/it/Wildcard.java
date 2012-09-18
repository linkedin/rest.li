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

package com.linkedin.data.it;


/**
 * Wildcard pattern components.
 * <p>
 * @see PathMatchesPatternPredicate
 */
public enum Wildcard
{
  /**
   * Matches any zero or more path components.
   */
  ANY_ZERO_OR_MORE,
  /**
   * Matches any zero or one path components.
   */
  ANY_ZERO_OR_ONE,
  /**
   * Matches any one or more path components.
   */
  ANY_ONE_OR_MORE,
  /**
   * Matches any one path component.
   */
  ANY_ONE
}
