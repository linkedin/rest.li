package com.linkedin.data.codec;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestData;
import com.linkedin.data.TestUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.testng.annotations.DataProvider;


public class CodecDataProviders
{
  @DataProvider
  public static Object[][] tempCodecData()
  {
    final Map<String, DataComplex> inputs = new TreeMap<>();

    {
      DataMap map1 = new DataMap();
      DataMap map11 = new DataMap();
      DataList list11 = new DataList();
      map1.put("map11", map11);
      map1.put("list11", list11);
      inputs.put("Map with empty map and list", map1);
    }

    return inputs.entrySet().stream()
        .map(entry -> new Object[] {entry.getKey(), entry.getValue()})
        .collect(Collectors.toList())
        .toArray(new Object[][] {});
  }

  @DataProvider
  public static Object[][] LICORCodecData()
  {
    List<Object[]> list = new ArrayList<>();
    for (Map.Entry<String, DataComplex> entry : codecDataInputs().entrySet())
    {
      list.add(new Object[] {entry.getKey(), entry.getValue(), true});
      list.add(new Object[] {entry.getKey(), entry.getValue(), false});
    }

    return list.toArray(new Object[][] {});
  }

  @DataProvider
  public static Object[][] codecData()
  {
    return codecDataInputs().entrySet().stream()
        .map(entry -> new Object[] {entry.getKey(), entry.getValue()})
        .collect(Collectors.toList())
        .toArray(new Object[][] {});
  }

  private static Map<String, DataComplex> codecDataInputs()
  {
    final Map<String, DataComplex> inputs = new TreeMap<>();

    inputs.put("Reference DataMap1", TestData.referenceDataMap1);
    inputs.put("Reference DataList1", TestData.referenceDataList1);

    {
      DataMap map1 = new DataMap();
      for (int i = 0; i < 100; ++i)
      {
        String key = "key_" + i;
        map1.put(key, new Boolean(i % 2 == 1));
      }
      inputs.put("Map of 100 booleans", map1);
    }

    {
      DataMap map1 = new DataMap();
      DataList list1 = new DataList();
      map1.put("list", list1);
      for (int i = 0; i < 100; ++i)
      {
        list1.add(new Integer(i));
      }
      inputs.put("List of 100 32-bit integers", map1);
    }

    {
      DataMap map1 = new DataMap();
      DataList list1 = new DataList();
      map1.put("list", list1);
      for (int i = 0; i < 100; ++i)
      {
        list1.add(new Double(i + 0.5));
      }
      inputs.put("List of 100 doubles", map1);
    }

    {
      DataMap map1 = new DataMap();
      for (int i = 0; i < 100; ++i)
      {
        String key = "key_" + i;
        map1.put(key, "12345678901234567890");
      }
      inputs.put("Map of 100 20-character strings", map1);
    }

    {
      DataMap map1 = new DataMap();
      for (int i = 0; i < 100; ++i)
      {
        String key = "key_" + i;
        map1.put(key, new Integer(i));
      }
      inputs.put("Map of 100 32-bit integers", map1);
    }

    {
      DataMap map1 = new DataMap();
      for (int i = 0; i < 100; ++i)
      {
        String key = "key_" + i;
        map1.put(key, new Double(i + 0.5));
      }
      inputs.put("Map of 100 doubles", map1);
    }

    {
      DataMap map1 = new DataMap();
      DataList list1 = new DataList();
      map1.put("list", list1);
      for (int i = 0; i < 100; ++i)
      {
        list1.add("12345678901234567890");
      }
      inputs.put("List of 100 20-character strings", list1);
      inputs.put("Map containing list of 100 20-character strings", map1);
    }

    {
      DataMap map1 = new DataMap();
      DataList list1 = new DataList();
      map1.put("list", list1);
      for (int i = 0; i < 100; ++i)
      {
        list1.add(ByteString.copyAvroString("12345678901234567890", false));
      }
      inputs.put("List of 100 20-byte bytes", list1);
      inputs.put("Map containing list of 100 20-byte bytes", map1);
    }

    {
      DataMap map1 = new DataMap();
      DataMap map11 = new DataMap();
      DataList list11 = new DataList();
      map1.put("map11", map11);
      map1.put("list11", list11);
      inputs.put("Map with empty map and list", map1);
    }

    {
      DataMap dataMap = new DataMap();
      dataMap.put("test", "Fourscore and seven years ago our fathers brought forth on this continent " +
          "a new nation, conceived in liberty and dedicated to the proposition that all men are created " +
          "equal. Now we are engaged in a great civil war, testing whether that nation or any nation so " +
          "conceived and so dedicated can long endure. We are met on a great battlefield of that war. " +
          "We have come to dedicate a portion of that field as a final resting-place for those who here " +
          "gave their lives that that nation might live. It is altogether fitting and proper that we " +
          "should do this. But in a larger sense, we cannot dedicate, we cannot consecrate, we cannot " +
          "hallow this ground. The brave men, living and dead who struggled here have consecrated it " +
          "far above our poor power to add or detract. The world will little note nor long remember " +
          "what we say here, but it can never forget what they did here. It is for us the living rather " +
          "to be dedicated here to the unfinished work which they who fought here have thus far so " +
          "nobly advanced. It is rather for us to be here dedicated to the great task remaining before " +
          "us--that from these honored dead we take increased devotion to that cause for which they " +
          "gave the last full measure of devotion--that we here highly resolve that these dead shall " +
          "not have died in vain, that this nation under God shall have a new birth of freedom, and " +
          "that government of the people, by the people, for the people shall not perish from the earth."
      );
      inputs.put("Map of long string", dataMap);
    }

    {
      DataMap mapOfStrings = new DataMap();

      ArrayList<Integer> lengths = new ArrayList<Integer>();

      for (int stringLength = 0; stringLength < 1024; stringLength += 113)
      {
        lengths.add(stringLength);
      }
      for (int stringLength = 1024; stringLength < (Short.MAX_VALUE * 4); stringLength *= 2)
      {
        lengths.add(stringLength);
      }

      for (int stringLength : lengths)
      {
        DataMap dataMap = new DataMap();

        StringBuilder stringBuilder = new StringBuilder(stringLength);
        char character = 32;
        for (int pos = 0; pos < stringLength; pos++)
        {
          if (character > 16384) character = 32;
          stringBuilder.append(character);
          character += 3;
        }
        // out.println("" + stringLength + " : " + (int) character);
        String key = "test" + stringLength;
        String value = stringBuilder.toString();
        dataMap.put(key, value);
        mapOfStrings.put(key, value);

        inputs.put("Map of " + stringLength + " character string", dataMap);
      }

      inputs.put("Map of variable length strings", mapOfStrings);
    }

    return inputs;
  }

