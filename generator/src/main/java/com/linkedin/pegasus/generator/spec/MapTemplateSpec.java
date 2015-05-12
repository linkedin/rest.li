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


import com.linkedin.data.schema.MapDataSchema;


/**
 * @author Keren Jin
 */
public class MapTemplateSpec extends ClassTemplateSpec
{
  private ClassTemplateSpec _valueClass;
  private ClassTemplateSpec _valueDataClass;
  private CustomInfoSpec _customInfo;

  public MapTemplateSpec(MapDataSchema schema)
  {
    setSchema(schema);
  }

  @Override
  public MapDataSchema getSchema()
  {
    return (MapDataSchema) super.getSchema();
  }

  public ClassTemplateSpec getValueClass()
  {
    return _valueClass;
  }

  public void setValueClass(ClassTemplateSpec valueClass)
  {
    _valueClass = valueClass;
  }

  public ClassTemplateSpec getValueDataClass()
  {
    return _valueDataClass;
  }

  public void setValueDataClass(ClassTemplateSpec valueDataClass)
  {
    _valueDataClass = valueDataClass;
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
