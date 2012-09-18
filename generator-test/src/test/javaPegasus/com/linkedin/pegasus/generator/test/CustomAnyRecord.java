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

package com.linkedin.pegasus.generator.test;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateOutputCastException;

public class CustomAnyRecord extends AnyRecord
{
  private final static RecordDataSchema SCHEMA = (new AnyRecord().schema());

  private RecordTemplate _cachedValue = null;

  public CustomAnyRecord()
  {
    super();
  }

  public CustomAnyRecord(DataMap data)
  {
    super(data);
  }

  public <T extends RecordTemplate> boolean isValueOfClass(Class<T> clazz)
  {
    DataMap map = data();
    DataSchema schema = DataTemplateUtil.getSchema(clazz);
    String key = schema.getUnionMemberKey();
    return map.size() == 1 && map.containsKey(key);
  }

  public <T extends RecordTemplate> T getValue(Class<T> clazz) throws TemplateOutputCastException
  {
    T result = null;
    DataMap map = data();
    DataSchema schema = DataTemplateUtil.getSchema(clazz);
    String key = schema.getUnionMemberKey();
    Object valueData = (map.size() == 1 ? map.get(key) : null);
    if (valueData != null)
    {
      if (_cachedValue != null && _cachedValue.data() == valueData && clazz.isInstance(_cachedValue))
      {
        @SuppressWarnings("unchecked")
        T value = (T) _cachedValue;
        result = value;
      }
      else
      {
        result = DataTemplateUtil.wrap(valueData, schema, clazz);
        _cachedValue = result;
      }
    }
    return result;
  }

  public <T extends RecordTemplate> AnyRecord setValue(T value)
  {
    DataSchema schema = value.schema();
    String key = schema.getUnionMemberKey();
    DataMap map = data();
    map.clear();
    map.put(key, value.data());
    _cachedValue = value;
    return this;
  }
}
