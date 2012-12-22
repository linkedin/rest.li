package com.linkedin.restli.internal.common;


import com.linkedin.data.ByteString;
import com.linkedin.pegasus.generator.test.Fruits;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestValueConverter
{
  @Test
  public void test()
  {
    Assert.assertSame(ValueConverter.coerceString(null, Object.class), null);

    Assert.assertEquals(ValueConverter.coerceString("Test String", String.class), "Test String");

    Assert.assertSame(ValueConverter.coerceString("true", boolean.class), true);
    Assert.assertSame(ValueConverter.coerceString("false", Boolean.class), false);

    Assert.assertEquals(ValueConverter.coerceString("1", short.class), (short) 1);
    Assert.assertEquals(ValueConverter.coerceString("2", Short.class), (short) 2);

    Assert.assertEquals(ValueConverter.coerceString("3", int.class), 3);
    Assert.assertEquals(ValueConverter.coerceString("4", Integer.class), 4);

    Assert.assertEquals(ValueConverter.coerceString("5", long.class), 5L);
    Assert.assertEquals(ValueConverter.coerceString("6", Long.class), 6L);

    Assert.assertEquals(ValueConverter.coerceString("7.8", float.class), 7.8F);
    Assert.assertEquals(ValueConverter.coerceString("9.10", Float.class), 9.1F);
    Assert.assertEquals(ValueConverter.coerceString("11", Float.class), 11F);

    Assert.assertEquals(ValueConverter.coerceString("12.13", double.class), 12.13D);
    Assert.assertEquals(ValueConverter.coerceString("14.15", Double.class), 14.15D);

    Assert.assertEquals(ValueConverter.coerceString(_bytes16, ByteString.class), ByteString.copyAvroString(_bytes16, true));

    Assert.assertSame(ValueConverter.coerceString("APPLE", Fruits.class), Fruits.APPLE);
    Assert.assertSame(ValueConverter.coerceString("ORANGE", Fruits.class), Fruits.ORANGE);
    Assert.assertSame(ValueConverter.coerceString("BLUEBERRY", Fruits.class), Fruits.$UNKNOWN);
  }

  private final String _bytes16 = "\u0001\u0002\u0003\u0004" +
      "\u0005\u0006\u0007\u0008" +
      "\u0009\n\u000B\u000C" +
      "\r\u000E\u000F\u0010";
}
