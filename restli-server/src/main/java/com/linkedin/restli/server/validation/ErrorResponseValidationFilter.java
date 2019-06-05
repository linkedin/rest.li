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


package com.linkedin.restli.server.validation;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.errors.ServiceError;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


/**
 * Rest.li validation filter that automatically validates outgoing error response,
 * and sends a HTTP 500 error response back to the client, if the service error hasn't been defined
 * on the RestLi resource or on the RestLi method through {@link @ServiceErrors} annotation.
 *
 * <p>Validation is based entirely on the {@link @ServiceErrors} annotation.
 *
 * <p>This filter will do following checks:
 * <ul>
 *   <li>Service error code should be in the list of acceptable codes.
 *   A code not on the list will result in a 500 error.</li>
 *   <li>Error details of outgoing error response should match error detail type defined for the service error code.
 *   A HTTP 500 error response will be sent back to the client, if error detail type is not defined.</li>
 *   <li>Http status code of the error response should match with Http status code defined for the service error;
 *   otherwise HTTP 500 error response will be sent back to the client.</li>
 *</ul>
 *
 * @author Gevorg Kurghinyan
 * @author Karthik Balasubramanian
 * @author Evan Williams
 */
public class ErrorResponseValidationFilter implements Filter
{
  private static final String ERROR_MESSAGE = "Server encountered an unexpected condition that prevented it from fulfilling the request";

  @Override
  public CompletableFuture<Void> onError(Throwable throwable, final FilterRequestContext requestContext,
      final FilterResponseContext responseContext)
  {
    CompletableFuture<Void> future = new CompletableFuture<>();

    if (throwable instanceof RestLiServiceException)
    {
      RestLiServiceException restLiServiceException = (RestLiServiceException) throwable;

      // do the validation only if the 'code' field is set on RestLiServiceException.
      if (restLiServiceException.hasCode())
      {
        List<ServiceError> methodServiceErrors = requestContext.getMethodServiceErrors();
        List<ServiceError> resourceServiceErrors = requestContext.getFilterResourceModel().getServiceErrors();

        // If service error code is not defined by @ServiceErrors annotations neither on the resource level
        // nor on the method level skip the validation.
        if (methodServiceErrors == null && resourceServiceErrors == null)
        {
          // If service error is not defined neither on the resource level nor on the method level,
          // error details should not be set on RestLiServiceException object.
          if (restLiServiceException.getErrorDetailsRecord() != null)
          {
            return completeExceptionallyWithHttp500(future, restLiServiceException);
          }

          future.completeExceptionally(restLiServiceException);
          return future;
        }

        Set<ServiceError> serviceErrors = new HashSet<>();
        if (methodServiceErrors != null)
        {
          serviceErrors.addAll(methodServiceErrors);
        }

        if (resourceServiceErrors != null)
        {
          serviceErrors.addAll(resourceServiceErrors);
        }

        // An empty list of codes means that any service error code will result in a Http 500 error response.
        if (serviceErrors.isEmpty())
        {
          return completeExceptionallyWithHttp500(future, restLiServiceException);
        }

        String errorCode = restLiServiceException.getCode();

        Optional<ServiceError> maybeServiceError = serviceErrors.stream()
            .filter(serviceError -> serviceError.code().equals(errorCode)).findFirst();

        // If service error code is not defined in ServiceErrors annotation,
        // convert given throwable to 500_INTERNAL_SERVER_ERROR exception.
        if (!maybeServiceError.isPresent())
        {
          return completeExceptionallyWithHttp500(future, restLiServiceException);
        }

        ServiceError definedServiceError = maybeServiceError.get();

        // Check that the error detail type is valid.
        if (restLiServiceException.hasErrorDetails())
        {
          Class<?> errorResponseErrorDetailType = restLiServiceException.getErrorDetailsRecord().getClass();
          Class<?> definedErrorDetailType = definedServiceError.errorDetailType();

          if (!errorResponseErrorDetailType.equals(definedErrorDetailType))
          {
            return completeExceptionallyWithHttp500(future, restLiServiceException);
          }
        }

        // If http status code is not defined for the resource,
        // convert given throwable to 500_INTERNAL_SERVER_ERROR exception.
        if (definedServiceError.httpStatus() != restLiServiceException.getStatus())
        {
          return completeExceptionallyWithHttp500(future, restLiServiceException);
        }

        // TODO: validate error message. What if the defined message in service error has placeholders, which gets filled based on some business logic in the code.
      }
    }

    future.completeExceptionally(throwable);
    return future;
  }

  /**
   * If not already completed, causes invocations of #get() method of {@link CompletableFuture} and related methods
   * to throw the given exception.
   *
   * Converts given {@link RestLiServiceException} to HttpStatus.S_500_INTERNAL_SERVER_ERROR.
   *
   * @param future A {@link Future} that may be explicitly completed (setting its value and status),
   *               and may be used as a CompletionStage, supporting dependent functions
   *               and actions that trigger upon its completion.
   * @param restLiServiceException The {@link RestLiServiceException} that caused the error response.
   * @return {@link CompletableFuture<Void>} - future result of filter execution.
   */
  private CompletableFuture<Void> completeExceptionallyWithHttp500(CompletableFuture<Void> future,
      RestLiServiceException restLiServiceException)
  {
    RestLiServiceException serviceException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
        ERROR_MESSAGE, restLiServiceException);
    serviceException.setRequestId(restLiServiceException.getRequestId());

    future.completeExceptionally(serviceException);
    return future;
  }
}