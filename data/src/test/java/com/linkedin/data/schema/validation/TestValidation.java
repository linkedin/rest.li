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

package com.linkedin.data.schema.validation;


import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;

import org.testng.annotations.Test;

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.DataElementUtil;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.validator.VisitedTrackingValidator;

import static com.linkedin.data.TestUtil.*;
import static org.testng.Assert.*;


/**
 * Test schema validation.
 */
public class TestValidation
{
  public static ValidationResult validate(DataMap map, DataSchema schema, ValidationOptions options)
  {
    VisitedTrackingValidator visitedTrackingValidator = new VisitedTrackingValidator(null);
    ValidationResult result = ValidateDataAgainstSchema.validate(map, schema, options, visitedTrackingValidator);
    assertEquals(visitedTrackingValidator.getVisitedMoreThanOnce(), Collections.EMPTY_SET);
    return result;
  }

  public static ValidationResult validate(DataElement element, ValidationOptions options)
  {
    VisitedTrackingValidator visitedTrackingValidator = new VisitedTrackingValidator(null);
    ValidationResult result = ValidateDataAgainstSchema.validate(element, options, visitedTrackingValidator);
    assertEquals(visitedTrackingValidator.getVisitedMoreThanOnce(), Collections.EMPTY_SET);
    return result;
  }

  public static ValidationOptions noCoercionValidationOption()
  {
    ValidationOptions options = new ValidationOptions();
    assertSame(options.getRequiredMode(), RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT);
    options.setCoercionMode(CoercionMode.OFF);
    return options;
  }

  public static ValidationOptions normalCoercionValidationOption()
  {
    ValidationOptions options = new ValidationOptions();
    assertSame(options.getRequiredMode(), RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT);
    assertEquals(options.getCoercionMode(), CoercionMode.NORMAL);
    return options;
  }

  public static ValidationOptions stringToPrimitiveCoercionValidationOption()
  {
    ValidationOptions options = new ValidationOptions();
    assertSame(options.getRequiredMode(), RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT);
    options.setCoercionMode(CoercionMode.STRING_TO_PRIMITIVE);
    return options;
  }

  public static ValidationOptions disallowUnrecognizedFieldOption()
  {
    ValidationOptions options = new ValidationOptions();
    assertSame(options.getUnrecognizedFieldMode(), UnrecognizedFieldMode.IGNORE);
    options.setUnrecognizedFieldMode(UnrecognizedFieldMode.DISALLOW);
    return options;
  }

  public static ValidationOptions trimUnrecognizedFieldOption()
  {
    ValidationOptions options = new ValidationOptions();
    assertSame(options.getUnrecognizedFieldMode(), UnrecognizedFieldMode.IGNORE);
    options.setUnrecognizedFieldMode(UnrecognizedFieldMode.TRIM);
    return options;
  }

  // For CoercionMode.STRING_TO_PRIMITIVE we want to coerce Strings into the correct datatype
  // also
  private static void assertAllowedClass(CoercionMode coercionMode, Class<?> clazz)
  {
    assertTrue(clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Float.class ||
               clazz == Double.class ||
               (coercionMode == CoercionMode.STRING_TO_PRIMITIVE && clazz == String.class));
  }

  public void testCoercionValidation(String schemaText,
                                     String key,
                                     Object[][] inputs,
                                     Object[] badObjects,
                                     CoercionMode coercionMode)
      throws IOException
  {
    assertTrue(coercionMode != CoercionMode.OFF);
    final boolean debug = false;
    ValidationOptions options = normalCoercionValidationOption();
    options.setCoercionMode(coercionMode);

    if (debug) out.println("--------------\nschemaText: " + schemaText);

    RecordDataSchema schema = (RecordDataSchema) dataSchemaFromString(schemaText);
    if (debug) out.println("schema: " + schema);
    assertTrue(schema != null);

    DataMap map = new DataMap();
    for (Object[] row : inputs)
    {
      if (debug) out.println("input: " + row[0] + " expected output: " + row[1]);
      map.put(key, row[0]);
      ValidationResult result = validate(map, schema, options);
      if (debug) out.println("result: " + result);
      assertTrue(result.isValid());
      if (result.hasFix())
      {
        DataMap fixedMap = (DataMap) result.getFixed();
        assertSame(fixedMap.getClass(), DataMap.class);
        Object fixed = fixedMap.get(key);
        assertTrue(fixed != null);
        Class<?> fixedClass = fixed.getClass();
        Class<?> goodClass = row[0].getClass();
        if (debug) out.println(goodClass + " " + fixedClass);
        switch (schema.getField(key).getType().getDereferencedType())
        {
          case BYTES:
          case FIXED:
            // String to ByteString conversion check
            assertNotSame(goodClass, fixedClass);
            assertSame(goodClass, String.class);
            assertSame(fixedClass, ByteString.class);
            assertEquals(((ByteString) fixed).asAvroString(), row[0]);
            break;
          case INT:
            // convert numbers to Integer
            assertNotSame(goodClass, fixedClass);
            assertAllowedClass(coercionMode, goodClass);
            assertSame(fixedClass, Integer.class);
            break;
          case LONG:
            // convert numbers to Long
            assertNotSame(goodClass, fixedClass);
            assertAllowedClass(coercionMode, goodClass);
            assertSame(fixedClass, Long.class);
            break;
          case FLOAT:
            // convert numbers to Float
            assertNotSame(goodClass, fixedClass);
            assertAllowedClass(coercionMode, goodClass);
            assertSame(fixedClass, Float.class);
            break;
          case DOUBLE:
            // convert numbers to Double
            assertNotSame(goodClass, fixedClass);
            assertAllowedClass(coercionMode, goodClass);
            assertSame(fixedClass, Double.class);
            break;
          case BOOLEAN:
            if(coercionMode == CoercionMode.STRING_TO_PRIMITIVE)
            {
              assertNotSame(goodClass, fixedClass);
              assertTrue(goodClass == String.class);
              assertSame(fixedClass, Boolean.class);
            }
            break;
          case RECORD:
          case ARRAY:
          case MAP:
          case UNION:
            assertSame(goodClass, fixedClass);
            break;
          default:
            throw new IllegalStateException("unknown conversion");
        }
        assertEquals(fixed, row[1]);
      }
      else
      {
        assertSame(map, result.getFixed());
      }
    }

    for (Object bad : badObjects)
    {
      if (debug) out.println("bad: " + bad);
      map.put(key, bad);
      ValidationResult result = validate(map, schema, options);
      if (debug) out.println(result);
      assertFalse(result.isValid());
      assertSame(map, result.getFixed());
    }
  }

  // Tests for CoercionMode.NORMAL
  public void testNormalCoercionValidation(String schemaText,
                                           String key,
                                           Object[][] inputs,
                                           Object[] badObjects) throws IOException
  {
    testCoercionValidation(schemaText, key, inputs, badObjects, CoercionMode.NORMAL);
  }

  // Tests for CoercionMode.STRING_TO_PRIMITIVE
  public void testStringToPrimitiveCoercionValidation(String schemaText,
                                                      String key,
                                                      Object[][] inputs,
                                                      Object[] badObjects) throws IOException
  {
    testCoercionValidation(schemaText, key, inputs, badObjects, CoercionMode.STRING_TO_PRIMITIVE);
  }

  public void testCoercionValidation(String schemaText,
                                     String key,
                                     Object[] goodObjects,
                                     Object[] badObjects,
                                     ValidationOptions options) throws IOException
  {
    final boolean debug = false;

    if (debug) out.println("--------------\nschemaText: " + schemaText);

    RecordDataSchema schema = (RecordDataSchema) dataSchemaFromString(schemaText);
    if (debug) out.println("schema: " + schema);
    assertTrue(schema != null);

    DataMap map = new DataMap();
    for (Object good : goodObjects)
    {
      if (debug) out.println("good: " + good);
      map.put(key, good);
      ValidationResult result = validate(map, schema, options);
      if (debug) out.println("result: " + result);
      assertTrue(result.isValid());
      assertFalse(result.hasFix());
      assertSame(map, result.getFixed());
    }

    for (Object bad : badObjects)
    {
      if (debug) out.println("bad: " + bad);
      map.put(key, bad);
      ValidationResult result = validate(map, schema, options);
      if (debug) out.println(result);
      assertFalse(result.isValid());
      assertSame(map, result.getFixed());
    }
  }