  @DataProvider
  public static Object[][] codecNumbersData()
  {
    return new Object[][]
        {
            {
                "{ \"intMax\" : " + Integer.MAX_VALUE + "}",
                TestUtil.asMap("intMax", Integer.MAX_VALUE)
            },
            {
                "{ \"intMin\" : " + Integer.MIN_VALUE + "}",
                TestUtil.asMap("intMin", Integer.MIN_VALUE)
            },
            {
                "{ \"longMax\" : " + Long.MAX_VALUE + "}",
                TestUtil.asMap("longMax", Long.MAX_VALUE)
            },
            {
                "{ \"longMin\" : " + Long.MIN_VALUE + "}",
                TestUtil.asMap("longMin", Long.MIN_VALUE)
            },
            {
                "{ \"long\" : 5573478247682805760 }",
                TestUtil.asMap("long", 5573478247682805760L)
            },
        };
  }

  @DataProvider
  public static Object[][] LICORNumbersData()
  {
    List<Object[]> list = new ArrayList<>();
    for (Object[] element : numbersData())
    {
      list.add(new Object[] {element[0], true});
      list.add(new Object[] {element[0], false});
    }

    return list.toArray(new Object[][] {});
  }

  @DataProvider
  public static Object[][] numbersData()
  {
    return new Object[][]{{Integer.MAX_VALUE}, {Integer.MIN_VALUE}, {Long.MAX_VALUE}, {Long.MIN_VALUE}, {5573478247682805760L}};
  }

  /**
   * Prior to version 2.4.3, Jackson could not handle map keys >= 262146 bytes, if the data source is byte array.
   * The issue is resolved in https://github.com/FasterXML/jackson-core/issues/152
   */
  @DataProvider
  public static Object[][] longKeyFromByteSource()
  {
    final StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\"");
    for (int i = 0; i < 43691; ++i)
    {
      jsonBuilder.append("6_byte");
    }
    jsonBuilder.append("\":0}");

    return new Object[][]
        {
            {jsonBuilder.toString().getBytes()}
        };
  }
}
