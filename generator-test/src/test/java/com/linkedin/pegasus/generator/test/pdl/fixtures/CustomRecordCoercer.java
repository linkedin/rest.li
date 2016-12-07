package com.linkedin.pegasus.generator.test.pdl.fixtures;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;


public class CustomRecordCoercer implements DirectCoercer<CustomRecord>
{
  static
  {
    Custom.registerCoercer(new CustomRecordCoercer(), CustomRecord.class);
  }

  private CustomRecordCoercer()
  {
  }

  @Override
  public Object coerceInput(CustomRecord object)
      throws ClassCastException
  {
    DataMap dataMap = new DataMap();
    dataMap.put("title", object.getTitle());
    dataMap.put("body", object.getBody());
    return dataMap;
  }

  @Override
  public CustomRecord coerceOutput(Object object)
      throws TemplateOutputCastException
  {
    if (!(object instanceof DataMap))
    {
      throw new IllegalArgumentException("object param must be DataMap, but was: " + object);
    }
    DataMap dataMap = (DataMap) object;
    return new CustomRecord(dataMap.getString("title"), dataMap.getString("body"));
  }
}
