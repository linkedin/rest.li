package com.linkedin.data.schema.annotation;

import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.stream.Collectors.joining;


public class TestSchemaAnnotationProcessor
{
  public String simpleTestSchema =
      "record Test {" +
      "  @customAnnotation = {" +
      "    \"/f1/f2\": \"sth\"" +
      "  }" +
      "  f0: record A {" +
      "    f1: A" +
      "    f2: string" +
      "  }" +
      "  f1: union[int, array[string]]" +
      "}";

  public final String TEST_ANNOTATION_LABEL = "customAnnotation";

  @DataProvider
  public Object[][] denormalizedSchemaTestCases_invalid()
  {
    return new Object[][]{
        {
          "denormalizedsource/invalid/3_2_cyclic_simple_overrides_invalid.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer :: Found overrides that forms a cyclic-referencing: Overrides entry in traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee\" with its pathSpec value \"/name\" is pointing to the field with traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer\" and schema name \"com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer\", this is causing cyclic-referencing.\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee :: Overriding pathSpec defined /name does not point to a valid primitive field\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee :: Overriding pathSpec defined /employee/name does not point to a valid primitive field\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee :: Overriding pathSpec defined /employee/employee/name does not point to a valid primitive field\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        },
        {
          "denormalizedsource/invalid/3_3_cyclic_invalid.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer :: Found overrides that forms a cyclic-referencing: Overrides entry in traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee\" with its pathSpec value \"/name\" is pointing to the field with traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer\" and schema name \"com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer\", this is causing cyclic-referencing.\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee :: Overriding pathSpec defined /name does not point to a valid primitive field\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee :: Overriding pathSpec defined /employee/name does not point to a valid primitive field\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employee :: Overriding pathSpec defined /employee/employee/name does not point to a valid primitive field\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        },
        {
          "denormalizedsource/invalid/3_3_cyclic_invalid_complex.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employeeMap/map/*/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer :: Found overrides that forms a cyclic-referencing: Overrides entry in traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employeeMap\" with its pathSpec value \"/*/name\" is pointing to the field with traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employeeMap/map/*/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer\" and schema name \"com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer\", this is causing cyclic-referencing.\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.employer/employeeMap :: Overriding pathSpec defined /*/name does not point to a valid primitive field\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        },
        {
          "denormalizedsource/invalid/3_3_cyclic_invalid_deep.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/r1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd2/rr1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd :: Found overrides that forms a cyclic-referencing: Overrides entry in traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/r1\" with its pathSpec value \"/rr1/r2\" is pointing to the field with traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/r1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd2/rr1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd\" and schema name \"com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd\", this is causing cyclic-referencing.\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/r1 :: Overriding pathSpec defined /rr1/r2 does not point to a valid primitive field\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        },
        {
          "denormalizedsource/invalid/3_4_cyclic_cross_ref_invalid.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A/a1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.B/b1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A :: Found overrides that forms a cyclic-referencing: Overrides entry in traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A/a1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.B/b1\" with its pathSpec value \"/a2\" is pointing to the field with traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A/a1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.B/b1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A\" and schema name \"com.linkedin.data.schema.annotation.denormalizedsource.invalid.A\", this is causing cyclic-referencing.\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.Test/a/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A/a1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.B/b1 :: Overriding pathSpec defined /a2 does not point to a valid primitive field\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        },
        {
            "denormalizedsource/invalid/3_5_cyclic_from_include.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.A/f1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A :: Found overrides that forms a cyclic-referencing: Overrides entry in traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A\" with its pathSpec value \"/f1/f2\" is pointing to the field with traverser path \"/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A/f1/com.linkedin.data.schema.annotation.denormalizedsource.invalid.A\" and schema name \"com.linkedin.data.schema.annotation.denormalizedsource.invalid.A\", this is causing cyclic-referencing.\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.A :: Overriding pathSpec defined /f1/f2 does not point to a valid primitive field\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        },
        {
          "denormalizedsource/invalid/5_pathSpec_invalid.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd :: Overrides entries in record schema properties should be pointing to fields in included record schemas only. The pathSpec defined /nonInlucdeField is not pointing to a included field.\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd :: Overriding pathSpec defined /includeField does not point to a valid primitive field: Path might be too short\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/f1 :: Overriding pathSpec defined /ff1 does not point to a valid primitive field: Path might be too short\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/f2 :: Overriding pathSpec defined /ff00 does not point to a valid primitive field\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/f3 :: Overriding pathSpec defined /ff1/fff1/fff2 does not point to a valid primitive field: Path might be too long\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/f4 :: MalFormatted key as PathSpec found: /$key/\n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.rcd/f5 :: Overrides entries should be key-value pairs that form a map\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        },
        {
            "denormalizedsource/invalid/6_record_schema_level_invalid.pdl",
            "Annotation processing encountered errors during resolution in \"customAnnotation\" handler. \n" +
            "ERROR :: /com.linkedin.data.schema.annotation.denormalizedsource.invalid.InvalidRecordLevelAnnotation :: Found annotations annotated at record schema level for annotation namespace \"customAnnotation\", which is not allowed\n" +
            "Annotation resolution processing failed at at least one of the handlers.\n"
        }
    };
  }

  @Test(dataProvider = "denormalizedSchemaTestCases_invalid")
  public void testDenormalizedSchemaProcessing_invalid(String filePath, String errorMsg) throws Exception
  {
    DataSchema dataSchema = TestUtil.dataSchemaFromPdlInputStream(getClass().getResourceAsStream(filePath));

    PegasusSchemaAnnotationHandlerImpl customAnnotationHandler = new PegasusSchemaAnnotationHandlerImpl(TEST_ANNOTATION_LABEL);
    SchemaAnnotationProcessor.SchemaAnnotationProcessResult result =
        SchemaAnnotationProcessor.process(Arrays.asList(customAnnotationHandler), dataSchema,
                                          new SchemaAnnotationProcessor.AnnotationProcessOption());
    System.out.println(result.getErrorMsgs());
    assert(result.hasError());
    assert(result.getErrorMsgs().equals(errorMsg));
  }

  @DataProvider
  public Object[][] denormalizedSchemaTestCases_valid()
  {
    // First element is test file name
    // Second element is array of array, which child array is an array of two elements: <PathSpec> and its annotation
    //   in fact the second element will list all primitive field without recursion.∂
    return new Object[][]{
        {
        // A base case to test primitive type resolvedProperties same as property
        "denormalizedsource/0_basecase.pdl",
        Arrays.asList(Arrays.asList("/a/aa", "customAnnotation=NONE"),
                      Arrays.asList("/a/bb", "customAnnotation=[{data_type=NAME}]"), Arrays.asList("/a/cc", ""),
                      Arrays.asList("/b/aa", "customAnnotation=NONE"),
                      Arrays.asList("/b/bb", "customAnnotation=[{data_type=NAME}]"), Arrays.asList("/b/cc", ""))
        },
        {
            // A base case where has a simple override
            "denormalizedsource/0_base_recursive_overrides.pdl",
            Arrays.asList(Arrays.asList("/f0/f1/f1/f2", ""),
                          Arrays.asList("/f0/f1/f2", "customAnnotation=sth"),
                          Arrays.asList("/f0/f2", "")),
        },
        {
        // a simple test case on overriding a record being defined
        "denormalizedsource/0_simpleoverrides.pdl",
        Arrays.asList(Arrays.asList("/a/aa", "customAnnotation=[{data_type=NAME}]"),
                      Arrays.asList("/a/bb", "customAnnotation=NONE"), Arrays.asList("/a/cc", ""),
                      Arrays.asList("/b/aa", "customAnnotation=NONE"),
                      Arrays.asList("/b/bb", "customAnnotation=[{data_type=NAME}]"), Arrays.asList("/b/cc", ""))
        },
        {
        // same as above, but this time test overriding the record that already defined.
        "denormalizedsource/0_simpleoverrides_2.pdl",
        Arrays.asList(Arrays.asList("/a/aa", "customAnnotation=NONE"),
                      Arrays.asList("/a/bb", "customAnnotation=[{data_type=NAME}]"), Arrays.asList("/a/cc", ""),
                      Arrays.asList("/b/aa", "customAnnotation=[{data_type=NAME}]"),
                      Arrays.asList("/b/bb", "customAnnotation=NONE"), Arrays.asList("/b/cc", ""))
        },
        {
        // Test case on selectively overriding fields in the record
        "denormalizedsource/1_0_multiplereference.pdl",
        Arrays.asList(Arrays.asList("/a/aa", "customAnnotation=NONE"), Arrays.asList("/a/bb", "customAnnotation=NONE"),
                      Arrays.asList("/b/aa", "customAnnotation=NONE"), Arrays.asList("/b/bb", "customAnnotation=12"),
                      Arrays.asList("/c/aa", "customAnnotation=21"), Arrays.asList("/c/bb", "customAnnotation=NONE"),
                      Arrays.asList("/d/aa", "customAnnotation=NONE"), Arrays.asList("/d/bb", "customAnnotation=NONE"))
        },
        {
        // Test case on selectively overriding fields in the record
        "denormalizedsource/1_1_testnestedshallowcopy.pdl",
        Arrays.asList(Arrays.asList("/a/aa", "customAnnotation=NONE"), Arrays.asList("/a/ab", "customAnnotation=NONE"),
                      Arrays.asList("/b/bb/aa", "customAnnotation=from_field_b"),
                      Arrays.asList("/b/bb/ab", "customAnnotation=NONE"),
                      Arrays.asList("/c/bb/aa", "customAnnotation=from_field_b"),
                      Arrays.asList("/c/bb/ab", "customAnnotation=NONE"),
                      Arrays.asList("/d/bb/aa", "customAnnotation=from_field_d"),
                      Arrays.asList("/d/bb/ab", "customAnnotation=NONE"))
        },
        {
        // Test case on map related field
        "denormalizedsource/2_1_1_map.pdl",
        Arrays.asList(Arrays.asList("/a/$key", "customAnnotation=[{data_type=NAME}]"),
                      Arrays.asList("/a/*", "customAnnotation=NONE"), Arrays.asList("/b/$key", ""),
                      Arrays.asList("/b/*/bb", ""), Arrays.asList("/c/$key", ""),
                      Arrays.asList("/c/*/bb", "customAnnotation=NONE"), Arrays.asList("/d/$key", "customAnnotation=1st_key"),
                      Arrays.asList("/d/*/$key", "customAnnotation=2nd_key"), Arrays.asList("/d/*/*", "customAnnotation=2nd_value"),
                      Arrays.asList("/e/$key", "customAnnotation=key_value"),
                      Arrays.asList("/e/*/*", "customAnnotation=array_value"),
                      Arrays.asList("/f/$key", "customAnnotation=key_value"),
                      Arrays.asList("/f/*/int", "customAnnotation=union_int_value"),
                      Arrays.asList("/f/*/string", "customAnnotation=union_string_value"),
                      Arrays.asList("/g/map/$key", "customAnnotation=key_value"),
                      Arrays.asList("/g/map/*", "customAnnotation=string_value"),
                      Arrays.asList("/g/int", "customAnnotation=union_int_value"))
        },
        {
        // Test case on array related fields
        "denormalizedsource/2_1_2_array.pdl",
        Arrays.asList(Arrays.asList("/address/*", "customAnnotation=[{dataType=ADDRESS}]"),
                      Arrays.asList("/address2/*", "customAnnotation=[{dataType=NONE}]"),
                      Arrays.asList("/name/*/*", "customAnnotation=[{dataType=ADDRESS}]"),
                      Arrays.asList("/name2/*/*", "customAnnotation=[{dataType=NONE}]"),
                      Arrays.asList("/nickname/*/int", "customAnnotation=[{dataType=NAME}]"),
                      Arrays.asList("/nickname/*/string", "customAnnotation=[{dataType=NAME}]"))
        },
        {
        // Test case on union related fields
        "denormalizedsource/2_1_3_union.pdl",
        Arrays.asList(Arrays.asList("/unionField/int", "customAnnotation=NONE"),
                      Arrays.asList("/unionField/string", "customAnnotation=[{dataType=MEMBER_ID, format=URN}]"),
                      Arrays.asList("/unionField/array/*", "customAnnotation={dataType=MEMBER_ID, format=URN}"),
                      Arrays.asList("/unionField/map/$key", "customAnnotation=[{dataType=MEMBER_ID, format=URN}]"),
                      Arrays.asList("/unionField/map/*", "customAnnotation=[{dataType=MEMBER_ID, format=URN}]"),
                      Arrays.asList("/answerFormat/multipleChoice", "customAnnotation=for multipleChoice"),
                      Arrays.asList("/answerFormat/shortAnswer", "customAnnotation=for shortAnswer"),
                      Arrays.asList("/answerFormat/longAnswer", "customAnnotation=for longAnswer"))
        },
        {
            //Test of fixed data schema
            "denormalizedsource/2_2_1_fixed.pdl",
            Arrays.asList(Arrays.asList("/a", "customAnnotation=NONE"),
                          Arrays.asList("/b/bb", "customAnnotation=b:bb"),
                          Arrays.asList("/c/bb", "customAnnotation=c:bb"),
                          Arrays.asList("/d", "customAnnotation=INNER"))
        },
        {
            //Test of enum
            "denormalizedsource/2_2_2_enum.pdl",
            Arrays.asList(Arrays.asList("/fruit","customAnnotation=fruit1"),
                          Arrays.asList("/otherFruits","customAnnotation=fruit2"))
        },
        {
            //Test of TypeRefs
            "denormalizedsource/2_2_3_typeref.pdl",
            Arrays.asList(Arrays.asList("/primitive_field", "customAnnotation=TYPEREF1"),
                          Arrays.asList("/primitive_field2", "customAnnotation=TYPEREF3"),
                          Arrays.asList("/primitive_field3", "customAnnotation=TYPEREF4"),
                          Arrays.asList("/primitive_field4", "customAnnotation=TYPEREF5"),
                          Arrays.asList("/a/$key", ""),
                          Arrays.asList("/a/*/a", "customAnnotation=TYPEREF1"),
                          Arrays.asList("/b/a", "customAnnotation=original_nested"),
                          Arrays.asList("/c/a", "customAnnotation=b: overriden_nested in c"),
                          Arrays.asList("/d", "customAnnotation=TYPEREF1"),
                          Arrays.asList("/e", "customAnnotation=TYPEREF2"),
                          Arrays.asList("/f/fa", "customAnnotation=fa"),
                          Arrays.asList("/f/fb", "customAnnotation=fb"))
        },
        {
            //Test of includes
            "denormalizedsource/2_2_4_includes.pdl",
            Arrays.asList(Arrays.asList("/a/aa","customAnnotation=/a/aa"),
                          Arrays.asList("/a/bb","customAnnotation=/bb"),
                          Arrays.asList("/b","customAnnotation=NONE"),
                          Arrays.asList("/c/ca","customAnnotation=includedRcd2"),
                          Arrays.asList("/c/cb",""),
                          Arrays.asList("/c/cc",""),
                          Arrays.asList("/e", ""))
        },
        {
        // simple example case for cyclic reference
        "denormalizedsource/3_1_cyclic_simple_valid.pdl",
        Arrays.asList(Arrays.asList("/name", "customAnnotation=none"))
        },
        {
        // example of valid usage of cyclic schema referencing: referencing a recursive structure, from outside
            "denormalizedsource/3_2_cyclic_multiplefields.pdl",
        Arrays.asList(Arrays.asList("/a/aa", "customAnnotation=aa"), Arrays.asList("/b/aa", "customAnnotation=b:/aa"),
                      Arrays.asList("/b/bb/aa", "customAnnotation=b:/bb/aa"),
                      Arrays.asList("/b/bb/bb/aa", "customAnnotation=b:/bb/bb/aa"),
                      Arrays.asList("/b/bb/bb/bb/aa", "customAnnotation=aa"),
                      Arrays.asList("/b/bb/bb/cc/aa", "customAnnotation=aa"), Arrays.asList("/b/bb/cc/aa", "customAnnotation=aa"),
                      Arrays.asList("/b/cc/aa", "customAnnotation=aa"), Arrays.asList("/c/aa", "customAnnotation=c:/aa"),
                      Arrays.asList("/c/bb/aa", "customAnnotation=c:/bb/aa"),
                      Arrays.asList("/c/bb/bb/aa", "customAnnotation=c:/bb/bb/aa"),
                      Arrays.asList("/c/bb/bb/bb/aa", "customAnnotation=aa"),
                      Arrays.asList("/c/bb/bb/cc/aa", "customAnnotation=aa"), Arrays.asList("/c/bb/cc/aa", "customAnnotation=aa"),
                      Arrays.asList("/c/cc/aa", "customAnnotation=aa"))
        },
        {
        // example of valid usage of cyclic schema referencing: referencing a recursive structure, from outside
            "denormalizedsource/4_1_comprehensive_example.pdl",
        Arrays.asList(Arrays.asList("/memberId", "customAnnotation=[{dataType=MEMBER_ID_INT, isPurgeKey=true}]"),
                      Arrays.asList("/memberData/usedNames/*/*", "customAnnotation=[{dataType=NAME}]"),
                      Arrays.asList("/memberData/phoneNumber", "customAnnotation=[{dataType=PHONE_NUMBER}]"),
                      Arrays.asList("/memberData/address/*", "customAnnotation=[{dataType=ADDRESS}]"),
                      Arrays.asList("/memberData/workingHistory/$key", "customAnnotation=workinghistory-$key"),
                      Arrays.asList("/memberData/workingHistory/*", "customAnnotation=workinghistory-value"),
                      Arrays.asList("/memberData/details/firstName", "customAnnotation=[{dataType=MEMBER_FIRST_NAME}]"),
                      Arrays.asList("/memberData/details/lastName", "customAnnotation=[{dataType=MEMBER_LAST_NAME}]"),
                      Arrays.asList("/memberData/details/otherNames/*/*/nickName", "customAnnotation=[{dataType=MEMBER_LAST_NAME}]"),
                      Arrays.asList("/memberData/details/otherNames/*/*/shortCutName", "customAnnotation=[{dataType=MEMBER_LAST_NAME}]"),
                      Arrays.asList("/memberData/education/string", "customAnnotation=NONE"),
                      Arrays.asList("/memberData/education/array/*/graduate", "customAnnotation=[{dataType=MEMBER_GRADUATION}]"))
        },
        {
            // example of valid usage of cyclic schema referencing: referencing a recursive structure, from outside
            "denormalizedsource/4_2_multiplepaths_deep_overrides.pdl",
            Arrays.asList(Arrays.asList("/a/a1", "customAnnotation=Level1: a1"),
                          Arrays.asList("/a/a2/aa1/aaa1", "customAnnotation=Level1: /a2/aa1/aaa1"),
                          Arrays.asList("/a/a2/aa1/aaa2", "customAnnotation=Level1: /a2/aa1/aaa2"),
                          Arrays.asList("/a/a2/aa1/aaa3/*", "customAnnotation=Level1: /a2/aa1/aaa3/*"),
                          Arrays.asList("/a/a2/aa1/aaa4/*/*", "customAnnotation=Level1: /a2/aa1/aaa4/*/*"),
                          Arrays.asList("/a/a2/aa1/aaa5/$key", "customAnnotation=Level1: /a2/aa1/aaa5/$key"),
                          Arrays.asList("/a/a2/aa1/aaa5/*", "customAnnotation=Level1: /a2/aa1/aaa5/*"),
                          Arrays.asList("/a/a2/aa1/aaa6/$key", "customAnnotation=Level1: /a2/aa1/aaa6/$key"),
                          Arrays.asList("/a/a2/aa1/aaa6/*/*", "customAnnotation=Level1: /a2/aa1/aaa6/*/*"),
                          Arrays.asList("/a/a2/aa1/aaa7/array/*", "customAnnotation=Level1: /a2/aa1/aaa7/array/*"),
                          Arrays.asList("/a/a2/aa1/aaa7/int", "customAnnotation=Level1: /a2/aa1/aaa7/int"),
                          Arrays.asList("/a/a2/aa1/aaa8/map/$key", "customAnnotation=Level1: /a2/aa1/aaa8/map/$key"),
                          Arrays.asList("/a/a2/aa1/aaa8/map/*", "customAnnotation=Level1: /a2/aa1/aaa8/map/*"),
                          Arrays.asList("/a/a2/aa1/aaa8/int", "customAnnotation=Level1: /a2/aa1/aaa8/int"),
                          Arrays.asList("/a/a3/bb1", "customAnnotation=Level1: /a3/bb1"),
                          Arrays.asList("/a/a3/bb2", "customAnnotation=Level1: /a3/bb2"))
        },
        {
            "denormalizedsource/6_1_enum_top.pdl",
            Arrays.asList(Arrays.asList("", "customAnnotation=fruits"))
        },
        {
            "denormalizedsource/6_2_fixed_top.pdl",
            Arrays.asList(Arrays.asList("", "customAnnotation=fixed"))
        },
        {
            "denormalizedsource/6_3_1_typeref_top.pdl",
            Arrays.asList(Arrays.asList("", "customAnnotation=NONE"))
        },
        {
            "denormalizedsource/6_3_2_typeref_top_2.pdl",
            Arrays.asList(Arrays.asList("", "customAnnotation=layer2"))
        },
    };
  }

  @Test(dataProvider = "denormalizedSchemaTestCases_valid")
  public void testDenormalizedSchemaProcessing(String filePath, List<List<String>> expected) throws Exception
  {
    DataSchema dataSchema = TestUtil.dataSchemaFromPdlInputStream(getClass().getResourceAsStream(filePath));

    PegasusSchemaAnnotationHandlerImpl customAnnotationHandler = new PegasusSchemaAnnotationHandlerImpl(TEST_ANNOTATION_LABEL);
    SchemaAnnotationProcessor.SchemaAnnotationProcessResult result =
        SchemaAnnotationProcessor.process(Arrays.asList(customAnnotationHandler), dataSchema,
                                          new SchemaAnnotationProcessor.AnnotationProcessOption());

    assert(!result.hasError());

    ResolvedPropertiesReaderVisitor resolvedPropertiesReaderVisitor = new ResolvedPropertiesReaderVisitor();
    DataSchemaRichContextTraverser traverser = new DataSchemaRichContextTraverser(resolvedPropertiesReaderVisitor);
    traverser.traverse(result.getResultSchema());
    Map<PathSpec, Map<String, Object>> pathSpecToResolvedPropertiesMap = resolvedPropertiesReaderVisitor.getLeafFieldsPathSpecToResolvedPropertiesMap();
    Assert.assertEquals(pathSpecToResolvedPropertiesMap.entrySet().size(), expected.size());

    for (List<String> pair : expected)
    {
      String pathSpec = pair.get(0);
      String expectedProperties = pair.get(1);
      Map<String, Object> resolvedProperties =
          SchemaAnnotationProcessor.getResolvedPropertiesByPath(pathSpec, result.getResultSchema());
      String resolvedPropertiesStr =
          resolvedProperties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(joining("&"));
      assert (expectedProperties.equals(resolvedPropertiesStr));
    }
  }

  @Test
  public void testHandlerResolveException() throws Exception
  {
    String failureMessage = "Intentional failure";
    SchemaAnnotationHandler testHandler = new SchemaAnnotationHandler()
    {
      @Override
      public ResolutionResult resolve(List<Pair<String, Object>> propertiesOverrides,
                                      ResolutionMetaData resolutionMetadata)
      {
        throw new RuntimeException(failureMessage);
      }

      @Override
      public String getAnnotationNamespace()
      {
        return TEST_ANNOTATION_LABEL;
      }

      @Override
      public AnnotationValidationResult validate(Map<String, Object> resolvedProperties, ValidationMetaData metaData)
      {
        return new AnnotationValidationResult();
      }
    };
    RecordDataSchema testSchema = (RecordDataSchema) TestUtil.dataSchemaFromPdlString(simpleTestSchema);

    try {
      SchemaAnnotationProcessor.SchemaAnnotationProcessResult result =
          SchemaAnnotationProcessor.process(Arrays.asList(testHandler), testSchema,
                                            new SchemaAnnotationProcessor.AnnotationProcessOption());
    }
    catch (IllegalStateException e)
    {
      e.getMessage()
       .equals(String.format(
           "Annotation processing failed when resolving annotations in the schema using the handler for " +
           "annotation namespace \"%s\"", TEST_ANNOTATION_LABEL));
    }
  }

  @Test
  public void testHandlerValidationFailure() throws Exception
  {
    RecordDataSchema testSchema = (RecordDataSchema) TestUtil.dataSchemaFromPdlString(simpleTestSchema);

    SchemaAnnotationHandler testHandlerWithFailure = new PegasusSchemaAnnotationHandlerImpl(TEST_ANNOTATION_LABEL)
    {
      @Override
      public AnnotationValidationResult validate(Map<String, Object> resolvedProperties, ValidationMetaData metaData)
      {
        AnnotationValidationResult result = new AnnotationValidationResult();
        result.setValid(false);
        return result;
      }
    };

    SchemaAnnotationProcessor.SchemaAnnotationProcessResult result =
        SchemaAnnotationProcessor.process(Arrays.asList(testHandlerWithFailure), testSchema,
                                          new SchemaAnnotationProcessor.AnnotationProcessOption());
    assert (!result.isValidationSuccess());
    assert (result.getErrorMsgs()
                  .equals(
                      String.format("Annotation validation process failed in \"%s\" handler. \n",
                                    TEST_ANNOTATION_LABEL)));

    SchemaAnnotationHandler testHandlerWithException = new PegasusSchemaAnnotationHandlerImpl(TEST_ANNOTATION_LABEL)
    {
      @Override
      public AnnotationValidationResult validate(Map<String, Object> resolvedProperties, ValidationMetaData metaData)
      {
        throw new RuntimeException();
      }
    };
    try
    {
      SchemaAnnotationProcessor.SchemaAnnotationProcessResult result2 =
          SchemaAnnotationProcessor.process(Arrays.asList(testHandlerWithException), testSchema,
                                            new SchemaAnnotationProcessor.AnnotationProcessOption());
    }
    catch (IllegalStateException e)
    {
      assert(e.getMessage()
       .equals(String.format("Annotation validation failed in \"%s\" handler.",
                             TEST_ANNOTATION_LABEL)));
    }
  }

  @Test
  public void testGetResolvedPropertiesByPath() throws Exception
  {
    RecordDataSchema testSchema = (RecordDataSchema) TestUtil.dataSchemaFromPdlString(simpleTestSchema);
    try
    {
      SchemaAnnotationProcessor.getResolvedPropertiesByPath("/f0/f3", testSchema);
    }
    catch (IllegalArgumentException e)
    {
      assert (e.getMessage().equals("Could not find path segment \"f3\" in PathSpec \"/f0/f3\""));
    }

    try
    {
      SchemaAnnotationProcessor.getResolvedPropertiesByPath("/f1/string", testSchema);
    }
    catch (IllegalArgumentException e)
    {
      assert (e.getMessage().equals("Could not find path segment \"string\" in PathSpec \"/f1/string\""));
    }
  }

}

