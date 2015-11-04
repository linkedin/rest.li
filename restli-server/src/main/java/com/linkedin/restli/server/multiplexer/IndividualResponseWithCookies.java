/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server.multiplexer;


import com.linkedin.restli.common.multiplexer.IndividualResponse;

import java.util.Collections;
import java.util.List;


/**
 * A container that contains both an IndividualResponse and a list of cookie.  This is used by the Multiplexer tasks
 * internally to represent a complete individual response.
 *
 * @author Gary Lin
 */
/* package private */ final class IndividualResponseWithCookies
{
  private final IndividualResponse _individualResponse;
  private final List<String> _cookies;

  /* package private */ IndividualResponseWithCookies(IndividualResponse individualResponse)
  {
    this(individualResponse, Collections.<String>emptyList());
  }

  /* package private */ IndividualResponseWithCookies(IndividualResponse individualResponse, List<String> cookies)
  {
    _individualResponse = individualResponse;
    if (cookies != null)
    {
      _cookies = cookies;
    }
    else
    {
      _cookies = Collections.emptyList();
    }
  }

  public IndividualResponse getIndividualResponse()
  {
    return _individualResponse;
  }

  public List<String> getCookies()
  {
    return _cookies;
  }
}
