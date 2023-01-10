package com.linkedin.data.avro.testevents;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import static org.testng.AssertJUnit.*;


public class TestArray extends SpecificRecordBase implements SpecificRecord {
  static final long serialVersionUID = 1L;

  private List<String> _recordArray;

  public static final org.apache.avro.Schema TEST_SCHEMA = AvroCompatibilityHelper.parse("{\"type\":\"record\",\"name\":\"TestArray\",\"namespace\":\"com.linkedin.data.avro.testevents\",\"fields\":[{\"name\":\"recordArray\",\"type\":{\"type\":\"array\",\"items\":\"string\"}}]}");
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
      fail("fetched invalid field from TestArray");
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(int field, Object value) {
    if(field == 0) {
      _recordArray = (List<String>) value;
    } else {
      fail("invalid field for TestArray");
    }
  }
}
