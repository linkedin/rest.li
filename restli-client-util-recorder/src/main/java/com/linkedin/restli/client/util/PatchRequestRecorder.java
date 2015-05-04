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

package com.linkedin.restli.client.util;


import com.linkedin.data.template.DataTemplate;
import com.linkedin.restli.common.PatchRequest;


/**
 * Enhanced {@link PatchTreeRecorder} which generates {@link PatchRequest}s.
 *
 * <p> {@link PatchTreeRecorder} allows the user to create patches which does not require the original
 * {@link com.linkedin.data.template.RecordTemplate} to diff and create a patch from.
 * {@link com.linkedin.restli.client.util.PatchGenerator#diff(com.linkedin.data.template.RecordTemplate, com.linkedin.data.template.RecordTemplate)}
 * is an alternative that creates a diff from original and revised data which can also remove fields.
 *
 * <p> The usage of the API is identical to {@link PatchTreeRecorder}, except that {@link PatchRequestRecorder} can
 * also generate {@link PatchRequest} instances.
 *
 * @author jflorencio
 * @see PatchTreeRecorder
 */
public class PatchRequestRecorder<T extends DataTemplate<?>> extends PatchTreeRecorder<T>
{
  public PatchRequestRecorder(Class<T> clazz)
  {
    super(clazz);
  }

  /**
   * @return a new patch request with a copy of the state recorded by the record template proxy returned from the
   *         {@link #getRecordingProxy()} method.
   */
  public PatchRequest<T> generatePatchRequest()
  {
    return PatchRequest.createFromPatchDocument(generatePatchTree().getDataMap());
  }
}
