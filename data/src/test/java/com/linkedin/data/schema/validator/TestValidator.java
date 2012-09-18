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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

  @SuppressWarnings("serial")
  static Map<String, Class<? extends Validator>> _validatorClassMap = new HashMap<String, Class<? extends Validator>>()
  {
    {
      put("fooValidator", FooValidator.class);
    }
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
          // validate using ValidateDataWithSchema
          VisitedTrackingValidator visitedValidator = new VisitedTrackingValidator(annotationValidator);
          ValidationOptions validationOptions = new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.NORMAL);
          ValidationResult result = ValidateDataAgainstSchema.validate(value.copy(), schema, validationOptions, visitedValidator);
          checkValidationResult(value, result, row, visitedValidator);
        }

        if ((tests & OBJECT_VALIDATOR) != 0)
        {
          // validate using ValidateWithValidator
          VisitedTrackingValidator visitedValidator = new VisitedTrackingValidator(annotationValidator);
          @SuppressWarnings("deprecation")
          ValidationResult result = ValidateWithValidator.validate(value.copy(), schema, visitedValidator);
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

}

