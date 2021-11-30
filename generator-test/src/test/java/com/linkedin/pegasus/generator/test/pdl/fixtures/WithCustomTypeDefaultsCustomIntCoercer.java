package com.linkedin.pegasus.generator.test.pdl.fixtures;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;


public class WithCustomTypeDefaultsCustomIntCoercer implements DirectCoercer<WithCustomTypeDefaultsCustomInt>
{
  static
  {
    Custom.registerCoercer(new WithCustomTypeDefaultsCustomIntCoercer(), WithCustomTypeDefaultsCustomInt.class);
  }

  private WithCustomTypeDefaultsCustomIntCoercer()
  {
  }

  @Override
  public Object coerceInput(WithCustomTypeDefaultsCustomInt object)
      throws ClassCastException
  {
    return object.getValue();
  }

  @Override
  public WithCustomTypeDefaultsCustomInt coerceOutput(Object object)
      throws TemplateOutputCastException
  {
    return new WithCustomTypeDefaultsCustomInt((Integer) object);
  }
}
