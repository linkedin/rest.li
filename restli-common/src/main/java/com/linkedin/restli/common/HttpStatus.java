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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * Source: https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
 */
public enum HttpStatus
{
  S_100_CONTINUE(100),
  S_101_SWITCHING_PROTOCOLS(101),
  S_102_PROCESSING(102),
  S_103_EARLY_HINTS(103),
  S_200_OK(200),
  S_201_CREATED(201),
  S_202_ACCEPTED(202),
  S_203_NON_AUTHORITATIVE_INFORMATION(203),
  S_204_NO_CONTENT(204),
  S_205_RESET_CONTENT(205),
  S_206_PARTIAL_CONTENT(206),
  S_207_MULTI_STATUS(207),
  S_208_ALREADY_REPORTED(208),
  S_226_IM_USED(226),
  S_300_MULTIPLE_CHOICES(300),
  S_301_MOVED_PERMANENTLY(301),
  S_302_FOUND(302),
  S_303_SEE_OTHER(303),
  S_304_NOT_MODIFIED(304),
  S_305_USE_PROXY(305),
  S_306_SWITCH_PROXY(306),
  S_307_TEMPORARY_REDIRECT(307),
  S_308_PERMANENT_REDIRECT(308),
  S_400_BAD_REQUEST(400),
  S_401_UNAUTHORIZED(401),
  S_402_PAYMENT_REQUIRED(402),
  S_403_FORBIDDEN(403),
  S_404_NOT_FOUND(404),
  S_405_METHOD_NOT_ALLOWED(405),
  S_406_NOT_ACCEPTABLE(406),
  S_407_PROXY_AUTHENTICATION_REQUIRED(407),
  S_408_REQUEST_TIMEOUT(408),
  S_409_CONFLICT(409),
  S_410_GONE(410),
  S_411_LENGTH_REQUIRED(411),
  S_412_PRECONDITION_FAILED(412),
  S_413_REQUEST_ENTITY_TOO_LARGE(413),
  S_414_REQUEST_URI_TOO_LONG(414),
  S_415_UNSUPPORTED_MEDIA_TYPE(415),
  S_416_REQUESTED_RANGE_NOT_SATISFIABLE(416),
  S_417_EXPECTATION_FAILED(417),
  S_418_IM_A_TEAPOT(418),
  S_421_MISDIRECTED_REQUEST(421),
  S_422_UNPROCESSABLE_ENTITY(422),
  S_423_LOCKED(423),
  S_424_FAILED_DEPENDENCY(424),
  S_426_UPGRADE_REQUIRED(426),
  S_428_PRECONDITION_REQUIRED(428),
  S_429_TOO_MANY_REQUESTS(429),
  S_431_REQUEST_HEADER_FIELDS_TOO_LARGE(431),
  S_451_UNAVAILABLE_FOR_LEGAL_REASONS(451),
  S_500_INTERNAL_SERVER_ERROR(500),
  S_501_NOT_IMPLEMENTED(501),
  S_502_BAD_GATEWAY(502),
  S_503_SERVICE_UNAVAILABLE(503),
  S_504_GATEWAY_TIMEOUT(504),
  S_505_HTTP_VERSION_NOT_SUPPORTED(505),
  S_506_VARIANT_ALSO_NEGOTIATES(506),
  S_507_INSUFFICIENT_STORAGE(507),
  S_508_LOOP_DETECTED(508),
  S_510_NOT_EXTENDED(510),
  S_511_NETWORK_AUTHENTICATION_REQUIRED(511);

  private static final Map<Integer, HttpStatus> _lookup = initialize();
  private final int _code;

  /**
   * Initialize an HttpStatus based on the given code.
   *
   * @param code an int representing a valid http status code
   */
  HttpStatus(int code)
  {
    _code = code;
  }

  /**
   * @return the http status code associated with this HttpStatus
   */
  public int getCode()
  {
    return _code;
  }

  /**
   * Return the HttpStatus associated with the given status code.
   *
   * @param code an int representing a valid http status code
   * @return and HttpStatus
   */
  public static HttpStatus fromCode(int code) {
    HttpStatus httpStatus = _lookup.get(code);
    if (httpStatus == null)
      throw new IllegalArgumentException("Unrecognized HttpStatus Code:" + code);

    return httpStatus;
  }

  private static Map<Integer, HttpStatus> initialize() {
    Map<Integer, HttpStatus> result = new HashMap<Integer, HttpStatus>(HttpStatus.values().length);
    for (HttpStatus status : HttpStatus.values()) {
      result.put(status.getCode(), status);
    }
    return Collections.unmodifiableMap(result);
  }
}
