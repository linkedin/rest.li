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


import com.linkedin.r2.message.stream.entitystream.Writer;


/**
 * Represents a custom data source that can serve as an attachment.
 *
 * @author Karim Vidhani
 */
public interface RestLiAttachmentDataSourceWriter extends Writer
{
  /**
   * Denotes a unique identifier for this attachment. It is recommended to choose identifiers with a high degree
   * of uniqueness, such as Type 1 UUIDs. For most use cases there should be a corresponding String field in a PDSC
   * to indicate affiliation.
   *
   * @return the {@link java.lang.String} representing this attachment.
   */
  public String getAttachmentID();
}