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
  private CustomInfoSpec _valueCustomInfo;

  private ClassTemplateSpec _keyClass;
  private ClassTemplateSpec _keyDataClass;
  private CustomInfoSpec _keyCustomInfo;

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

  public CustomInfoSpec getValueCustomInfo()
  {
    return _valueCustomInfo;
  }

  public void setValueCustomInfo(CustomInfoSpec customInfo)
  {
    _valueCustomInfo = customInfo;
  }

  /**
   * Same as {#link getValueCustomInfo}. Provided for backward compatibility.
   */
  public CustomInfoSpec getCustomInfo()
  {
    return getValueCustomInfo();
  }

  /**
   * Same as {#link setValueCustomInfo}. Provided for backward compatibility.
   */
  public void setCustomInfo(CustomInfoSpec valueCustomInfo)
  {
    setValueCustomInfo(valueCustomInfo);
  }

  public ClassTemplateSpec getKeyClass()
  {
    return _keyClass;
  }

  public void setKeyClass(ClassTemplateSpec keyClass)
  {
    _keyClass = keyClass;
  }

  public ClassTemplateSpec getKeyDataClass()
  {
    return _keyDataClass;
  }

  public void setKeyDataClass(ClassTemplateSpec keyDataClass)
  {
    _keyDataClass = keyDataClass;
  }

  public CustomInfoSpec getKeyCustomInfo()
  {
    return _keyCustomInfo;
  }

  public void setKeyCustomInfo(CustomInfoSpec keyCustomInfo)
  {
    _keyCustomInfo = keyCustomInfo;
  }
}
