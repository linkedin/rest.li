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
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.DataSchemaParser;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.DataTemplateUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestAnyRecordValidator
{
  private static final String DATA_SCHEMA_JSON =
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"AnyRecord\",\n" +
    "  \"namespace\" : \"com.linkedin.data.schema.validator\",\n" +
    "  \"fields\" : [],\n" +
    "  \"validate\" : {\n" +
    "    \"com.linkedin.data.schema.validator.AnyRecordValidator\" : { }\n" +
    "  }\n" +
    "}" +
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"AnyRecordClient\",\n" +
    "  \"namespace\" : \"com.linkedin.data.schema.validator\",\n" +
    "  \"fields\" : [\n" +
    "    {\n" +
    "      \"name\" : \"required\",\n" +
    "      \"type\" : \"AnyRecord\"\n" +
    "    }\n" +
    "  ]\n" +
    "}";

  private static final RecordDataSchema ANYRECORD_SCHEMA;
  private static final RecordDataSchema ANYRECORDCLIENT_SCHEMA;

  static
  {
    try
    {
      DataSchemaParser parser = TestUtil.schemaParserFromString(DATA_SCHEMA_JSON);
      List<DataSchema> schemas = parser.topLevelDataSchemas();
      ANYRECORD_SCHEMA = (RecordDataSchema) schemas.get(0);
      ANYRECORDCLIENT_SCHEMA = (RecordDataSchema) schemas.get(1);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
  }

  public static final String ANYRECORD_SCHEMA_FULLNAME = ANYRECORD_SCHEMA.getFullName();

  private static final Map<String, NamedDataSchema> RESOLVER_MAP = TestUtil.asMap(
    "com.linkedin.Foo",
    DataTemplateUtil.parseSchema(
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"com.linkedin.Foo\",\n" +
      "  \"fields\" : []\n" +
      "}"
    ),
    "com.linkedin.Bar",
    DataTemplateUtil.parseSchema(
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"com.linkedin.Bar\",\n" +
      "  \"fields\" : [\n" +
      "    { \"name\" : \"b\", \"type\" : \"string\" }\n" +
      "  ]\n" +
      "}"
    ),
    "com.linkedin.Cat",
    DataTemplateUtil.parseSchema(
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"com.linkedin.Cat\",\n" +
      "  \"fields\" : [\n" +
      "    { \"name\" : \"c\", \"type\" : \"double\" }\n" +
      "  ]\n" +
      "}"
    ),
    ANYRECORD_SCHEMA.getFullName(),
    ANYRECORD_SCHEMA
  );

  private static final String RESOLVER_ERROR_MESSAGE = "Unable to find schema";

  private static enum ResultFlag
  {
    VALID,
    NOT_VALID,
    HAS_FIX,
    NOT_HAS_FIX,
    HAS_FIXUP_READONLY_ERROR,
    NOT_HAS_FIXUP_READONLY_ERROR
  };


  private static void checkValidationResultFlag(ValidationResult result, ResultFlag flag)
  {
    switch (flag)
    {
      case VALID:
        assertTrue(result.isValid());
        break;
      case NOT_VALID:
        assertFalse(result.isValid());
        break;
      case HAS_FIX:
        assertTrue(result.hasFix());
        break;
      case NOT_HAS_FIX:
        assertFalse(result.hasFix());
        break;
      case HAS_FIXUP_READONLY_ERROR:
        assertTrue(result.hasFixupReadOnlyError());
        break;
      case NOT_HAS_FIXUP_READONLY_ERROR:
        assertFalse(result.hasFixupReadOnlyError());
        break;
    }
  }

  private static void checkValidationResult(ValidationResult result, Object[] row, int index, boolean debug)
  {
    String resultString = result.toString();
    if (debug) TestUtil.out.println(resultString);

    int i = index;
    while (row[i] instanceof ResultFlag)
    {
      ResultFlag flag = (ResultFlag) row[i++];
      checkValidationResultFlag(result, flag);
    }

    if (i < row.length)
    {
      String[] expectedMatches = (String []) row[i++];
      for (String expected : expectedMatches)
      {
        assertTrue(resultString.contains(expected), resultString + " does not contain \"" + expected + "\"");
      }
    }
  }

  private DataSchemaResolver _resolver = new DefaultDataSchemaResolver()
  {
    @Override
    public NamedDataSchema findDataSchema(String name, StringBuilder sb)
    {
      NamedDataSchema schema = RESOLVER_MAP.get(name);
      if (schema == null)
        sb.append(RESOLVER_ERROR_MESSAGE);
      return schema;
    }
  };

  @Test
  public void testAnyRecordValidation() throws IOException
  {


    Object[][] inputs =
      {
        {
          // DataMap is empty
          ANYRECORD_SCHEMA,
          "{" +
          "}",
          new AnyRecordValidator.Parameter(false, null),
          ResultFlag.NOT_VALID,
          new String[] { "expects data to be a DataMap with one entry" }
        },
        {
          // DataMap has more than one entry
          ANYRECORD_SCHEMA,
          "{" +
          "  \"a\" : 1," +
          "  \"b\" : 2" +
          "}",
          new AnyRecordValidator.Parameter(false, null),
          ResultFlag.NOT_VALID,
          new String[] { "expects data to be a DataMap with one entry" }
        },
        {
          // no resolver, any type schema need not be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"abc\" : { }\n" +
          "}",
          new AnyRecordValidator.Parameter(false, null),
          ResultFlag.VALID,
          new String[] { "INFO", "cannot obtain schema for \"abc\", no resolver" },
        },
        {
          // no resolver, any type schema must be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"abc\" : { }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, null),
          ResultFlag.NOT_VALID,
          new String[] { "ERROR", "cannot obtain schema for \"abc\", no resolver" },
        },
        {
          // no resolver but schema exists, any type schema must be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"com.linkedin.Foo\" : { }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, null),
          ResultFlag.NOT_VALID,
          new String[] { "ERROR", "cannot obtain schema for \"com.linkedin.Foo\", no resolver" },
        },
        {
          // schema exists, any type schema must be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"com.linkedin.Foo\" : { }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, _resolver),
          ResultFlag.VALID,
          new String[] { },
        },
        {
          // resolver cannot resolve name to schema, any type schema must be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"com.linkedin.DoesNotExist\" : { }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, _resolver),
          ResultFlag.NOT_VALID,
          new String[] { "ERROR", "cannot obtain schema for \"com.linkedin.DoesNotExist\" (" + RESOLVER_ERROR_MESSAGE + ")" },
        },
        {
          // resolver cannot resolve name to schema, any type schema need not be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"com.linkedin.DoesNotExist\" : { }\n" +
          "}",
          new AnyRecordValidator.Parameter(false, _resolver),
          ResultFlag.VALID,
          new String[] { "INFO", "cannot obtain schema for \"com.linkedin.DoesNotExist\" (" + RESOLVER_ERROR_MESSAGE + ")" },
        },
        {
          // type schema is valid and any data is valid, any type schema must be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"com.linkedin.Bar\" : { \"b\" : \"hello\" }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, _resolver),
          ResultFlag.VALID,
          new String[] { },
        },
        {
          // type schema is valid and any data is not valid, any type schema must be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"com.linkedin.Bar\" : { \"b\" : 1 }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, _resolver),
          ResultFlag.NOT_VALID,
          new String[] { "ERROR", "1 cannot be coerced to String" },
        },
        {
          // type schema is valid and any data is not valid, any type schema must be valid
          ANYRECORD_SCHEMA,
          "{" +
          "  \"com.linkedin.Bar\" : { \"b\" : 1 }\n" +
          "}",
          new AnyRecordValidator.Parameter(false, _resolver),
          ResultFlag.NOT_VALID,
          new String[] { "ERROR", "1 cannot be coerced to String" },
        },


        //
        // AnyRecordClient tests
        //

        {
          // AnyRecord is field, must sure that the field is being validated
          ANYRECORDCLIENT_SCHEMA,
          "{" +
          "  \"required\" : {\n" +
          "    \"com.linkedin.Bar\" : { \"b\" : 1 }\n" +
          "  }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, _resolver),
          ResultFlag.NOT_VALID,
          new String[] { "ERROR",  "/required/com.linkedin.Bar/b", "1 cannot be coerced to String" },
        },

        {
          // AnyRecord within AnyRecord, make sure nested AnyRecord is validated
          ANYRECORDCLIENT_SCHEMA,
          "{" +
          "  \"required\" : {\n" +
          "    \"com.linkedin.data.schema.validator.AnyRecord\" : {\n" +
          "      \"com.linkedin.Bar\" : { \"b\" : 1 }\n" +
          "    }\n" +
          "  }\n" +
          "}",
          new AnyRecordValidator.Parameter(true, _resolver),
          ResultFlag.NOT_VALID,
          new String[] { "ERROR",  "/required/com.linkedin.data.schema.validator.AnyRecord/com.linkedin.Bar/b", "1 cannot be coerced to String" },
        }
      };

    final boolean debug = false;

    ValidationOptions options = new ValidationOptions();

    for (Object[] row : inputs)
    {
      int i = 0;
      DataSchema schema = (DataSchema) row[i++];
      Object object = TestUtil.dataMapFromString((String) row[i++]);
      AnyRecordValidator.Parameter anyRecordValidatorParameter = (AnyRecordValidator.Parameter) row[i++];

      AnyRecordValidator.setParameter(options, anyRecordValidatorParameter);
      DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(schema);
      if (debug) TestUtil.out.println(validator);
      ValidationResult result = ValidateDataAgainstSchema.validate(object, schema, options, validator);
      checkValidationResult(result, row, i, debug);
    }
  }

  @Test
  public void testValidationResultFlags() throws IOException
  {

    Object[][] inputs = {
      {
        ANYRECORDCLIENT_SCHEMA,
        "{" +
        "  \"required\" : {\n" +
        "    \"com.linkedin.data.schema.validator.AnyRecord\" : {\n" +
        "      \"com.linkedin.Cat\" : { \"c\" : 1 }\n" +
        "    }\n" +
        "  }\n" +
        "}",
        false,
        new ValidationOptions(RequiredMode.IGNORE, CoercionMode.NORMAL),
        ResultFlag.VALID,
        ResultFlag.HAS_FIX,
        ResultFlag.NOT_HAS_FIXUP_READONLY_ERROR,
        new String[] { },
      },
      {
        ANYRECORDCLIENT_SCHEMA,
        "{" +
        "  \"required\" : {\n" +
        "    \"com.linkedin.data.schema.validator.AnyRecord\" : {\n" +
        "      \"com.linkedin.Cat\" : { \"c\" : 1 }\n" +
        "    }\n" +
        "  }\n" +
        "}",
        true,
        new ValidationOptions(RequiredMode.IGNORE, CoercionMode.NORMAL),
        ResultFlag.NOT_VALID,
        ResultFlag.HAS_FIX,
        ResultFlag.HAS_FIXUP_READONLY_ERROR,
        new String[] { "ERROR",  "/required/com.linkedin.data.schema.validator.AnyRecord/com.linkedin.Cat/c", "cannot be fixed because DataMap backing com.linkedin.Cat type is read-only" },
      }
    };

    final boolean debug = false;

    final AnyRecordValidator.Parameter anyRecordValidatorParameter = new AnyRecordValidator.Parameter(true, _resolver);

    for (Object[] row : inputs)
    {
      int i = 0;
      DataSchema schema = (DataSchema) row[i++];
      DataMap object = TestUtil.dataMapFromString((String) row[i++]);
      boolean makeReadOnly = (Boolean) row[i++];
      ValidationOptions options = (ValidationOptions) row[i++];

      if (makeReadOnly)
      {
        object.makeReadOnly();
      }
      AnyRecordValidator.setParameter(options, anyRecordValidatorParameter);
      DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(schema);
      if (debug) TestUtil.out.println(validator);
      ValidationResult result = ValidateDataAgainstSchema.validate(object, schema, options, validator);
      checkValidationResult(result, row, i, debug);
    }
  }
}
