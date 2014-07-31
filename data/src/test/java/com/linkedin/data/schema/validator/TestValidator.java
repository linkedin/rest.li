/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.data.schema.validator;


import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.TestUtil.dataMapFromString;
import static com.linkedin.data.TestUtil.dataSchemaFromString;
import static com.linkedin.data.TestUtil.out;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class TestValidator
{
  private static boolean debug = false;

  // Example of custom Validator.
  public static class FooValidator extends AbstractValidator
  {
    public FooValidator(DataMap dataMap)
    {
      super(dataMap);
    }

    @Override
    public void validate(ValidatorContext ctx)
    {
      DataElement element = ctx.dataElement();
      if (element.getChild("debug") != null)
      {
        ctx.addResult(new Message(element.path(), false, " = %1$s", element.getValue()));
      }
    }
  }

  // Validator that throws exception in constructor
  public static class BadValidator extends AbstractValidator
  {
    public BadValidator(DataMap map)
    {
      super(map);
      throw new UnsupportedOperationException();
    }

    @Override
    public void validate(ValidatorContext ctx)
    {
    }
  }

  @Test
  public void testInitializationClassLookup() throws IOException
  {
    Object [][] input =
      {
        {
          // Validator class provided in key-to-class map
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : {\"regex\" : { \"regex\" : \"[0-9]+\" } }\n" +
          "}\n"
        },
        {
          // Validator class name provided by key
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\",\n" +
          "  \"validate\" : { \"" + FooValidator.class.getName() + "\" : { } }\n" +
          "}\n"
        },
        {
          // Validator class name derived
          // package name is always "com.linkedin.data.validator",
          // class name is derived by capitializing 1st character of key and appending "Validator"
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : { \"noop\" : { } }\n" +
          "}\n"
        },
      };

    for (Object[] row : input)
    {
      String schemaText = (String) row[0];
      DataSchema schema = dataSchemaFromString(schemaText);
      DataSchemaAnnotationValidator annotationValidator = new DataSchemaAnnotationValidator();
      annotationValidator.init(schema);
      assertTrue(annotationValidator.isInitOk());
      assertTrue(annotationValidator.getInitMessages().isEmpty());
    }

  }

  @Test
  public void testBadInitializationOfDataSchemaAnnotationValidator() throws IOException
  {
    Object [][] input =
      {
        {
          // validate property is not a DataMap
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : 32\n" +
          "}\n",
          "/Foo",
          "not a DataMap"
        },
        {
          // validate property has key that cannot be resolved
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : { \"unknown\" : { } }\n" +
          "}\n",
          "/Foo",
          "unable to find Validator for \"unknown\""
        },
        {
          // value associated with key is not a DataMap
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : { \"bad\" : 10 }\n" +
          "}\n",
          "/Foo",
          "value of \"bad\" is not a DataMap"
        },
        {
          // constructor throws exception
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : { \"bad\" : { } }\n" +
          "}\n",
          "/Foo",
          "BadValidator cannot be instantiated for \"bad\""
        },
        {
          // validate in field bad
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"record\",\n" +
          "  \"fields\" : [\n" +
          "    {\n" +
          "      \"name\" : \"intField\",\n" +
          "      \"type\" : \"int\",\n" +
          "      \"validate\" : { \"bad\" : { } }\n" +
          "    }\n" +
          "  ]\n" +
          "}\n",
          "/Foo/intField",
          "BadValidator cannot be instantiated for \"bad\""
        },
        {
          // test path
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : {\n" +
          "    \"name\" : \"Bar\",\n" +
          "    \"type\" : \"record\",\n" +
          "    \"fields\" : [\n" +
          "      {\n" +
          "        \"name\" : \"arrayField\",\n" +
          "        \"type\" : {\n" +
          "          \"type\" : \"array\",\n" +
          "          \"items\" : {\n" +
          "             \"type\" : \"map\",\n" +
          "             \"values\" : \"string\",\n" +
          "             \"validate\" : { \"bad\" : { } }\n" +
          "          }\n" +
          "        }\n" +
          "      }\n" +
          "    ]\n" +
          "  }\n" +
          "}\n",
          "/Foo/ref/Bar/arrayField/array/items/map",
          "BadValidator cannot be instantiated for \"bad\""
        },
        {
          // Validator key identifies class that is not a Validator.
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : { \"java.util.Map\" : { } }\n" +
          "}\n",
          "/Foo",
          "java.util.Map is not a com.linkedin.data.schema.validator.Validator",
          "unable to find Validator for \"java.util.Map\""
        },
        {
          // Validator key identifies class that is not a validator.
          // Validator key identifies class that is not a Validator.
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"typeref\",\n" +
          "  \"ref\" : \"int\", \n" +
          "  \"validate\" : { \"notA\" : { } }\n" +
          "}\n",
          "/Foo",
          "com.linkedin.data.schema.validator.NotAValidator is not a com.linkedin.data.schema.validator.Validator",
          "unable to find Validator for \"notA\""
        }
      };

    final boolean debug = false;

    Map<String, Class<? extends Validator>> validatorClassMap = new HashMap<String, Class<? extends Validator>>();
    validatorClassMap.put("bad", BadValidator.class);

    for (Object[] row : input)
    {
      String schemaText = (String) row[0];
      if (debug) TestUtil.out.println(schemaText);
      DataSchema schema = dataSchemaFromString(schemaText);
      DataSchemaAnnotationValidator annotationValidator = new DataSchemaAnnotationValidator();
      annotationValidator.init(schema, validatorClassMap);
      if (debug) TestUtil.out.println(annotationValidator.getInitMessages());
      assertFalse(annotationValidator.isInitOk());
      assertFalse(annotationValidator.getInitMessages().isEmpty());
      for (int i = 1; i < row.length; i++)
      {
        assertTrue(annotationValidator.getInitMessages().toString().contains((String) row[i]));
      }
    }
  }

  static Map<String, Class<? extends Validator>> _validatorClassMap = new HashMap<String, Class<? extends Validator>>();
  static
  {
    _validatorClassMap.put("fooValidator", FooValidator.class);
  };

  private static final int SCHEMA_VALIDATOR = 0x1;
  private static final int OBJECT_VALIDATOR = 0x2;
  private static final int ALL_VALIDATORS = 0xff;

  public void testValidator(String schemaText,
                            Object[][] input,
                            Map<String, Class<? extends Validator>> validatorClassMap,
                            String[] validatorCheckStrings,
                            int tests)
    throws IOException, InstantiationException
  {
    DataSchema schema = dataSchemaFromString(schemaText);
    DataSchemaAnnotationValidator annotationValidator = new DataSchemaAnnotationValidator();
    annotationValidator.init(schema, validatorClassMap);
    if (debug) annotationValidator.setDebugMode(true);
    String annotationValidatorString = annotationValidator.toString();
    if (debug) out.println(annotationValidator);
    for (String checkString : validatorCheckStrings)
    {
      assertTrue(annotationValidatorString.contains(checkString));
    }

    for (Object[] row : input)
    {
      DataMap value = (DataMap) row[0];

      try
      {
        if ((tests & SCHEMA_VALIDATOR) != 0)
        {
          VisitedTrackingValidator visitedValidator = new VisitedTrackingValidator(annotationValidator);
          ValidationOptions validationOptions = new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.NORMAL);
          ValidationResult result = ValidateDataAgainstSchema.validate(value.copy(), schema, validationOptions, visitedValidator);
          checkValidationResult(value, result, row, visitedValidator);
        }

        if ((tests & OBJECT_VALIDATOR) != 0)
        {
          VisitedTrackingValidator visitedValidator = new VisitedTrackingValidator(annotationValidator);
          ValidationOptions validationOptions = new ValidationOptions();
          ValidationResult result = ValidateDataAgainstSchema.validate(value.copy(), schema, validationOptions, visitedValidator);
          checkValidationResult(value, result, row, visitedValidator);
        }
      }
      catch (CloneNotSupportedException e)
      {
        throw new IllegalStateException("unexpected exception", e);
      }
    }
  }

  public void checkValidationResult(Object value, ValidationResult result, Object[] row, VisitedTrackingValidator visitedValidator) throws IOException
  {
    Collection<Message> messages = result.getMessages();
    String resultString =
      dataMapToString((DataMap)result.getFixed()) + "\n" +
      messages.toString();
    if (debug) out.println("value: " + value.toString() + "\nresult:\n" + resultString);
    for (int col = 1; col < row.length; col++)
    {
      String checkString = (String) row[col];
      boolean ok = resultString.contains(checkString);
      assertTrue(ok);
    }
    Set<String> visitedMoreThanOnce = visitedValidator.getVisitedMoreThanOnce();
    assertTrue(visitedMoreThanOnce.isEmpty(), visitedMoreThanOnce + " is visited more than once");
  }

  private static final String fooSchemaText =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : \"Foo\", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"strlen10\", " +
      "      \"type\" : " +
      "      { " +
      "        \"name\" : \"StrLen10\", " +
      "        \"type\" : \"typeref\", " +
      "        \"ref\"  : \"string\", " +
      "        \"validate\" : " +
      "        { " +
      "          \"strlen\" : { \"max\" : 10 } "+
      "        } " +
      "      }, " +
      "      \"optional\" : true " +
      "    }, " +
      "    { " +
      "      \"name\" : \"digits\", " +
      "      \"type\" : " +
      "      { " +
      "        \"name\" : \"Digits\", " +
      "        \"type\" : \"typeref\", " +
      "        \"ref\"  : \"string\", " +
      "        \"validate\" : " +
      "        { " +
      "          \"regex\" : { \"regex\" : \"[0-9]+\" } "+
      "        } " +
      "      }, " +
      "      \"optional\" : true " +
      "    }, " +
      "    { " +
      "      \"name\" : \"digitsMin5\", " +
      "      \"type\" : " +
      "      { " +
      "        \"name\" : \"DigitsMin5\", " +
      "        \"type\" : \"typeref\", " +
      "        \"ref\"  : \"Digits\", " +
      "        \"validate\" : " +
      "        { " +
      "          \"strlen\" : { \"min\" : 5 } "+
      "        } " +
      "      }," +
      "      \"optional\" : true " +
      "    }, " +
      // validate property at field level
      "    { " +
      "      \"name\" : \"lettersMin3\", " +
      "      \"type\" : \"string\", " +
      "      \"validate\" : " +
      "      { " +
      "        \"strlen\" : { \"min\" : 3 }, "+
      "        \"regex\" : { \"regex\" : \"[A-Za-z]+\" } "+
      "      }, " +
      "      \"optional\" : true " +
      "    }, " +
      // validate at within multi-level nested types
      "    { " +
      "      \"name\" : \"nested\", " +
      "      \"type\" : { " +
      "        \"type\" : \"array\", " +
      "        \"items\" : { " +
      "          \"type\" : \"map\", " +
      "          \"values\" : \"Foo\" " +
      "        } " +
      "      }, " +
      "      \"optional\" : true " +
      "    } " +
      "  ], " +
      "  \"validate\" : " +
      "  { " +
      "    \"fooValidator\" : { } " +
      "  } " +
      "}";

  String[] _fooSchemaValidatorCheckStrings =
  {
    "StrLen10",
    "Digits",
    "DigitsMin5",
    "Foo"
  };

  public void testFooSchemaValidator(Object[][] input) throws IOException, InstantiationException
  {
    testValidator(fooSchemaText, input, _validatorClassMap, _fooSchemaValidatorCheckStrings, ALL_VALIDATORS);
  }

  private static final JacksonDataCodec codec = new JacksonDataCodec();

  public String dataMapToString(DataMap map) throws IOException
  {
    byte[] bytes = codec.mapToBytes(map);
    return new String(bytes);
  }

  @Test
  public void testFooValidator() throws IOException, InstantiationException
  {
    Object[][] input =
    {
      {
        new DataMap(asMap("debug", 1, "xxx", "yyy")),
        "debug=1", "xxx=yyy"
      }
    };

    testFooSchemaValidator(input);
  }

  @Test
  public void testStringLengthValidator() throws IOException, InstantiationException
  {
    Object[][] input =
    {
      {
        new DataMap(asMap("strlen10", "")),
        "{\"strlen10\":\"\"}"
      },
      {
        new DataMap(asMap("strlen10", "1234567890")),
        "{\"strlen10\":\"1234567890\"}"
      },
      {
        new DataMap(asMap("strlen10", "123456789012")),
        "ERROR", "is out of range 0...10"
      }
    };

    testFooSchemaValidator(input);
  }

  @Test
  public void testRegexValidator() throws IOException, InstantiationException
  {
    Object[][] input =
    {
      {
        new DataMap(asMap("digits", "1")),
        "{\"digits\":\"1\"}"
      },
      {
        new DataMap(asMap("digits", "1234567890")),
        "{\"digits\":\"1234567890\"}"
      },
      {
        new DataMap(asMap("digits", "")),
        "ERROR", "does not match [0-9]+"
      }
    };

    testFooSchemaValidator(input);
  }

  @Test
  public void testDigitMin5() throws IOException, InstantiationException
  {
    Object[][] input =
    {
      {
        new DataMap(asMap("digitsMin5", "00000")),
        "{\"digitsMin5\":\"00000\"}"
      },
      {
        new DataMap(asMap("digitsMin5", "12345")),
        "{\"digitsMin5\":\"12345\"}"
      },
      {
        new DataMap(asMap("digitsMin5", "67890")),
        "{\"digitsMin5\":\"67890\"}"
      },
      {
        new DataMap(asMap("digitsMin5", "xxxxx")),
        "ERROR", "does not match [0-9]+",
      },
      {
        new DataMap(asMap("digitsMin5", "0123")),
        "ERROR", "is out of range 5...",
      },
      {
        new DataMap(asMap("digitsMin5", "")),
        "ERROR", "does not match [0-9]+", "is out of range 5...",
      },
      {
        new DataMap(asMap("digitsMin5", "x")),
        "ERROR", "does not match [0-9]+", "is out of range 5..."
      }
    };

    testFooSchemaValidator(input);
  }

  @Test
  public void testNested() throws IOException, InstantiationException
  {
    Object[][] input =
    {
      {
        dataMapFromString("{ \"nested\" : [ { \"key1\" : { \"digits\" : \"123\" } } ] }"),
        "{\"nested\":[{\"key1\":{\"digits\":\"123\"}}]}"
      },
      {
        dataMapFromString("{ \"nested\" : [ { \"key1\" : { \"digits\" : \"x\" } } ] }"),
        "ERROR", "/nested/0/key1/digits", "does not match [0-9]+"
      },
      {
        dataMapFromString("{ \"nested\" : [ { \"key1\" : { \"strlen10\" : \"1234567890\" } } ] }"),
        "{\"nested\":[{\"key1\":{\"strlen10\":\"1234567890\"}}]}"
      },
      {
        dataMapFromString("{ \"nested\" : [ { \"key1\" : { \"strlen10\" : \"123456789012\" } } ] }"),
        "ERROR", "/nested/0/key1/strlen10", "is out of range 0...10"
      },
      {
        dataMapFromString("{ \"nested\" : [ { \"key1\" : { \"nested\" : [ { \"key2\" : { \"digits\" : \"123\" } } ] } } ] }"),
        "{\"nested\":[{\"key1\":{\"nested\":[{\"key2\":{\"digits\":\"123\"}}]}}]}"
      },
      {
        dataMapFromString("{ \"nested\" : [ { \"key1\" : { \"nested\" : [ { \"key2\" : { \"digits\" : \"x\" } } ] } } ] }"),
        "ERROR", "/nested/0/key1/nested/0/key2/digits", "does not match [0-9]+"
      },
    };

    testFooSchemaValidator(input);
  }

  @Test
  public void testValidateAtFieldLevel() throws IOException, InstantiationException
  {
    Object[][] input =
    {
      {
        new DataMap(asMap("lettersMin3", "ABC")),
        "{\"lettersMin3\":\"ABC\"}"
      },
      {
        new DataMap(asMap("lettersMin3", "abc"))
      },
      {
        new DataMap(asMap("lettersMin3", "xyz")),
      },
      {
        new DataMap(asMap("lettersMin3", "012")),
        "ERROR", "does not match [A-Za-z]+",
      },
      {
        new DataMap(asMap("lettersMin3", "ab")),
        "ERROR", "is out of range 3...",
      },
      {
        new DataMap(asMap("lettersMin3", "")),
        "ERROR", "does not match [A-Za-z]+", "is out of range 3...",
      },
      {
        new DataMap(asMap("lettersMin3", "0")),
        "ERROR", "does not match [A-Za-z]+", "is out of range 3..."
      }
    };

    testFooSchemaValidator(input);
  }

  @Test
  public void testPathNameInValidationMessages() throws IOException, InstantiationException
  {
    String schemaText =
          "{ \n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [\n" +
          "    {\n" +
          "      \"name\" : \"bar\",\n" +
          "      \"type\" : {\n" +
          "        \"name\" : \"Bar\",\n" +
          "        \"type\" : \"record\",\n" +
          "        \"fields\" : [\n" +
          "          {\n" +
          "            \"name\" : \"baz\",\n" +
          "            \"type\" : \"int\"\n" +
          "          }\n" +
          "        ]\n" +
          "      }\n" +
          "    }\n" +
          "  ]\n" +
          "}\n";

    Object[][] input =
    {
      {
        new DataMap(asMap("bar", new DataMap(asMap("baz", "string")))),
        "ERROR", "/bar/baz"
      }
    };

    Map<String, Class<? extends Validator>> validatorClassMap = Collections.emptyMap();
    String[] checkStrings = new String[0];
    testValidator(schemaText, input, validatorClassMap, checkStrings, SCHEMA_VALIDATOR);
  }

  public static class InstanceOfValidator extends AbstractValidator
  {
    private static Class<?> _class;

    public InstanceOfValidator(DataMap map) throws ClassNotFoundException
    {
      super(map);
      _class = Class.forName((String) map.get("class"));
    }

    @Override
    public void validate(ValidatorContext context)
    {
      DataElement element = context.dataElement();
      Object value = element.getValue();
      if (debug) out.println("InstanceOf: value=" + value + "(" + value.getClass().getSimpleName() + ")");
      if (_class.isInstance(value) == false)
      {
        context.addResult(new Message(element.path(), "is not a %1$s", _class.getSimpleName()));
      }
    }
  }

  @Test
  public void testValidatorHasFixedValue() throws IOException, InstantiationException
  {
    String schemaText =
          "{ \n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"fields\" : [\n" +
          "    {\n" +
          "      \"name\" : \"baz\",\n" +
          "      \"type\" : {\n" +
          "        \"type\" : \"typeref\",\n" +
          "        \"name\" : \"IntRef\",\n" +
          "        \"ref\" : \"int\",\n" +
          "        \"validate\" : {\n" +
          "          \"instanceOf\" : { \"class\" : \"java.lang.Integer\" }\n" +
          "        }\n" +
          "      }\n" +
          "    }\n" +
          "  ]\n" +
          "}\n";

    Object[][] input =
    {
      {
        new DataMap(asMap("baz", 1.0f)),
        "{\"baz\":1}"
      },
      {
        new DataMap(asMap("baz", 1.4f)),
        "{\"baz\":1}"
      },
      {
        new DataMap(asMap("baz", "string")),
        "ERROR", "/baz", "is not a Integer"
      }
    };

    @SuppressWarnings("serial")
    Map<String, Class<? extends Validator>> validatorClassMap = new HashMap<String, Class<? extends Validator>>()
    {
      {
        put("instanceOf", InstanceOfValidator.class);
      }
    };

    String[] checkStrings = { "InstanceOfValidator"};
    testValidator(schemaText, input, validatorClassMap, checkStrings, SCHEMA_VALIDATOR);
  }

  private static class OrderEntry
  {
    private final String _path;
    private final String _validatorName;

    private OrderEntry(String path, String validatorName)
    {
      _path = path;
      _validatorName = validatorName;
    }

    private OrderEntry(String s)
    {
      String[] tokens = s.split(":");
      _path = tokens[0];
      _validatorName = tokens[1];
    }

    @Override
    public String toString()
    {
      return _path + ":" + _validatorName;
    }

    @Override
    public boolean equals(Object other)
    {
      if (other == null || other.getClass() != OrderEntry.class)
        return false;
      OrderEntry o = (OrderEntry) other;
      return _path.equals(o._path) && _validatorName.equals(o._validatorName);
    }
  }

  private static class OrderRelation
  {
    private final OrderEntry _beforeEntry;
    private final OrderEntry _afterEntry;

    protected OrderRelation(OrderEntry before, OrderEntry after)
    {
      _beforeEntry = before;
      _afterEntry = after;
    }

    private boolean isSatisfied(List<OrderEntry> list)
    {
      int beforeIndex = list.indexOf(_beforeEntry);
      int afterIndex = list.indexOf(_afterEntry);

      assertTrue(beforeIndex >= 0);
      assertTrue(afterIndex >= 0);

      // out.println("before " + _beforeEntry + " index " + beforeIndex + ", after " + _afterEntry + " index " + afterIndex);
      return beforeIndex < afterIndex;
    }
  }

  public static class OrderValidator extends AbstractValidator
  {
    private final String _name;
    private static final List<OrderEntry> _orderList = new ArrayList<OrderEntry>();

    public OrderValidator(DataMap dataMap)
    {
      super(dataMap);
      _name = dataMap.getString("name");
    }

    @Override
    public void validate(ValidatorContext ctx)
    {
      DataElement element = ctx.dataElement();
      OrderEntry entry = new OrderEntry(element.pathAsString(), _name);
      _orderList.add(entry);
    }
  }

  @Test
  public void testValidatorPriority() throws IOException
  {
    Map<String, Class<? extends Validator>> validatorClassMap = new HashMap<String, Class<? extends Validator>>();
    validatorClassMap.put("v1", OrderValidator.class);
    validatorClassMap.put("v2", OrderValidator.class);
    validatorClassMap.put("v3", OrderValidator.class);
    validatorClassMap.put("v4", OrderValidator.class);
    validatorClassMap.put("v5", OrderValidator.class);
    validatorClassMap.put("v6", OrderValidator.class);

    Object[][] inputs = {
      {
        // positive priority values
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [], \n" +
        "  \"validate\" : {\n" +
        "    \"v1\" : { \"validatorPriority\" : 1, \"name\" : \"p1\" },\n" +
        "    \"v2\" : { \"validatorPriority\" : 2, \"name\" : \"p2\" },\n" +
        "    \"v3\" : { \"validatorPriority\" : 3, \"name\" : \"p3\" },\n" +
        "    \"v4\" : { \"validatorPriority\" : 4, \"name\" : \"p4\" },\n" +
        "    \"v5\" : { \"validatorPriority\" : 5, \"name\" : \"p5\" },\n" +
        "    \"v6\" : { \"validatorPriority\" : 6, \"name\" : \"p6\" }\n" +
        "  }\n" +
        "}\n",
        "{}",
        new String[] {
          ":p6", ":p5",
          ":p5", ":p4",
          ":p4", ":p3",
          ":p3", ":p2",
          ":p2", ":p1"
        }
      },
      {
        // negative priority values
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [], \n" +
        "  \"validate\" : {\n" +
        "    \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"p-1\" },\n" +
        "    \"v2\" : { \"validatorPriority\" : 0, \"name\" : \"p=0\" },\n" +
        "    \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"p+1\" }\n" +
        "  }\n" +
        "}\n",
        "{}",
        new String[] {
          ":p+1", ":p=0",
          ":p=0", ":p-1"
        }
      },
      {
        // default priority value
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [], \n" +
        "  \"validate\" : {\n" +
        "    \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"p-1\" },\n" +
        "    \"v2\" : { \"name\" : \"pdefault\" },\n" +
        "    \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"p+1\" }\n" +
        "  }\n" +
        "}\n",
        "{}",
        new String[] {
          ":p+1", ":pdefault",
          ":pdefault", ":p-1"
        }
      },
      {
        // same priority values
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [], \n" +
        "  \"validate\" : {\n" +
        "    \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"p-1a\" },\n" +
        "    \"v2\" : { \"validatorPriority\" : -1, \"name\" : \"p-1b\" },\n" +
        "    \"v3\" : { \"validatorPriority\" : 0, \"name\" : \"p0a\" },\n" +
        "    \"v4\" : { \"validatorPriority\" : 0, \"name\" : \"p0b\" },\n" +
        "    \"v5\" : { \"validatorPriority\" : 1, \"name\" : \"p+1a\" },\n" +
        "    \"v6\" : { \"validatorPriority\" : 1, \"name\" : \"p+1b\" }\n" +
        "  }\n" +
        "}\n",
        "{}",
        new String[] {
          ":p+1a", ":p0a",
          ":p+1a", ":p0b",
          ":p+1b", ":p0a",
          ":p+1b", ":p0b",
          ":p0a", ":p-1a",
          ":p0a", ":p-1b",
          ":p0b", ":p-1a",
          ":p0b", ":p-1b"
        }
      },
      {
        // typeref inner before outer
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [\n" +
        "    {\n" +
        "      \"name\" : \"i\",\n" +
        "      \"type\" : {\n" +
        "        \"type\" : \"typeref\",\n" +
        "        \"name\" : \"Ref1\",\n" +
        "        \"ref\" : {\n" +
        "          \"type\" : \"typeref\",\n" +
        "          \"name\" : \"Ref2\",\n" +
        "          \"ref\" : \"int\",\n" +
        "          \"validate\" : {\n" +
        "            \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"r2-1\" },\n" +
        "            \"v2\" : { \"name\" : \"r2=0\" },\n" +
        "            \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"r2+1\" }\n" +
        "          }\n" +
        "        },\n" +
        "        \"validate\" : {\n" +
        "          \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"r1-1\" },\n" +
        "          \"v2\" : { \"name\" : \"r1=0\" },\n" +
        "          \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"r1+1\" }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}\n",
        "{\n" +
        "  \"i\" : 4\n" +
        "}",
        new String[] {
          "/i:r2+1", "/i:r2=0",
          "/i:r2=0", "/i:r2-1",
          "/i:r2-1", "/i:r1+1",
          "/i:r1+1", "/i:r1=0",
          "/i:r1=0", "/i:r1-1",
        }
      },
      {
        // array items before array
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [\n" +
        "    {\n" +
        "      \"name\" : \"a\",\n" +
        "      \"type\" : {\n" +
        "        \"type\" : \"array\",\n" +
        "        \"items\" : {\n" +
        "          \"type\" : \"typeref\",\n" +
        "          \"name\" : \"IntRef\",\n" +
        "          \"ref\" : \"int\",\n" +
        "          \"validate\" : {\n" +
        "            \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"i-1\" },\n" +
        "            \"v2\" : { \"name\" : \"i=0\" },\n" +
        "            \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"i+1\" }\n" +
        "          }\n" +
        "        },\n" +
        "        \"validate\" : {\n" +
        "          \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"a-1\" },\n" +
        "          \"v2\" : { \"name\" : \"a=0\" },\n" +
        "          \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"a+1\" }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}\n",
        "{\n" +
        "  \"a\" : [ 1 ]\n" +
        "}",
        new String[] {
          "/a/0:i+1", "/a/0:i=0",
          "/a/0:i=0", "/a/0:i-1",
          "/a/0:i-1", "/a:a+1",
          "/a:a+1", "/a:a=0",
          "/a:a=0", "/a:a-1",
        }
      },
      {
        // map values before map
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [\n" +
        "    {\n" +
        "      \"name\" : \"m\",\n" +
        "      \"type\" : {\n" +
        "        \"type\" : \"map\",\n" +
        "        \"values\" : {\n" +
        "          \"type\" : \"typeref\",\n" +
        "          \"name\" : \"IntRef\",\n" +
        "          \"ref\" : \"int\",\n" +
        "          \"validate\" : {\n" +
        "            \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"v-1\" },\n" +
        "            \"v2\" : { \"name\" : \"v=0\" },\n" +
        "            \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"v+1\" }\n" +
        "          }\n" +
        "        },\n" +
        "        \"validate\" : {\n" +
        "          \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"m-1\" },\n" +
        "          \"v2\" : { \"name\" : \"m=0\" },\n" +
        "          \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"m+1\" }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}\n",
        "{\n" +
        "  \"m\" : { \"x\" : 1 } }\n" +
        "}",
        new String[] {
          "/m/x:v+1", "/m/x:v=0",
          "/m/x:v=0", "/m/x:v-1",
          "/m/x:v-1", "/m:m+1",
          "/m:m+1", "/m:m=0",
          "/m:m=0", "/m:m-1",
        }
      },
      {
        // union member before typeref of union
        "{\n" +
        "  \"name\" : \"Foo\",\n" +
        "  \"type\" : \"record\",\n" +
        "  \"fields\" : [\n" +
        "    {\n" +
        "      \"name\" : \"u\",\n" +
        "      \"type\" : {\n" +
        "        \"type\" : \"typeref\",\n" +
        "        \"name\" : \"Union\",\n" +
        "        \"ref\" : [\n" +
        "          {\n" +
        "            \"type\" : \"typeref\",\n" +
        "            \"name\" : \"Int\",\n" +
        "            \"ref\" : \"int\",\n" +
        "            \"validate\" : {\n" +
        "              \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"i-1\" },\n" +
        "              \"v2\" : { \"name\" : \"i=0\" },\n" +
        "              \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"i+1\" }\n" +
        "            }\n" +
        "          },\n" +
        "          \"string\"\n" +
        "        ],\n" +
        "        \"validate\" : {\n" +
        "          \"v1\" : { \"validatorPriority\" : -1, \"name\" : \"u-1\" },\n" +
        "          \"v2\" : { \"name\" : \"u=0\" },\n" +
        "          \"v3\" : { \"validatorPriority\" : 1, \"name\" : \"u+1\" }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}\n",
        "{\n" +
        "  \"u\" : { \"int\" : 4 }\n" +
        "}",
        new String[] {
          "/u/int:i+1", "/u/int:i=0",
          "/u/int:i=0", "/u/int:i-1",
          "/u/int:i-1", "/u:u+1",
          "/u:u+1", "/u:u=0",
          "/u:u=0", "/u:u-1",
        }
      },

    };

    boolean debug = false;

    for (Object[] row : inputs)
    {
      int i = 0;
      String schemaText = (String) row[i++];
      String dataMapText = (String) row[i++];

      DataSchema schema = dataSchemaFromString(schemaText);
      DataMap dataMap = dataMapFromString(dataMapText);

      DataSchemaAnnotationValidator dataSchemaAnnotationValidator = new DataSchemaAnnotationValidator();
      dataSchemaAnnotationValidator.init(schema, validatorClassMap);
      if (debug) out.println(dataSchemaAnnotationValidator.getInitMessages());
      assertTrue(dataSchemaAnnotationValidator.isInitOk());
      if (debug) out.println(dataSchemaAnnotationValidator);
      dataSchemaAnnotationValidator.setDebugMode(debug);

      OrderValidator._orderList.clear();

      ValidationOptions validationOptions = new ValidationOptions();
      ValidationResult validationResult =
        ValidateDataAgainstSchema.validate(dataMap, schema, validationOptions, dataSchemaAnnotationValidator);
      assertTrue(validationResult.isValid());

      if (debug) out.println(validationResult.getMessages());
      if (debug) out.println(OrderValidator._orderList);

      String[] expectedRelations = (String[]) row[i++];
      assertTrue(expectedRelations.length % 2 == 0);
      for (int r = 0; r < expectedRelations.length; r += 2)
      {
        OrderRelation orderRelation = new OrderRelation(new OrderEntry(expectedRelations[r]),
                                                        new OrderEntry(expectedRelations[r + 1]));
        assertTrue(orderRelation.isSatisfied(OrderValidator._orderList));
      }
    }
  }
}

