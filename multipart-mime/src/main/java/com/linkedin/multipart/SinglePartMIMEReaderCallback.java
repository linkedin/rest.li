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

package com.linkedin.multipart;


import com.linkedin.data.ByteString;


/**
 * Used to register with {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader} to
 * asynchronously drive through the reading of a single part.
 *
 * Most implementations of this should pass along a reference to the {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader}
 * during construction. This way when they are invoked on {@link SinglePartMIMEReaderCallback#onPartDataAvailable(com.linkedin.data.ByteString)},
 * they can then turn around and call {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader#requestPartData()}.
 *
 * @author Karim Vidhani
 */
public interface SinglePartMIMEReaderCallback
{
  /**
   * Invoked when data is available to be read on the current part.
   *
   * @param partData the {@link com.linkedin.data.ByteString} representing the current window of part data.
   */
  public void onPartDataAvailable(ByteString partData);

  /**
   * Invoked when the current part is finished being read.
   */
  public void onFinished();

  /**
   * Invoked when the current part is finished being abandoned.
   */
  public void onAbandoned();

  /**
   * Invoked when there was an error reading from the multipart envelope.
   *
   * @param throwable the Throwable that caused this to happen.
   */
  public void onStreamError(Throwable throwable);
}