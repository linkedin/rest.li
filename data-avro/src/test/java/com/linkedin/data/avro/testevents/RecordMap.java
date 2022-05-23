package com.linkedin.data.avro.testevents;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import static org.testng.AssertJUnit.*;


public class RecordMap extends SpecificRecordBase implements SpecificRecord {
  static final long serialVersionUID = 1L;
  private Map<String, StringRecord> _recordMap;
  public static final org.apache.avro.Schema TEST_SCHEMA = AvroCompatibilityHelper.parse(
      "{ "
          +        "\"type\": \"record\","
          + "     \"name\" : \"RecordMap\",\n" + " "
          + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "     \"fields\": ["
          +       "{ "
          + "     \"name\" : \"recordMap\",\n" + " "
          + "     \"type\" : {\n"
          + "        \"type\" : \"map\",\n"
          + "        \"values\" :   "

          + "           {"
          +"                \"type\": \"record\","
          + "               \"name\": \"StringRecord\","
          +                 "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "                 \"doc\": \"Array union field\","
          + "                  \"fields\": ["
          + "                    {"
          + "                       \"name\": \"stringField\","
          + "                       \"type\": ["
          + "                         \"string\""
          + "                      ],"
          + "                   \"doc\": \"string field\""
          + "                    }"
          + "                  ]"
          + "             }"

          + "}}]}"
  );

  @Override
  public Schema getSchema() {
    return TEST_SCHEMA;
  }

  @Override

  public Object get(int field) {
    if (field == 0) {
      return _recordMap;
    }
    else {
      fail("fetched invalid field from RecordMap");
      return null;
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(int field, Object value) {
    if(field == 0) {
      _recordMap = (Map<String, StringRecord>) value;
    } else {
      fail("invalid field for RecordMap");
    }
  }
}
