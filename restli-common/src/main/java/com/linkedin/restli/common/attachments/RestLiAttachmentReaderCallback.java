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


/**
 * Used to register with {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader} to asynchronously
 * drive through the reading of a multipart mime envelope.
 *
 * @author Karim Vidhani
 */
public interface RestLiAttachmentReaderCallback
{
  /**
   * Invoked (at some time in the future) upon a registration with a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader}.
   * Also invoked when previous attachments are finished and new attachments are available.
   *
   * @param singleRestLiAttachmentReader the {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader}
   *                                     which can be used to walk through this attachment.
   */
  public void onNewAttachment(RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader);

  /**
   * Invoked when this reader is finished which means all attachments have been consumed.
   */
  public void onFinished();

  /**
   * Invoked as a result of calling {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader#drainAllAttachments()}.
   * This will be invoked at some time in the future when all the attachments in this reader have been drained.
   */
  public void onDrainComplete();

  /**
   * Invoked when there was an error reading attachments.
   *
   * @param throwable the Throwable that caused this to happen.
   */
  public void onStreamError(Throwable throwable);
}
