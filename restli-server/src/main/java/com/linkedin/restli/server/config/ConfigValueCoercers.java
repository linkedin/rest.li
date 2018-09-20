package com.linkedin.restli.server.config;

import com.linkedin.parseq.function.Function1;

import java.util.HashSet;
import java.util.Set;

/**
 * Class holding some common coercers (Long, Integer, Boolean) for transforming config values to their desired data type.
 * This was inspired by how ParSeqRestClients coerces config values specified by caller.
 *
 * @author jodzga
 * @author mnchen
 */
public class ConfigValueCoercers
{
  public static final Function1<Object, Long> LONG = val -> {
    if (val instanceof Long)
    {
      return (Long)val;
    }
    else if (val instanceof Integer)
    {
      return (long)(Integer)val;
    }
    else if (val instanceof Short)
    {
      return (long)(Short)val;
    }
    else if (val instanceof String)
    {
      try
      {
        String trimmed = ((String)val).trim();
        return isHexNumber(trimmed) ? Long.decode(trimmed) : Long.valueOf(trimmed);
      }
      catch (NumberFormatException e)
      {
        throw new Exception("Caught error parsing String to Long, String value: " + val, e);
      }
    }
    throw failCoercion(val, Long.class);
  };

  public static Function1<Object, Integer> INTEGER = val -> {
    if (val instanceof Integer)
    {
      return (Integer) val;
    }
    else if (val instanceof Short)
    {
      return (int)(Short)val;
    }
    if (val instanceof String)
    {
      try
      {
        String trimmed = ((String)val).trim();
        return isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer.valueOf(trimmed);
      }
      catch (NumberFormatException e)
      {
        throw new Exception("Caught error parsing String to Integer, String value: " + val, e);
      }
    }
    throw failCoercion(val, Integer.class);
  };

  private static final Set<String> TRUE_VALUES = new HashSet<>(4);
  private static final Set<String> FALSE_VALUES = new HashSet<>(4);
  static {
    TRUE_VALUES.add("true");
    FALSE_VALUES.add("false");

    TRUE_VALUES.add("on");
    FALSE_VALUES.add("off");

    TRUE_VALUES.add("yes");
    FALSE_VALUES.add("no");

    TRUE_VALUES.add("1");
    FALSE_VALUES.add("0");
  }

  public static Function1<Object, Boolean> BOOLEAN = val -> {
    if (val instanceof Boolean)
    {
      return (Boolean) val;
    }
    if (val instanceof String)
    {
      String value = ((String)val).trim();
      if (value.length() == 0)
      {
        return null;
      }
      else if (TRUE_VALUES.contains(value))
      {
        return Boolean.TRUE;
      }
      else if (FALSE_VALUES.contains(value))
      {
        return Boolean.FALSE;
      }
    }
    throw failCoercion(val, Boolean.class);
  };

  /**
   * Determine whether the given value String indicates a hex number, i.e. needs to be
   * passed into <code>Long.decode</code> instead of <code>Long.valueOf</code> (etc).
   */
  private static boolean isHexNumber(String value) {
    int index = (value.startsWith("-") ? 1 : 0);
    return (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index));
  }

  /**
   * Generates a consistent exception that can be used if coercion fails.
   */
  private static Exception failCoercion(final Object object, final Class<?> targetType)
  {
    return new Exception("Could not convert object to " + targetType.getSimpleName() + ". Object is instance of: "
            + object.getClass().getName() + ", value: " + object.toString());
  }
}
