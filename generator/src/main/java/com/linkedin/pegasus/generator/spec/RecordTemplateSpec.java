/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.spec;


import com.linkedin.data.schema.RecordDataSchema;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Keren Jin
 */
public class RecordTemplateSpec extends ClassTemplateSpec
{
  private List<Field> _fields;

  public RecordTemplateSpec(RecordDataSchema schema)
  {
    setSchema(schema);
    _fields = new ArrayList<Field>();
  }

  @Override
  public RecordDataSchema getSchema()
  {
    return (RecordDataSchema) super.getSchema();
  }

  public List<Field> getFields()
  {
    return _fields;
  }

  public void addField(Field field)
  {
    _fields.add(field);
  }

  public static class Field
  {
    private RecordDataSchema.Field _schemaField;
    private ClassTemplateSpec _type;
    private ClassTemplateSpec _dataClass;
    private CustomInfoSpec _customInfo;

    public RecordDataSchema.Field getSchemaField()
    {
      return _schemaField;
    }

    public void setSchemaField(RecordDataSchema.Field schemaField)
    {
      _schemaField = schemaField;
    }

    public ClassTemplateSpec getType()
    {
      return _type;
    }

    public void setType(ClassTemplateSpec type)
    {
      _type = type;
    }

    public ClassTemplateSpec getDataClass()
    {
      return _dataClass;
    }

    public void setDataClass(ClassTemplateSpec dataClass)
    {
      _dataClass = dataClass;
    }

    public CustomInfoSpec getCustomInfo()
    {
      return _customInfo;
    }

    public void setCustomInfo(CustomInfoSpec customInfo)
    {
      _customInfo = customInfo;
    }
  }
}
