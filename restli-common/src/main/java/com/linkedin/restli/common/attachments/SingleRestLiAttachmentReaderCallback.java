/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.common.attachments;


import com.linkedin.data.ByteString;


/**
 * Used to register with {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader}
 * to asynchronously drive through the reading of a single attachment.
 *
 * Most implementations of this should pass along a reference to the {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader}
 * during construction. This way when they are invoked on
 * {@link SingleRestLiAttachmentReaderCallback#onAttachmentDataAvailable(com.linkedin.data.ByteString)},
 * they can then turn around and call {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader#requestAttachmentData()}
 * to further progress.
 *
 * @author Karim Vidhani
 */
public interface SingleRestLiAttachmentReaderCallback
{
  /**
   * Invoked when data is available to be read on the attachment.
   *
   * @param attachmentData the {@link com.linkedin.data.ByteString} representing the current window of attachment data.
   */
  public void onAttachmentDataAvailable(ByteString attachmentData);

  /**
   * Invoked when the current attachment is finished being read.
   */
  public void onFinished();

  /**
   * Invoked when the current attachment is finished being drained.
   */
  public void onDrainComplete();

  /**
   * Invoked when there was an error reading the attachments.
   *
   * @param throwable the Throwable that caused this to happen.
   */
  public void onAttachmentError(Throwable throwable);
}
