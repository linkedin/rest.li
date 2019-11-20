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


import com.linkedin.data.ByteString;
import com.linkedin.data.template.GetMode;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.multiplexer.IndividualBody;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.internal.common.DataMapConverter;

import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import java.io.IOException;
import java.net.URI;

import javax.activation.MimeTypeParseException;


/**
 * A task responsible for converting an IndividualRequest into a RestRequest and inherit cookies from the envelope request
 *
 * This task will catch all known failures and fail the task with an {@link IndividualResponseException}.
 * The task can fail immediately if the previous task failed. Any other unexpected exception can also cause the task to fail.
 *
 * @author Gary Lin
 */
/* package private */ final class SyntheticRequestCreationTask extends BaseTask<RestRequest>
{
  private final RestRequest _envelopeRequest;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final BaseTask<IndividualRequest> _individualRequest;
  private final String _individualRequestId;

  /* package private */ SyntheticRequestCreationTask(String individualRequestId, RestRequest envelopeRequest,
    ErrorResponseBuilder errorResponseBuilder, BaseTask<IndividualRequest> individualRequest)
  {
    _individualRequestId = individualRequestId;
    _envelopeRequest = envelopeRequest;
    _errorResponseBuilder = errorResponseBuilder;
    _individualRequest = individualRequest;
  }

  @Override
  protected Promise<? extends RestRequest> run(Context context) throws Throwable
  {
    if (_individualRequest.isFailed())
    {
      return Promises.error(_individualRequest.getError());
    }

    try
    {
      return Promises.value(createSyntheticRequest(_envelopeRequest, _individualRequest.get()));
    }
    catch (MimeTypeParseException e)
    {
      return Promises.error(new IndividualResponseException(HttpStatus.S_415_UNSUPPORTED_MEDIA_TYPE, "Unsupported media type for request id=" + _individualRequestId, e,
        _errorResponseBuilder));
    }
    catch (IOException e)
    {
      return Promises.error(new IndividualResponseException(HttpStatus.S_400_BAD_REQUEST, "Invalid request body for request id=" + _individualRequestId, e,
        _errorResponseBuilder));
    }
    catch (Exception e)
    {
      return Promises.error(e);
    }
  }

  private static RestRequest createSyntheticRequest(RestRequest envelopeRequest, IndividualRequest individualRequest) throws MimeTypeParseException, IOException
  {
    URI uri = URI.create(individualRequest.getRelativeUrl());
    ByteString entity = getBodyAsByteString(individualRequest);

    //
    // For mux, remove accept header, and use the default accept type aka JSON for the individual requests. If we don't
    // do this and use a codec that relies on field ordering for the overall mux response, then the overall response
    // can break, on account of individual responses inheriting that accept header and ordering their responses.
    //
    return new RestRequestBuilder(uri)
      .setMethod(individualRequest.getMethod())
      .setHeaders(individualRequest.getHeaders())
      .removeHeader(RestConstants.HEADER_ACCEPT)
      .setCookies(envelopeRequest.getCookies())
      .setEntity(entity)
      .build();
  }

  private static ByteString getBodyAsByteString(IndividualRequest individualRequest) throws MimeTypeParseException, IOException
  {
    IndividualBody body = individualRequest.getBody(GetMode.NULL);

    ByteString entity = ByteString.empty();
    if (body != null)
    {
      entity = DataMapConverter.dataMapToByteString(individualRequest.getHeaders(), body.data());
    }
    return entity;
  }
}
