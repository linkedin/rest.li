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

package com.linkedin.data.schema.compatibility;


import com.linkedin.data.message.Message;
import java.util.Formatter;


/**
 * A single message providing details on an incompatibility.
 */
public class CompatibilityMessage extends Message
{
  public enum Impact
  {
    /**
     * New reader is incompatible with old writer.
     */
    BREAKS_NEW_READER(true),
    /**
     * Old reader is incompatible with new writer.
     */
    BREAKS_OLD_READER(true),
    /**
     * New reader is incompatible with old writer and
     * Old reader is also incompatible with new writer.
     */
    BREAKS_NEW_AND_OLD_READERS(true),
    /**
     * New reader ignores data provided by old writer.
     */
    NEW_READER_IGNORES_DATA(false),
    /**
     * Old reader ignores data provided by new reader.
     */
    OLD_READER_IGNORES_DATA(false),
    /**
     * Numeric promotion.
     */
    VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW(false);

    private final boolean _error;

    Impact(boolean error)
    {
      _error = error;
    }

    public boolean isError()
    {
      return _error;
    }
  }

  private final Impact _impact;

  public CompatibilityMessage(Object[] path, Impact impact, String format, Object... args)
  {
    super(path, impact.isError(), format, args);
    _impact = impact;
  }

  private CompatibilityMessage(CompatibilityMessage message, boolean error)
  {
    super(message.getPath(), error, message.getFormat(), message.getArgs());
    _impact = message.getImpact();
  }

  public Impact getImpact()
  {
    return _impact;
  }

  @Override
  public CompatibilityMessage asErrorMessage()
  {
    return isError() ? this : new CompatibilityMessage(this, true);
  }

  @Override
  public CompatibilityMessage asInfoMessage()
  {
    return isError() ? new CompatibilityMessage(this, false) : this;
  }

  @Override
  public Formatter format(Formatter formatter, String fieldSeparator)
  {
    formatError(formatter);
    formatSeparator(formatter, fieldSeparator);
    formatCompatibilityType(formatter);
    formatSeparator(formatter, fieldSeparator);
    formatPath(formatter);
    formatSeparator(formatter, fieldSeparator);
    formatArgs(formatter);
    formatErrorDetails(formatter);

    return formatter;
  }

  private void formatCompatibilityType(Formatter formatter)
  {
    formatter.format(_impact.toString());
  }
}