/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.server;


import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;

import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestResourceContextImpl
{
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetIdHeader() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(RestConstants.HEADER_ID, "foobar");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetRestLiIdHeader() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(RestConstants.HEADER_RESTLI_ID, "foobar");
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testUnmodifiableHeaders() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    context.getResponseHeaders().put(RestConstants.HEADER_ID, "foobar");
  }
}
