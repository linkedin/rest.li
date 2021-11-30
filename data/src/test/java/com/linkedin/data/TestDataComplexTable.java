/*
   Copyright (c) 2021 LinkedIn Corp.

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
    public DataComplex clone() throws CloneNotSupportedException
    {
      return (DataComplex) super.clone();
    }

    @Override
    public DataComplex copy() throws CloneNotSupportedException
    {
      return this.clone();
    }

    @Override
    public void makeReadOnly()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMadeReadOnly()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Object> values()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void startInstrumentingAccess()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stopInstrumentingAccess()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clearInstrumentedData()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void collectInstrumentedData(StringBuilder keyPrefix, Map<String, Map<String, Object>> instrumentedData,
        boolean collectAllData)
    {
      throw new UnsupportedOperationException();
    }
  }
}
