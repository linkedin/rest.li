package com.linkedin.restli.server;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;


public class TestRecordTemplateClass {

  static class Foo extends RecordTemplate
  {
    private Foo(DataMap map)
    {
      super(map, null);
    }

    static Foo createFoo(String key, String value)
    {
      DataMap dataMap = new DataMap();
      dataMap.put(key, value);
      return new Foo(dataMap);
    }
  }

  static class Bar extends RecordTemplate
  {
    private Bar(DataMap map)
    {
      super(map, null);
    }

    static Bar createBar(String key, String value)
    {
      DataMap dataMap = new DataMap();
      dataMap.put(key, value);
      return new Bar(dataMap);
    }
  }
}
