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

/* $Id$ */
package com.linkedin.common.callback;

/**
 * Adapts the successful result type of a callback to another type.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class CallbackAdapter<OLD, NEW> implements Callback<NEW>
{
  private final Callback<OLD> _callback;

  protected CallbackAdapter(final Callback<OLD> callback)
  {
    _callback = callback;
  }

  /**
   * Method to be implement to do the conversion of the new response type to the old
   * response type.
   *
   * @param response
   *          new response type
   * @return an instance of the old response type
   * @throws Exception
   *           if an error occurs during conversion
   */
  protected abstract OLD convertResponse(NEW response) throws Exception;

  /**
   * Optionally override this method to convert Throwables as well.
   * @param error The original error
   * @return A Throwable suitable for delivery to the original Callback.onError()
   */
  protected Throwable convertError(final Throwable error)
  {
    return error;
  }

  @Override
  public void onSuccess(final NEW response)
  {
    try
    {
      final OLD newResponse = convertResponse(response);
      _callback.onSuccess(newResponse);
    }
    catch (Exception e)
    {
      onError(e);
    }
  }

  @Override
  public void onError(final Throwable e)
  {
    _callback.onError(convertError(e));
  }
}
