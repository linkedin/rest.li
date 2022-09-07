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

package com.linkedin.data.avro.testevents;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import static org.testng.AssertJUnit.*;


public class RecordArray extends SpecificRecordBase implements SpecificRecord {
  static final long serialVersionUID = 1L;
  private List<StringRecord> _recordArray;
  public static final org.apache.avro.Schema TEST_SCHEMA = AvroCompatibilityHelper.parse(
       "{ "
          +        "\"type\": \"record\","
          + "     \"name\" : \"RecordArray\",\n" + " "
           + "\"namespace\":\"com.linkedin.data.avro.testevents\","
           + "     \"fields\": ["
          +       "{ "
          + "     \"name\" : \"recordArray\",\n" + " "
          + "     \"type\" : {\n"
          + "        \"type\" : \"array\",\n"
          + "        \"items\" :    {"
      +"\"type\": \"record\","
          + "                   \"name\": \"StringRecord\","
           + "\"namespace\":\"com.linkedin.data.avro.testevents\","
          + "                   \"doc\": \"Array union field\","
          + "                  \"fields\": ["
          + "                    {"
          + "                       \"name\": \"stringField\","
          + "                  \"type\": ["
          + "                         \"string\""
          + "                      ],"
          + "                   \"doc\": \"string field\""
          + "                    }"
          + "                  ]"
           + "}"
          + "}}]}"
  );

  @Override
  public Schema getSchema() {
    return TEST_SCHEMA;
  }

  @Override

  public Object get(int field) {
    if (field == 0) {
      return _recordArray;
    }
    else {
      fail("fetched invalid field from RecordArray");
      return null;
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(int field, Object value) {
    if(field == 0) {
      _recordArray = (List<StringRecord>) value;
    } else {
      fail("invalid field for RecordArray");
    }
  }
}
