package com.linkedin.pegasus.generator.test.pdl.fixtures;

public class WithCustomTypeDefaultsCustomInt
{
  private final int _i;

  public WithCustomTypeDefaultsCustomInt(int i)
  {
    _i = i;
  }

  public int getValue()
  {
    return _i;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof WithCustomTypeDefaultsCustomInt)
    {
      return _i == ((WithCustomTypeDefaultsCustomInt) obj)._i;
    }
    else
    {
      return false;
    }
  }

  @Override
  public int hashCode()
  {
    return _i;
  }
}
