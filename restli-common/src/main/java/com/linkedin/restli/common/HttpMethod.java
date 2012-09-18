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

/**
 * $Id: $
 */

package com.linkedin.restli.common;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public enum HttpMethod
{
  GET     (true,  true),
  PUT     (false, true),
  POST    (false, false),
  DELETE  (false, true),
  OPTIONS (true,  true),
  HEAD    (true,  true),
  TRACE   (true,  true);

  HttpMethod(boolean safe, boolean idempotent)
  {
    _safe = safe;
    _idempotent = idempotent;
  }

  /**
   * Determine whether this HTTP method is safe, as defined in RFC 2616
   *
   * 9.1.1 Safe Methods
   * Implementors should be aware that the software represents the user in their interactions over the Internet, and
   * should be careful to allow the user to be aware of any actions they might take which may have an unexpected
   * significance to themselves or others.
   *
   * In particular, the convention has been established that the GET and HEAD methods SHOULD NOT have the significance
   * of taking an action other than retrieval. These methods ought to be considered "safe". This allows user agents to
   * represent other methods, such as POST, PUT and DELETE, in a special way, so that the user is made aware of the
   * fact that a possibly unsafe action is being requested.
   *
   * Naturally, it is not possible to ensure that the server does not generate side-effects as a result of performing
   * a GET request; in fact, some dynamic resources consider that a feature. The important distinction here is that
   * the user did not request the side-effects, so therefore cannot be held accountable for them.
   *
   * @return a boolean
   */
  public boolean isSafe()
  {
    return _safe;
  }

  /**
   * Determine whether this HTTP method is idempotent, as defined in RFC 2616
   *
   * 9.1.2 Idempotent Methods
   *
   * Methods can also have the property of "idempotence" in that (aside from error or expiration issues) the
   * side-effects of N > 0 identical requests is the same as for a single request. The methods GET, HEAD, PUT and
   * DELETE share this property. Also, the methods OPTIONS and TRACE SHOULD NOT have side effects, and so are
   * inherently idempotent.
   *
   * However, it is possible that a sequence of several requests is non- idempotent, even if all of the methods executed
   * in that sequence are idempotent. (A sequence is idempotent if a single execution of the entire sequence always
   * yields a result that is not changed by a reexecution of all, or part, of that sequence.) For example, a sequence is
   * non-idempotent if its result depends on a value that is later modified in the same sequence.
   *
   * A sequence that never has side effects is idempotent, by definition (provided that no concurrent operations are
   * being executed on the same set of resources).
   *
   * @return a boolean
   */
  public boolean isIdempotent()
  {
    return _idempotent;
  }

  private boolean _safe;
  private boolean _idempotent;
}
