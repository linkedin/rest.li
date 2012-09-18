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

package com.linkedin.data.schema.validation;

import com.linkedin.data.message.Message;
import java.util.Collection;

/**
 * Stores a validation result.
 *
 * @author slim
 */
public interface ValidationResult
{
  /**
   * Return whether any fixes has been proposed.
   *
   * Proposed fixes will not be applied to read-only objects.
   * This should return true only if {@link com.linkedin.data.schema.validation.ValidationOptions}'s
   * {@code fixup} attribute is true.
   *
   * @return true if at least one fix has been proposed.
   */
  public boolean hasFix();

  /**
   * Return whether any fixes could not be applied because of read-only complex objects.
   *
   * If this method returns true, {@link #hasFix()} also always return true.
   *
   * @return true if at least one proposed fix cannot be applied because a read-only
   *         complex object cannot be changed.
   */
  public boolean hasFixupReadOnlyError();

  /**
   * Get the fixed Data object.
   *
   * The fixed Data object may be the input Data object if no fixes
   * have been applied or the fixes can be applied in place.
   * For complex objects, fixes are applied in place.
   * For primitive objects, fixes cannot be applied in place because
   * they are immutable, hence new Data object must be returned if there
   * is a fix.
   *
   * @return the fixed Data object.
   */
  public Object getFixed();

  /**
   * Get whether the fixed Data object is valid for the given {@link com.linkedin.data.schema.DataSchema}.
   *
   * @return whether the fixed Data object is valid for the given {@link com.linkedin.data.schema.DataSchema}.
   */
  public boolean isValid();

  /**
   * Get all the messages emitted during the validation process.
   */
  public Collection<Message> getMessages();
}
