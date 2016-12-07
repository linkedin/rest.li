package com.linkedin.pegasus.generator.test.pdl.fixtures;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;


public class CustomIntCoercer implements DirectCoercer<CustomInt>
{
  static
  {
    Custom.registerCoercer(new CustomIntCoercer(), CustomInt.class);
  }

  private CustomIntCoercer()
  {
  }

  @Override
  public Object coerceInput(CustomInt object)
      throws ClassCastException
  {
    return object.getValue();
  }

  @Override
  public CustomInt coerceOutput(Object object)
      throws TemplateOutputCastException
  {
    return new CustomInt((Integer) object);
  }
}
