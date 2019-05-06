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

package com.linkedin.data.message;

import com.linkedin.data.element.DataElement;
import com.linkedin.data.template.RecordTemplate;
import java.io.IOException;
import java.util.Formatter;

/**
 * Used to provide an error status and formattable message.
 *
 * {@link Message}s may be emitted as part of validating
 * a Data object against a {@link com.linkedin.data.schema.DataSchema} or applying
 * patches.
 */
public class Message
{
  public static final String MESSAGE_FIELD_SEPARATOR = " :: ";

  private static final String ERROR = "ERROR";
  private static final String INFO = "INFO";
  private static final String DETAILS = "DETAILS";

  private final Object[] _path;
  private final boolean _error;
  private final String _format;
  private final Object[] _args;
  private final RecordTemplate _errorDetails;

  public Message(Object[] path, String format, Object... args)
  {
    this(path, true, format, args);
  }

  public Message(Object[] path, boolean error, String format, Object... args)
  {
    this(path, null, error, format, args);
  }

  public Message(Object[] path, RecordTemplate errorDetails, boolean error, String format, Object... args)
  {
    _path = path;
    _error = error;
    _format = format;
    _args = args;
    _errorDetails = errorDetails;
  }

  public Object[] getPath()
  {
    return _path;
  }

  public String getFormat()
  {
    return _format;
  }

  public Object[] getArgs()
  {
    return _args;
  }

  public boolean isError()
  {
    return _error;
  }

  public RecordTemplate getErrorDetails()
  {
    return _errorDetails;
  }

  /**
   * Return this {@link Message} if it is an error message
   * else return copy of this {@link Message} that is an error message.
   *
   * @return this {@link Message} if it is an error message
   *         else return copy of this {@link Message}
   *         that is an error message.
   */
  public Message asErrorMessage()
  {
    return _error ? this : new Message(_path, true, _format, _args);
  }

  /**
   * Return this {@link Message} if it is an info message
   * else return copy of this {@link Message} that is an info message.
   *
   * @return this {@link Message} if it is an info message
   *         else return copy of this {@link Message}
   *         that is an info message.
   */
  public Message asInfoMessage()
  {
    return _error ? new Message(_path, false, _format, _args) : this;
  }

  /**
   * Write contents of this message into the provided {@link Formatter} using
   * the provided field separator.
   *
   * @param formatter provides the {@link Formatter} to write this {@link Message} to.
   * @param fieldSeparator provides the field separator to use for separating message fields.
   * @return the provided {@link Formatter}.
   */
  public Formatter format(Formatter formatter, String fieldSeparator)
  {
    formatError(formatter);
    formatSeparator(formatter, fieldSeparator);
    formatPath(formatter);
    formatSeparator(formatter, fieldSeparator);
    formatArgs(formatter);
    formatErrorDetails(formatter);

    return formatter;
  }

  protected void formatSeparator(Formatter formatter, String fieldSeparator)
  {
    try
    {
      Appendable out = formatter.out();
      out.append(fieldSeparator);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
  }

  protected void formatError(Formatter formatter)
  {
    formatter.format(isError() ? ERROR : INFO);
  }

  protected void formatPath(Formatter formatter)
  {
    Appendable appendable = formatter.out();
    try
    {
      for (Object component : _path)
      {
        appendable.append(DataElement.SEPARATOR);
        appendable.append(component.toString());
      }
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
  }

  protected void formatArgs(Formatter formatter)
  {
    formatter.format(_format, _args);
  }

  protected void formatErrorDetails(Formatter formatter)
  {
    if (_errorDetails == null)
    {
      return;
    }

    Appendable appendable = formatter.out();

    try
    {
      appendable.append(MESSAGE_FIELD_SEPARATOR);
      appendable.append(DETAILS);
      appendable.append(MESSAGE_FIELD_SEPARATOR);
      appendable.append(_errorDetails.toString());
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Write contents of this message into the provided {@link Formatter} using
   * the the field separator provided by {@link #getFieldSeparator()}.
   *
   * @param formatter provides the {@link Formatter} to write this {@link Message} to.
   * @return the provided {@link Formatter}.
   */
  public Formatter format(Formatter formatter)
  {
    return format(formatter, getFieldSeparator());
  }

  /**
   * Allow subclasses to override the {@link Message}'s field separator.
   *
   * @return the {@link Message}'s field separator.
   */
  public String getFieldSeparator()
  {
    return MESSAGE_FIELD_SEPARATOR;
  }

  @Override
  public String toString()
  {
    String fieldSeparator = getFieldSeparator();
    StringBuilder sb = new StringBuilder();
    Formatter formatter = new Formatter(sb);
    format(formatter, fieldSeparator);
    formatter.flush();
    formatter.close();
    return sb.toString();
  }
}