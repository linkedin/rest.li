package com.linkedin.restli.examples.greetings.server.defaults;

import com.linkedin.restli.examples.defaults.api.HighLevelRecordWithDefault;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;


@RestLiCollection(name = "fillInDefaults", namespace = "com.linkedin.restli.examples.defaults.api")
public class FieldFillInDefaultResources extends CollectionResourceTemplate<Long, HighLevelRecordWithDefault>
{
  @Override
  public HighLevelRecordWithDefault get(Long keyId)
  {
    HighLevelRecordWithDefault result = new HighLevelRecordWithDefault();
    result.setIntDefaultFieldB(-1);
    return result;
  }
}

