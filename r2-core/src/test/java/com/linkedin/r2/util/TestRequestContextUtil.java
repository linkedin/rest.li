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

package com.linkedin.r2.util;

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.util.finalizer.RequestFinalizerManagerImpl;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Tests for {@link RequestContextUtil}.
 *
 * @author Chris Zhang
 */
public class TestRequestContextUtil
{
  private static final String KEY = "key";
  private static final String VALUE = "value";

  private RequestContext _requestContext;

  @BeforeMethod
  public void setup()
  {
    _requestContext = new RequestContext();
  }

  @Test
  public void testGetObjectWithKey()
  {
    _requestContext.putLocalAttr(KEY, VALUE);

    Assert.assertEquals(RequestContextUtil.getObjectWithKey(KEY, _requestContext, String.class), VALUE);
  }

  @Test
  public void testGetObjectWithKeySuperclass()
  {
    final ArrayList<String> value = new ArrayList<>();
    _requestContext.putLocalAttr(KEY, value);

    Assert.assertEquals(RequestContextUtil.getObjectWithKey(KEY, _requestContext, List.class), value);
  }

  @Test
  public void testGetObjectWithKeyMissing()
  {
    Assert.assertNull(RequestContextUtil.getObjectWithKey(KEY, _requestContext, String.class));
  }

  @Test
  public void testGetObjectWithKeyNotInstanceOf()
  {
    _requestContext.putLocalAttr(KEY, VALUE);

    Assert.assertNull(RequestContextUtil.getObjectWithKey(KEY, _requestContext, Integer.class));
  }

  @Test
  public void testGetServerRequestFinalizerManager()
  {
    Assert.assertNull(RequestContextUtil.getServerRequestFinalizerManager(_requestContext));

    _requestContext.putLocalAttr(R2Constants.SERVER_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY,
        new RequestFinalizerManagerImpl(null, null));

    Assert.assertNotNull(RequestContextUtil.getServerRequestFinalizerManager(_requestContext));
  }

  @Test
  public void testGetClientRequestFinalizerManager()
  {
    Assert.assertNull(RequestContextUtil.getClientRequestFinalizerManager(_requestContext));

    _requestContext.putLocalAttr(R2Constants.CLIENT_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY,
        new RequestFinalizerManagerImpl(null, null));

    Assert.assertNotNull(RequestContextUtil.getClientRequestFinalizerManager(_requestContext));
  }
}