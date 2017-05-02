package com.linkedin.pegasus.generator.test;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.data.template.TestDataTemplateUtil;
import com.linkedin.data.template.UnionTemplate;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestUnionWithAliases {

  @Test
  public void testAccessorsForUnionWithAliases() throws IOException
  {
    Object inputs[][] =
      {
        {
          "memInt", 1
        },
        {
          "memAnotherInt", 2
        },
        {
          "memLong", 2L
        },
        {
          "memFloat", 3.0f
        },
        {
          "memDouble", 4.0
        },
        {
          "memBoolean", Boolean.TRUE
        },
        {
          "memString", "abc"
        },
        {
          "memAnotherString", "xyz"
        },
        {
          "memBytes", ByteString.copyAvroString("xyz", false)
        },
        {
          "memEnum", Alphabet.A
        },
        {
          "memRecord", new RecordBar().setLocation("exotic")
        },
        {
          "memAnotherRecord", new RecordBar().setLocation("alien")
        },
        {
          "memFixed", new FixedMD5(ByteString.copyAvroString("0123456789abcdef", false))
        },
        {
          "memArray", new StringArray(Arrays.asList("a1", "b2", "c3"))
        },
        {
          "memMap", new LongMap(TestUtil.dataMapFromString("{ \"k1\" : \"v1\" }"))
        },
        {
          "memAnotherMap", new StringMap(TestUtil.dataMapFromString("{ \"k2\" : \"value2\" }"))
        }
      };

    for (Object[] row : inputs)
    {
      testTypeValue(UnionTest.UnionWithAliases.class, (String) row[0], row[1]);
    }
  }

  private <T extends UnionTemplate> void testTypeValue(Class<T> unionClass, String memberAlias, Object memberValue)
  {
    try
    {
      // constructor with argument
      Constructor<T> constructor = unionClass.getDeclaredConstructor(Object.class);
      T unionFromConstructor = constructor.newInstance(buildUnionData(memberAlias, memberValue));
      verifyMemberValueWithAccessorMethods(unionFromConstructor, memberAlias, memberValue);

      // constructor with no argument followed by set
      String setTypeMethodName = TestDataTemplateUtil.methodName("set", memberAlias);
      Method setMethod = unionClass.getMethod(setTypeMethodName, memberValue.getClass());
      T unionToSet = unionClass.getConstructor().newInstance();
      setMethod.invoke(unionToSet, memberValue);
      verifyMemberValueWithAccessorMethods(unionToSet, memberAlias, memberValue);

      // create method
      String createMethodName = TestDataTemplateUtil.methodName("createWith", memberAlias);
      Method createMethod = unionClass.getMethod(createMethodName, memberValue.getClass());
      @SuppressWarnings("unchecked")
      T unionFromCreate = (T) createMethod.invoke(null, memberValue);
      verifyMemberValueWithAccessorMethods(unionFromCreate, memberAlias, memberValue);
    }
    catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException ex)
    {
      throw new IllegalStateException(ex);
    }
  }

  private <T extends UnionTemplate> void verifyMemberValueWithAccessorMethods(
      T union, String memberAlias, Object memberValue)
  {
    String isTypeMethodName = TestDataTemplateUtil.methodName("is", memberAlias);
    String getTypeMethodName = TestDataTemplateUtil.methodName("get", memberAlias);
    Class<? extends UnionTemplate> unionClass = union.getClass();
    try
    {
      Method[] methods = unionClass.getMethods();
      boolean foundIsMethod = false;
      boolean foundGetMethod = false;
      for (Method method : methods)
      {
        String methodName = method.getName();
        if (methodName.startsWith("is"))
        {
          boolean expectedValue = methodName.equals(isTypeMethodName);
          Boolean value = (Boolean) method.invoke(union);
          assertEquals(value.booleanValue(), expectedValue);

          foundIsMethod |= methodName.equals(isTypeMethodName);
        }
        if (methodName.startsWith("get") && !methodName.equals("getClass"))
        {
          Object expectedGetValue = methodName.equals(getTypeMethodName) ? memberValue : null;
          Object getValue = method.invoke(union);
          assertEquals(getValue, expectedGetValue);

          foundGetMethod |= methodName.equals(getTypeMethodName);
        }
      }
      assertTrue(foundGetMethod);
      assertTrue(foundIsMethod);
    }
    catch (IllegalAccessException | InvocationTargetException ex)
    {
      throw new IllegalStateException(ex);
    }
  }

  private <T> DataMap buildUnionData(String memberAlias, Object memberValue)
  {
    String key = memberAlias;
    Object value = null;
    if (memberValue instanceof DataTemplate)
    {
      DataTemplate<?> dataTemplate = (DataTemplate<?>) memberValue;
      value = dataTemplate.data();
    }
    else if (memberValue instanceof Enum)
    {
      value = memberValue.toString();
    }
    else
    {
      value = memberValue;
    }

    DataMap dataMap = new DataMap();
    dataMap.put(key, value);
    return dataMap;
  }
}
