package com.linkedin.data.avro.testevents;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import static org.testng.AssertJUnit.*;


public class MapOfMapOfArrayOfMapArrayUnion extends SpecificRecordBase implements SpecificRecord{
  static final long serialVersionUID = 1L;

  private Map<String, MapOfArrayOfMapArrayUnion> _recordMap;
  public static final org.apache.avro.Schema TEST_SCHEMA = AvroCompatibilityHelper.parse(
      "{ "
          +        "\"type\": \"record\","
          + "     \"name\" : \"MapOfMapOfArrayOfMapArrayUnion\",\n" + " "
          + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "     \"fields\": ["
          +       "{ "
          + "     \"name\" : \"recordMap\",\n" + " "
          + "     \"type\" : {\n"
          + "        \"type\" : \"map\",\n"
          + "        \"values\" :   "

          + "{ "
          +        "\"type\": \"record\","
          + "     \"name\" : \"MapOfArrayOfMapArrayUnion\",\n" + " "
          + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "     \"fields\": ["
          +       "{ "
          + "     \"name\" : \"recordMap\",\n" + " "
          + "     \"type\" : {\n"
          + "        \"type\" : \"map\",\n"
          + "        \"values\" :   "

          + "{\n"
          + "        \"type\" : \"array\",\n"
          + "        \"items\" :    "

          +             "{"
          + "                   \"type\": \"record\","
          + "                   \"name\": \"MapArrayUnion\","
          + "                   \"doc\": \"Map Array union\","
          + "                  \"fields\": ["
          + "                    {"
          + "                       \"name\": \"mapOrArray\","
          + "                         \"type\": ["

          + "                         { "
          +                              "\"type\": \"record\","
          + "                             \"name\" : \"TestMap\",\n"
          + "                               \"fields\": ["
          +                                  "{ "
          + "                                 \"name\" : \"recordMap\",\n" + " "
          + "                                   \"type\" : {\n"
          + "                                       \"type\" : \"map\",\n"
          + "                                        \"values\" :    {"
          +"                                              \"type\": \"record\","
          + "                                             \"name\": \"StringRecord\","
          + "                                               \"doc\": \"Array union field\","
          + "                                               \"fields\": ["
          + "                                                   {"
          + "                                                     \"name\": \"stringField\","
          + "                                                       \"type\": ["
          + "                                                        \"string\""
          + "                                                          ],"
          + "                                                        \"doc\": \"string field\""
          + "                                                     }"
          + "                                                   ]"
          +                                           "}"
          +                                         "}}]},"

          +"{ "
          +        "\"type\": \"record\","
          + "     \"name\" : \"TestArray\",\n"
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

          + "}"

          + "}}]}"

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
      fail("fetched invalid field from MapOfMapOfArrayOfMapArrayUnion");
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(int field, Object value) {
    if(field == 0) {
      _recordMap = (Map<String, MapOfArrayOfMapArrayUnion>) value;
    } else {
      fail("invalid field for MapOfMapOfArrayOfMapArrayUnion");
    }
  }
}
