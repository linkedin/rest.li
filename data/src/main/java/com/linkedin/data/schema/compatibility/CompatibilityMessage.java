package com.linkedin.data.schema.compatibility;


import com.linkedin.data.message.Message;
import java.util.Formatter;


/**
 * A single message providing details on an incompatibility.
 */
public class CompatibilityMessage extends Message
{
  public static enum Impact
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

    private Impact(boolean error)
    {
      _error = error;
    }

    public boolean isError()
    {
      return _error;
    }
  }

  public CompatibilityMessage(Object[] path, Impact impact, String format, Object... args)
  {
    super(path, impact.isError(), format, args);
    _impact = impact;
  }

  protected CompatibilityMessage(CompatibilityMessage message, boolean error)
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
    return formatter;
  }

  protected void formatCompatibilityType(Formatter formatter)
  {
    formatter.format(_impact.toString());
  }

  private final Impact _impact;
}
