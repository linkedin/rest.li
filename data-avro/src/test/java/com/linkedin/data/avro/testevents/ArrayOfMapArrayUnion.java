package com.linkedin.data.avro.testevents;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import static org.testng.AssertJUnit.*;


public class ArrayOfMapArrayUnion extends SpecificRecordBase implements SpecificRecord {
  static final long serialVersionUID = 1L;

  private List<MapArrayUnion> _mapArrayUnionList;

  public static final org.apache.avro.Schema TEST_SCHEMA = AvroCompatibilityHelper.parse(
      "{ "
          +        "\"type\": \"record\","
          + "     \"name\" : \"ArrayOfMapArrayUnion\",\n" + " "
          + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "     \"fields\": ["
          +       "{ "
          + "     \"name\" : \"recordArray\",\n" + " "
          + "     \"type\" : {\n"
          + "        \"type\" : \"array\",\n"
          + "        \"items\" :    "

          +  "{"
          + "                   \"type\": \"record\","
          + "                   \"name\": \"MapArrayUnion\","
          + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "                   \"doc\": \"Map Array union\","
          + "                  \"fields\": ["
          + "                    {"
          + "                       \"name\": \"mapOrArray\","
          + "                  \"type\": ["
          +"{\n" + "                    \"type\":\"array\",\n" + "                    \"items\":\"string\"\n"
          + "                },"
          + "{\n" + "                    \"type\":\"map\",\n" + "                    \"values\":\"string\"\n"
          + "                }"

          + "                      ]"
          + "                    }"
          + "                  ]"
          + "                  }"

          + "}}]}"
  );



  @Override
  public Schema getSchema() {
    return TEST_SCHEMA;
  }

  @Override
  public Object get(int field) {
    if (field == 0) {
      return _mapArrayUnionList;
    }
    else {
      fail("fetched invalid field from ArrayOfMapArrayUnion");
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(int field, Object value) {
    if(field == 0) {
      _mapArrayUnionList = (List<MapArrayUnion>) value;
    } else {
      fail("invalid field for ArrayOfMapArrayUnion");
    }
  }
}
