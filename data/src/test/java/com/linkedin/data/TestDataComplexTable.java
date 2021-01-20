package com.linkedin.data;

import java.util.Collection;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;


public class TestDataComplexTable
{
  @Test
  public void testKeysWithSameHash() throws Exception
  {
    DataComplexTable table = new DataComplexTable();
    MockDataComplex item1 = new MockDataComplex();
    item1.setHashCode(1);
    DataComplex item1Clone = item1.clone();
    MockDataComplex item2 = new MockDataComplex();
    item2.setHashCode(1);
    DataComplex item2Clone = item2.clone();

    table.put(item1, item1Clone);
    table.put(item2, item2Clone);

    Assert.assertNotNull(table.get(item1));
    Assert.assertNotNull(table.get(item2));
    Assert.assertSame(item1Clone, table.get(item1));
    Assert.assertSame(item2Clone, table.get(item2));
  }

  private static class MockDataComplex implements DataComplex
  {
    private int _hashCode = 0;
    @Override
    public int dataComplexHashCode()
    {
      return _hashCode;
    }

    public void setHashCode(int hashCode)
    {
      this._hashCode = hashCode;
    }

    @Override
    public void makeReadOnly()
    {
    }

    @Override
    public boolean isMadeReadOnly()
    {
      return false;
    }

    @Override
    public Collection<Object> values()
    {
      return null;
    }

    @Override
    public DataComplex clone() throws CloneNotSupportedException
    {
      return (DataComplex) super.clone();
    }

    @Override
    public void setReadOnly()
    {
    }

    @Override
    public boolean isReadOnly()
    {
      return false;
    }

    @Override
    public void invalidate()
    {
    }

    @Override
    public DataComplex copy() throws CloneNotSupportedException
    {
      return this.clone();
    }

    @Override
    public void startInstrumentingAccess()
    {
    }

    @Override
    public void stopInstrumentingAccess()
    {
    }

    @Override
    public void clearInstrumentedData()
    {
    }

    @Override
    public void collectInstrumentedData(StringBuilder keyPrefix, Map<String, Map<String, Object>> instrumentedData,
        boolean collectAllData)
    {
    }
  }
}
