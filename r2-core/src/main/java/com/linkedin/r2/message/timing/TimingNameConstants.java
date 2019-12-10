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

package com.linkedin.r2.message.timing;

/**
 * A central listing for all used {@link TimingKey#getName()}s, making it convenient to
 * see and find where timings are used. Any new timing names should be added here and
 * referenced in code or javadoc.
 */
public class TimingNameConstants
{
  public static final String D2_TOTAL = "d2-total";
  public static final String D2_UPDATE_PARTITION = "d2_update_partition";

  public static final String TIMED_REST_FILTER = "timed_rest_filter";
  public static final String TIMED_STREAM_FILTER = "timed_stream_filter";

  public static final String DNS_RESOLUTION = "dns_resolution";
  public static final String SSL_HANDSHAKE = "ssl_handshake";
}
