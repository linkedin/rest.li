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

package com.linkedin.data.transform.patch.validator;

import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.DataElementUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.Validator;
import com.linkedin.data.schema.validator.ValidatorContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asList;
import static com.linkedin.data.TestUtil.dataMapFromString;
import static com.linkedin.data.TestUtil.dataSchemaFromString;
import static com.linkedin.data.TestUtil.out;
import static com.linkedin.data.transform.patch.validator.PatchFilterValidator.Mode;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;


public class TestPatchFilterValidator
{
  private static boolean debug = false;

  public class VisitedValidator implements Validator
  {
    public List<String> _visitedPaths = new ArrayList<>();

    public void validate(ValidatorContext ctx)
    {
      DataElement element = ctx.dataElement();
      _visitedPaths.add(element.pathAsString());
    }
  }

  @Test
  public void testPatchFilterValidator() throws IOException
  {
    String schemaText =
      "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"Foo\",\n" +
      "  \"fields\" : [\n"+
      "    { \"name\" : \"fooInt\", \"type\" : \"int\", \"optional\" : true },\n" +
      "    { \"name\" : \"fooString\", \"type\" : \"string\", \"optional\" : true },\n" +
      "    {\n" +
      "      \"name\" : \"bar\",\n" +
      "      \"type\" : {\n" +
      "        \"type\" : \"record\",\n" +
      "        \"name\" : \"Bar\",\n" +
      "        \"fields\" : [\n" +
      "          { \"name\" : \"barInt\", \"type\" : \"int\", \"optional\" : true },\n" +
      "          { \"name\" : \"barString\", \"type\" : \"string\", \"optional\" : true },\n" +
      "          {\n" +
      "            \"name\" : \"baz\",\n" +
      "            \"type\" : {\n" +
      "               \"type\" : \"record\",\n" +
      "               \"name\" : \"Baz\",\n" +
      "               \"fields\" : [\n" +
      "                { \"name\" : \"bazInt\", \"type\" : \"int\", \"optional\" : true },\n" +
      "                { \"name\" : \"bazString\", \"type\" : \"string\", \"optional\" : true }\n" +
      "              ]\n" +
      "            },\n" +
      "            \"optional\" : true\n" +
      "          }\n" +
      "        ]\n" +
      "      },\n" +
      "      \"optional\" : true\n" +
      "    }\n" +
      "  ]\n" +
      "}\n";

    Object[][] inputs =
      {
        // input format
        // {
        //   1st string is the post patch DataMap.
        //   2nd string is the patch operations DataMap.
        //   3rd string is the path where patch is applied
        //   followed by a list of checks to be performed, each check is coded as a list of objects:
        //     1st object in list provides the Mode to used.
        //     followed by strings that are paths that should be contained within paths visited.
        // }
        {
          // deleted /fooInt
          "{ }",
          "{ \"$delete\" : [ \"fooInt\" ] }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, ""),
          asList(Mode.ANCESTOR_AND_SET, ""),
        },
        {
          // deleted /fooInt
          //   must not call validator for parents and ancestors not modified
          "{ \"fooString\" : \"excluded\", \"bar\" : { \"barInt\" : -1 } }",
          "{ \"$delete\" : [ \"fooInt\" ] }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, ""),
          asList(Mode.ANCESTOR_AND_SET, ""),
        },
        {
          // deleted /bar/barInt
          "{ \"bar\" : { } }",
          "{ \"bar\" : { \"$delete\" : [ \"barInt\" ] } }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, "/bar"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar"),
        },
        {
          // deleted /bar/barInt
          //   must not call validator for parents and ancestors not modified
          "{ \"fooInt\" : -1, \"fooString\" : \"excluded\", \"bar\" : { } }",
          "{ \"bar\" : { \"$delete\" : [ \"barInt\" ] } }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, "/bar"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar"),
        },
        {
          // deleted /bar/baz/bazInt
          "{ \"bar\" : { \"baz\" : { } } }",
          "{ \"bar\" : { \"baz\" : { \"$delete\" : [ \"bazInt\" ] } } }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, "/bar/baz"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz"),
        },
        {
          // deleted /bar/baz/bazInt
          //   must not call validator for parents and ancestors not modified
          "{ \"fooInt\" : -1, \"fooString\" : \"excluded\", \"bar\" : { \"barInt\" : -1, \"baz\" : { } } }",
          "{ \"bar\" : { \"baz\" : { \"$delete\" : [ \"bazInt\" ] } } }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, "/bar/baz"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz"),
        },
        {
          // deleted /bar/baz
          "{ \"bar\" : { } }",
          "{ \"bar\" : { \"$delete\" : [ \"baz\" ] } }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, "/bar"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar"),
        },
        {
          // deleted /bar/baz
          //   must not call validator for parents and ancestors not modified
          "{ \"fooInt\" : -1, \"fooString\" : \"excluded\", \"bar\" : { \"barInt\" : -1 } }",
          "{ \"bar\" : { \"$delete\" : [ \"baz\" ] } }",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET, "/bar"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar"),
        },
        {
          // set /fooInt (set field in root record)
          "{ \"fooInt\" : 2 }",
          "{ \"$set\" : { \"fooInt\" : 2 } }",
          "",
          asList(Mode.SET_ONLY, "/fooInt"),
          asList(Mode.PARENT_AND_SET, "", "/fooInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/fooInt"),
        },
        {
          // set /fooInt (set field in root record)
          //   must not call validator for parents and ancestors not modified
          "{ \"fooInt\" : 2, \"fooString\" : \"excluded\", \"bar\" : { \"baz\" : { } } }",
          "{ \"$set\" : { \"fooInt\" : 2 } }",
          "",
          asList(Mode.SET_ONLY, "/fooInt"),
          asList(Mode.PARENT_AND_SET, "", "/fooInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/fooInt"),
        },
        {
          // set /bar/barInt (set field in nested child record)
          //   must not call validator for parents and ancestors not modified
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excluded\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : 2,\n" +
          "    \"barString\" : \"excluded\",\n" +
          "    \"baz\" : {}\n" +
          "  }\n" +
          "}",
          "{ \"bar\" : { \"$set\" : { \"barInt\" : 2 } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/barInt"),
          asList(Mode.PARENT_AND_SET, "/bar", "/bar/barInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/barInt"),
        },
        {
          // set /bar/barString
          //   must not call validator for parents and ancestors not modified
          "{\n" +
          "  \"fooInt\" : -1,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"barString\" : \"x\",\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"excludes\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{ \"bar\" : { \"$set\" : { \"barString\" : \"x\" } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/barString"),
          asList(Mode.PARENT_AND_SET, "/bar", "/bar/barString"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/barString"),
        },
        {
          // set /bar/baz/bazInt (set field in nested grandchild record)
          "{ \"bar\" : { \"baz\" : { \"bazInt\" : 2 } } }",
          "{ \"bar\" : { \"baz\" : { \"$set\" : { \"bazInt\" : 2 } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // set /bar/baz/bazInt (set field in nested grandchild record)
          //   must not call validator for parents and ancestors not modified
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excluded\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : 2,\n" +
          "    \"barString\" : \"excluded\",\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"excluded\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{ \"bar\" : { \"baz\" : { \"$set\" : { \"bazInt\" : 2 } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // set /bar/baz/bazString
          //   must not call validator for parents and ancestors not modified
          "{\n" +
          "  \"fooInt\" : -1,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"excludes\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{ \"bar\" : { \"baz\" : { \"$set\" : { \"bazInt\" : 2 } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // set /bar (set record in root record)
          "{ \"bar\" : { \"baz\" : { \"bazInt\" : 2 } } }",
          "{ \"$set\" : { \"bar\" : { \"baz\" : { \"bazInt\" : 2 } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar", "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET,  "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // set /bar (set field in nested grandchild record)
          //   must not call validator for parents and ancestors not modified
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excluded\",\n" +
          "  \"bar\" : {\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{ \"bar\" : { \"baz\" : { \"$set\" : { \"bazInt\" : 2 } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // set /bar/baz (set record in nested child record)
          "{ \"bar\" : { \"baz\" : { \"bazInt\" : 2 } } }",
          "{ \"bar\" : { \"$set\" : { \"baz\" : { \"bazInt\" : 2 } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar", "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // set /bar/baz (set field in nested grandchild record)
          //   must not call validator for parents and ancestors not modified
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excluded\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : 2,\n" +
          "    \"barString\" : \"excluded\",\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{ \"bar\" : { \"$set\" : { \"baz\" : { \"bazInt\" : 2 } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar", "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // set /bar/baz
          //   must not call validator for parents and ancestors not modified
          "{\n" +
          "  \"fooInt\" : -1,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"barString\" : \"excludes\",\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"x\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{ \"bar\" : { \"$set\" : { \"baz\" : { \"bazInt\" : 2, \"bazString\" : \"x\" } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.PARENT_AND_SET, "/bar", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString")
        },
        {
          // set /fooInt, /fooString (set multiple fields in root record)
          "{ \"fooInt\" : 2, \"fooString\" : \"x\" }",
          "{ \"$set\" : { \"fooInt\" : 2, \"fooString\" : \"x\" } }",
          "",
          asList(Mode.SET_ONLY, "/fooInt", "/fooString"),
          asList(Mode.PARENT_AND_SET, "", "/fooInt", "/fooString"),
          asList(Mode.ANCESTOR_AND_SET, "", "/fooInt", "/fooString"),
        },
        {
          // set /bar/barInt, /bar/barString (set multiple fields in nested child record)
          "{ \"bar\" : { \"barInt\" : 2, \"barString\" : \"x\" } }",
          "{ \"bar\" : { \"$set\" : { \"barInt\" : 2, \"barString\" : \"x\" } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/barInt", "/bar/barString"),
          asList(Mode.PARENT_AND_SET, "/bar", "/bar/barInt", "/bar/barString"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/barInt", "/bar/barString"),
        },
        {
          // set /bar/baz/bazInt, /bar/baz/bazString (set multiple fields in nested grandchild record)
          "{ \"bar\" : { \"baz\" : { \"bazInt\" : 2, \"bazString\" : \"x\" } } }",
          "{ \"bar\" : { \"baz\" : { \"$set\" : { \"bazInt\" : 2, \"bazString\" : \"x\" } } } }",
          "",
          asList(Mode.SET_ONLY, "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.PARENT_AND_SET, "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
        },
        {
          // set /fooInt, /bar/barInt, /bar/baz/bazInt
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : 2,\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{\n" +
          "  \"$set\" : { \"fooInt\" : 2 },\n" +
          "  \"bar\" : {\n" +
          "    \"$set\" : { \"barInt\" : 2 },\n" +
          "    \"baz\" : {\n" +
          "      \"$set\" : { \"bazInt\" : 2 }\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "",
          asList(Mode.SET_ONLY, "/fooInt", "/bar/barInt", "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "", "/fooInt", "/bar", "/bar/barInt", "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/fooInt", "/bar", "/bar/barInt", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // nothing set
          "{\n" +
          "  \"fooInt\" : -1,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : -1,\n" +
          "      \"bazString\" : \"excludes\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{}",
          "",
          asList(Mode.SET_ONLY),
          asList(Mode.PARENT_AND_SET),
          asList(Mode.ANCESTOR_AND_SET),
        },
        {
          // set /fooInt, /bar/baz
          //   must call next validator for all children of /bar/baz
          //   must not call next validator for /fooString, /bar/barInt
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"x\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{\n" +
          "  \"$set\" : { \"fooInt\" : 2 },\n" +
          "  \"bar\" : {\n" +
          "    \"$set\" : {\n" +
          "      \"baz\" : { \"bazInt\" : 2, \"bazString\" : \"x\" }\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "",
          asList(Mode.SET_ONLY, "/fooInt", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.PARENT_AND_SET, "", "/fooInt", "/bar", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.ANCESTOR_AND_SET, "", "/fooInt", "/bar", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
        },
        {
          // patch start is /bar, set /bar/baz
          //   must call next validator for /bar/baz and descendants
          //   must not call next validator for /fooInt, /fooString, /bar/barInt
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"x\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{\n" +
          "  \"$set\" : {\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"x\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "/bar",
          asList(Mode.SET_ONLY, "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.PARENT_AND_SET, "/bar", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString"),
        },
        {
          // patch start is /bar, set /bar/baz/bazInt
          //   must call next validator for /bar/baz and descendants
          //   must not call next validator for /fooInt, /fooString, /bar/barInt
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"x\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{\n" +
          "  \"baz\" : {\n" +
          "    \"$set\" : {\n" +
          "      \"bazInt\" : 2\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "/bar",
          asList(Mode.SET_ONLY, "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
        {
          // patch start is /bar/baz, set /bar/baz/bazInt
          //   must call next validator for /bar/baz/bazInt
          //   must not call next validator for /fooInt, /fooString, /bar/barInt
          "{\n" +
          "  \"fooInt\" : 2,\n" +
          "  \"fooString\" : \"excludes\",\n" +
          "  \"bar\" : {\n" +
          "    \"barInt\" : -1,\n" +
          "    \"baz\" : {\n" +
          "      \"bazInt\" : 2,\n" +
          "      \"bazString\" : \"x\"\n" +
          "    }\n" +
          "  }\n" +
          "}",
          "{\n" +
          "  \"$set\" : { \"bazInt\" : 2 }\n" +
          "}",
          "/bar/baz",
          asList(Mode.SET_ONLY, "/bar/baz/bazInt"),
          asList(Mode.PARENT_AND_SET, "/bar/baz", "/bar/baz/bazInt"),
          asList(Mode.ANCESTOR_AND_SET, "", "/bar", "/bar/baz", "/bar/baz/bazInt"),
        },
      };

    DataSchema schema = dataSchemaFromString(schemaText);
    ValidationOptions options = new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.NORMAL);
    for (Object[] row : inputs)
    {
      String value = (String) row[0];
      String patch = (String) row[1];
      String patchPath = (String) row[2];
      DataMap valueMap = dataMapFromString(value);
      DataMap opMap = dataMapFromString(patch);
      if (debug)
      {
        out.println("Value: " + value);
        out.println("Patch: " + patch);
        if (patchPath.isEmpty() == false) out.println("PatchPath: " + patchPath);
      }
      for (int i = 3; i < row.length; i++)
      {
        VisitedValidator visitedValidator = new VisitedValidator();
        @SuppressWarnings("unchecked")
        List<Object> check = (List<Object>) row[i];
        Mode mode = (Mode) check.get(0);
        Validator validator;
        if (patchPath.isEmpty())
        {
          validator = new PatchFilterValidator(visitedValidator, opMap, mode);
        }
        else
        {
          DataElement patchElement = DataElementUtil.element(valueMap, schema, patchPath);
          assertNotSame(patchElement, null);
          validator = new PatchFilterValidator(visitedValidator, opMap, mode, patchElement);
        }
        ValidationResult result = ValidateDataAgainstSchema.validate(valueMap, schema, options, validator);
        if (debug)
        {
          out.println("Mode: " + mode);
          out.print("Result: " + result);
          out.println("VisitedPaths: " + visitedValidator._visitedPaths);
        }
        assertTrue(result.isValid());
        for (int j = 1; j < check.size(); j++)
        {
          assertTrue(visitedValidator._visitedPaths.contains(check.get(j)));
        }
        assertEquals(visitedValidator._visitedPaths.size(), check.size() - 1);
      }
    }
  }
}
