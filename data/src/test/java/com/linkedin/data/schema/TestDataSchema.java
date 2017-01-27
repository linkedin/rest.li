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

package com.linkedin.data.schema;

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.*;
import static org.testng.Assert.*;

public class TestDataSchema
{
  @Test
  public void testSchemaParser() throws IOException
  {
    // Test the parser parses the input schema in JSON form.
    // Re-encode the parsed schema into JSON.
    // This tests compares the expected output with the actual re-encoded schema JSON.
    //
    // Array of tests, each test is an array of 2 or more elements.
    //   index 0 - the input to the schema parsers.
    //   index 1 - the expected output from encoding the DataSchema's from the parser in JSON.
    String[][] testData =
      {
        // test allow comments
        {
          "/* xxx */ { \"type\" : \"fixed\", /* yyy */ \"name\" : \"md5\", // abc\n \"size\" : 16 }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"size\" : 16 }"
        },
        // test more verbose way of writing primitive types
        {
          "{ \"type\": \"boolean\" }",
          "\"boolean\""
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : {\"type\" : \"int\"} } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : {\"type\" : \"array\", \"items\" : {\"type\":\"long\"} } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"long\" } } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [{\"type\": \"float\"},{\"type\": \"double\"}] } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"float\", \"double\" ] } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : {\"type\" : \"bytes\"} } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"bytes\" } } ] }"
        },
        // test fixed, with name overriding namespace
        {
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"size\" : 16 }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"size\" : 16 }"
        },
        {
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"namespace\" : \"name.space\", \"size\" : 16 }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"namespace\" : \"name.space\", \"size\" : 16 }"
        },
        {
          "{ \"type\" : \"fixed\", \"name\" : \"name.md5\", \"size\" : 16 }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"namespace\" : \"name\", \"size\" : 16 }"
        },
        {
          "{ \"type\" : \"fixed\", \"name\" : \"name.md5\", \"namespace\" : \"name.space\", \"size\" : 16 }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"namespace\" : \"name\", \"size\" : 16 }"
        },
        // test fixed, with aliases
        {
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"size\" : 16, \"aliases\" : [ \"m1\", \"m2\" ] }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"size\" : 16, \"aliases\" : [ \"m1\", \"m2\" ] }"
        },
        // test fixed, with fully qualified aliases
        {
          "{ \"type\" : \"fixed\", \"name\" : \"abc.md5\", \"size\" : 16, \"aliases\" : [ \"m1\", \"cde.m2\" ] }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"namespace\" : \"abc\", \"size\" : 16, \"aliases\" : [ \"abc.m1\", \"cde.m2\" ] }"
        },
        // test fixed, with doc
        {
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"doc\" : \"documentation\", \"size\" : 16 }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"doc\" : \"documentation\", \"size\" : 16 }"
        },
        // test fixed, with doc and aliases
        {
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"doc\" : \"documentation\", \"size\" : 16, \"aliases\" : [ \"m1\", \"m2\" ] }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"doc\" : \"documentation\", \"size\" : 16, \"aliases\" : [ \"m1\", \"m2\" ] }"
        },
        // test fixed, with doc, properties, and aliases
        {
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"doc\" : \"documentation\", \"size\" : 16, \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"m1\", \"m2\" ] }",
          "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"doc\" : \"documentation\", \"size\" : 16, \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"m1\", \"m2\" ] }"
        },
        // test record with name overriding namespace and propagating namespace to scoped objects
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"size\" : 32 } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"size\" : 32 } } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"name.space\", \"fields\" : " +
          "[ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"size\" : 32 } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"name.space\", \"fields\" : [ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"size\" : 32 } } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"name.foo\", \"fields\" : " +
          "[ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"size\" : 32 } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"name\", \"fields\" : [ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"size\" : 32 } } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"name.foo\", \"fields\" : " +
          "[ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"namespace\" : \"foo.space\", \"size\" : 32 } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"name\", \"fields\" : [ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"namespace\" : \"foo.space\", \"size\" : 32 } } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"name.foo\", \"fields\" : " +
          "[ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"foo.bar\", \"size\" : 32 } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"name\", \"fields\" : [ { \"name\" : \"foo\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"bar\", \"namespace\" : \"foo\", \"size\" : 32 } } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"foo.bar\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\" }, { \"name\" : \"float\", \"type\" : \"float\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"namespace\" : \"foo.bar\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\" }, { \"name\" : \"float\", \"type\" : \"float\" } ] }"
        },
        // test record, with doc
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"doc\" : \"documentation\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"doc\" : \"documentation\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\" } ] }"
        },
        // test record, with properties
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\" } ], \"prop1\" : \"xx\", \"prop2\" : \"yy\" }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\" } ], \"prop1\" : \"xx\", \"prop2\" : \"yy\" }"
        },
        // test record, with aliases
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\" } ], \"aliases\" : [ \"bar\", \"baz\" ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\" } ], \"aliases\" : [ \"bar\", \"baz\" ] }"
        },
        // test record, with doc, properties, aliases
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"doc\" : \"documentation\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\" } ], \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"bar\", \"baz\" ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"doc\" : \"documentation\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\" } ], \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"bar\", \"baz\" ] }"
        },
        // test record, field with doc
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"doc\" : \"documentation for integer\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\", \"doc\" : \"documentation for integer\" } ] }"
        },
        // test record, field with aliases
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"aliases\" : [ \"int1\", \"int2\" ] } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\", \"aliases\" : [ \"int1\", \"int2\" ] } ] }"
        },
        // test record, field is optional
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"optional\" : true } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\", \"optional\" : true } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"optional\" : false } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\" } ] }"
        },
        // test record, field with sort order
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"order\" : \"descending\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\", \"order\" : \"descending\" } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"order\" : \"ASCENDING\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\" } ] }"
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"order\" : \"IgNoRe\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\", \"order\" : \"ignore\" } ] }"
        },
        // test record, field with properties
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"prop1\" : \"xx\", \"prop2\" : \"yy\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\", \"prop1\" : \"xx\", \"prop2\" : \"yy\" } ] }"
        },

        // test record, field with everything
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"integer\", \"type\" : \"int\", \"doc\" : \"documentation\", \"default\" : 3, \"optional\" : true, \"order\" : \"ignore\", \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"int1\", \"int2\" ] } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"integer\", \"type\" : \"int\", \"doc\" : \"documentation\", \"default\" : 3, \"optional\" : true, \"order\" : \"ignore\", \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"int1\", \"int2\" ] } ] }"
        },
        // test enum
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }"
        },
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"namespace\" : \"fruit.bar\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"namespace\" : \"fruit.bar\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }"
        },
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruit.bar.fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"namespace\" : \"fruit.bar\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }"
        },
        // test enum, with doc
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"doc\" : \"documentation\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"doc\" : \"documentation\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }"
        },
         // test enum, with symbol docs
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"symbolDocs\" : { \"ORANGE\" : \"DOC_ORANGE\", \"APPLE\" : \"DOC_APPLE\" } }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"symbolDocs\" : { \"APPLE\" : \"DOC_APPLE\", \"ORANGE\" : \"DOC_ORANGE\" } }"
        },
        // test enum, with properties
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"abc\" ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"abc\" ] }"
        },
        // test enum, with aliases
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"aliases\" : [ \"abc\" ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"aliases\" : [ \"abc\" ] }"
        },
        // test enum, with doc, symbol docs, properties and aliases
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"doc\" : \"documentation\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"symbolDocs\" : { \"APPLE\" : \"DOC_APPLE\", \"ORANGE\" : \"DOC_ORANGE\" }, \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"abc\" ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"doc\" : \"documentation\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ], \"symbolDocs\" : { \"APPLE\" : \"DOC_APPLE\", \"ORANGE\" : \"DOC_ORANGE\" }, \"prop1\" : \"xx\", \"prop2\" : \"yy\", \"aliases\" : [ \"abc\" ] }"
        },
        // test map, with properties
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\", \"prop1\" : \"xx\", \"prop2\" : \"yy\" } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\", \"prop1\" : \"xx\", \"prop2\" : \"yy\" } } ] }"
        },
        // test map of int
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" } } ] }"
        },
        // test map of enum
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] } } } ] }"
        },
        // test array, with properties
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\", \"prop1\" : \"xx\", \"prop2\" : \"yy\" } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\", \"prop1\" : \"xx\", \"prop2\" : \"yy\" } } ] }"
        },
        // test array of ints
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } } ] }"
        },
        // test array of maps
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" :  { \"type\" : \"map\", \"values\" : \"int\" } } } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"map\", \"values\" : \"int\" } } } ] }"
        },
        // test union of primitives
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : [ \"null\", \"boolean\", \"int\", \"long\", \"float\", \"double\", \"bytes\", \"string\" ] } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"null\", \"boolean\", \"int\", \"long\", \"float\", \"double\", \"bytes\", \"string\" ] } ] }"
        },
        // test union of complex types
        {
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }" +
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : [ \"fruits\", { \"type\" : \"array\", \"items\" :  { \"type\" : \"map\", \"values\" : \"int\" } } ] } ] }",
          "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\" ] }{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ \"fruits\", { \"type\" : \"array\", \"items\" : { \"type\" : \"map\", \"values\" : \"int\" } } ] } ] }"
        },
        // test default for boolean
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"boolean\", \"default\" : true } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"boolean\", \"default\" : true } ] }"
        },
        // test default for int
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 1 } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"default\" : 1 } ] }"
        },
        // test default for long
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"long\", \"default\" : 23 } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"long\", \"default\" : 23 } ] }"
        },
        // test default for float
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"float\", \"default\" : 52.5 } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"float\", \"default\" : 52.5 } ] }"
        },
        // test default for double
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"double\", \"default\" : 66.5 } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"double\", \"default\" : 66.5 } ] }"
        },
        // test default for string
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"string\", \"default\" : \"default string\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"string\", \"default\" : \"default string\" } ] }"
        },
        // test default for bytes
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"bytes\", \"default\" : \"\u0040\u0041\u0042\u0043\" } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"bytes\", \"default\" : \"\u0040\u0041\u0042\u0043\" } ] }"
        },
        // test default for fixed
        {
          "{ \"type\" : \"fixed\", \"name\" : \"fixed5\", \"size\" : 5 }" +
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : \"fixed5\", \"default\" : \"\\u0001\\u0002\\u0003\\u0004\\u0005\" } ] }",
          "{ \"type\" : \"fixed\", \"name\" : \"fixed5\", \"size\" : 5 }{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"fixed5\", \"default\" : \"\\u0001\\u0002\\u0003\\u0004\\u0005\" } ] }"
        },
        // test default for array
        {
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : " +
          "[ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" }, \"default\" : [ 1, 3, 5 ] } ] }",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" }, \"default\" : [ 1, 3, 5 ] } ] }"
        },
        // linked list example from Avro spec
        {
          "{ " +
          "  \"type\": \"record\"," +
          "  \"name\": \"LongList\"," +
          "  \"aliases\": [\"LinkedLongs\"]," +
          "  \"fields\" : [" +
          "    {\"name\": \"value\", \"type\": \"long\"}, " +
          "    {\"name\": \"next\", \"type\": [\"LongList\", \"null\"]} " +
          "  ]" +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"LongList\", \"fields\" : [ { \"name\" : \"value\", \"type\" : \"long\" }, { \"name\" : \"next\", \"type\" : [ \"LongList\", \"null\" ] } ], \"aliases\" : [ \"LinkedLongs\" ] }"
        },
        // typeref of primitive
        {
          "{ " +
          "  \"name\" : \"urn\", " +
          "  \"type\" : \"typeref\", " +
          "  \"ref\" : \"string\" " +
          "}",
          "{ \"type\" : \"typeref\", \"name\" : \"urn\", \"ref\" : \"string\" }"
        },
        // typeref of array
        {
          "{ " +
          "  \"name\" : \"intArray\", " +
          "  \"type\" : \"typeref\", " +
          "  \"ref\" : { \"type\" : \"array\", \"items\" : \"int\" } " +
          "}",
          "{ \"type\" : \"typeref\", \"name\" : \"intArray\", \"ref\" : { \"type\" : \"array\", \"items\" : \"int\" } }"
        },
        // typeref of typeref of primitive
        {
          "{ " +
          "  \"name\" : \"int3\", " +
          "  \"type\" : \"typeref\", " +
          "  \"ref\" : { \"type\" : \"typeref\", \"name\" : \"int2\", \"ref\" : \"int\" } " +
          "}",
          "{ \"type\" : \"typeref\", \"name\" : \"int3\", \"ref\" : { \"type\" : \"typeref\", \"name\" : \"int2\", \"ref\" : \"int\" } }"
        },
        // typeref in array
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"bar\", " +
          "      \"type\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } } " +
          "    } " +
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"array\", \"items\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } } } ] }"
        },
        // typeref in map
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"bar\", " +
          "      \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } } " +
          "    } " +
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } } } ] }"
        },
        // typeref in field
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"bar\", " +
          "      \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } } " +
          "    }, " +
          "    { " +
          "      \"name\" : \"intRef\", " +
          "      \"type\" : \"IntRef\" " +
          "    } "+
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"values\" : { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" } } }, { \"name\" : \"intRef\", \"type\" : \"IntRef\" } ] }"
        },
        // typeref in union
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"bar\", " +
          "      \"type\" : [ " +
          "        { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, " +
          "        \"double\" " +
          "      ] " +
          "    } "+
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : [ { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, \"double\" ] } ] }"
        },
        // include
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"include\" : [ " +
          "    { " +
          "      \"type\" : \"record\", " +
          "      \"name\" : \"bar\", " +
          "      \"fields\" : [ " +
          "        { \"name\" : \"b1\", \"type\" : \"int\" } " +
          "      ] " +
          "    } " +
          "  ], " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"f1\", " +
          "      \"type\" : \"double\" " +
          "    } "+
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"include\" : [ { \"type\" : \"record\", \"name\" : \"bar\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : \"int\" } ] } ], \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"double\" } ] }"
        },
        // include with typeref
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"bar\", " +
          "  \"fields\" : [ " +
          "    { \"name\" : \"b1\", \"type\" : \"int\" } " +
          "  ] " +
          "} " +
          "{ " +
          "  \"type\" : \"typeref\", " +
          "  \"name\" : \"barRef\", " +
          "  \"ref\"  : \"bar\" " +
          "}" +
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"include\" : [ " +
          "    \"barRef\" " +
          "  ], " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"f1\", " +
          "      \"type\" : \"double\" " +
          "    } "+
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"bar\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : \"int\" } ] }{ \"type\" : \"typeref\", \"name\" : \"barRef\", \"ref\" : \"bar\" }{ \"type\" : \"record\", \"name\" : \"foo\", \"include\" : [ \"barRef\" ], \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"double\" } ] }"
        },
        // include with include
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"bar\", " +
          "  \"fields\" : [ " +
          "    { \"name\" : \"b1\", \"type\" : \"int\" } " +
          "  ] " +
          "} " +
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"include\" : [ " +
          "    \"bar\" " +
          "  ], " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"f1\", " +
          "      \"type\" : \"double\" " +
          "    } "+
          "  ] " +
          "}" +
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"jaz\", " +
          "  \"include\" : [ " +
          "    \"foo\" " +
          "  ], " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"j1\", " +
          "      \"type\" : \"float\" " +
          "    } "+
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"bar\", \"fields\" : [ { \"name\" : \"b1\", \"type\" : \"int\" } ] }{ \"type\" : \"record\", \"name\" : \"foo\", \"include\" : [ \"bar\" ], \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"double\" } ] }{ \"type\" : \"record\", \"name\" : \"jaz\", \"include\" : [ \"foo\" ], \"fields\" : [ { \"name\" : \"j1\", \"type\" : \"float\" } ] }"
        },
        // order of processing includes and fields is important when includes defines a named type
        // include before fields
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"Foo\", " +
          "  \"include\" : [ " +
          "    { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ ] } " +
          "  ], " +
          "  \"fields\" : [ " +
          "    { \"name\" : \"b1\", \"type\" : \"Bar\" } " +
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"include\" : [ { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [  ] } ], \"fields\" : [ { \"name\" : \"b1\", \"type\" : \"Bar\" } ] }"
        },
        // order of processing includes and fields is important when includes defines a named type,
        // fields before include
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"Foo\", " +
          "  \"fields\" : [ " +
          "    { \"name\" : \"b1\", \"type\" : { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ ] } } " +
          "  ], " +
          "  \"include\" : [ " +
          "    \"Bar\" " +
          "  ] " +
          "}",
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"include\" : [ { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [  ] } ], \"fields\" : [ { \"name\" : \"b1\", \"type\" : \"Bar\" } ] }"
        }
      };

    for (String[] test : testData)
    {
      PegasusSchemaParser parser = schemaParserFromString(test[0]);
      String result = (parser.hasError() ? "ERROR: " + parser.errorMessage() : parser.schemasToString());
      if (test[1] != null)
      {
        assertEquals(result, test[1]);
        // test equals and hashCode
        PegasusSchemaParser parser2 = schemaParserFromString(test[0]);
        for (int i = 0; i < parser.topLevelDataSchemas().size(); ++i)
        {
          assertEquals(parser.topLevelDataSchemas().get(i), parser2.topLevelDataSchemas().get(i));
          assertEquals(parser.topLevelDataSchemas().get(i).hashCode(), parser2.topLevelDataSchemas().get(i).hashCode());

          // test getType && getDeferencedType
          // test getDeferencedDataSchema
          DataSchema schema = parser.topLevelDataSchemas().get(i);
          if (schema.getType() == DataSchema.Type.TYPEREF)
          {
            assertSame(schema.getClass(), TyperefDataSchema.class);
            assertNotSame(schema.getType(), schema.getDereferencedType());
            assertNotSame(schema, schema.getDereferencedDataSchema());
            DataSchema dereferencedSchema = schema.getDereferencedDataSchema();
            assertFalse(dereferencedSchema instanceof TyperefDataSchema);
            assertNotSame(dereferencedSchema.getType(), DataSchema.Type.TYPEREF);
            assertSame(schema.getDereferencedType(), dereferencedSchema.getType());
          }
          else
          {
            assertSame(schema.getType(), schema.getDereferencedType());
            assertSame(schema, schema.getDereferencedDataSchema());
          }
        }
      }
      else
      {
        out.println(result);
      }
    }
  }

  @Test
  public void testInclude() throws IOException
  {
    // Test the presence of included fields.
    // Array of tests, each test is an array of 2 or more elements.
    //   index 0 - the input to the schema parsers.
    //   index 1 - names of fields that should be present the last schema in the input.
    Object[][] testData = {
      // include
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"foo\", " +
        "  \"include\" : [ " +
        "    { " +
        "      \"type\" : \"record\", " +
        "      \"name\" : \"bar\", " +
        "      \"fields\" : [ " +
        "        { \"name\" : \"b1\", \"type\" : \"int\" } " +
        "      ] " +
        "    } " +
        "  ], " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f1\", " +
        "      \"type\" : \"double\" " +
        "    } "+
        "  ] " +
        "}",
        new String[] { "f1", "b1" }
      },
      // include with typeref
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"bar\", " +
        "  \"fields\" : [ " +
        "    { \"name\" : \"b1\", \"type\" : \"int\" } " +
        "  ] " +
        "} " +
        "{ " +
        "  \"type\" : \"typeref\", " +
        "  \"name\" : \"barRef\", " +
        "  \"ref\"  : \"bar\" " +
        "}" +
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"foo\", " +
        "  \"include\" : [ " +
        "    \"barRef\" " +
        "  ], " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f1\", " +
        "      \"type\" : \"double\" " +
        "    } "+
        "  ] " +
        "}",
        new String[] { "f1", "b1" }
      },
      // include with include (transitive include)
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"bar\", " +
        "  \"fields\" : [ " +
        "    { \"name\" : \"b1\", \"type\" : \"int\" } " +
        "  ] " +
        "} " +
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"foo\", " +
        "  \"include\" : [ " +
        "    \"bar\" " +
        "  ], " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f1\", " +
        "      \"type\" : \"double\" " +
        "    } "+
        "  ] " +
        "}" +
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"jaz\", " +
        "  \"include\" : [ " +
        "    \"foo\" " +
        "  ], " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"j1\", " +
        "      \"type\" : \"float\" " +
        "    } "+
        "  ] " +
        "}",
        new String[] { "b1", "f1", "j1" }
      },
      // include before fields
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"a.b.bar\", " +
        "  \"include\" : [ " +
        "    { " +
        "      \"type\" : \"record\", " +
        "      \"name\" : \"foo\", " +
        "      \"fields\" : [ " +
        "        { " +
        "          \"name\" : \"f1\", " +
        "          \"type\" : \"int\" " +
        "        } " +
        "      ] " +
        "    } " +
        "  ]," +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f2\", " +
        "      \"type\" : \"foo\" "+
        "    } " +
        "  ] " +
        "}",
        new String[] { "f1", "f2" }
      },
      // fields before include
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"a.b.bar\", " +
        "  \"fields\" : [" +
        "    { " +
        "      \"name\" : \"f1\", " +
        "      \"type\" : { " +
        "        \"type\" : \"record\", " +
        "        \"name\" : \"foo\", " +
        "        \"fields\" : [ " +
        "          { " +
        "            \"name\" : \"f2\", " +
        "            \"type\" : \"int\" " +
        "          } " +
        "        ] " +
        "      } " +
        "    } " +
        "  ]," +
        "  \"include\" : [ \"foo\" ]" +
        "}",
        new String[] { "f1", "f2" }
      },
      // include before fields, test namespace handling
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"a.b.bar\", " +
        "  \"include\" : [ " +
        "    { " +
        "      \"type\" : \"record\", " +
        "      \"name\" : \"b.c.foo\", " +
        "      \"fields\" : [ " +
        "        { " +
        "          \"name\" : \"i1\", " +
        "          \"type\" : { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [] } " +
        "        } " +
        "      ] " +
        "    }, " +
        "    { " +
        "      \"type\" : \"record\", " +
        "      \"name\" : \"foofoo\", " +
        "      \"fields\" : [ " +
        "        { " +
        "          \"name\" : \"i2\", " +
        "          \"type\" : \"int\" " +
        "        } " +
        "      ] " +
        "    } " +
        "  ]," +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f1\", " +
        "      \"type\" : \"b.c.fruits\" "+
        "    }, " +
        "    { " +
        "      \"name\" : \"f2\", " +
        "      \"type\" : \"b.c.foo\" "+
        "    }, " +
        "    { " +
        "      \"name\" : \"f3\", " +
        "      \"type\" : \"foofoo\" "+
        "    } " +
        "  ] " +
        "}",
        new String[] { "i1", "i2", "f1", "f2", "f3"}
      },
      // fields before include, test namespace handling
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"a.b.bar\", " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f1\", " +
        "      \"type\" : { " +
        "        \"type\" : \"record\", " +
        "        \"name\" : \"b.c.foo\", " +
        "        \"fields\" : [ " +
        "          { " +
        "            \"name\" : \"i1\", " +
        "            \"type\" : { \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [] } " +
        "          } " +
        "        ] " +
        "      } " +
        "    }, " +
        "    { " +
        "      \"name\" : \"f2\", " +
        "      \"type\" : { " +
        "        \"type\" : \"record\", " +
        "        \"name\" : \"foofoo\", " +
        "        \"fields\" : [ " +
        "          { " +
        "            \"name\" : \"i2\", " +
        "            \"type\" : \"int\" " +
        "          } " +
        "        ] " +
        "      } " +
        "    }, " +
        "    { " +
        "      \"name\" : \"f3\", " +
        "      \"type\" : \"foofoo\" "+
        "    }, " +
        "    { " +
        "      \"name\" : \"f4\", " +
        "      \"type\" : \"b.c.foo\" "+
        "    }, " +
        "    { " +
        "      \"name\" : \"f5\", " +
        "      \"type\" : \"b.c.fruits\" "+
        "    } " +
        "  ], " +
        "  \"include\" : [ " +
        "    \"b.c.foo\", \"foofoo\" " +
        "  ] " +
        "}",
        new String[] { "i1", "i2", "f1", "f2", "f3", "f4", "f5"}
      }
    };

    for (Object[] test : testData)
    {
      // test schema from string

      String schemaText = (String) test[0];
      String[] expectedFields = (String[]) test[1];

      testIncludeForSchemaText(schemaText, expectedFields);
    }
  }

  private String typerefSchema(String refName, String schemaText)
  {
    return
      "{ " +
      "  \"type\" : \"typeref\", " +
      "  \"name\" : " + refName + ", " +
      "  \"ref\" : " + schemaText +
      "}";
  }

  private String mapSchema(String values)
  {
    return "{ \"type\" : \"map\", \"values\" : " + values + " }";
  }

  private String arraySchema(String items)
  {
    return "{ \"type\" : \"array\", \"items\" : " + items + " }";
  }

  @Test
  public void testIncludeFieldsOrdering() throws IOException
  {
    final String fooName = "\"foo\"";
    final String fooSchema =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : " + fooName + ", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"f1\", " +
      "      \"type\" : \"int\" " +
      "    } " +
      "  ] " +
      "}";

    final String fooRefName = "\"fooRef\"";
    final String fooRefSchema = typerefSchema(fooRefName, fooSchema);

    final String fruitsName = "\"fruits\"";
    final String fruitsSchema = "{ \"type\" : \"enum\", \"name\" : " + fruitsName + ", \"symbols\" : [ \"APPLE\" ] }";

    final String fruitsRefName = "\"fruitsRef\"";
    final String fruitsRefSchema = typerefSchema(fruitsRefName, fruitsSchema);

    final String md5Name = "\"md5\"";
    final String md5Schema = "{ \"type\" : \"fixed\", \"name\" : " + md5Name + ", \"size\" : 16 }";

    final String md5RefName = "\"md5Ref\"";
    final String md5RefSchema = typerefSchema(md5RefName, md5Schema);

    final String unionSchema = "[ \"string\", \"int\", " + fruitsRefSchema + ", " + md5RefSchema + ", " + fooRefSchema + "]";

    final String unionRefName = "\"unionRef\"";
    final String unionRefSchema = typerefSchema(unionRefName, unionSchema);

    // record containing enum, fixed, record

    final String recordName = "\"r1\"";
    final String recordSchema =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : " + recordName + ", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"r1fruits\", " +
      "      \"type\" : " + fruitsRefSchema +
      "    }, " +
      "    { " +
      "      \"name\" : \"r1Foo\", " +
      "      \"type\" : " + fooRefSchema +
      "    }, " +
      "    { " +
      "      \"name\" : \"r1Md5\", " +
      "      \"type\" : " + md5RefSchema +
      "    } " +
      "  ] " +
      "}";

    final String recordRefName = "\"r1Ref\"";
    final String recordRefSchema = typerefSchema(recordRefName, recordSchema);

    // record containing union of enum, fixed, record

    final String rUnionName = "\"r2\"";
    final String rUnionSchema =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : " + rUnionName + ", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"r2Union\", " +
      "      \"type\" : " + unionRefSchema +
      "    } " +
      "  ] " +
      "}";

    final String rUnionRefName = "\"r2Ref\"";
    final String rUnionRefSchema = typerefSchema(rUnionRefName, rUnionSchema);

    // record containing arrays of enum, fixed, record

    final String rArrayName = "\"r3\"";
    final String rArraySchema =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : " + rArrayName + ", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"r3fruits\", " +
      "      \"type\" : " + arraySchema(fruitsRefSchema) +
      "    }, " +
      "    { " +
      "      \"name\" : \"r3Foo\", " +
      "      \"type\" : " + arraySchema(fooRefSchema) +
      "    }, " +
      "    { " +
      "      \"name\" : \"r3Md5\", " +
      "      \"type\" : " + arraySchema(md5RefSchema) +
      "    } " +
      "  ] " +
      "}";

    final String rArrayRefName = "\"r3Ref\"";
    final String rArrayRefSchema = typerefSchema(rArrayRefName, rArraySchema);

    // record containing maps of enum, fixed, record

    final String rMapName = "\"r3\"";
    final String rMapSchema =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : " + rMapName + ", " +
      "  \"fields\" : [ " +
      "    { " +
      "      \"name\" : \"r3fruits\", " +
      "      \"type\" : " + mapSchema(fruitsRefSchema) +
      "    }, " +
      "    { " +
      "      \"name\" : \"r3Foo\", " +
      "      \"type\" : " + mapSchema(fooRefSchema) +
      "    }, " +
      "    { " +
      "      \"name\" : \"r3Md5\", " +
      "      \"type\" : " + mapSchema(md5RefSchema) +
      "    } " +
      "  ] " +
      "}";

    final String rMapRefName = "\"r3Ref\"";
    final String rMapRefSchema = typerefSchema(rMapRefName, rMapSchema);

    final String[][] substitutions =
      {
        // enum

        {
          fruitsSchema,
          fruitsName,
        },
        {
          fruitsRefSchema,
          fruitsName,
          fruitsRefName
        },

        // record

        {
          fooSchema,
          fooName,
        },
        {
          fooRefSchema,
          fooName,
          fooRefName
        },

        // fixed

        {
          md5Schema,
          md5Name,
        },
        {
          md5RefSchema,
          md5Name,
          md5RefName
        },

        // union

        {
          unionSchema,
          fruitsName,
          fruitsRefName,
          md5Name,
          md5RefName,
          fooName,
          fooRefName,
        },
        {
          unionRefSchema,
          fruitsName,
          fruitsRefName,
          md5Name,
          md5RefName,
          fooName,
          fooRefName,
          unionRefName,
        },

        // record

        {
          recordSchema,
          fruitsName,
          fruitsRefName,
          md5Name,
          md5RefName,
          fooName,
          fooRefName,
        },
        {
          recordRefSchema,
          fruitsName,
          fruitsRefName,
          md5Name,
          md5RefName,
          fooName,
          fooRefName,
          recordRefName
        },

        // record of union

        {
          rUnionRefSchema,
          fruitsName,
          fruitsRefName,
          md5Name,
          md5RefName,
          fooName,
          fooRefName,
          rUnionRefName
        },

        // record of arrays

        {
          rArrayRefSchema,
          fruitsName,
          fruitsRefName,
          md5Name,
          md5RefName,
          fooName,
          fooRefName,
          rArrayRefName
        },

        // record of maps

        {
          rMapRefSchema,
          fruitsName,
          fruitsRefName,
          md5Name,
          md5RefName,
          fooName,
          fooRefName,
          rMapRefName
        },

      };

    final String[] testSchemas =
      {
        // include before fields
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"a.b.bar\", " +
        "  \"include\" : [ " +
        "     { " +
        "       \"type\" : \"record\", " +
        "       \"name\" : \"inc\", " +
        "       \"fields\" : [ " +
        "         { " +
        "           \"name\" : \"f1\", " +
        "           \"type\" : ##DEFINE " +
        "         } " +
        "       ]" +
        "     } " +
        "  ]," +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f2\", " +
        "      \"type\" : ##REFERENCE "+
        "    }" +
        "  ] " +
        "}",
        // fields before include
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"a.b.bar\", " +
        "  \"fields\" : [ " +
        "    { " +
        "      \"name\" : \"f2\", " +
        "      \"type\" : ##DEFINE "+
        "    }" +
        "  ], " +
        "  \"include\" : [ " +
        "     { " +
        "       \"type\" : \"record\", " +
        "       \"name\" : \"inc\", " +
        "       \"fields\" : [ " +
        "         { " +
        "           \"name\" : \"f1\", " +
        "           \"type\" : ##REFERENCE " +
        "         } " +
        "       ] " +
        "     } " +
        "  ]" +
        "}",
      };

    final String[] expectedFields = { "f1", "f2" };

    boolean debug = false;

    for (String schemaTemplate : testSchemas )
    {
      if (debug) System.out.println(schemaTemplate);

      for (String[] sub : substitutions)
      {
        String include = sub[0];
        String[] includes =
          {
            include,
            arraySchema(include),
            mapSchema(include)
          };
        for (String includeSchema : includes)
        {
          for (int i = 1; i < sub.length; i++)
          {
            String fieldType = sub[i];

            String schemaText = schemaTemplate.replaceAll("##DEFINE", includeSchema).replaceAll("##REFERENCE", fieldType);
            if (debug) System.out.println(schemaText);

            // if fields and include order parsing is not done
            // in the right order, then the schema parsing will
            // fail with unable to resolve ##REFERENCE that is
            // defined somewhere in ##DEFINE

            testIncludeForSchemaText(schemaText, expectedFields);
          }
        }
      }
    }
  }

  private void testIncludeForSchemaText(String schemaText, String[] expectedFields) throws IOException
  {
    // test schema with DataLocation
    PegasusSchemaParser parser = schemaParserFromString(schemaText);
    RecordDataSchema recordDataSchema = testIncludeWithSchemaParserOutputForExpectedFields(parser, expectedFields);

    // test schema without DataLocation
    PegasusSchemaParser dataMapSchemaParser = schemaParserFromObjectsString(schemaText); // no location information
    RecordDataSchema recordDataSchemaFromDataMap = testIncludeWithSchemaParserOutputForExpectedFields(
      dataMapSchemaParser,
      expectedFields);

    assertEquals(recordDataSchemaFromDataMap, recordDataSchema);
  }

  private RecordDataSchema testIncludeWithSchemaParserOutputForExpectedFields(PegasusSchemaParser parser,
                                                                              String[] expectedFields) throws IOException
  {
    if (parser.hasError())
    {
      throw new IOException(parser.errorMessage());
    }
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();
    assertFalse(topLevelSchemas.isEmpty());
    DataSchema lastSchema = topLevelSchemas.get(topLevelSchemas.size() - 1);
    assertSame(lastSchema.getClass(), RecordDataSchema.class);
    RecordDataSchema recordDataSchema = (RecordDataSchema) lastSchema;
    for (String f : expectedFields)
    {
      assertNotNull(recordDataSchema.getField(f));
    }
    return recordDataSchema;
  }

  @Test
  public void testIncludeInvalidTypes() throws IOException
  {
    final String schemaTemplate =
      "{ " +
      "  \"type\" : \"record\", " +
      "  \"name\" : \"a.b.bar\", " +
      "  \"include\" : [ ##TYPE ], " +
      "  \"fields\" : [] " +
      "}";

    final String[] types =
      {
        "\"null\"",
        "\"int\"",
        "\"long\"",
        "\"float\"",
        "\"double\"",
        "\"string\"",
        "\"bytes\"",
        "\"boolean\"",
        arraySchema("\"int\""),
        mapSchema("\"int\""),
        "{ \"type\" : \"enum\", \"name\" : \"fruits\", \"symbols\" : [] }",
        "{ \"type\" : \"fixed\", \"name\" : \"md5\", \"size\" : 16 }",
        "[ \"int\", \"string\" ]"
      };

    final String[] expected = { "cannot include", "because it is not a record" };

    for (String baseType : types)
    {
      final String[] derivedTypes = { baseType, typerefSchema("\"xyz\"", baseType) };
      for (String t : derivedTypes)
      {
        String schemaText = schemaTemplate.replaceAll("##TYPE", t);
        checkBadSchema(schemaText, expected);
      }
    }
  }

  private void checkBadSchema(String schemaText, String[] expected) throws IOException
  {
    checkBadSchema(schemaText, expected, 0);
  }

  private void checkBadSchema(String schemaText, String[] expected, int index) throws IOException
  {
    boolean debug = false;

    if (debug) out.println(schemaText);

    // test schema with DataLocation
    PegasusSchemaParser parser = schemaParserFromString(schemaText);
    String message = parser.errorMessage();
    if (debug) { out.println(parser.schemasToString()) ; out.println(message); }
    assertTrue(parser.hasError());
    assertFalse(message.isEmpty());
    checkEachLineStartsWithLocation(message);
    checkExpected(message, expected, index);

    // test schema without DataLocation
    parser = schemaParserFromObjectsString(schemaText); // no location information
    message = parser.errorMessage();
    if (debug) { out.println(parser.schemasToString()) ; out.println(message); }
    assertTrue(parser.hasError());
    assertFalse(message.isEmpty());
    checkExpected(message, expected, index);
  }

  private void checkExpected(String text, String[] contains)
  {
    checkExpected(text, contains, 0);
  }

  private void checkExpected(String text, String[] contains, int startIndex)
  {
    for (int i = startIndex; i < contains.length; i++)
    {
      String expected = contains[i];
      assertTrue(text.contains(expected), text + " should contain \"" + expected + "\"");
    }
  }

  @Test
  public void testPretty() throws UnsupportedEncodingException, IOException
  {
    String schemaText =
      "{ " +
      "  \"type\": \"record\"," +
      "  \"name\": \"LongList\"," +
      "  \"fields\" : [" +
      "    {\"name\": \"value\", \"type\": \"long\"}, " +
      "    {\"name\": \"next\", \"type\": [\"LongList\", \"null\"]} " +
      "  ]" +
      "}";

    String br = System.getProperty("line.separator");
    Object[][] testData =
    {
        {
          JsonBuilder.Pretty.COMPACT,
          "{\"type\":\"record\",\"name\":\"LongList\",\"fields\":[{\"name\":\"value\",\"type\":\"long\"},{\"name\":\"next\",\"type\":[\"LongList\",\"null\"]}]}"
        },
        {
          JsonBuilder.Pretty.SPACES,
          "{ \"type\" : \"record\", \"name\" : \"LongList\", \"fields\" : [ { \"name\" : \"value\", \"type\" : \"long\" }, { \"name\" : \"next\", \"type\" : [ \"LongList\", \"null\" ] } ] }"
        },
        {
          JsonBuilder.Pretty.INDENTED,
          "{" + br +
          "  \"type\" : \"record\"," + br +
          "  \"name\" : \"LongList\"," + br +
          "  \"fields\" : [ {" + br +
          "    \"name\" : \"value\"," + br +
          "    \"type\" : \"long\"" + br +
          "  }, {" + br +
          "    \"name\" : \"next\"," + br +
          "    \"type\" : [ \"LongList\", \"null\" ]" + br +
          "  } ]" + br +
          "}"
        },
    };

    for (Object[] input : testData)
    {
      PegasusSchemaParser parser = schemaParserFromString(schemaText);
      String result;
      if (parser.hasError())
      {
        result = "ERROR: " + parser.errorMessage();
      }
      else
      {
        result = SchemaToJsonEncoder.schemasToJson(parser.topLevelDataSchemas(), (JsonBuilder.Pretty) input[0]);
      }
      if (input[1] != null)
      {
        assertEquals(result, input[1]);
      }
      else
      {
        out.println(result);
      }
    }
  }

  @Test
  public void testEncodeOriginal() throws IOException
  {
      SchemaParser parser = new SchemaParser();
      parser.parse("{ \"type\": \"record\", \"name\": \"ReferencedFieldType\", \"fields\" : []}");
      parser.parse("{ \"type\": \"record\", \"name\": \"ReferencedMapValuesType\", \"fields\" : []}");
      parser.parse("{ \"type\": \"record\", \"name\": \"ReferencedArrayItemsType\", \"fields\" : []}");
      parser.parse("{ \"type\": \"record\", \"name\": \"ReferencedTyperefType\", \"fields\" : []}");
      parser.parse("{ \"type\": \"record\", \"name\": \"ReferencedUnionMemberType\", \"fields\" : []}");
      String originalSchemaJson = "{ " +
          "  \"type\": \"record\"," +
          "  \"name\": \"Original\"," +
          "  \"namespace\": \"org.example\"," +
          "  \"package\": \"org.example.packaged\"," +
          "  \"doc\": \"A record\"," +
          "  \"java\": { \"class\": \"org.example.X\", \"coercerClass\": \"org.example.XCoercer\" }," +
          "  \"fields\" : [" +
          "    {\"name\": \"inlineFieldType\", \"type\": { \"type\": \"record\", \"name\": \"Inline\", \"fields\": [] }}," +
          "    {\"name\": \"inlineMapValueType\", \"type\": { \"type\": \"map\", \"values\": { \"type\": \"record\", \"name\": \"InlineValue\", \"fields\": [] } }}," +
          "    {\"name\": \"inlineArrayItemsType\", \"type\": { \"type\": \"array\", \"items\": { \"type\": \"record\", \"name\": \"InlineItems\", \"fields\": [] } }}," +
          "    {\"name\": \"inlineTyperefType\", \"type\": { \"type\": \"typeref\", \"name\": \"InlinedTyperef\", \"ref\": { \"type\": \"record\", \"name\": \"InlineRef\", \"fields\": [] } }}," +
          "    {\"name\": \"inlineUnionType\", \"type\": [ \"string\", { \"type\": \"record\", \"name\": \"InlineUnionMember\", \"fields\": [] } ]}," +
          "    {\"name\": \"referencedFieldType\", \"type\": \"ReferencedFieldType\" }," +
          "    {\"name\": \"referencedMapValueType\", \"type\": { \"type\": \"map\", \"values\": \"ReferencedMapValuesType\" }}," +
          "    {\"name\": \"referencedArrayItemsType\", \"type\": { \"type\": \"array\", \"items\": \"ReferencedArrayItemsType\" }}," +
          "    {\"name\": \"referencedTyperefType\", \"type\": { \"type\": \"typeref\", \"name\": \"ReferencedTyperef\", \"ref\": \"ReferencedTyperefType\" }}," +
          "    {\"name\": \"referencedUnionType\", \"type\": [ \"string\", \"ReferencedUnionMemberType\" ]}" +
          "  ]" +
          "}";
      parser.parse(originalSchemaJson);
      DataSchema originalSchema = parser.topLevelDataSchemas().get(parser.topLevelDataSchemas().size()-1);
      JsonBuilder originalBuilder = new JsonBuilder(JsonBuilder.Pretty.INDENTED);
      SchemaToJsonEncoder originalEncoder = new SchemaToJsonEncoder(originalBuilder);
      originalEncoder.setTypeReferenceFormat(SchemaToJsonEncoder.TypeReferenceFormat.PRESERVE);
      originalEncoder.encode(originalSchema);
      JacksonDataCodec codec = new JacksonDataCodec();
      DataMap original = codec.readMap(new StringReader(originalSchemaJson));
      DataMap roundTripped = codec.readMap(new StringReader(originalBuilder.result()));
      assertEquals(original, roundTripped);
  }

  @Test
  public void testFieldDefaultsAndUnionMemberKeys() throws IOException
  {
    String schemaText =
      "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : \n" +
      "[ { \"name\" : \"bar\", \"type\" : { \"name\" : \"barType\", \"type\" : \"record\", \"fields\" : [ \n" +
      "{ \"name\" : \"boolean\", \"type\" : \"boolean\", \"default\" : true }, \n" +
      "{ \"name\" : \"int\", \"type\" : \"int\", \"default\" : -1 }, \n" +
      "{ \"name\" : \"long\", \"type\" : \"long\", \"default\" : -2 }, \n" +
      "{ \"name\" : \"float\", \"type\" : \"float\", \"default\" : -3.0 }, \n" +
      "{ \"name\" : \"double\", \"type\" : \"double\", \"default\" : -4.0 }, \n" +
      "{ \"name\" : \"string\", \"type\" : \"string\", \"default\" : \"default_string\" }, \n" +
      "{ \"name\" : \"bytes\", \"type\" : \"bytes\", \"default\" : \"default_bytes\" }, \n" +
      "{ \"name\" : \"array\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" }, \"default\" : [ -1, -2, -3, -4 ] }, \n" +
      "{ \"name\" : \"enum\", \"type\" : { \"type\" : \"enum\", \"name\" : \"enumType\", \"symbols\" : [ \"apple\", \"orange\", \"banana\" ] }, \"default\" : \"apple\" }, \n" +
      "{ \"name\" : \"fixed\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"fixedType\", \"size\" : 4 }, \"default\" : \"1234\" }, \n" +
      "{ \"name\" : \"map\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" }, \"default\" : { \"key1\" : -5 } }, \n" +
      "{ \"name\" : \"record\", \"type\" : { \"type\" : \"record\", \"name\" : \"recordType\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] }, \"default\" : { \"int\" : -6 } }, \n" +
      "{ \"name\" : \"union\", \"type\" : [ \"int\", \"recordType\", \"enumType\", \"fixedType\" ], \"default\" : { \"enumType\" : \"orange\"} }, \n" +
      "{ \"name\" : \"unionWithNull\", \"type\" : [ \"null\", \"enumType\", \"fixedType\" ], \"default\" : null } \n" +
      "] } } ] }";

    String key = "bar";

    // Test default values.

    Object defaultInput[][] =
    {
        {
          "boolean",
          true
        },
        {
          "int",
          -1
        },
        {
          "long",
          -2L
        },
        {
          "float",
          -3.0f
        },
        {
          "double",
          -4.0
        },
        {
          "string",
          "default_string"
        },
        {
          "bytes",
          ByteString.copyAvroString("default_bytes", false)
        },
        {
          "array",
          new DataList(asList(-1, -2, -3, -4))
        },
        {
          "enum",
          "apple"
        },
        {
          "fixed",
          ByteString.copyAvroString("1234", false)
        },
        {
          "map",
          new DataMap(asMap("key1", -5))
        },
        {
          "record",
          new DataMap(asMap("int", -6))
        },
        {
          "union",
          new DataMap(asMap("enumType", "orange"))
        },
        {
          "unionWithNull",
          Data.NULL
        }
    };

    DataSchema schema = dataSchemaFromString(schemaText);
    RecordDataSchema fooSchema = (RecordDataSchema) schema;
    RecordDataSchema.Field barField = fooSchema.getField(key);
    RecordDataSchema barSchema = (RecordDataSchema) barField.getType();

    for (Object[] pair : defaultInput)
    {
      String targetKey = (String) pair[0];
      RecordDataSchema.Field targetField = barSchema.getField(targetKey);
      assertEquals(pair[1], targetField.getDefault());
    }

    // Test default values.

    Object unionMemberKeyInput[][] =
    {
        {
          "boolean",
          "boolean"
        },
        {
          "int",
          "int"
        },
        {
          "long",
          "long"
        },
        {
          "float",
          "float"
        },
        {
          "double",
          "double"
        },
        {
          "string",
          "string"
        },
        {
          "bytes",
          "bytes"
        },
        {
          "array",
          "array"
        },
        {
          "enum",
          "enumType"
        },
        {
          "fixed",
          "fixedType"
        },
        {
          "map",
          "map"
        },
        {
          "record",
          "recordType"
        },
        {
          "union",
          "union"
        },
        {
          "unionWithNull",
          "union"
        }
    };

    for (Object[] pair : unionMemberKeyInput)
    {
      String targetKey = (String) pair[0];
      RecordDataSchema.Field targetField = barSchema.getField(targetKey);
      assertEquals(pair[1], targetField.getType().getUnionMemberKey());
    }
  }

  @Test
  public void testOptional() throws IOException
  {
    Object[][] optionalFlagInputs =
    {
      { // optional is true
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"optional\" : true } ] }",
        true
      },
      { // optional is false
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\", \"optional\" : false } ] }",
        false
      },
      { // optional is not specified
        "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"int\" } ] }",
        false
      }
    };

    // test optional flag
    for (Object[] pair : optionalFlagInputs)
    {
      String schemaText = (String) pair[0];
      boolean optional = (Boolean) pair[1];
      RecordDataSchema schema = (RecordDataSchema) dataSchemaFromString(schemaText);

      RecordDataSchema.Field targetField = schema.getField("bar");
      assertEquals(optional, targetField.getOptional());
    }
  }

  @Test
  public void testAliases() throws IOException
  {
    String[][] inputs =
    {
        {
          "{ \"name\" : \"a\", \"type\" : \"fixed\", \"size\" : 5, \"aliases\" : [ \"b\", \"c.d\" ] }",
          "a",
          "b",
          "c.d"
        },
        {
          "{ \"name\" : \"a.a\", \"type\" : \"fixed\", \"size\" : 5, \"aliases\" : [ \"b\", \"c.d\" ] }",
          "a.a",
          "a.b",
          "c.d"
        },
        {
          "{ \"name\" : \"a\", \"type\" : \"record\", \"aliases\" : [ \"b\", \"c.d\" ], \"fields\" : [ " +
              "{ \"name\" : \"a1\", \"type\" : \"a\" }," +
              "{ \"name\" : \"a2\", \"type\" : \"b\" }," +
              "{ \"name\" : \"a3\", \"type\" : \"c.d\" }" +
          "] }",
          "a",
          "b",
          "c.d"
        }
    };

    boolean debug = false;

    for (String[] input : inputs)
    {
      String schema = input[0];
      if (debug) out.println(schema);
      PegasusSchemaParser parser = schemaParserFromString(schema);
      if (debug) out.println(parser.errorMessage());
      assertFalse(parser.hasError());
      for (int i = 1; i < input.length; ++i)
      {
        DataSchema found = parser.lookupName(input[i]);
        assertTrue(found != null);
      }
    }
  }

  @Test
  public void testProperties() throws IOException
  {
    for (PrimitiveDataSchema schema: DataSchemaUtil._TYPE_STRING_TO_PRIMITIVE_DATA_SCHEMA_MAP.values())
    {
      assertTrue(schema.getProperties().isEmpty());
      assertEquals(schema.getProperties().size(), 0);
      assertEquals(schema.getProperties(), Collections.emptyMap());
      testPropertiesUnmodifiable(schema);
    }

    Object[][] inputs =
    {
        {
          // fixed, string valued property
          "{ \"name\" : \"a\", \"type\" : \"fixed\", \"size\" : 5, \"p1\" : \"string\" }",
          asMap("p1", "string")
        },
        {
          // fixed, integer valued property
          "{ \"name\" : \"a\", \"type\" : \"fixed\", \"size\" : 5, \"p1\" : 2 }",
          asMap("p1", 2)
        },
        {
          // fixed, float valued property
          "{ \"name\" : \"a\", \"type\" : \"fixed\", \"size\" : 5, \"p1\" : 0.5 }",
          asMap("p1", 0.5)
        },
        {
          // fixed, boolean valued property
          "{ \"name\" : \"a\", \"type\" : \"fixed\", \"size\" : 5, \"p1\" : true }",
          asMap("p1", true)
        },
        {
          // record, list valued property
          "{ \"name\" : \"a\", \"type\" : \"record\", \"p1\" : [ 1, 2.0, \"s\" ], \"fields\" : [] }",
          asMap("p1", asList(1, 2.0, "s"))
        },
        {
          // record, object values property
          "{ \"name\" : \"a\", \"type\" : \"record\", \"p1\" : { \"p11\" : \"v11\", \"p12\" : \"v12\" }, \"fields\" : [] }",
          asMap("p1", asMap("p11", "v11", "p12", "v12"))
        },
        {
          // fixed, more than one property
          "{ \"name\" : \"a.a\", \"type\" : \"fixed\", \"size\" : 5, \"p1\" : 1, \"p2\" : 2.0 }",
          asMap("p1", 1, "p2", 2.0)
        },
        {
          // enum, more than one property
          "{ \"name\" : \"fruits\", \"type\" : \"enum\", \"symbols\" : [ \"apple\", \"orange\" ], \"s1\" : \"s1\", \"i2\" : 2 }",
          asMap("s1", "s1", "i2", 2)
        }
    };

    boolean debug = false;

    for (Object[] input : inputs)
    {
      String schema = (String) input[0];
      if (debug) out.println(schema);
      PegasusSchemaParser parser = schemaParserFromString(schema);
      if (debug) out.println(parser.errorMessage());
      assertFalse(parser.hasError());
      Object expected = input[1];
      NamedDataSchema namedSchema = (NamedDataSchema) parser.topLevelDataSchemas().get(0);
      assertEquals(namedSchema.getProperties(), expected);
      testPropertiesUnmodifiable(namedSchema);
    }
  }

  private void testPropertiesUnmodifiable(DataSchema schema)
  {
    Map<String, Object> properties = schema.getProperties();
    Exception exc;
    try
    {
      exc = null;
      properties.put("a", "a");
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
  }

  @Test
  public void testBadSchemas() throws UnsupportedEncodingException, IOException
  {
    String[][] badInputs =
    {
        {
          // bad type
          "{ \"type\" : 4 }",
          "not a string"
        },
        {
          // bad name, empty string
          "{ \"type\" : \"fixed\", \"name\" : \"\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad name, starts with number
          "{ \"type\" : \"fixed\", \"name\" : \"67\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad name, 2nd component starts with number
          "{ \"type\" : \"fixed\", \"name\" : \"foo.67\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad namespace, starts with number
          "{ \"type\" : \"fixed\", \"name\" : \"foo\", \"namespace\" : \"67\", \"size\" : 4 }",
          "invalid namespace"
        },
        {
          // bad namespace, 2nd component starts with number
          "{ \"type\" : \"fixed\", \"name\" : \"foo\", \"namespace\" : \"bar.67\", \"size\" : 4 }",
          "invalid namespace"
        },
        {
          // bad alias, empty string
          "{ \"aliases\" : [ \"\" ], \"type\" : \"fixed\", \"name\" : \"foo\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad alias, starts with number
          "{ \"aliases\" : [ \"67\" ], \"type\" : \"fixed\", \"name\" : \"foo\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad alias, 2nd component starts with number
          "{ \"aliases\" : [ \"foo.67\" ], \"type\" : \"fixed\", \"name\" : \"foo\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad alias, starts with number
          "{ \"aliases\" : [ \"67.foo\" ], \"type\" : \"fixed\", \"name\" : \"foo\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad alias, bad alias not 1st alias
          "{ \"aliases\" : [ \"bar\", \"foo.bar\", \"67.foo\" ], \"type\" : \"fixed\", \"name\" : \"foo\", \"size\" : 4 }",
          "invalid name"
        },
        {
          // bad properties
          "{ \"name\" : \"foo\", \"type\" : \"record\", \"fields\" : [], \"p1\" : null }",
          "is a property and its value must not be null"
        },
        {
          // redefine boolean
          "{ \"type\" : \"fixed\", \"name\" : \"boolean\", \"size\" : 4 }",
          "cannot be redefined"
        },
        {
          // redefine int
          "{ \"type\" : \"fixed\", \"name\" : \"int\", \"size\" : 4 }",
          "cannot be redefined"
        },
        {
          // redefine long
          "{ \"type\" : \"fixed\", \"name\" : \"long\", \"size\" : 4 }",
          "cannot be redefined"
        },
        {
          // redefine float
          "{ \"type\" : \"fixed\", \"name\" : \"float\", \"size\" : 4 }",
          "cannot be redefined"
        },
        {
          // redefine double
          "{ \"type\" : \"fixed\", \"name\" : \"double\", \"size\" : 4 }",
          "cannot be redefined"
        },
        {
          // redefine bytes
          "{ \"type\" : \"fixed\", \"name\" : \"bytes\", \"size\" : 4 }",
          "cannot be redefined"
        },
        {
          // redefine string
          "{ \"type\" : \"fixed\", \"name\" : \"string\", \"size\" : 4 }",
          "cannot be redefined"
        },
        {
          // redefine the same name
          "{ \"type\" : \"fixed\", \"name\" : \"fixed4\", \"size\" : 4 }" +
          "{ \"type\" : \"fixed\", \"name\" : \"fixed4\", \"size\" : 4 }",
          "already defined"
        },
        {
          // redefine the same name with namespace
          "{ \"type\" : \"fixed\", \"name\" : \"foo.fixed4\", \"size\" : 4 }" +
          "{ \"type\" : \"fixed\", \"name\" : \"fixed4\", \"namespace\" : \"foo\", \"size\" : 4 }",
          "already defined"
        },
        {
          // array must have items
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"type\" : \"array\" } } \n" +
          "] }",
          "is required but it is not present"
        },
        {
          // array must not have name
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"name\" : \"notgood\", \"type\" : \"array\" } } \n" +
          "] }",
          "must not have name"
        },
        {
          // array must not have namespace
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"namespace\" : \"notgood\", \"type\" : \"array\" } } \n" +
          "] }",
          "must not have namespace"
        },
        {
          // array must not have aliases
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"aliases\" : [ ], \"type\" : \"array\" } } \n" +
          "] }",
          "must not have aliases"
        },
        {
          // enum must have name
          "{ \"type\" : \"enum\", \"symbols\" : [ \"apple\" ] }",
          "is required but it is not present"
        },
        {
          // enum must have symbols
          "{ \"type\" : \"enum\", \"name\" : \"foo\" }",
          "is required but it is not present"
        },
        {
          // enum with invalid symbols
          "{ \"type\" : \"enum\", \"name\" : \"foo\", \"symbols\" : \"apple\" }",
          "is not an array"
        },
        {
          // enum with invalid symbols
          "{ \"type\" : \"enum\", \"name\" : \"foo\", \"symbols\" : [ 67 ] }",
          "is not a string"
        },
        {
          // enum with invalid symbols
          "{ \"type\" : \"enum\", \"name\" : \"foo\", \"symbols\" : [ \"67\" ] }",
          "is an invalid enum symbol"
        },
        {
          // enum with duplicate symbols
          "{ \"type\" : \"enum\", \"name\" : \"foo\", \"symbols\" : [ \"apple\", \"banana\", \"apple\" ] }",
          "defined more than once in enum symbols"
        },
        {
          // enum with invalid symbol docs
          "{ \"type\" : \"enum\", \"name\" : \"foo\", \"symbols\" : [ \"apple\", \"banana\" ], \"symbolDocs\" : \"docs\" }",
          "is not a map"
        },
        {
          // enum with invalid symbol docs
          "{ \"type\" : \"enum\", \"name\" : \"foo\", \"symbols\" : [ \"apple\", \"banana\" ], \"symbolDocs\" : { \"apple\" : \"doc_apple\", \"banana\" : 5 } }",
          "symbol has an invalid documentation value"
        },
        {
          // enum with invalid symbol docs
          "{ \"type\" : \"enum\", \"name\" : \"foo\", \"symbols\" : [ \"apple\", \"banana\" ], \"symbolDocs\" : { \"apple\" : \"doc_apple\", \"orange\" : \"doc_orange\" } }",
          "This symbol does not exist"
        },
        {
          // fixed must have name
          "{ \"type\" : \"fixed\", \"size\" : 4 }",
          "is required but it is not present"
        },
        {
          // fixed must have size
          "{ \"type\" : \"fixed\", \"name\" : \"foo\" }",
          "is required but it is not present"
        },
        {
          // fixed size must not be negative
          "{ \"type\" : \"fixed\", \"name\" : \"foo\", \"size\" : -1 }",
          "size must not be negative"
        },
        {
          // map must have values
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"type\" : \"map\" } } \n" +
          "] }",
          "is required but it is not present"
        },
        {
          // map must not have name
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"name\" : \"notgood\", \"values\" : \"int\" } } \n" +
          "] }",
          "must not have name"
        },
        {
          // map must not have namespace
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"namespace\" : \"notgood\", \"values\" : \"int\" } } \n" +
          "] }",
          "must not have namespace"
        },
        {
          // map must not have aliases
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : { \"type\" : \"map\", \"aliases\" : [ ], \"values\" : \"int\" } } \n" +
          "] }",
          "must not have aliases"
        },
        {
          // record must have name
          "{ \"type\" : \"record\", \"fields\" : [ ] }",
          "is required but it is not present"
        },
        {
          // record must have fields
          "{ \"type\" : \"record\", \"name\" : \"foo\" }",
          "is required but it is not present"
        },
        {
          // field must have name
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"type\" : \"int\" } \n" +
          "] }",
          "is required but it is not present"
        },
        {
          // field must have type
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\" } \n" +
          "] }",
          "is required but it is not present"
        },
        {
          // field name defined more than once
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : \"int\" }, \n" +
          "{ \"name\" : \"bar\", \"type\" : \"string\" } \n" +
          "] }",
          "defined more than once"
        },
        {
          // field type invalid
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : \"undefined\" } \n" +
          "] }",
          "cannot be resolved"
        },
        {
          // field order invalid
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"bar\", \"type\" : \"int\", \"order\" : \"xxx\" } \n" +
          "] }",
          "invalid sort order"
        },
        {
          // union within union
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"u1\", \"type\" : [ \"null\", \"int\", [ \"null\", \"string\" ] ] } \n" +
          "] }",
          "union cannot be inside another union"
        },
        {
          // union with duplicate types
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"u1\", \"type\" : [ \"int\", \"string\", \"int\" ] } \n" +
          "] }",
          "appears more than once in a union"
        },
        {
          // union with duplicate named types
          "{ \"type\" : \"fixed\", \"name\" : \"fixed4\", \"size\" : 4 } \n" +
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"u1\", \"type\" : [ \"fixed4\", \"string\", \"int\", \"fixed4\" ] } \n" +
          "] }",
          "appears more than once in a union"
        },
        {
          // union with member that cannot be resolved
          "{ \"type\" : \"record\", \"name\" : \"foo\", \"fields\" : [ \n" +
          "{ \"name\" : \"u1\", \"type\" : [ \"undefined\", \"string\", \"int\" ] } \n" +
          "] }",
          "cannot be resolved"
        },
        {
          // circular typeref - direct
          "{ \"type\" : \"typeref\", \"name\" : \"foo\", \"ref\" : \"foo\" }",
          "cannot be resolved"
        },
        {
          // circular typeref - indirect
          "{ \"type\" : \"typeref\", \"name\" : \"foo\", \"ref\" : { \"type\" : \"array\", \"items\" : \"foo\" } }",
          "cannot be resolved"
        },
        {
          // union with typeref and same type appears twice in union
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"bar\", " +
          "      \"type\" : [ " +
          "        { \"type\" : \"typeref\", \"name\" : \"IntRef\", \"ref\" : \"int\" }, " +
          "        \"int\" " +
          "      ] " +
          "    } "+
          "  ] " +
          "}",
          "appears more than once in a union"
        },
        {
          // union with typeref of union as member
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"bar\", " +
          "      \"type\" : [ " +
          "        { \"type\" : \"typeref\", \"name\" : \"unionRef\", \"ref\" : [ \"int\", \"string\" ] }, " +
          "        \"int\" " +
          "      ] " +
          "    } "+
          "  ] " +
          "}",
          "union cannot be inside another union"
        },
        {
          // typeref with with invalid referenced type
          "{ " +
          "  \"type\" : \"typeref\", " +
          "  \"name\" : \"foo\", " +
          "  \"ref\" : \"xxx\" " +
          "}",
          "\"xxx\" cannot be resolved"
        },
        {
          // array with invalid items type
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"field1\", " +
          "      \"type\" : { " +
          "         \"type\" : \"array\", " +
          "         \"items\" : \"xxx\" " +
          "      }" +
          "    } " +
          "  ]" +
          "}",
          "\"xxx\" cannot be resolved"
        },
        {
          // map with invalid values type
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"field1\", " +
          "      \"type\" : { " +
          "         \"type\" : \"map\", " +
          "         \"values\" : \"xxx\" " +
          "      }" +
          "    } " +
          "  ]" +
          "}",
          "\"xxx\" cannot be resolved"
        },
        {
          // field with invalid type
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"field1\", " +
          "      \"type\" : \"xxx\" " +
          "    } " +
          "  ]" +
          "}",
          "\"xxx\" cannot be resolved"
        },
        {
          // invalid referenced type
          // duplicate definition of type
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"field1\", " +
          "      \"type\" : { " +
          "         \"type\" : \"typeref\", " +
          "         \"name\" : \"ref1\", " +
          "         \"ref\" : \"xxx\" " +
          "      }" +
          "    }, " +
          "    { " +
          "      \"name\" : \"field2\", " +
          "      \"type\" : { " +
          "         \"type\" : \"typeref\", " +
          "         \"name\" : \"ref1\", " +
          "         \"ref\" : \"int\" " +
          "      }" +
          "    } " +
          "  ]" +
          "}",
          "\"ref1\" already defined as { \"type\" : \"typeref\", \"name\" : \"ref1\", \"ref\" : \"null\" }",
          "\"xxx\" cannot be resolved"
        },
        // include of non-record type
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"include\" : [ \"int\" ], " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"f1\", " +
          "      \"type\" : \"double\" " +
          "    } "+
          "  ] " +
          "}",
          "\"foo\" cannot include \"int\" because it is not a record"
        },
        // include with duplicate fields
        {
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"bar\", " +
          "  \"fields\" : [ " +
          "    { \"name\" : \"b1\", \"type\" : \"int\" } " +
          "  ] " +
          "} " +
          "{ " +
          "  \"type\" : \"record\", " +
          "  \"name\" : \"foo\", " +
          "  \"include\" : [ " +
          "    \"bar\" " +
          "  ], " +
          "  \"fields\" : [ " +
          "    { " +
          "      \"name\" : \"b1\", " +
          "      \"type\" : \"double\" " +
          "    } "+
          "  ] " +
          "}",
          "Field \"b1\" defined more than once, with \"int\" defined in \"bar\" and \"double\" defined in \"foo\""
        },
      // include non-existent schema
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"foo\", " +
        "  \"include\" : [ " +
        "    \"crap\" " +
        "  ], " +
        "  \"fields\" : [ " +
        "  ] " +
        "}",
        "\"crap\" cannot be resolved"
      },
    };

    for (String[] input : badInputs)
    {
      int i = 0;
      String schema = input[i++];
      checkBadSchema(schema, input, i);
    }
  }

  @Test
  public void testEnumDataSchema() throws Exception
  {
    final String schemaString = "{ \"type\" : \"enum\", \"name\" : \"numbers\", \"symbols\" : [ \"ONE\", \"TWO\", \"THREE\", \"FOUR\", \"FIVE\"], \"symbolDocs\" : { \"FIVE\" : \"DOC_FIVE\", \"ONE\" : \"DOC_ONE\" } }";
    PegasusSchemaParser parser = schemaParserFromString(schemaString);
    EnumDataSchema schema = (EnumDataSchema)parser.topLevelDataSchemas().get(0);

    String[] orderedSymbols = {"ONE", "TWO", "THREE", "FOUR", "FIVE" };
    for (int i = 0; i < orderedSymbols.length; ++i)
    {
      Assert.assertEquals(schema.index(orderedSymbols[i]), i);
      Assert.assertTrue(schema.contains(orderedSymbols[i]));
    }

    String[] missingSymbols = {"SIX", "SEVEN", "EIGHT"};
    for (String missingSymbol : missingSymbols)
    {
      Assert.assertFalse(schema.contains(missingSymbol));
      Assert.assertEquals(schema.index(missingSymbol), -1);
    }

    Assert.assertEquals(schema.getSymbols(), Arrays.asList(orderedSymbols));

    String[] symbolDocKeys = {"ONE", "FIVE"};
    for (String symbolDocKey : symbolDocKeys)
    {
      Assert.assertTrue(schema.getSymbolDocs().containsKey(symbolDocKey) && schema.getSymbolDocs().get(symbolDocKey).equals("DOC_" + symbolDocKey));
    }

    String[] missingSymbolDocs = {"TWO", "THREE", "FOUR"};
    for (String missingSymbol : missingSymbols)
    {
      Assert.assertFalse(schema.getSymbolDocs().containsKey(missingSymbol));
    }
  }

  @Test
  public void testNameLookup() throws IOException
  {
    String[][] inputs = {
      {
        // refer to Ref within Record
        "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : \"string\" }\n" +
        "{ \"type\" : \"record\", \"name\" : \"Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : \"Ref\" } ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : \"string\" }",
        "{ \"type\" : \"record\", \"name\" : \"Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : \"string\" } } ] }",
      },
      {
        // refer to a.b.Ref within Record
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }\n" +
        "{ \"type\" : \"record\", \"name\" : \"Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : \"a.b.Ref\" } ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }",
        "{ \"type\" : \"record\", \"name\" : \"Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" } } ] }",
      },
      {
        // refer to Ref within a.b.Record
        "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : \"string\" }\n" +
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : \"Ref\" } ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : \"string\" }",
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"Ref\", \"namespace\" : \"\", \"ref\" : \"string\" } } ] }",
      },
      {
        // refer to a.b.Ref within a.b.Record using Ref (not fullname)
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }\n" +
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : \"Ref\" } ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }",
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" } } ] }",
      },
      {
        // refer to a.b.Ref within a.b.Record using a.b.Ref (fullname)
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }\n" +
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : \"a.b.Ref\" } ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }",
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" } } ] }",
      },
      {
        // both Ref and a.b.Ref present, refer a.b.Ref within a.b.Record using Ref (not fulname)
        "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : \"string\" }\n" +
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }\n" +
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : \"Ref\" } ] }",
        "{ \"type\" : \"typeref\", \"name\" : \"Ref\", \"ref\" : \"string\" }",
        "{ \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" }",
        "{ \"type\" : \"record\", \"name\" : \"a.b.Record\", \"fields\" : [ { \"name\" : \"ref\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"a.b.Ref\", \"ref\" : \"string\" } } ] }",
      },
    };

    for (String[] row : inputs)
    {
      int i = 0;
      String schemaText = row[i++];
      PegasusSchemaParser parser = schemaParserFromString(schemaText);
      assertFalse(parser.hasError(), parser.errorMessage());
      List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();
      for (DataSchema schema : topLevelSchemas)
      {
        String expected = row[i++];
        DataSchema schemaFromExpected = dataSchemaFromString(expected);
        assertEquals(schema, schemaFromExpected);
      }
    }
  }
}
