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


/**
 * Used to register with {@link com.linkedin.multipart.MultiPartMIMEReader} to asynchronously
 * drive through the reading of a multipart mime envelope.
 *
 * @author Karim Vidhani
 */
public interface MultiPartMIMEReaderCallback
{
  /**
   * Invoked (at some time in the future) upon a registration with a {@link com.linkedin.multipart.MultiPartMIMEReader}.
   * Also invoked when previous parts are finished and new parts are available.
   *
   * @param singlePartMIMEReader the {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader}
   *                            which can be used to walk through this part.
   */
  public void onNewPart(final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader);

  /**
   * Invoked when this reader is finished and the multipart mime envelope has been completely read.
   */
  public void onFinished();

  /**
   * Invoked as a result of calling {@link com.linkedin.multipart.MultiPartMIMEReader#abandonAllParts()}. This will be invoked
   * at some time in the future when all the parts from this multipart mime envelope are abandoned.
   */
  public void onAbandoned();

  /**
   * Invoked when there was an error reading from the multipart envelope.
   *
   * @param throwable the Throwable that caused this to happen.
   */
  public void onStreamError(final Throwable throwable);
}