  @Test
  public void testStringValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"string\" } ] }";

    Object goodObjects[] =
    {
        "a valid string"
    };

    Object badObjects[] =
    {
        new Boolean(false),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
    };

    // There is no coercion for this type.
    // Test with all coercion modes, result should be the same for all cases.
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, normalCoercionValidationOption());
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, stringToPrimitiveCoercionValidationOption());
  }

  @Test
  public void testBooleanValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"boolean\" } ] }";

    Object goodObjects[] =
    {
        new Boolean(true),
        new Boolean(false)
    };

    Object badObjects[] =
    {
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, normalCoercionValidationOption());
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testBooleanStringToPrimitiveFixupValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : \"boolean\" } ] }";

    Object input[][] =
        {
            { new String("true"), Boolean.TRUE },
            { new String("false"), Boolean.FALSE },
        };

    Object badObjects[] =
        {
            new Integer(1),
            new Long(1),
            new Float(1),
            new Double(1),
            new String("abc"),
            new DataMap(),
            new DataList()
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar", input, badObjects);
  }

  @Test
  public void testIntegerNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"int\" } ] }";

    Object goodObjects[] =
    {
        new Integer(1),
        new Integer(-1),
        Integer.MAX_VALUE,
        Integer.MAX_VALUE - 1
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Long(1),
        new Float(1),
        new Double(1),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testIntegerNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"int\" } ] }";


    Object input[][] =
      {
        { new Integer(1), new Integer(1) },
        { new Integer(-1), new Integer(-1) },
        { Integer.MAX_VALUE, Integer.MAX_VALUE },
        { Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1 },
        { new Long(1), new Integer(1) },
        { new Float(1), new Integer(1) },
        { new Double(1), new Integer(1) }
      };

    Object badObjects[] =
      {
        new Boolean(true),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
      };

    testNormalCoercionValidation(schemaText, "bar", input, badObjects);
  }

  @Test
  public void testIntegerStringToPrimitiveCoercionValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : \"int\" } ] }";

    Object input[][] =
        {
            { new String("1"), new Integer(1) },
            { new String("-1"), new Integer(-1) },
            { new String("" + Integer.MAX_VALUE), Integer.MAX_VALUE},
            { new String("" + (Integer.MAX_VALUE - 1)), Integer.MAX_VALUE - 1},
            { new String("1.5"), new Integer(1) },
            { new String("-1.5"), new Integer(-1) },

            { new Integer(1), new Integer(1) },
            { new Integer(-1), new Integer(-1) },
            { Integer.MAX_VALUE, Integer.MAX_VALUE },
            { Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1 },
            { new Long(1), new Integer(1) },
            { new Float(1), new Integer(1) },
            { new Double(1), new Integer(1) }
        };

    Object badObjects[] =
        {
            new Boolean(true),
            new String("abc"),
            ByteString.copyAvroString("bytes", false),
            new DataMap(),
            new DataList()
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar", input, badObjects);
  }

  @Test
  public void testLongNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"long\" } ] }";

    Object goodObjects[] =
    {
        new Long(1),
        new Long(-1)
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Float(1),
        new Double(1),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testLongNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"long\" } ] }";

    Object inputs[][] =
      {
        { new Long(1), new Long(1) },
        { new Long(-1), new Long(-1) },
        { new Integer(1), new Long(1) },
        { new Float(1), new Long(1) },
        { new Double(1), new Long(1) }
      };

    Object badObjects[] =
      {
        new Boolean(true),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
      };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testLongStringToPrimitiveCoercionValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : \"long\" } ] }";

    Object inputs[][] =
        {
            { new String("1"), new Long(1) },
            { new String("-1"), new Long(-1) },
            { new String("" + Long.MAX_VALUE), Long.MAX_VALUE },

            { new Long(1), new Long(1) },
            { new Long(-1), new Long(-1) },
            { new Integer(1), new Long(1) },
            { new Float(1), new Long(1) },
            { new Double(1), new Long(1) }
        };


    Object badObjects[] =
        {
            new Boolean(true),
            new String("abc"),
            ByteString.copyAvroString("bytes", false),
            new DataMap(),
            new DataList()
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testFloatNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"float\" } ] }";

    Object goodObjects[] =
    {
        new Float(1),
        new Float(-1)
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Double(1),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testFloatNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"float\" } ] }";

    Object inputs[][] =
      {
        { new Float(1), new Float(1) },
        { new Float(-1), new Float(-1) },
        { new Integer(1), new Float(1) },
        { new Long(1), new Float(1) },
        { new Double(1), new Float(1) }
      };

    Object badObjects[] =
      {
        new Boolean(true),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
      };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testFloatStringToPrimitiveCoercionValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : \"float\" } ] }";

    Object inputs[][] =
        {
            { new String("1"), new Float(1) },
            { new String("-1"), new Float(-1) },
            { new String("1.01"), new Float(1.01) },
            { new String("-1.01"), new Float(-1.01) },
            { new String("" + Float.MAX_VALUE), Float.MAX_VALUE },

            { new Float(1), new Float(1) },
            { new Float(1), new Float(1) },
            { new Float(-1), new Float(-1) },
            { new Integer(1), new Float(1) },
            { new Long(1), new Float(1) },
            { new Double(1), new Float(1) }
        };

    Object badObjects[] =
        {
            new Boolean(true),
            new String("abc"),
            ByteString.copyAvroString("bytes", false),
            new DataMap(),
            new DataList()
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testDoubleNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"double\" } ] }";

    Object goodObjects[] =
    {
        new Double(1),
        new Double(-1)
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testDoubleNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"double\" } ] }";

    Object inputs[][] =
      {
        { new Double(1), new Double(1) },
        { new Double(-1), new Double(-1) },
        { new Integer(1), new Double(1) },
        { new Long(1), new Double(1) },
        { new Float(1), new Double(1) }
      };

    Object badObjects[] =
      {
        new Boolean(true),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
      };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testDoubleStringToPrimitiveCoercionValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : \"double\" } ] }";

    Object inputs[][] =
        {
            { new String("1"), new Double(1) },
            { new String("-1"), new Double(-1) },
            { new String("1.01"), new Double(1.01) },
            { new String("-1.01"), new Double(-1.01) },
            { new String("" + Double.MAX_VALUE), Double.MAX_VALUE },

            { new Double(1), new Double(1) },
            { new Double(-1), new Double(-1) },
            { new Integer(1), new Double(1) },
            { new Long(1), new Double(1) },
            { new Float(1), new Double(1) }
        };

    Object badObjects[] =
        {
            new Boolean(true),
            new String("abc"),
            ByteString.copyAvroString("bytes", false),
            new DataMap(),
            new DataList()
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testBytesValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : \"bytes\" } ] }";

    Object goodObjects[] =
    {
        "abc",
        ByteString.copyAvroString("bytes", false),
        ByteString.empty()
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new DataMap(),
        new DataList(),
        new String("\u0100"),
        new String("ab\u0100c"),
        new String("ab\u0100c\u0200")
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());

    Object inputs[][] =
    {
        { ByteString.copyAvroString("abc", false), ByteString.copyAvroString("abc", false) },
        { "abc", ByteString.copyAvroString("abc", false) }
    };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
    testStringToPrimitiveCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testFixedValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : { \"name\" : \"fixed4\", \"type\" : \"fixed\", \"size\" : 4 } } ] }";

    Object goodObjects[] =
    {
        "abcd",
        "1234",
        "\u0001\u0002\u0003\u0004",
        "\u0001\u0002\u00ff\u00fe",
        ByteString.copyAvroString("abcd", false),
        ByteString.copyAvroString("1234", false),
        ByteString.copyAvroString("\u0001\u0002\u0003\u0004", false),
        ByteString.copyAvroString("\u0001\u0002\u00ff\u00fe", false)
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new DataMap(),
        new DataList(),
        new String(),
        "1",
        "12",
        "123",
        "12345",
        "\u0100",
        "ab\u0100c",
        "b\u0100c\u0200",
        ByteString.empty(),
        ByteString.copyAvroString("1", false),
        ByteString.copyAvroString("12", false),
        ByteString.copyAvroString("123", false),
        ByteString.copyAvroString("12345", false)
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());

    Object inputs[][] =
    {
        { "abcd", ByteString.copyAvroString("abcd", false) },
        { "\u0001\u0002\u0003\u0004", ByteString.copyAvroString("\u0001\u0002\u0003\u0004", false) },
        { ByteString.copyAvroString("abcd", false), ByteString.copyAvroString("abcd", false) },
        { ByteString.copyAvroString("\u0001\u0002\u0003\u0004", false), ByteString.copyAvroString("\u0001\u0002\u0003\u0004", false) }
    };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
    testStringToPrimitiveCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testEnumCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : { \"name\" : \"fruits\", \"type\" : \"enum\", \"symbols\" : [ \"apple\", \"orange\", \"banana\" ] } } ] }";

    Object goodObjects[] =
    {
        new String("apple"),
        new String("orange"),
        new String("banana")
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String(),
        new String("foobar"),
        new String("Apple"),
        new String("Orange"),
        new String("BaNaNa"),
        new DataMap(),
        new DataList()
    };

    // There is no coercion for this type.
    // Test with all coercion validation options, result should be the same for all cases.
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, normalCoercionValidationOption());
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, stringToPrimitiveCoercionValidationOption());
  }

  @Test
  public void testArrayNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } } ] }";

    Object goodObjects[] =
    {
        new DataList(),
        new DataList(asList(new Integer(1))),
        new DataList(asList(new Integer(2), new Integer(3))),
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String(),
        new DataMap(),
        new DataList(asList(new Boolean(true))),
        new DataList(asList(new Long(1))),
        new DataList(asList(new Float(1))),
        new DataList(asList(new Double(1))),
        new DataList(asList(new String("1"))),
        new DataList(asList(new DataMap())),
        new DataList(asList(new DataList())),
        new DataList(asList(new Boolean(true), new Integer(1))),
        new DataList(asList(new Integer(1), new Boolean(true)))
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testArrayNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } } ] }";

    Object inputs[][] =
      {
        { new DataList(), new DataList() },
        { new DataList(asList(1)), new DataList(asList(1)) },
        { new DataList(asList(2, 3)), new DataList(asList(2, 3)) },
        { new DataList(asList(1L)), new DataList(asList(1)) },
        { new DataList(asList(1.0f)), new DataList(asList(1)) },
        { new DataList(asList(1.0)), new DataList(asList(1)) }
      };

    Object badObjects[] =
      {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String(),
        new DataMap(),
        new DataList(asList(new Boolean(true))),
        new DataList(asList(new String("1"))),
        new DataList(asList(new DataMap())),
        new DataList(asList(new DataList())),
        new DataList(asList(new Boolean(true), new Integer(1))),
        new DataList(asList(new Integer(1), new Boolean(true)))
      };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testArrayStringToPrimitiveCoercionValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } } ] }";

    Object inputs[][] =
        {
            { new DataList(asList("1")), new DataList(asList(1)) },
            { new DataList(asList("1", "2", "3")), new DataList(asList(1, 2, 3)) },

            { new DataList(), new DataList() },
            { new DataList(asList(1)), new DataList(asList(1)) },
            { new DataList(asList(2, 3)), new DataList(asList(2, 3)) },
            { new DataList(asList(1L)), new DataList(asList(1)) },
            { new DataList(asList(1.0f)), new DataList(asList(1)) },
            { new DataList(asList(1.0)), new DataList(asList(1)) }
        };

    Object badObjects[] =
        {
            new Boolean(true),
            new Integer(1),
            new Long(1),
            new Float(1),
            new Double(1),
            new String(),
            new DataMap(),
            new DataList(asList(new Boolean(true))),
            new DataList(asList(new DataMap())),
            new DataList(asList(new DataList())),
            new DataList(asList(new Boolean(true), new Integer(1))),
            new DataList(asList(new Integer(1), new Boolean(true)))
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testMapNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" } } ] }";

    Object goodObjects[] =
    {
        new DataMap(),
        new DataMap(asMap("key1", 1)),
        new DataMap(asMap("key1", 1, "key2", 2)),
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String(),
        new DataList(),
        new DataMap(asMap("key1", new Boolean(true))),
        new DataMap(asMap("key1", new Long(1))),
        new DataMap(asMap("key1", new Float(1))),
        new DataMap(asMap("key1", new Double(1))),
        new DataMap(asMap("key1", new String("1"))),
        new DataMap(asMap("key1", new DataMap())),
        new DataMap(asMap("key1", new DataList())),
        new DataMap(asMap("key1", new Integer(1), "key2", new Long(1))),
        new DataMap(asMap("key1", new Long(1), "key2", new Integer(1)))
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testMapNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" } } ] }";

    Object inputs[][] =
      {
        { new DataMap(), new DataMap() },
        { new DataMap(asMap("key1", 1)), new DataMap(asMap("key1", 1)) },
        { new DataMap(asMap("key1", 1, "key2", 2)), new DataMap(asMap("key1", 1, "key2", 2)) },
        { new DataMap(asMap("key1", 1L)), new DataMap(asMap("key1", 1)) },
        { new DataMap(asMap("key1", 1.0)), new DataMap(asMap("key1", 1)) },
        { new DataMap(asMap("key1", 1.0f)), new DataMap(asMap("key1", 1)) },
        { new DataMap(asMap("key1", 1, "key2", 2L)), new DataMap(asMap("key1", 1, "key2", 2)) },
        { new DataMap(asMap("key1", 1L, "key2", 2.0)), new DataMap(asMap("key1", 1, "key2", 2)) },
      };

    Object badObjects[] =
      {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String(),
        new DataList(),
        new DataMap(asMap("key1", new Boolean(true))),
        new DataMap(asMap("key1", new String("1"))),
        new DataMap(asMap("key1", new DataMap())),
        new DataMap(asMap("key1", new DataList())),
      };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testMapStringToPrimitiveValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" } } ] }";

    Object inputs[][] =
        {
            { new DataMap(asMap("key1", "1")), new DataMap(asMap("key1", 1)) },
            { new DataMap(asMap("key1", "1", "key2", "2")), new DataMap(asMap("key1", 1, "key2", 2)) },

            { new DataMap(), new DataMap() },
            { new DataMap(asMap("key1", 1)), new DataMap(asMap("key1", 1)) },
            { new DataMap(asMap("key1", 1, "key2", 2)), new DataMap(asMap("key1", 1, "key2", 2)) },
            { new DataMap(asMap("key1", 1L)), new DataMap(asMap("key1", 1)) },
            { new DataMap(asMap("key1", 1.0)), new DataMap(asMap("key1", 1)) },
            { new DataMap(asMap("key1", 1.0f)), new DataMap(asMap("key1", 1)) },
            { new DataMap(asMap("key1", 1, "key2", 2L)), new DataMap(asMap("key1", 1, "key2", 2)) },
            { new DataMap(asMap("key1", 1L, "key2", 2.0)), new DataMap(asMap("key1", 1, "key2", 2)) },
        };

    Object badObjects[] =
        {
            new Boolean(true),
            new Integer(1),
            new Long(1),
            new Float(1),
            new Double(1),
            new String(),
            new DataList(),
            new DataMap(asMap("key1", new Boolean(true))),
            new DataMap(asMap("key1", new DataMap())),
            new DataMap(asMap("key1", new DataList())),
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testUnionNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"foo\",\n" +
      "  \"fields\" : [\n" +
      "    {\n" +
      "      \"name\" : \"bar\",\n" +
      "      \"type\" : [\n" +
      "        \"null\",\n" +
      "        \"int\",\n" +
      "        \"string\",\n" +
      "        { \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }\n" +
      "      ]\n" +
      "    }\n" +
      "  ]\n" +
      "}";

    Object goodObjects[] =
    {
        Data.NULL,
        new DataMap(asMap("int", new Integer(1))),
        new DataMap(asMap("string", "x")),
        new DataMap(asMap("Fruits", "APPLE")),
        new DataMap(asMap("Fruits", "ORANGE")),
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String(),
        new DataList(),
        new DataMap(),
        new DataMap(asMap("int", new Boolean(true))),
        new DataMap(asMap("int", new String("1"))),
        new DataMap(asMap("int", new Long(1L))),
        new DataMap(asMap("int", new Float(1.0f))),
        new DataMap(asMap("int", new Double(1.0))),
        new DataMap(asMap("int", new DataMap())),
        new DataMap(asMap("int", new DataList())),
        new DataMap(asMap("string", new Boolean(true))),
        new DataMap(asMap("string", new Integer(1))),
        new DataMap(asMap("string", new Long(1L))),
        new DataMap(asMap("string", new Float(1.0f))),
        new DataMap(asMap("string", new Double(1.0))),
        new DataMap(asMap("string", new DataMap())),
        new DataMap(asMap("string", new DataList())),
        new DataMap(asMap("Fruits", "foobar")),
        new DataMap(asMap("Fruits", new Integer(1))),
        new DataMap(asMap("Fruits", new DataMap())),
        new DataMap(asMap("Fruits", new DataList())),
        new DataMap(asMap("int", new Integer(1), "string", "x")),
        new DataMap(asMap("x", new Integer(1), "y", new Long(1))),
    };

    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testUnionNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"foo\",\n" +
      "  \"fields\" : [\n" +
      "    {\n" +
      "      \"name\" : \"bar\",\n" +
      "      \"type\" : [\n" +
      "        \"null\",\n" +
      "        \"int\",\n" +
      "        \"string\",\n" +
      "        { \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }\n" +
      "      ]\n" +
      "    }\n" +
      "  ]\n" +
      "}";

    Object inputs[][] =
      {
        { Data.NULL, Data.NULL },
        { new DataMap(asMap("int", 1)), new DataMap(asMap("int", 1)) },
        { new DataMap(asMap("string", "x")), new DataMap(asMap("string", "x")) },
        { new DataMap(asMap("Fruits", "APPLE")), new DataMap(asMap("Fruits", "APPLE")) },
        { new DataMap(asMap("Fruits", "ORANGE")), new DataMap(asMap("Fruits", "ORANGE")) },
        { new DataMap(asMap("int", 1L)), new DataMap(asMap("int", 1)) },
        { new DataMap(asMap("int", 1.0f)), new DataMap(asMap("int", 1)) },
        { new DataMap(asMap("int", 1.0)), new DataMap(asMap("int", 1)) },

      };

    Object badObjects[] =
      {
        new Boolean(true),
        new Integer(1),
        new Long(1),
        new Float(1),
        new Double(1),
        new String(),
        new DataList(),
        new DataMap(asMap("int", new Boolean(true))),
        new DataMap(asMap("int", new String("1"))),
        new DataMap(asMap("int", new DataMap())),
        new DataMap(asMap("int", new DataList())),
        new DataMap(asMap("string", new Boolean(true))),
        new DataMap(asMap("string", new Integer(1))),
        new DataMap(asMap("string", new Long(1L))),
        new DataMap(asMap("string", new Float(1.0f))),
        new DataMap(asMap("string", new Double(1.0))),
        new DataMap(asMap("string", new DataMap())),
        new DataMap(asMap("string", new DataList())),
        new DataMap(asMap("Fruits", "foobar")),
        new DataMap(asMap("Fruits", new Integer(1))),
        new DataMap(asMap("Fruits", new DataMap())),
        new DataMap(asMap("Fruits", new DataList())),
        new DataMap(asMap("int", new Integer(1), "string", "x")),
        new DataMap(asMap("x", new Integer(1), "y", new Long(1))),
      };

    testNormalCoercionValidation(schemaText, "bar", inputs, badObjects);
  }

  @Test
  public void testTyperefNoCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar1\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"int2\", \"ref\" : \"int\" }, \"optional\" : true }," +
      "  { \"name\" : \"bar2\", \"type\" : \"int2\", \"optional\" : true }, " +
      "  { \"name\" : \"bar3\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"int3\", \"ref\" : \"int2\" }, \"optional\" : true }," +
      "  { \"name\" : \"bar4\", \"type\" : \"int3\", \"optional\" : true }" +
      "] }";

    Object goodObjects[] =
    {
        new Integer(1),
        new Integer(-1)
    };

    Object badObjects[] =
    {
        new Boolean(true),
        new Long(1),
        new Float(1),
        new Double(1),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
    };

    testCoercionValidation(schemaText, "bar1", goodObjects, badObjects, noCoercionValidationOption());
    testCoercionValidation(schemaText, "bar2", goodObjects, badObjects, noCoercionValidationOption());
    testCoercionValidation(schemaText, "bar3", goodObjects, badObjects, noCoercionValidationOption());
    testCoercionValidation(schemaText, "bar4", goodObjects, badObjects, noCoercionValidationOption());
  }

  @Test
  public void testTyperefNormalCoercionValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ { \"name\" : \"bar1\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"int2\", \"ref\" : \"int\" }, \"optional\" : true }," +
      "  { \"name\" : \"bar2\", \"type\" : \"int2\", \"optional\" : true }, " +
      "  { \"name\" : \"bar3\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"int3\", \"ref\" : \"int2\" }, \"optional\" : true }," +
      "  { \"name\" : \"bar4\", \"type\" : \"int3\", \"optional\" : true }" +
      "] }";

    Object inputs[][] =
      {
        { new Integer(1), new Integer(1) },
        { new Integer(-1), new Integer(-1) },
        { new Long(1), new Integer(1) },
        { new Float(1), new Integer(1) },
        { new Double(1), new Integer(1) }
      };

    Object badObjects[] =
      {
        new Boolean(true),
        new String("abc"),
        ByteString.copyAvroString("bytes", false),
        new DataMap(),
        new DataList()
      };

    testNormalCoercionValidation(schemaText, "bar1", inputs, badObjects);
    testNormalCoercionValidation(schemaText, "bar2", inputs, badObjects);
    testNormalCoercionValidation(schemaText, "bar3", inputs, badObjects);
    testNormalCoercionValidation(schemaText, "bar4", inputs, badObjects);
  }

  @Test
  public void testTyperefStringToPrimitiveCoercionValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar1\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"int2\", \"ref\" : \"int\" }, \"optional\" : true }," +
            "  { \"name\" : \"bar2\", \"type\" : \"int2\", \"optional\" : true }, " +
            "  { \"name\" : \"bar3\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"int3\", \"ref\" : \"int2\" }, \"optional\" : true }," +
            "  { \"name\" : \"bar4\", \"type\" : \"int3\", \"optional\" : true }" +
            "] }";

    Object inputs[][] =
        {
            { new String("1"), new Integer(1) },

            { new Integer(1), new Integer(1) },
            { new Integer(-1), new Integer(-1) },
            { new Long(1), new Integer(1) },
            { new Float(1), new Integer(1) },
            { new Double(1), new Integer(1) }
        };

    Object badObjects[] =
        {
            new Boolean(true),
            new String("abc"),
            ByteString.copyAvroString("bytes", false),
            new DataMap(),
            new DataList()
        };

    testStringToPrimitiveCoercionValidation(schemaText, "bar1", inputs, badObjects);
    testStringToPrimitiveCoercionValidation(schemaText, "bar2", inputs, badObjects);
    testStringToPrimitiveCoercionValidation(schemaText, "bar3", inputs, badObjects);
    testStringToPrimitiveCoercionValidation(schemaText, "bar4", inputs, badObjects);
  }

  @Test
  public void testRecordValidation() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
      "[ " +
        "{ \"name\" : \"bar\", \"type\" : { \"name\" : \"barType\", \"type\" : \"record\", \"fields\" : [" +
        "{ \"name\" : \"requiredInt\", \"type\" : \"int\" }," +
        "{ \"name\" : \"requiredString\", \"type\" : \"string\" }," +
        "{ \"name\" : \"defaultString\", \"type\" : \"string\", \"default\" : \"apple\" }," +
        "{ \"name\" : \"optionalBoolean\", \"type\" : \"boolean\", \"optional\" : true }," +
        "{ \"name\" : \"optionalDouble\", \"type\" : \"double\", \"optional\" : true }," +
        "{ \"name\" : \"optionalWithDefaultString\", \"type\" : \"string\", \"optional\" : true, \"default\" : \"orange\" }" +
      "] } } ] }";

    Object good[][][] =
    {
      {
        {
          new ValidationOptions(RequiredMode.IGNORE, CoercionMode.OFF)
        },
        {
          new DataMap(),
          new DataMap(asMap("requiredInt", 12)),
          new DataMap(asMap("requiredString", "")),
          new DataMap(asMap("defaultString", "")),
          new DataMap(asMap("optionalBoolean", false)),
          new DataMap(asMap("requiredInt", 12, "requiredString", "")),
          new DataMap(asMap("requiredInt", 34, "defaultString", "cow")),
          new DataMap(asMap("requiredString", "dog", "defaultString", "cow")),
        }
      },
      {
        {
          new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
          new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.OFF),
          new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT, CoercionMode.OFF)
        },
        {
          new DataMap(asMap("requiredInt", 12, "requiredString", "", "defaultString", "")),
          new DataMap(asMap("requiredInt", 34, "requiredString", "cow", "defaultString", "dog")),
          new DataMap(asMap("requiredInt", 56, "requiredString", "cat", "defaultString", "pig", "optionalBoolean", false)),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "defaultString", "cog", "optionalBoolean", true, "optionalDouble", 999.5)),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "defaultString", "cog", "optionalBoolean", true, "optionalDouble", 999.5, "optionalWithDefaultString", "tag")),
          // unnecessary keys
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "defaultString", "cog", "extra1", new Boolean(true))),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "defaultString", "cog", "optionalBoolean", true, "optionalDouble", 999.5, "extra1", new Boolean(true)))
        }
      },
      {
        {
          new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.OFF),
          new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT, CoercionMode.NORMAL)
        },
        {
          new DataMap(asMap("requiredInt", 12, "requiredString", "")),
          new DataMap(asMap("requiredInt", 34, "requiredString", "cow")),
          new DataMap(asMap("requiredInt", 56, "requiredString", "cat", "optionalBoolean", false)),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "optionalBoolean", true, "optionalDouble", 999.5)),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "optionalBoolean", true, "optionalDouble", 999.5, "optionalWithDefaultString", "tag")),
          // unnecessary keys
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "extra1", new Boolean(true))),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "optionalBoolean", true, "optionalDouble", 999.5, "extra1", new Boolean(true)))
        }
      }
    };

    final String notBackedBy = "is not backed by";
    final String isRequired = "is required";

    Object bad[][][] =
    {
      {
        {
          new ValidationOptions(RequiredMode.IGNORE, CoercionMode.OFF),
          new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
          new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.OFF),
          new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT, CoercionMode.OFF)
        },
        {
          new Boolean(true),
          new Integer(1),
          new Long(1),
          new Float(1),
          new Double(1),
          new String(),
          new DataList(),
          // invalid field value types
          new DataMap(asMap("requiredInt", 78, "requiredString", true,  "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", 123,   "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", 123L,  "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", 12.5f, "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", 12.5,  "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", new DataMap(),  "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", new DataList(), "defaultString", "")),
          new DataMap(asMap("requiredInt", true,  "requiredString", "cow", "defaultString", "")),
          new DataMap(asMap("requiredInt", 124L,  "requiredString", "cow", "defaultString", "")),
          new DataMap(asMap("requiredInt", 12.5f, "requiredString", "cow", "defaultString", "")),
          new DataMap(asMap("requiredInt", 12.5,  "requiredString", "cow", "defaultString", "")),
          new DataMap(asMap("requiredInt", "cat", "requiredString", "cow", "defaultString", "")),
          new DataMap(asMap("requiredInt", new DataMap(),  "requiredString", "cow", "defaultString", "")),
          new DataMap(asMap("requiredInt", new DataList(), "requiredString", "cow", "defaultString", ""))
        },
        {
          notBackedBy
        },
        {
          new String[] { "/bar" },
          new String[] { "/bar" },
          new String[] { "/bar" },
          new String[] { "/bar/requiredString" },
          new String[] { "/bar/requiredString" },
          new String[] { "/bar/requiredString" },
          new String[] { "/bar/requiredString" },
          new String[] { "/bar/requiredString" },
          new String[] { "/bar/requiredString" },
          new String[] { "/bar/requiredString" },
          new String[] { "/bar/requiredInt" },
          new String[] { "/bar/requiredInt" },
          new String[] { "/bar/requiredInt" },
          new String[] { "/bar/requiredInt" },
          new String[] { "/bar/requiredInt" },
          new String[] { "/bar/requiredInt" },
          new String[] { "/bar/requiredInt" }
        }
      },
      {
        {
          new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
        },
        {
          // combinations of missing required and added optionals
          new DataMap(asMap("requiredInt", 78)),
          new DataMap(asMap("requiredString", "dog")),
          new DataMap(asMap("defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog")),
          new DataMap(asMap("requiredInt", 78, "defaultString", "")),
          new DataMap(asMap("requiredString", "dog", "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "requiredString", "dog", "optionalBoolean", true)),
          new DataMap(asMap("requiredInt", 78, "defaultString", "", "optionalDouble", 999.5)),
          new DataMap(asMap("requiredString", "dog", "defaultString", "", "optionalBoolean", true, "optionalDouble", 999.5)),
        },
        {
          isRequired
        },
        {
          new String[] { "/bar", "/bar/requiredString", "/bar", "/bar/defaultString" },
          new String[] { "/bar", "/bar/requiredInt", "/bar", "/bar/defaultString" },
          new String[] { "/bar", "/bar/requiredInt", "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/defaultString" },
          new String[] { "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredInt" },
          new String[] { "/bar", "/bar/defaultString" },
          new String[] { "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredInt" }
        }
      },
      {
        {
          new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.OFF),
        },
        {
          // combinations of missing required and added optionals
          new DataMap(asMap("requiredInt", 78)),
          new DataMap(asMap("requiredString", "dog")),
          new DataMap(asMap("defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "defaultString", "")),
          new DataMap(asMap("requiredString", "dog", "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "optionalBoolean", true)),
          new DataMap(asMap("requiredInt", 78, "defaultString", "", "optionalBoolean", true)),
          new DataMap(asMap("requiredString", "dog", "optionalDouble", 999.5)),
          new DataMap(asMap("requiredString", "dog", "defaultString", "", "optionalBoolean", true, "optionalDouble", 999.5)),
        },
        {
          isRequired
        },
        {
          new String[] { "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredInt" },
          new String[] { "/bar", "/bar/requiredInt", "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredInt" },
          new String[] { "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredInt" },
          new String[] { "/bar", "/bar/requiredInt" }
        }
      },
      {
        {
          new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT, CoercionMode.OFF)
        },
        {
          // combinations of missing required and added optionals
          new DataMap(asMap("requiredInt", 78)),
          new DataMap(asMap("requiredString", "dog")),
          new DataMap(asMap("defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "defaultString", "")),
          new DataMap(asMap("requiredString", "dog", "defaultString", "")),
          new DataMap(asMap("requiredInt", 78, "optionalBoolean", true)),
          new DataMap(asMap("requiredInt", 78, "defaultString", "", "optionalBoolean", true)),
          new DataMap(asMap("requiredString", "dog", "optionalDouble", 999.5)),
          new DataMap(asMap("requiredString", "dog", "defaultString", "", "optionalBoolean", true, "optionalDouble", 999.5)),
        },
        {
          isRequired
        },
        {
          new String[] { "/bar", "/bar/requiredString", "/bar", "/bar/defaultString" },
          new String[] { "/bar", "/bar/requiredInt", "/bar", "/bar/defaultString" },
          new String[] { "/bar", "/bar/requiredInt", "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredString" },
          new String[] { "/bar", "/bar/requiredInt" }
        }
      }
    };

    testValidationWithDifferentValidationOptions(schemaText, "bar", good, bad);
  }

  public void testValidationWithDifferentValidationOptions(String schemaText,
                                                           String key,
                                                           Object[][][] goodInput,
                                                           Object[][][] badInput) throws IOException
  {
    final boolean debug = false;
    final boolean printExpectedOutput = false;
    final String[][] emptyErrorPaths = {};

    if (debug) out.println("--------------\nschemaText: " + schemaText);

    DataSchema schema = dataSchemaFromString(schemaText);
    if (debug) out.println("schema: " + schema);
    assertTrue(schema != null);

    DataMap map = new DataMap();
    for (Object[][] rows : goodInput)
    {
      Object[] modes = rows[0];
      Object[] dataObjects = rows[1];
      for (Object mode : modes)
      {
        ValidationOptions validationOptions = (ValidationOptions) mode;
        for (Object dataObject : dataObjects)
        {
          if (debug) out.println("good " + mode + ": " + dataObject);
          map.put(key, dataObject);
          ValidationResult result = validate(map, schema, validationOptions);
          assertTrue(result.isValid());
          if (result.hasFix() == false)
          {
            assertSame(map, result.getFixed());
          }
        }
      }
    }

    for (Object[][] rows : badInput)
    {
      Object[] modes =  rows[0];
      Object[] dataObjects = rows[1];
      String expectedString = (String) rows[2][0];
      String[][] errorPaths = rows.length > 4 ? (String[][]) rows[3] : emptyErrorPaths;
      for (Object mode : modes)
      {
        ValidationOptions validationOptions = (ValidationOptions) mode;
        int index = 0;
        for (Object dataObject : dataObjects)
        {
          if (debug) out.println("bad " + mode + ": " + dataObject);
          map.put(key, dataObject);
          ValidationResult result = validate(map, schema, validationOptions);
          if (debug) out.println(result.getMessages());
          assertFalse(result.isValid());
          assertSame(map, result.getFixed());
          checkMessages(debug, result.getMessages(), expectedString);
          if (printExpectedOutput) printErrorPaths(out, result.getMessages());
          if (index < errorPaths.length)
          {
            checkMessagesErrorPath(result.getMessages(), errorPaths[index]);
          }
          index++;
        }
        if (printExpectedOutput) out.println();
      }
    }
  }

  private void checkMessages(boolean debug, Collection<Message> messages, String expectedString)
  {
    for (Message m : messages)
    {
      if (debug) out.println(m.getFormat() + " --- " + expectedString);
      assertTrue(m.getFormat().contains(expectedString));
    }
  }

  private void checkMessagesErrorPath(Collection<Message> messages, String[] errorPaths)
  {
    int index = 0;
    for (Message m : messages)
    {
      if (index >= errorPaths.length)
      {
        break;
      }
      String path = pathAsString(m.getPath());
      assertEquals(path, errorPaths[index]);
      index++;
    }
  }

  private void printErrorPaths(PrintStream out, Collection<Message> messages)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("new String[] { ");
    boolean first = true;
    for (Message m : messages)
    {
      if (first == false) sb.append(", ");
      sb.append("\"").append(pathAsString(m.getPath())).append("\"");
      first = false;
    }
    sb.append(" },");
    out.println(sb);
  }

  private String pathAsString(Object[] path)
  {
    StringBuilder sb = new StringBuilder();
    for (Object component : path)
    {
      sb.append(DataElement.SEPARATOR);
      sb.append(component.toString());
    }
    return sb.toString();
  }

  @Test
  public void testValidationWithNormalCoercion() throws IOException, CloneNotSupportedException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : \n" +
      "[ { \"name\" : \"bar\", \"type\" : { \"name\" : \"barType\", \"type\" : \"record\", \"fields\" : [ \n" +
      "{ \"name\" : \"boolean\", \"type\" : \"boolean\", \"optional\" : true }, \n" +
      "{ \"name\" : \"int\", \"type\" : \"int\", \"optional\" : true }, \n" +
      "{ \"name\" : \"long\", \"type\" : \"long\", \"optional\" : true }, \n" +
      "{ \"name\" : \"float\", \"type\" : \"float\", \"optional\" : true }, \n" +
      "{ \"name\" : \"double\", \"type\" : \"double\", \"optional\" : true }, \n" +
      "{ \"name\" : \"string\", \"type\" : \"string\", \"optional\" : true }, \n" +
      "{ \"name\" : \"bytes\", \"type\" : \"bytes\", \"optional\" : true }, \n" +
      "{ \"name\" : \"array\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" }, \"optional\" : true }, \n" +
      "{ \"name\" : \"enum\", \"type\" : { \"type\" : \"enum\", \"name\" : \"enumType\", \"symbols\" : [ \"apple\", \"orange\", \"banana\" ] }, \"optional\" : true }, \n" +
      "{ \"name\" : \"fixed\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"fixedType\", \"size\" : 4 }, \"optional\" : true }, \n" +
      "{ \"name\" : \"map\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" }, \"optional\" : true }, \n" +
      "{ \"name\" : \"record\", \"type\" : { \"type\" : \"record\", \"name\" : \"recordType\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] }, \"optional\" : true }, \n" +
      "{ \"name\" : \"union\", \"type\" : [ \"int\", \"recordType\", \"enumType\", \"fixedType\" ], \"optional\" : true }, \n" +
      "{ \"name\" : \"unionWithNull\", \"type\" : [ \"null\", \"enumType\", \"fixedType\" ], \"optional\" : true } \n" +
      "] } } ] }";

    String key = "bar";
    DataSchema schema = dataSchemaFromString(schemaText);

    Object input[][][] =
    {
      {
        {
          new ValidationOptions(RequiredMode.IGNORE),
          new ValidationOptions(RequiredMode.MUST_BE_PRESENT),
          new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT),
          new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT)
        },
        // int
        {
          new DataMap(asMap("int", 1L)),
          new DataMap(asMap("int", 1))
        },
        {
          new DataMap(asMap("int", 1.0f)),
          new DataMap(asMap("int", 1))
        },
        {
          new DataMap(asMap("int", 1.0)),
          new DataMap(asMap("int", 1))
        },
        // long
        {
          new DataMap(asMap("long", 1)),
          new DataMap(asMap("long", 1L))
        },
        {
          new DataMap(asMap("long", 1.0f)),
          new DataMap(asMap("long", 1L))
        },
        {
          new DataMap(asMap("long", 1.0)),
          new DataMap(asMap("long", 1L))
        },
        // float
        {
          new DataMap(asMap("float", 1)),
          new DataMap(asMap("float", 1.0f))
        },
        {
          new DataMap(asMap("float", 1L)),
          new DataMap(asMap("float", 1.0f))
        },
        {
          new DataMap(asMap("float", 1.0)),
          new DataMap(asMap("float", 1.0f))
        },
        // double
        {
          new DataMap(asMap("double", 1)),
          new DataMap(asMap("double", 1.0))
        },
        {
          new DataMap(asMap("double", 1L)),
          new DataMap(asMap("double", 1.0))
        },
        {
          new DataMap(asMap("double", 1.0f)),
          new DataMap(asMap("double", 1.0))
        },
        // array of int's
        {
          new DataMap(asMap("array", new DataList(asList(1, 2, 3, 1.0, 2.0, 3.0, 1.0f, 2.0f, 3.0f, 1.0, 2.0, 3.0)))),
          new DataMap(asMap("array", new DataList(asList(1, 2, 3, 1,   2,   3,   1,    2,    3,    1,   2,   3  ))))
        },
        // map of int's
        {
          new DataMap(asMap("map", new DataMap(asMap("int1", 1, "long", 1L, "float", 1.0f, "double", 1.0)))),
          new DataMap(asMap("map", new DataMap(asMap("int1", 1, "long", 1, "float", 1, "double", 1))))
        },
        // record with int fields
        {
          new DataMap(asMap("record", new DataMap(asMap("int", 1L)))),
          new DataMap(asMap("record", new DataMap(asMap("int", 1))))
        },
        {
          new DataMap(asMap("record", new DataMap(asMap("int", 1.0f)))),
          new DataMap(asMap("record", new DataMap(asMap("int", 1))))
        },
        {
          new DataMap(asMap("record", new DataMap(asMap("int", 1.0)))),
          new DataMap(asMap("record", new DataMap(asMap("int", 1))))
        },
        // union with int
        {
          new DataMap(asMap("union", new DataMap(asMap("int", 1L)))),
          new DataMap(asMap("union", new DataMap(asMap("int", 1))))
        },
        {
          new DataMap(asMap("union", new DataMap(asMap("int", 1.0f)))),
          new DataMap(asMap("union", new DataMap(asMap("int", 1))))
        },
        {
          new DataMap(asMap("union", new DataMap(asMap("int", 1.0)))),
          new DataMap(asMap("union", new DataMap(asMap("int", 1))))
        },
        // union with record containing int
        {
          new DataMap(asMap("union", new DataMap(asMap("recordType", new DataMap(asMap("int", 1L)))))),
          new DataMap(asMap("union", new DataMap(asMap("recordType", new DataMap(asMap("int", 1))))))
        },
        {
          new DataMap(asMap("union", new DataMap(asMap("recordType", new DataMap(asMap("int", 1.0f)))))),
          new DataMap(asMap("union", new DataMap(asMap("recordType", new DataMap(asMap("int", 1))))))
        },
        {
          new DataMap(asMap("union", new DataMap(asMap("recordType", new DataMap(asMap("int", 1.0)))))),
          new DataMap(asMap("union", new DataMap(asMap("recordType", new DataMap(asMap("int", 1))))))
        }
      }
    };

    testValidationWithNormalCoercion(schema, key, input);
  }

  @Test
  public void testValidationWithFixupAbsentWithDefault() throws IOException, CloneNotSupportedException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : \n" +
      "[ { \"name\" : \"bar\", \"type\" : { \"name\" : \"barType\", \"type\" : \"record\", \"fields\" : [ \n" +
      "{ \"name\" : \"boolean\", \"type\" : \"boolean\", \"default\" : true }, \n" +
      "{ \"name\" : \"int\", \"type\" : \"int\", \"default\" : 1 }, \n" +
      "{ \"name\" : \"long\", \"type\" : \"long\", \"default\" : 2 }, \n" +
      "{ \"name\" : \"float\", \"type\" : \"float\", \"default\" : 3.0 }, \n" +
      "{ \"name\" : \"double\", \"type\" : \"double\", \"default\" : 4.0 }, \n" +
      "{ \"name\" : \"string\", \"type\" : \"string\", \"default\" : \"cow\" }, \n" +
      "{ \"name\" : \"bytes\", \"type\" : \"bytes\", \"default\" : \"dog\" }, \n" +
      "{ \"name\" : \"array\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" }, \"default\" : [ -1, -2, -3 ] }, \n" +
      "{ \"name\" : \"enum\", \"type\" : { \"type\" : \"enum\", \"name\" : \"enumType\", \"symbols\" : [ \"apple\", \"orange\", \"banana\" ] }, \"default\" : \"apple\" }, \n" +
      "{ \"name\" : \"fixed\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"fixedType\", \"size\" : 4 }, \"default\" : \"1234\" }, \n" +
      "{ \"name\" : \"map\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" }, \"default\" : { \"1\" : 1, \"2\" : 2 } }, \n" +
      "{ \"name\" : \"record\", \"type\" : { \"type\" : \"record\", \"name\" : \"recordType\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] }, \"default\" : { \"int\" : 1 } }, \n" +
      "{ \"name\" : \"union\", \"type\" : [ \"int\", \"recordType\", \"enumType\", \"fixedType\" ], \"default\" : { \"enumType\" : \"orange\" } }, \n" +
      "{ \"name\" : \"unionWithNull\", \"type\" : [ \"null\", \"enumType\", \"fixedType\" ], \"default\" : null }, \n" +
      "{ \"name\" : \"optionalInt\", \"type\" : \"int\", \"optional\" : true }, \n" +
      "{ \"name\" : \"optionalDefaultInt\", \"type\" : \"int\", \"optional\" : true, \"default\" : 42 } \n" +
      "] } } ] }";

    String key = "bar";
    DataSchema schema = dataSchemaFromString(schemaText);
    assertTrue(schema != null);

    Object input[][][] =
    {
      {
        {
          new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT)
        },
        {
          new DataMap(),
          new DataMap(asMap(
            "boolean", true,
            "int", 1,
            "long", 2L,
            "float", 3.0f,
            "double", 4.0,
            "string", "cow",
            "bytes", ByteString.copyAvroString("dog", false),
            "array", new DataList(asList(-1, -2, -3)),
            "enum", "apple",
            "fixed", ByteString.copyAvroString("1234", false),
            "map", new DataMap(asMap("1", 1, "2", 2)),
            "record", new DataMap(asMap("int", 1)),
            "union", new DataMap(asMap("enumType", "orange")),
            "unionWithNull", Data.NULL
          ))
        }
      }
    };

    testValidationWithNormalCoercion(schema, key, input);
  }

  private void testValidationWithNormalCoercion(DataSchema schema, String key, Object[][][] input) throws CloneNotSupportedException
  {
    for (Object[][] row : input)
    {
      Object[] options = row[0];
      Object[][] pairs = new Object[row.length - 1][];
      System.arraycopy(row, 1, pairs, 0, pairs.length);
      for (Object option : options)
      {

        ValidationOptions validationOptions = (ValidationOptions) option;
        validationOptions.setCoercionMode(CoercionMode.NORMAL);

        // Data object is read-only.
        for (Object[] pair : pairs)
        {
          DataMap foo = new DataMap();
          foo.put(key, pair[0]);
          foo.makeReadOnly();
          assertTrue(foo.isReadOnly());
          assertTrue(((DataComplex) pair[0]).isReadOnly());
          assertSame(foo.get(key), pair[0]);
          ValidationResult result = ValidateDataAgainstSchema.validate(foo, schema, validationOptions);
     System.out.println(result);
          assertFalse(result.isValid());
          assertTrue(result.hasFix());
          assertTrue(result.hasFixupReadOnlyError());
          assertTrue(foo.isReadOnly());
          assertTrue(((DataComplex) pair[0]).isReadOnly());
          DataMap fooFixed = (DataMap) result.getFixed();
          Object barFixed = fooFixed.get(key);
          assertEquals(pair[0], barFixed); // not changed
          assertSame(fooFixed, foo);
          assertTrue(fooFixed.isReadOnly());
          assertTrue(((DataComplex) barFixed).isReadOnly());
          assertSame(barFixed, pair[0]);
        }

        // Data object is read-write
        for (Object[] pair : pairs)
        {
          DataMap foo = new DataMap();
          DataMap pair0 = ((DataMap) pair[0]).copy(); // get read-write clone
          assertFalse(pair0.isReadOnly());
          foo.put(key, pair0);
          ValidationResult result = validate(foo, schema, validationOptions);
          assertTrue(result.isValid());
          DataMap fooFixed = (DataMap) result.getFixed();
          Object barFixed = fooFixed.get(key);
          assertTrue(result.isValid());
          assertTrue(result.hasFix());
          assertFalse(result.hasFixupReadOnlyError());
          assertFalse(foo.isReadOnly());
          assertFalse(pair0.isReadOnly());
          assertEquals(pair[1], barFixed);
          assertSame(result.getFixed(), foo); // modify in place
          assertFalse(((DataComplex) barFixed).isReadOnly());
          assertSame(barFixed, pair0); // modify in place
        }
      }
    }
  }

  @Test
  public void testJsonValidation() throws IOException
  {
    Object[][] input =
      {
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"intField\", \"type\" : \"int\" } ] }",
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.NORMAL),
            "{ \"intField\" : " + Integer.MAX_VALUE + " }"
          },
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
            "{ \"intField\" : " + Integer.MAX_VALUE + " }"
          },
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.NORMAL),
            "{ \"intField\" : " + Integer.MIN_VALUE + " }"
          },
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
            "{ \"intField\" : " + Integer.MIN_VALUE + " }"
          },
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
            "{ \"intField\" : " + ((long) Integer.MAX_VALUE + 1) + " }",
            "ERROR :: /intField :: 2147483648 is not backed by a Integer"
          },
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"longField\", \"type\" : \"long\" } ] }",
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.NORMAL),
            "{ \"longField\" : " + Long.MAX_VALUE + " }"
          },
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
            "{ \"longField\" : " + Long.MAX_VALUE + " }"
          },
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.NORMAL),
            "{ \"longField\" : " + Long.MIN_VALUE + " }"
          },
          new Object[] {
            new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.OFF),
            "{ \"longField\" : " + Long.MIN_VALUE + " }"
          },
        }
      };

    for (Object[] row : input)
    {
      String schemaText = (String) row[0];
      for (int i = 1; i < row.length; i++)
      {
        Object[] test = (Object[]) row[i];
        ValidationOptions options = (ValidationOptions) test[0];
        String dataText = (String) test[1];
        String expectedResult = test.length > 2 ? (String) test[2] : null;
        DataSchema schema = dataSchemaFromString(schemaText);
        DataMap dataMap = dataMapFromString(dataText);
        ValidationResult result = ValidateDataAgainstSchema.validate(dataMap, schema, options);
        if (expectedResult == null)
        {
          assertTrue(result.isValid());
          assertEquals(result.getMessages().size(), 0);
        }
        else
        {
          assertTrue(result.toString().contains(expectedResult));
        }
      }
    }
  }

  @Test
  public void testNonRootStartDataElement() throws IOException
  {
    String schemaText =
      "{\n" +
      "  \"name\" : \"Foo\",\n" +
      "  \"type\" : \"record\",\n" +
      "  \"fields\" : [\n" +
      "    { \"name\" : \"intField\", \"type\" : \"int\", \"optional\" : true },\n" +
      "    { \"name\" : \"stringField\", \"type\" : \"string\", \"optional\" : true },\n" +
      "    { \"name\" : \"arrayField\", \"type\" : { \"type\" : \"array\", \"items\" : \"Foo\" }, \"optional\" : true },\n" +
      "    { \"name\" : \"mapField\", \"type\" : { \"type\" : \"map\", \"values\" : \"Foo\" }, \"optional\" : true },\n" +
      "    { \"name\" : \"unionField\", \"type\" : [ \"int\", \"string\", \"Foo\" ], \"optional\" : true },\n" +
      "    { \"name\" : \"fooField\", \"type\" : \"Foo\", \"optional\" : true }\n" +
      "  ]\n" +
      "}\n";

    String[] empty = {};

    Object[][] input =
      {
        {
          "{ \"intField\" : \"bad\", \"fooField\" : { \"intField\" : 32 } }",
          "/fooField",
          empty,
          new String[] { "ERROR" }
        },
        {
          "{ \"intField\" : 32, \"fooField\" : { \"intField\" : \"bad\" } }",
          "/fooField",
          new String[] { "ERROR", "/fooField/intField" },
          empty
        },
        {
          "{\n" +
          "  \"stringField\" : 32,\n" +
          "  \"arrayField\" : [ { \"intField\" : \"bad0\" }, { \"intField\" : \"bad1\" } ]\n" +
          "}\n",
          "/arrayField/0",
          new String[] { "ERROR", "/arrayField/0/intField" },
          new String[] { "/stringField", "/arrayField/1/intField" }
        },
        {
          "{\n" +
          "  \"stringField\" : 32,\n" +
          "  \"mapField\" : { \"m0\" : { \"intField\" : \"bad0\" }, \"m1\" : { \"intField\" : \"bad1\" } }\n" +
          "}\n",
          "/mapField/m1",
          new String[] { "ERROR", "/mapField/m1/intField" },
          new String[] { "/stringField", "/mapField/m0/intField" }
        },
        {
          "{\n" +
          "  \"stringField\" : 32,\n" +
          "  \"arrayField\" : [\n" +
          "    { \"unionField\" : { \"Foo\" : { \"intField\" : \"bad0\" } } },\n" +
          "    { \"unionField\" : { \"int\" : \"bad1\" } }\n" +
          "  ]\n" +
          "}\n",
          "/arrayField/0/unionField",
          new String[] { "ERROR", "/arrayField/0/unionField/Foo/intField" },
          new String[] { "/stringField", "/arrayField/1/unionField/int" }
        },
        {
          "{\n" +
          "  \"stringField\" : 32,\n" +
          "  \"fooField\" : {\n" +
          "    \"stringField\" : 45,\n" +
          "    \"fooField\" : { \"intField\" : \"bad1\" } }\n" +
          "  }\n" +
          "}\n",
          "/fooField/fooField",
          new String[] { "ERROR", "/fooField/fooField/intField" },
          new String[] { "/stringField", "/fooField/stringField" }
        },
      };

    DataSchema schema = dataSchemaFromString(schemaText);
    for (Object[] row : input)
    {
      String dataString = (String) row[0];
      String startPath = (String) row[1];
      String[] expectedStrings = (String[]) row[2];
      String[] notExpectedStrings = (String[]) row[3];
      DataMap map = dataMapFromString(dataString);
      DataElement startElement = DataElementUtil.element(map, schema, startPath);
      assertNotSame(startElement, null);
      ValidationResult result = validate(startElement, new ValidationOptions());
      String message = result.getMessages().toString();
      for (String expected : expectedStrings)
      {
        assertTrue(message.contains(expected), message + " does not contain " + expected);
      }
      for (String notExpected : notExpectedStrings)
      {
        assertFalse(message.contains(notExpected), message + " contains " + notExpected);
      }
    }
  }

  @Test
  public void testUnrecognizedFieldValidation() throws IOException
  {
    String schemaText =
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
            "[ { \"name\" : \"bar\", \"type\" : \"string\" } ] }";

    Object goodObjects[] =
        {
            "a valid string"
        };

    Object badObjects[] =
        {
        };

    // There is no coercion for this type.
    // Test with all coercion modes, result should be the same for all cases.
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, disallowUnrecognizedFieldOption());
    testCoercionValidation(schemaText, "bar", goodObjects, badObjects, trimUnrecognizedFieldOption());

    Object allowedForUnrecognizedField[] =
        {
        };

    Object disallowedForUnrecognizedField[] =
        {
            "a string",
            new Boolean(false),
            new Integer(1),
            new Long(1),
            new Float(1),
            new Double(1),
            ByteString.copyAvroString("bytes", false),
            new DataMap(),
            new DataList()
        };

    testCoercionValidation(schemaText, "unrecognized", allowedForUnrecognizedField, disallowedForUnrecognizedField, disallowUnrecognizedFieldOption());
  }

  @Test
  public void testUnrecognizedFieldTrimming() throws IOException
  {
    ValidationOptions options = new ValidationOptions(
        RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT,
        CoercionMode.NORMAL,
        UnrecognizedFieldMode.TRIM);

    String schemaText =
        "{\n" +
            "  \"name\" : \"Foo\",\n" +
            "  \"type\" : \"record\",\n" +
            "  \"fields\" : [\n" +
            "    { \"name\" : \"primitive\", \"type\" : \"int\", \"optional\" : true },\n" +
            "    { \"name\" : \"arrayField\", \"type\" : { \"type\" : \"array\", \"items\" : \"Foo\" }, \"optional\" : true },\n" +
            "    { \"name\" : \"mapField\", \"type\" : { \"type\" : \"map\", \"values\" : \"Foo\" }, \"optional\" : true },\n" +
            "    { \"name\" : \"unionField\", \"type\" : [ \"int\", \"string\", \"Foo\" ], \"optional\" : true },\n" +
            "    { \"name\" : \"recordField\", \"type\" : \"Foo\", \"optional\" : true }\n" +
            "  ]\n" +
            "}\n";

    DataSchema schema = dataSchemaFromString(schemaText);

    String dataString =
        "{\n" +
        "  \"primitive\" : 1,\n" +
        "  \"unrecognizedPrimitive\": -1,\n" +
        "  \"arrayField\" : [ { \"primitive\": 2, \"unrecognizedInArray\": -2 } ],\n" +
        "  \"mapField\" : { \"key\": { \"primitive\": 3, \"unrecognizedInMap\": -3 } },\n" +
        "  \"unionField\" : { \"Foo\": { \"primitive\": 4, \"unrecognizedInMap\": -4 } },\n" +
        "  \"recordField\" : {\n" +
        "    \"primitive\" : 5,\n" +
        "    \"unrecognizedPrimitive\": -5\n" +
        "  },\n" +
        "  \"unrecognizedMap\": { \"key\": -100},\n" +
        "  \"unrecognizedArray\": [ -101 ]\n" +
        "}";

    String trimmedDataString =
        "{\n" +
        "  \"primitive\" : 1,\n" +
        "  \"arrayField\" : [ { \"primitive\": 2 } ],\n" +
        "  \"mapField\" : { \"key\": { \"primitive\": 3 } },\n" +
        "  \"unionField\" : { \"Foo\": { \"primitive\": 4 } },\n" +
        "  \"recordField\" : {\n" +
        "    \"primitive\" : 5\n" +
        "  }\n" +
        "}";

    // mutable
    DataMap toValidate = dataMapFromString(dataString);
    ValidationResult result = validate(toValidate, schema, options);
    assertTrue(result.isValid());
    assertEquals(result.getFixed(), dataMapFromString(trimmedDataString));
    assertSame(toValidate, result.getFixed());

    // read-only
    DataMap readOnlyToValidate = dataMapFromString(dataString);
    readOnlyToValidate.makeReadOnly();
    ValidationResult readOnlyResult = validate(readOnlyToValidate, schema, options);
    assertTrue(readOnlyResult.hasFixupReadOnlyError());
    String message = readOnlyResult.getMessages().toString();
    assertTrue(readOnlyResult.getMessages().size() == 8);
    String[] expectedStrings = new String[] {
        "/unrecognizedMap/key",
        "/unrecognizedMap",
        "/unrecognizedArray",
        "/recordField/unrecognizedPrimitive",
        "/unionField/Foo/unrecognizedInMap",
        "/unrecognizedPrimitive",
        "/arrayField/0/unrecognizedInArray",
        "/mapField/key/unrecognizedInMap"
    };
    for (String expected : expectedStrings)
    {
      assertTrue(message.contains(expected), message + " does not contain " + expected);
    }
  }
}
