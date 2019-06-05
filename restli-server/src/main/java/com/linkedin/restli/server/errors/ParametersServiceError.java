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

package com.linkedin.restli.server.errors;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.annotations.ParamError;
import java.util.Arrays;
import javax.annotation.Nonnull;


/**
 * Implementation of {@link ServiceError} which defines a service error by including method parameter names.
 * This is primarily used in the Rest.li framework to construct complete service error definitions by combining
 * user-defined service errors with service error parameters, which are defined using {@link ParamError}.
 *
 * @author Evan Williams
 */
public final class ParametersServiceError implements ServiceError
{
  private HttpStatus _httpStatus;
  private String _code;
  private String _message;
  private Class<? extends RecordTemplate> _errorDetailType;
  private String[] _parameterNames;

  public ParametersServiceError(ServiceError baseServiceError, @Nonnull String[] parameterNames)
  {
    _httpStatus = baseServiceError.httpStatus();
    _code = baseServiceError.code();
    _message = baseServiceError.message();
    _errorDetailType = baseServiceError.errorDetailType();
    _parameterNames = Arrays.copyOf(parameterNames, parameterNames.length);
  }

  @Override
  public HttpStatus httpStatus()
  {
    return _httpStatus;
  }

  @Override
  public String code()
  {
    return _code;
  }

  @Override
  public String message()
  {
    return _message;
  }

  @Override
  public Class<? extends RecordTemplate> errorDetailType()
  {
    return _errorDetailType;
  }

  /**
   * Resource method parameters for which this service error applies, if any. Allowed only for method-level service
   * errors. If provided, the Rest.li framework will validate the parameter name to ensure it matches one of the
   * method's parameters.
   *
   * e.g. { 'firstName', 'lastName' }
   */
  public String[] parameterNames()
  {
    return _parameterNames;
  }
}