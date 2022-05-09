package com.linkedin.data.avro.testevents;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import static org.testng.AssertJUnit.*;


public class MapArrayUnion extends SpecificRecordBase implements SpecificRecord {
  static final long serialVersionUID = 1L;

  private Object _mapOrArray;

  public static final org.apache.avro.Schema TEST_SCHEMA = AvroCompatibilityHelper.parse(
      "{"
          + "                   \"type\": \"record\","
          + "                   \"name\": \"MapArrayUnion\","
          + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "                   \"doc\": \"Map Array union\","
          + "                  \"fields\": ["
          + "                    {"
          + "                       \"name\": \"mapOrArray\","
          + "                  \"type\": ["

          + "     { "
          +        "\"type\": \"record\","
          + "     \"name\" : \"TestMap\",\n" + " "
          + "     \"namespace\":\"com.linkedin.data.avro.testevents\","
          + "     \"fields\": ["
          +       "{ "
          + "     \"name\" : \"recordMap\",\n" + " "
          + "     \"type\" : {\n"
          + "        \"type\" : \"map\",\n"
          + "        \"values\" :    {"
          +"          \"type\": \"record\","
          + "                   \"name\": \"StringRecord\","
          + "                   \"namespace\":\"com.linkedin.data.avro.testevents\","
          + "                   \"doc\": \"Array union field\","
          + "                  \"fields\": ["
          + "                    {"
          + "                       \"name\": \"stringField\","
          + "                       \"type\": ["
          + "                       \"string\""
          + "                      ],"
          + "                   \"doc\": \"string field\""
          + "                    }"
          + "                  ]"
          + "}"
          + "}}]},"

          +"{ "
          +        "\"type\": \"record\","
          + "     \"name\" : \"TestArray\",\n" + " "
          + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "     \"fields\": ["
          +       "{ "
          + "     \"name\" : \"recordArray\",\n" + " "
          + "     \"type\" : {\n"
          + "        \"type\" : \"array\",\n"
          +         "\"items\" : \"string\""
          +  "      }"
          + "      }"
          + "     ]"
          + "   }"

          + "                      ]"
          + "                    }"
          + "                  ]"
          + "                  }"
  );

  @Override
  public Schema getSchema() {
    return TEST_SCHEMA;
  }

  @Override
  public Object get(int field) {
    if (field == 0) {
      return _mapOrArray;
    }
    else {
      fail("fetched invalid field from MapArrayUnion");
      return null;
    }
  }

  @Override
  public void put(int field, Object value) {
    if (field == 0) {
      _mapOrArray = value;
    }
    else {
      fail("invalid field from MapArrayUnion");
    }
  }
}
