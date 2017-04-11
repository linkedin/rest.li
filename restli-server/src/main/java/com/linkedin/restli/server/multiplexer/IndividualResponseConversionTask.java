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
import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.multiplexer.IndividualBody;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.internal.common.DataMapConverter;

import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import java.io.IOException;

import javax.activation.MimeTypeParseException;


/**
 * A task responsible for converting RestResponse to an IndividualResponseWithCookies object. This task will also convert
 * any exception carried over from previous task into an IndividualResponse. This task will never fail.
 *
 * @author Gary Lin
 */
/* package private */ final class IndividualResponseConversionTask extends BaseTask<IndividualResponseWithCookies>
{
  private final BaseTask<RestResponse> _restResponse;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final String _restResponseId;

  /* package private */ IndividualResponseConversionTask(String restResponseId, ErrorResponseBuilder errorResponseBuilder,
    BaseTask<RestResponse> restResponse)
  {
    _restResponse = restResponse;
    _errorResponseBuilder = errorResponseBuilder;
    _restResponseId = restResponseId;
  }

  @Override
  protected Promise<? extends IndividualResponseWithCookies> run(Context context) throws Throwable
  {
    if (_restResponse.isFailed())
    {
      return Promises.value(toErrorIndividualResponse(_restResponse.getError(), _errorResponseBuilder));
    }

    try
    {
      RestResponse restResponse = _restResponse.get();
      IndividualResponse response = toIndividualResponse(_restResponseId, restResponse);
      return Promises.value(new IndividualResponseWithCookies(response, restResponse.getCookies()));
    }
    catch (MimeTypeParseException e)
    {
      return Promises.value(createInternalServerErrorResponse("Invalid content type for individual response: " + _restResponseId, _errorResponseBuilder));
    }
    catch (IOException e)
    {
      return Promises.value(createInternalServerErrorResponse("Unable to set body for individual response: " + _restResponseId, _errorResponseBuilder));
    }
    catch(Exception e)
    {
      return Promises.value(toErrorIndividualResponse(e, _errorResponseBuilder));
    }
  }

  private static IndividualResponse toIndividualResponse(String id, RestResponse restResponse) throws MimeTypeParseException, IOException
  {
    IndividualResponse individualResponse = new IndividualResponse();
    individualResponse.setStatus(restResponse.getStatus());
    individualResponse.setHeaders(new StringMap(restResponse.getHeaders()));
    ByteString entity = restResponse.getEntity();
    if (!entity.isEmpty())
    {
      // TODO Avoid converting bytes to datamap here. Individual response should have only the bytes.
      individualResponse.setBody(new IndividualBody(DataMapConverter.bytesToDataMap(restResponse.getHeaders(), entity)));
    }
    return individualResponse;
  }

  private static IndividualResponseWithCookies createInternalServerErrorResponse(String message, ErrorResponseBuilder errorResponseBuilder)
  {
    return new IndividualResponseWithCookies(IndividualResponseException.createInternalServerErrorIndividualResponse(message, errorResponseBuilder));
  }

  private static IndividualResponseWithCookies toErrorIndividualResponse(Throwable error, ErrorResponseBuilder errorResponseBuilder)
  {
    // There can only be two types of errors at this stage. If any previous task failed "gracefully", it should
    // return an IndividualResponseException. Any other type of exception will be treated as unexpected error and will
    // be converted into a 500 Internal Server Error.
    if (error instanceof  IndividualResponseException)
    {
      return new IndividualResponseWithCookies(((IndividualResponseException) error).getResponse());
    }
    else
    {
      return new IndividualResponseWithCookies(IndividualResponseException.createInternalServerErrorIndividualResponse(error, errorResponseBuilder));
    }
  }
}
