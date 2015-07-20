package com.linkedin.restli.examples.custom.types;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

import java.util.Calendar;

/**
 * Test class for custom coercing
 *
 * @author Soojung Ha
 */
@SuppressWarnings("rawtypes")
public class CalendarCoercer implements DirectCoercer<Calendar>
{
  private static final Object REGISTER_COERCER = Custom.registerCoercer(new CalendarCoercer(), Calendar.class);

  @Override
  public Integer coerceInput(Calendar object) throws ClassCastException
  {
    return object.get(Calendar.YEAR);
  }

  @Override
  public Calendar coerceOutput(Object object) throws TemplateOutputCastException
  {
    if (object instanceof Integer)
    {
      return Calendar.getInstance();
    }
    throw new TemplateOutputCastException("Output " + object + " is not an int, and cannot be coerced to " + Calendar.class.getName());
  }
}
