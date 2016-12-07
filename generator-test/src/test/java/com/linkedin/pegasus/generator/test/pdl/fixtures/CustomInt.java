package com.linkedin.pegasus.generator.test.pdl.fixtures;

public class CustomInt
{
  private final int _i;

  public CustomInt(int i)
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
    if (obj instanceof CustomInt)
    {
      return _i == ((CustomInt) obj)._i;
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
