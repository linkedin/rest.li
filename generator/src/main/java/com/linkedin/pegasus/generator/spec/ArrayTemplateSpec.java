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


import com.linkedin.data.schema.ArrayDataSchema;


/**
 * @author Keren Jin
 */
public class ArrayTemplateSpec extends ClassTemplateSpec
{
  private ClassTemplateSpec _itemClass;
  private ClassTemplateSpec _itemDataClass;
  private CustomInfoSpec _customInfo;

  public ArrayTemplateSpec(ArrayDataSchema schema)
  {
    setSchema(schema);
  }

  @Override
  public ArrayDataSchema getSchema()
  {
    return (ArrayDataSchema) super.getSchema();
  }

  public ClassTemplateSpec getItemClass()
  {
    return _itemClass;
  }

  public void setItemClass(ClassTemplateSpec itemClass)
  {
    _itemClass = itemClass;
  }

  public ClassTemplateSpec getItemDataClass()
  {
    return _itemDataClass;
  }

  public void setItemDataClass(ClassTemplateSpec itemDataClass)
  {
    _itemDataClass = itemDataClass;
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
