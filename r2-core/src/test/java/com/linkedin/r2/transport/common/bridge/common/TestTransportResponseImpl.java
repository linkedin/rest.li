/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.transport.common.bridge.common;

import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestTransportResponseImpl
{
  private static final Throwable THROWABLE = new Throwable();
  private static final Map<String, String> CASE_SENSITIVE_WIRE_ATTRIBUTES = new HashMap<>();
  private static final Object RESPONSE = new Object();

  @Test
  public void testSuccessResponse()
  {
    doTestSuccessResponse(TransportResponseImpl.success(RESPONSE));
    doTestSuccessResponse(TransportResponseImpl.success(RESPONSE, CASE_SENSITIVE_WIRE_ATTRIBUTES));
  }

  @Test
  public void testErrorResponse()
  {
    doTestErrorResponse(TransportResponseImpl.error(THROWABLE));
    doTestErrorResponse(TransportResponseImpl.error(THROWABLE, CASE_SENSITIVE_WIRE_ATTRIBUTES));
  }

  @Test
  public void testWireAttributeCaseInsensitivity()
  {
    doTestCaseInsensitivity(TransportResponseImpl.error(THROWABLE));
    doTestCaseInsensitivity(TransportResponseImpl.error(THROWABLE, CASE_SENSITIVE_WIRE_ATTRIBUTES));
    doTestCaseInsensitivity(TransportResponseImpl.success(RESPONSE));
    doTestCaseInsensitivity(TransportResponseImpl.success(RESPONSE, CASE_SENSITIVE_WIRE_ATTRIBUTES));
  }

  /**
   * Helper method that verifies the behavior of a successful {@link TransportResponseImpl}.
   * @param response {@link TransportResponseImpl} to test
   */
  public void doTestSuccessResponse(TransportResponse<Object> response)
  {
    assertNotNull(response);
    assertFalse(response.hasError());
    assertSame(response.getResponse(), RESPONSE);
    assertNull(response.getError());
    assertNotNull(response.getWireAttributes());
  }

  /**
   * Helper method that verifies the behavior of a erroneous {@link TransportResponseImpl}.
   * @param response {@link TransportResponseImpl} to test
   */
  public void doTestErrorResponse(TransportResponse<Object> response)
  {
    assertNotNull(response);
    assertTrue(response.hasError());
    assertNull(response.getResponse());
    assertSame(response.getError(), THROWABLE);
    assertNotNull(response.getWireAttributes());
  }

  /**
   * Helper method that verifies the wire attributes implementation in a {@link TransportResponseImpl}
   * is case-insensitive. Asserts if the implementation is not case insensitive.
   * @param response {@link TransportResponseImpl} to test
   */
  private static void doTestCaseInsensitivity(TransportResponse<Object> response)
  {
    Map<String, String> attrs = response.getWireAttributes();
    attrs.put("key", "value");
    attrs.put("KEY", "value");

    assertEquals(attrs.size(), 1);
    assertTrue(attrs.containsKey("KEY"));
    assertTrue(attrs.containsKey("Key"));

    attrs.remove("KEY");
    assertEquals(attrs.size(), 0);
  }
}
