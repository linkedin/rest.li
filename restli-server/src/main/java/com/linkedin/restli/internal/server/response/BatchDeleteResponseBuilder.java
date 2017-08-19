/*
    Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.RestLiResponseData;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


public class BatchDeleteResponseBuilder extends BatchResponseBuilder<RestLiResponseData<BatchDeleteResponseEnvelope>>
{
  public BatchDeleteResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    super(errorResponseBuilder);
  }

  @Override
  RestLiResponseData<BatchDeleteResponseEnvelope> buildResponseData(HttpStatus status,
      Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap,
      Map<String, String> headers,
      List<HttpCookie> cookies)
  {
    return new RestLiResponseDataImpl<>(new BatchDeleteResponseEnvelope(status, batchResponseMap), headers, cookies);
  }
}
