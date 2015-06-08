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

package com.linkedin.restli.client;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.r2.RemoteInvocationException;


/**
 * Exposes the response from a REST operation to the client.
 *
 * @param <T> response entity template class
 *
 * @author Eran Leshem
 */
public interface ResponseFuture<T> extends Future<Response<T>>
{
  /**
   * Returns the response of a REST request. An exception is thrown in case of a communication issue or
   * an error response from the server.
  */
  Response<T> getResponse() throws RemoteInvocationException;

  /**
   * Returns the response of a REST request. An exception is thrown in case of any communication issue or
   * an error response from the server. If the specified timeout elapses before getting response a
   * {@link TimeoutException} is thrown.
   */
  Response<T> getResponse(long timeout, TimeUnit unit) throws RemoteInvocationException, TimeoutException;

  /**
   * Same as {@link #getResponse()} followed by {@link com.linkedin.restli.client.Response#getEntity()}.
   */
  T getResponseEntity() throws RemoteInvocationException;

  /**
   * Same as {@link #getResponse(long, TimeUnit)} followed by {@link com.linkedin.restli.client.Response#getEntity()}.
   */
  T getResponseEntity(long timeout, TimeUnit unit) throws RemoteInvocationException, TimeoutException;
}
