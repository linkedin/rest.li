package com.linkedin.restli.tools.sample;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;


public class CustomLong
{
  private Long l;

  static
  {
    Custom.registerCoercer(new CustomLongCoercer(), CustomLong.class);
  }

  public CustomLong(Long l)
  {
    this.l = l;
  }

  public Long toLong()
  {
    return l;
  }

  public static class CustomLongCoercer implements DirectCoercer<CustomLong>
  {
    @Override
    public Object coerceInput(CustomLong object)
            throws ClassCastException
    {
      return object.toLong();
    }

    @Override
    public CustomLong coerceOutput(Object object)
            throws TemplateOutputCastException
    {
      if (!(object instanceof Long) && !(object instanceof Integer))
      {
        throw new TemplateOutputCastException("Output " + object + " is not a long or integer, and cannot be coerced to " + CustomLong.class.getName());
      }
      return new CustomLong(((Number)object).longValue());
    }
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof CustomLong)
    {
      CustomLong other = (CustomLong)obj;
      return l.equals(other.l);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return l.hashCode();
  }

  @Override
  public String toString()
  {
    return "CustomLong:" + l.toString();
  }
}