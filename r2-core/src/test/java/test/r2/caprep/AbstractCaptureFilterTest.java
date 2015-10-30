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

/* $Id$ */
package test.r2.caprep;

import com.linkedin.r2.caprep.CaptureFilter;
import com.linkedin.r2.caprep.db.TransientDb;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.linkedin.r2.testutils.filter.FilterUtil;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class AbstractCaptureFilterTest extends AbstractCapRepTest
{
  @Test
  public void testInitialCapture()
  {
    final RestRequest req = request();
    final RestResponse res = response();

    Assert.assertNull(getDb().replay(req));

    FilterUtil.fireUntypedRequestResponse(getFilterChain(), req, res);

    Assert.assertEquals(res, getDb().<Response>replay(req));
  }

  @Test
  public void testTwoDifferentRequests()
  {
    final RestRequest req1 = request();
    final RestRequest req2 = req1.builder().setEntity("This is a different request".getBytes()).build();
    final RestResponse res1 = response();
    final RestResponse res2 = res1.builder().setEntity("This is a different response".getBytes()).build();

    FilterUtil.fireUntypedRequestResponse(getFilterChain(), req1, res1);
    FilterUtil.fireUntypedRequestResponse(getFilterChain(), req2, res2);

    // Should have created two separate entries
    Assert.assertEquals(res1, getDb().<Response>replay(req1));
    Assert.assertEquals(res2, getDb().<Response>replay(req2));
  }

  @Test
  public void testSameRequestDifferentResponses()
  {
    final RestRequest req = request();
    final RestResponse res1 = response();
    final RestResponse res2 = res1.builder().setEntity("This is a different response".getBytes()).build();

    FilterUtil.fireUntypedRequestResponse(getFilterChain(), req, res1);
    FilterUtil.fireUntypedRequestResponse(getFilterChain(), req, res2);

    // Last one wins
    Assert.assertEquals(res2, getDb().<Response>replay(req));
  }

  @Test
  public void testException()
  {
    final RestRequest req = request();
    final Exception ex = new Exception();

    FilterUtil.fireUntypedRequestError(getFilterChain(), req, ex);

    // Request / response should not be recorded
    Assert.assertNull(getDb().replay(req));
  }

  @Override
  protected RestFilter createFilter(TransientDb db)
  {
    return new CaptureFilter(db);
  }
}
