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

package com.linkedin.data.schema.compatibility;


import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.compatibility.CompatibilityChecker;
import com.linkedin.data.schema.compatibility.CompatibilityOptions;
import com.linkedin.data.schema.compatibility.CompatibilityResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestCompatibilityChecker
{
  private boolean _debug = false;

  private static final CompatibilityOptions _dataCompatibilityNoCheckNames = new CompatibilityOptions()
    .setMode(CompatibilityOptions.Mode.DATA)
    .setCheckNames(false);

  private static final CompatibilityOptions _dataCompatibilityCheckNames = new CompatibilityOptions()
    .setMode(CompatibilityOptions.Mode.DATA)
    .setCheckNames(true);

  private static final CompatibilityOptions _schemaCompatibilityNoCheckNames = new CompatibilityOptions()
    .setMode(CompatibilityOptions.Mode.SCHEMA)
    .setCheckNames(false);

  private static final CompatibilityOptions _schemaCompatibilityCheckNames = new CompatibilityOptions()
    .setMode(CompatibilityOptions.Mode.SCHEMA)
    .setCheckNames(true);

  private static final CompatibilityOptions _allowPromotionsData = new CompatibilityOptions().
    setMode(CompatibilityOptions.Mode.DATA).
    setAllowPromotions(true);

  private static final CompatibilityOptions _allowPromotionsSchema = new CompatibilityOptions().
    setMode(CompatibilityOptions.Mode.SCHEMA).
    setAllowPromotions(true);

  private static final List<CompatibilityOptions> _allowPromotions = list(
    _allowPromotionsData,
    _allowPromotionsSchema
  );

  private static final List<CompatibilityOptions> _dataOnly = list(
      _dataCompatibilityCheckNames,
      _dataCompatibilityNoCheckNames
    );
  private static final List<CompatibilityOptions> _schemaOnly = list(_schemaCompatibilityCheckNames,
                                                                     _schemaCompatibilityNoCheckNames);
  private static final List<CompatibilityOptions> _checkNamesOnly = list(
      _dataCompatibilityCheckNames,
      _schemaCompatibilityCheckNames
    );
  private static final List<CompatibilityOptions> _noCheckNamesOnly = list(
      _dataCompatibilityNoCheckNames,
      _schemaCompatibilityNoCheckNames
    );
  private static final List<CompatibilityOptions> _dataAndSchema = list(
      _dataCompatibilityCheckNames,
      _dataCompatibilityNoCheckNames,
      _schemaCompatibilityCheckNames,
      _schemaCompatibilityNoCheckNames
    );

  private static final String _mapSchemaText = "{ \"type\" : \"map\", \"values\" : \"int\" }";
  private static final String _arraySchemaText = "{ \"type\" : \"array\", \"items\" : \"int\" }";
  private static final String _enumSchemaText = "{ \"name\" : \"Fruits\", \"type\" : \"enum\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }";
  private static final String _recordSchemaText = "{ \"name\" : \"Foo\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"field1\", \"type\" : \"int\" } ] }";
  private static final String _fixedSchemaText = "{ \"name\" : \"Md5\", \"type\" : \"fixed\", \"size\" : 16 }";
  private static final String _unionSchemaText = "[ \"int\", \"string\", { \"type\" : \"map\", \"values\" : \"string\" } ]";
  private static final String _intSchemaText = "\"int\"";
  private static final String _longSchemaText = "\"long\"";
  private static final String _floatSchemaText = "\"float\"";
  private static final String _doubleSchemaText = "\"double\"";
  private static final String _booleanSchemaText = "\"boolean\"";
  private static final String _stringSchemaText = "\"string\"";
  private static final String _bytesSchemaText = "\"bytes\"";
  private static final String _typerefSchemaText = "{ \"name\" : \"ColorRef\", \"type\" : \"typeref\", \"ref\" : { \"name\" : \"Color\", \"type\" : \"enum\", \"symbols\" : [ \"RED\", \"BLUE\", \"GREEN\" ] } }";

  private static final List<String> _numericSchemaText = list(
    _intSchemaText,
    _longSchemaText,
    _floatSchemaText,
    _doubleSchemaText
  );

  private static final List<String> _nonNumericPrimitiveText = list(
    _booleanSchemaText,
    _stringSchemaText,
    _bytesSchemaText
  );

  private static final List<String> _primitiveSchemaText = union(
    _numericSchemaText,
    _nonNumericPrimitiveText
  );

  private static final List<String> _complexSchemaText = list(
    _mapSchemaText,
    _arraySchemaText,
    _enumSchemaText,
    _recordSchemaText,
    _fixedSchemaText,
    _unionSchemaText
  );

  private static final List<String> _nonTyperefSchemaText = union(_complexSchemaText, _primitiveSchemaText);
  private static final List<String> _allSchemaText = add(_nonTyperefSchemaText, _typerefSchemaText);

  // warning says "Possible heap pollution from parameterized vararg type T"
  @SuppressWarnings("unchecked")
  private static final <T> List<T> list(T...args)
  {
    return new ArrayList<T>(Arrays.asList(args));
  }

  private static final <T> List<T> union(List<T> arg1, List<T> arg2)
  {
    ArrayList<T> result = new ArrayList<T>(arg1);
    result.addAll(arg2);
    return result;
  }

  // warning says "Possible heap pollution from parameterized vararg type T"
  @SuppressWarnings("unchecked")
  private static final <T> List<T> add(List<T> arg, T... args)
  {
    ArrayList<T> result = new ArrayList<T>(arg);
    result.addAll(Arrays.asList(args));
    return result;
  }

  // warning says "Possible heap pollution from parameterized vararg type T"
  @SuppressWarnings("unchecked")
  private static final <T> List<T> subtract(List<T> arg, T... args)
  {
    ArrayList<T> result = new ArrayList<T>(arg);
    result.removeAll(Arrays.asList(args));
    return result;
  }

  @Test
  public void testCycles() throws IOException
  {
    Object[][] inputs =
      {
        {
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"a.b.Record\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"a.b.Record\", \"optional\" : true } ] }",
          _dataAndSchema,
          false
        },
        {
          "{ \"name\" : \"a.b.Record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"a.b.Record1\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"a.b.Record2\", \"optional\" : true } ] }",
          _noCheckNamesOnly,
          false
        },
        {
          "{ \"name\" : \"a.b.Record1\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"a.b.Record1\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record2\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"a.b.Record2\", \"optional\" : true } ] }",
          _checkNamesOnly,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record2 :: name changed from a.b.Record1 to a.b.Record2"
        },
      };

    testCompatibility(inputs);
  }

  @Test
  public void testRecord() throws IOException
  {
    Object[][] inputs =
      {
        {
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ ] }",
          _dataAndSchema,
          false
        },
        {
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : ##TYPE } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : ##TYPE } ] }",
          _allSchemaText,
          _dataAndSchema,
          false
        },
        {
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"string\" } ] }",
          _dataOnly,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record/f1/string :: schema type changed from int to string"
        },
        {
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : ##TYPE } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : ##NOTTYPE } ] }",
          _nonTyperefSchemaText,
          _dataOnly,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record/f1/\\S+ :: schema type changed from \\S+ to \\S+"
        },
        {
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : ##TYPE } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : ##NOTTYPE } ] }",
          _allSchemaText,
          _schemaOnly,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record/f1/\\S+ :: schema type changed from \\S+ to \\S+"
        },
        {
          // order does not matter.
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f0\", \"type\" : \"int\" }, { \"name\" : \"f1\", \"type\" : ##TYPE }, { \"name\" : \"f2\", \"type\" : \"string\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f2\", \"type\" : \"string\" }, { \"name\" : \"f1\", \"type\" : ##TYPE }, { \"name\" : \"f0\", \"type\" : \"int\" } ] }",
          _allSchemaText,
          _dataAndSchema,
          false
        },
        {
          // field changed from optional to required
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record changed optional fields to required fields f1"
        },
        {
          // field changed from required to optional
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_OLD_READER :: /a.b.Record :: new record changed required fields to optional fields f1"
        },
        {
          // field changed from required with default to optional
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"default\": 1 } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record :: new record changed required fields with defaults to optional fields f1. This change is compatible for "
          + "Pegasus but incompatible for Avro, if this record schema is never converted to Avro, this error may "
          + "safely be ignored."
        },
        {
          // added required fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" }, { \"name\" : \"f2\", \"type\" : \"string\" } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record added required fields f1, f2"
        },
        {
          // added required field with default
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"default\": 1 } ] }",
          _dataAndSchema,
          false
        },
        {
          // modified required field to have a default
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"default\": 1 } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_OLD_READER :: /a.b.Record :: new record added default to required fields f1"
        },
        {
          // modified required field to no longer have a default
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"default\": 1 } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record removed default from required fields f1"
        },
        {
          // modified optional field to required field with default
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\": true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"default\": 1 } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record :: new record changed optional fields to required fields with defaults f1. This change is compatible for "
          + "Pegasus but incompatible for Avro, if this record schema is never converted to Avro, this error may "
          + "safely be ignored."
        },
        {
          // removed required fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" }, { \"name\" : \"f2\", \"type\" : \"string\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_OLD_READER :: /a.b.Record :: new record removed required fields f1, f2"
        },
        {
          // added optional fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true }, { \"name\" : \"f2\", \"type\" : \"string\", \"optional\" : true } ] }",
          _dataAndSchema,
          false,
          "INFO :: OLD_READER_IGNORES_DATA :: /a.b.Record :: new record added optional fields f1, f2"
        },
        {
          // removed optional fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true }, { \"name\" : \"f2\", \"type\" : \"string\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record :: new record removed optional fields f1, f2. This allows a new field to be added with the same name but different type in the future."
        },
        {
          // removed optional fields, added required fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f2\", \"type\" : \"string\" }] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record added required fields f2",
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record :: new record removed optional fields f1. This allows a new field to be added with the same name but different type in the future."
        },
        {
          // removed required fields, added optional fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f2\", \"type\" : \"string\", \"optional\" : true } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_OLD_READER :: /a.b.Record :: new record removed required fields f1",
          "INFO :: OLD_READER_IGNORES_DATA :: /a.b.Record :: new record added optional fields f2"
        },
        {
          // removed optional fields, added required fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f2\", \"type\" : \"string\" }] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record added required fields f2",
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record :: new record removed optional fields f1. This allows a new field to be added with the same name but different type in the future."
        },
        {
          // changed required to optional fields, added optional fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true }, { \"name\" : \"f2\", \"type\" : \"string\", \"optional\" : true } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_OLD_READER :: /a.b.Record :: new record changed required fields to optional fields f1",
          "INFO :: OLD_READER_IGNORES_DATA :: /a.b.Record :: new record added optional fields f2"
        },
        {
          // changed optional to required fields, added optional fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" }, { \"name\" : \"f2\", \"type\" : \"string\", \"optional\" : true } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record changed optional fields to required fields f1",
          "INFO :: OLD_READER_IGNORES_DATA :: /a.b.Record :: new record added optional fields f2"
        },
        {
          // changed required to optional fields, added required fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true }, { \"name\" : \"f2\", \"type\" : \"string\" } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_OLD_READER :: /a.b.Record :: new record changed required fields to optional fields f1",
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record added required fields f2"
        },
        {
          // changed optional to required fields, added required fields
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"optional\" : true } ] }",
          "{ \"name\" : \"a.b.Record\", \"type\" : \"record\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\" }, { \"name\" : \"f2\", \"type\" : \"string\" } ] }",
          _dataAndSchema,
          true,
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record changed optional fields to required fields f1",
          "ERROR :: BREAKS_NEW_READER :: /a.b.Record :: new record added required fields f2"
        },
        {
          "{ \"name\" : \"a.b.Record1\", \"type\" : \"record\", \"fields\" : [ ] }",
          "{ \"name\" : \"a.b.Record2\", \"type\" : \"record\", \"fields\" : [ ] }",
          _checkNamesOnly,
          true,
          "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Record2 :: name changed from a.b.Record1 to a.b.Record2"
        },
        {
          "{ \"name\" : \"a.b.Record1\", \"type\" : \"record\", \"fields\" : [ ] }",
          "{ \"name\" : \"a.b.Record2\", \"type\" : \"record\", \"fields\" : [ ] }",
          _noCheckNamesOnly,
          false
        },
      };

    testCompatibility(inputs);
  }

  @Test
  public void testUnion() throws IOException
  {
    Object[][] inputs =
    {
      {
        "[ ]",
        "[ ]",
        _dataAndSchema,
        false
      },
      {
        "[ ##TYPE ]",
        "[ ##TYPE ]",
        subtract(_allSchemaText, _unionSchemaText),
        _dataAndSchema,
        false
      },
      {
        "[ \"int\", ##TYPE, \"string\" ]",
        "[ \"string\", ##TYPE, \"int\" ]",
        subtract(_allSchemaText, _intSchemaText, _stringSchemaText, _unionSchemaText),
        _dataAndSchema,
        false
      },
      {
        "[ \"int\", \"string\" ]",
        "[ \"int\", \"string\" ]",
        _dataAndSchema,
        false
      },
      {
        "[ \"int\", \"string\", \"float\" ]",
        "[ \"string\" ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_READER :: /union :: new union removed members int, float"
      },
      {
        "[ \"string\" ]",
        "[ \"int\", \"string\", \"float\" ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_OLD_READER :: /union :: new union added members int, float"
      },
      {
        "[ ]",
        "[ \"int\" ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_OLD_READER :: /union :: new union added members int"
      },
      {
        "[ \"int\" ]",
        "[ ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_READER :: /union :: new union removed members int"
      },
      {
        "[ \"string\", \"double\" ]",
        "[ \"int\", \"string\", \"float\" ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_READER :: /union :: new union removed members double",
        "ERROR :: BREAKS_OLD_READER :: /union :: new union added members int, float"
      },
      {
        "[ { \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"A\", \"B\", \"C\", \"D\" ] } ]",
        "[ { \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"B\", \"D\", \"E\" ] } ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_OLD_READER :: /union/a.b.Enum/symbols :: new enum added symbols E",
        "ERROR :: BREAKS_NEW_READER :: /union/a.b.Enum/symbols :: new enum removed symbols A, C"
      },
      {
        "[ \"int\", { \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"A\", \"B\", \"C\", \"D\" ] }, \"string\" ]",
        "[ \"string\", { \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"B\", \"D\", \"E\" ] }, \"int\" ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_OLD_READER :: /union/a.b.Enum/symbols :: new enum added symbols E",
        "ERROR :: BREAKS_NEW_READER :: /union/a.b.Enum/symbols :: new enum removed symbols A, C"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ ] }",
        _dataAndSchema,
        false
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ ##TYPE ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ ##TYPE ] }",
        subtract(_allSchemaText, _unionSchemaText),
        _dataAndSchema,
        false
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ \"int\", \"string\", \"float\" ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ \"string\" ] }",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_READER :: /a.b.Union/ref/union :: new union removed members int, float"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ \"string\", \"float\" ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Union\", \"ref\" : [ \"int\", \"string\", \"float\" ] }",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_OLD_READER :: /a.b.Union/ref/union :: new union added members int"
      },
      // Adding aliases to an existing Union
      {
        "[ \"int\", \"string\" ]",
        "[ { \"alias\" : \"count\", \"type\" : \"int\" }, { \"alias\" : \"message\", \"type\" : \"string\" } ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /union :: new union added member aliases"
      },
      // Removing aliases from an existing Union
      {
        "[ { \"alias\" : \"count\", \"type\" : \"int\" }, { \"alias\" : \"message\", \"type\" : \"string\" } ]",
        "[ \"int\", \"string\" ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /union :: new union removed member aliases"
      },
      // Adding a new member to an aliased Union
      {
        "[ { \"alias\" : \"count\", \"type\" : \"int\" } ]",
        "[ { \"alias\" : \"count\", \"type\" : \"int\" }, { \"alias\" : \"message\", \"type\" : \"string\" } ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_OLD_READER :: /union :: new union added members message"
      },
      // Removing a member from an aliased Union
      {
        "[ { \"alias\" : \"count\", \"type\" : \"int\" }, { \"alias\" : \"message\", \"type\" : \"string\" } ]",
        "[ { \"alias\" : \"count\", \"type\" : \"int\" } ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_READER :: /union :: new union removed members message"
      },
      // Updating the alias for a member in an Union
      {
        "[ { \"alias\" : \"count\", \"type\" : \"int\" }, { \"alias\" : \"message\", \"type\" : \"string\" } ]",
        "[ { \"alias\" : \"count\", \"type\" : \"int\" }, { \"alias\" : \"text\", \"type\" : \"string\" } ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_READER :: /union :: new union removed members message",
        "ERROR :: BREAKS_OLD_READER :: /union :: new union added members text"
      },
      // Updating the type of an aliased member in an Union
      {
        "[ { \"alias\" : \"count\", \"type\" : \"int\" }, { \"alias\" : \"message\", \"type\" : \"string\" } ]",
        "[ { \"alias\" : \"count\", \"type\" : \"long\" }, { \"alias\" : \"message\", \"type\" : \"string\" } ]",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /union/count/long :: schema type changed from int to long"
      },
    };

    testCompatibility(inputs);
  }

  @Test
  public void testTyperef() throws IOException
  {
    Object[][] inputs = {
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        _nonTyperefSchemaText,
        _dataAndSchema,
        false
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : \"int\" }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : \"float\" }",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Typeref/ref/float :: schema type changed from int to float"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##NOTTYPE }",
        _nonTyperefSchemaText,
        _dataOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Typeref/ref/\\S+ :: schema type changed from \\S+ to \\S+"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##NOTTYPE }",
        _nonTyperefSchemaText,
        _schemaOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Typeref/ref/\\S+ :: schema type changed from \\S+ to \\S+"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref1\", \"ref\" : \"string\" }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref2\", \"ref\" : \"string\" }",
        _schemaCompatibilityCheckNames,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Typeref2 :: name changed from a.b.Typeref1 to a.b.Typeref2"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : \"int\" }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        list(_longSchemaText, _floatSchemaText, _doubleSchemaText),
        _allowPromotions,
        false,
        "INFO :: VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW :: /a.b.Typeref/ref/(\\S+) :: numeric type promoted from int to \\1"
      },
      {
        _intSchemaText,
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        list(_longSchemaText, _floatSchemaText, _doubleSchemaText),
        _allowPromotionsData,
        false,
        "INFO :: VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW :: /a.b.Typeref/ref/(\\S+) :: numeric type promoted from int to \\1"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : \"int\" }",
        "##TYPE",
        list(_longSchemaText, _floatSchemaText, _doubleSchemaText),
        _allowPromotionsData,
        false,
        "INFO :: VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW :: /(\\S+) :: numeric type promoted from int to \\1"
      },
      {
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref1\", \"ref\" : \"string\" }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref2\", \"ref\" : \"string\" }",
        _noCheckNamesOnly,
        false
      },
    };

    testCompatibility(inputs);
  }

  @Test
  public void testArray() throws IOException
  {
    Object[][] inputs = {
      {
        "{ \"type\" : \"array\", \"items\" : ##TYPE }",
        "{ \"type\" : \"array\", \"items\" : ##TYPE }",
        _allSchemaText,
        _dataAndSchema,
        false
      },
      {
        "{ \"type\" : \"array\", \"items\" : \"int\" }",
        "{ \"type\" : \"array\", \"items\" : \"float\" }",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /array/items/float :: schema type changed from int to float"
      },
      {
        "{ \"type\" : \"array\", \"items\" : ##TYPE }",
        "{ \"type\" : \"array\", \"items\" : ##NOTTYPE }",
        _nonTyperefSchemaText,
        _dataOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /array/items/\\S+ :: schema type changed from \\S+ to \\S+"
      },
      {
        "{ \"type\" : \"array\", \"items\" : ##TYPE }",
        "{ \"type\" : \"array\", \"items\" : ##NOTTYPE }",
        _allSchemaText,
        _schemaOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /array/items/\\S+ :: schema type changed from \\S+ to \\S+"
      },
    };

    testCompatibility(inputs);
  }

  @Test
  public void testMap() throws IOException
  {
    Object[][] inputs = {
      {
        "{ \"type\" : \"map\", \"values\" : ##TYPE }",
        "{ \"type\" : \"map\", \"values\" : ##TYPE }",
        _allSchemaText,
        _dataAndSchema,
        false
      },
      {
        "{ \"type\" : \"map\", \"values\" : \"int\" }",
        "{ \"type\" : \"map\", \"values\" : \"float\" }",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /map/values/float :: schema type changed from int to float"
      },
      {
        "{ \"type\" : \"map\", \"values\" : ##TYPE }",
        "{ \"type\" : \"map\", \"values\" : ##NOTTYPE }",
        _nonTyperefSchemaText,
        _dataOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /map/values/\\S+ :: schema type changed from \\S+ to \\S+"
      },
      {
        "{ \"type\" : \"map\", \"values\" : ##TYPE }",
        "{ \"type\" : \"map\", \"values\" : ##NOTTYPE }",
        _allSchemaText,
        _schemaOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /map/values/\\S+ :: schema type changed from \\S+ to \\S+"
      },
    };

    testCompatibility(inputs);
  }

  @Test
  public void testFixed() throws IOException
  {
    Object[][] inputs = {
      {
        "{ \"type\" : \"fixed\", \"name\" : \"a.b.Fixed\", \"size\" : 5 }",
        "{ \"type\" : \"fixed\", \"name\" : \"a.b.Fixed\", \"size\" : 6 }",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Fixed/size :: fixed size changed from 5 to 6"
      },
      {
        "{ \"type\" : \"fixed\", \"name\" : \"a.b.Fixed1\", \"size\" : 6 }",
        "{ \"type\" : \"fixed\", \"name\" : \"a.b.Fixed2\", \"size\" : 6 }",
        _checkNamesOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Fixed2 :: name changed from a.b.Fixed1 to a.b.Fixed2"
      },
      {
        "{ \"type\" : \"fixed\", \"name\" : \"a.b.Fixed1\", \"size\" : 6 }",
        "{ \"type\" : \"fixed\", \"name\" : \"a.b.Fixed2\", \"size\" : 6 }",
        _noCheckNamesOnly,
        false
      },
    };

    testCompatibility(inputs);
  }

  @Test
  public void testEnum() throws IOException
  {
    Object[][] inputs = {
      {
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ ] }",
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ ] }",
        _dataAndSchema,
        false
      },
      {
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"A\", \"B\", \"C\" ] }",
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"A\", \"B\", \"C\" ] }",
        _dataAndSchema,
        false
      },
      {
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"A\", \"B\", \"C\", \"D\" ] }",
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum\", \"symbols\" : [ \"B\", \"D\", \"E\" ] }",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_OLD_READER :: /a.b.Enum/symbols :: new enum added symbols E",
        "ERROR :: BREAKS_NEW_READER :: /a.b.Enum/symbols :: new enum removed symbols A, C"
      },
      {
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum1\", \"symbols\" : [ \"A\", \"B\" ] }",
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum2\", \"symbols\" : [ \"A\", \"B\" ] }",
        _checkNamesOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Enum2 :: name changed from a.b.Enum1 to a.b.Enum2"
      },
      {
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum1\", \"symbols\" : [ \"A\", \"B\" ] }",
        "{ \"type\" : \"enum\", \"name\" : \"a.b.Enum2\", \"symbols\" : [ \"A\", \"B\" ] }",
        _noCheckNamesOnly,
        false
      },
    };

    testCompatibility(inputs);
  }

  @Test
  public void testPrimitive() throws IOException
  {
    Object[][] inputs = {
      {
        "##TYPE",
        "##TYPE",
        _primitiveSchemaText,
        _dataAndSchema,
        false
      },
      {
        "##TYPE",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        _primitiveSchemaText,
        _dataOnly,
        false
      },
      {
        "##TYPE",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Typeref\", \"ref\" : ##TYPE }",
        _primitiveSchemaText,
        _schemaOnly,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /a.b.Typeref :: schema type changed from \\S+ to typeref"
      },
      {
        "\"int\"",
        "\"string\"",
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /string :: schema type changed from int to string"
      },
      {
        "##TYPE",
        "##NOTTYPE",
        _primitiveSchemaText,
        _dataAndSchema,
        true,
        "ERROR :: BREAKS_NEW_AND_OLD_READERS :: /\\S+ :: schema type changed from \\S+ to \\S+"
      }
    };

    testCompatibility(inputs);
  }

  @Test
  public void testPromotions() throws IOException
  {


    Object[][] inputs =
    {
      {
        _intSchemaText,
        _intSchemaText,
        _allowPromotions,
        false
      },
      {
        _intSchemaText,
        "##TYPE",
        list(_longSchemaText, _floatSchemaText, _doubleSchemaText),
        _allowPromotions,
        false,
        "INFO :: VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW :: /(\\S+) :: numeric type promoted from int to \\1"
      },
      {
        _longSchemaText,
        _longSchemaText,
        _allowPromotions,
        false
      },
      {
        _longSchemaText,
        "##TYPE",
        list(_floatSchemaText, _doubleSchemaText),
        _allowPromotions,
        false,
        "INFO :: VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW :: /(\\S+) :: numeric type promoted from long to \\1"
      },
      {
        _floatSchemaText,
        _floatSchemaText,
        _allowPromotions,
        false
      },
      {
        _floatSchemaText,
        "##TYPE",
        list(_doubleSchemaText),
        _allowPromotions,
        false,
        "INFO :: VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW :: /(\\S+) :: numeric type promoted from float to \\1"
      },
    };

    testCompatibility(inputs);
  }

  private void testCompatibility(Object[][] inputs) throws IOException
  {
    for (Object[] row : inputs)
    {
      int i = 0;
      String olderSchemaText = (String) row[i++];
      String newerSchemaText = (String) row[i++];

      String[] substitutions = null;
      if (olderSchemaText.contains("##") || newerSchemaText.contains("##"))
      {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) row[i++];
        substitutions = list.toArray(new String[0]);
      }

      @SuppressWarnings("unchecked")
      List<CompatibilityOptions> compatibilityOptions =
        row[i] instanceof CompatibilityOptions ?
          new ArrayList<CompatibilityOptions>(Arrays.asList((CompatibilityOptions) row[i++])) :
          (List<CompatibilityOptions>) row[i++];
      boolean hasError = i < row.length ? (Boolean) row[i++] : false;

      if (substitutions != null)
      {
        for (int s = 0; s < substitutions.length; s++)
        {
          String typeText = substitutions[s];
          String olderText = olderSchemaText.replaceAll("##TYPE", typeText);
          String newerText = newerSchemaText.replaceAll("##TYPE", typeText);

          if (olderText.contains("##NOTTYPE") || newerText.contains("##NOTTYPE"))
          {
            for (int n = 0; n < substitutions.length; n++)
            {
              if (n != s)
              {
                String notTypeText = substitutions[n];
                String olderTextWithNotType = olderText.replaceAll("##NOTTYPE", notTypeText);
                String newerTextWithNotType = newerText.replaceAll("##NOTTYPE", notTypeText);
                testCompatibility(olderTextWithNotType, newerTextWithNotType, compatibilityOptions, hasError, row, i);
              }
            }
          }
          else
          {
            testCompatibility(olderText, newerText, compatibilityOptions, hasError, row, i);
          }
        }
      }
      else
      {
        testCompatibility(olderSchemaText, newerSchemaText, compatibilityOptions, hasError, row, i);
      }
    }
  }

  private void testCompatibility(String olderSchemaText, String newerSchemaText,
                                 List<CompatibilityOptions> compatibilityOptions,
                                 boolean hasError,
                                 Object[] expected, int expectedIndex) throws IOException
  {
    if (_debug) out.println(olderSchemaText + "\n" + newerSchemaText);
    DataSchema olderSchema = TestUtil.dataSchemaFromString(olderSchemaText);
    DataSchema newerSchema = TestUtil.dataSchemaFromString(newerSchemaText);
    assertNotNull(olderSchema, olderSchemaText);
    assertNotNull(newerSchema, newerSchemaText);

    for (CompatibilityOptions option : compatibilityOptions)
    {
      CompatibilityResult result = CompatibilityChecker.checkCompatibility(olderSchema, newerSchema, option);
      String messageText = result.getMessages().toString();

      if (_debug) out.println(result);

      assertEquals(result.isError(), hasError, olderSchemaText + "\n" + newerSchemaText + "\n" + messageText);
      for (int i = expectedIndex; i < expected.length; i++)
      {
        String expectedText = ".*" + expected[i] + "\n.*";
        if (_debug) out.println(expectedText);
        Pattern pattern = Pattern.compile(expectedText, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(messageText);
        boolean matches = matcher.matches();
        assertTrue(matches, messageText + "\n" + expectedText);
      }
    }
  }
}
