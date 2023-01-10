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
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import static org.testng.AssertJUnit.*;

public class TestEventWithUnionAndEnum extends SpecificRecordBase implements SpecificRecord {
  static final long serialVersionUID = 1L;
  private String _fieldName;
  private Object _eventData;
  private EnumData _enumData;
  public  static final org.apache.avro.Schema TEST_SCHEMA =
      AvroCompatibilityHelper.parse(
          "{"
              + "\"type\":\"record\","
              + "\"name\":\"TestEventWithUnionAndEnum\","
              + "\"namespace\":\"com.linkedin.data.avro.testevents\","
              + "\"fields\":["
              + "   {"
              + "       \"name\":\"fieldName\","
              + "       \"type\":\"string\","
              + "       \"doc\":\"Dummy text for the field\""
              + "   },"
              + "   {"
              + "       \"name\":\"eventData\","
              + "       \"type\":[\"string\",\"long\"],"
              + "       \"doc\":\"Dummy text for the field\""
              + "   },"
              +"    {"
              + "       \"name\":\"enumData\","
              + "       \"type\":{\n" + "  \"name\": \"EnumData\",\n"
              + "  \"doc\": \"Test Enum.\",\n" + "  \"type\": \"enum\",\n"
              + "  \"namespace\": \"com.linkedin.data.avro.testevents\",\n" + "  \"symbols\": [\n" + "    \"APPROVED\",\n"
              + "    \"REJECTED\",\n" + "    \"CANCELLED\"\n" + "  ],\n" + "  \"symbolDocs\": {\n"
              + "    \"APPROVED\": \"approved.\",\n"
              + "    \"REJECTED\": \"rejected.\",\n"
              + "    \"CANCELLED\": \"cancelled.\"\n" + "  }\n" + "},"
              + "       \"doc\":\"Dummy enum\""
              + "   }"
              + "]"
              + "}");

  @Override
  public void put(int i, Object v) {
    if (i == 0) {
      _fieldName = (String) v;
    } else if (i == 1) {
      _eventData = v;
    } else if (i == 2) {
      _enumData = EnumData.valueOf(v.toString());
    }
  }

  @Override
  public Object get(int i) {
    if (i == 0) {
      return _fieldName;
    } else if (i == 1) {
      return _eventData;
    } else if (i == 2) {
      return _enumData;
    } else {
      fail("fetched invalid field from TestEventWithUnionAndEnum");
      return null;
    }
  }

  @Override
  public Schema getSchema() {
    return TEST_SCHEMA;
  }

  public TestEventWithUnionAndEnum() {
  }

}
