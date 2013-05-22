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

package com.linkedin.restli.examples.typeref.server;


import com.linkedin.data.ByteString;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.restli.examples.custom.types.CustomNonNegativeLong;
import com.linkedin.restli.examples.typeref.api.BooleanRef;
import com.linkedin.restli.examples.typeref.api.BytesRef;
import com.linkedin.restli.examples.typeref.api.CustomNonNegativeLongRef;
import com.linkedin.restli.examples.typeref.api.DoubleRef;
import com.linkedin.restli.examples.typeref.api.FloatRef;
import com.linkedin.restli.examples.typeref.api.Fruits;
import com.linkedin.restli.examples.typeref.api.FruitsRef;
import com.linkedin.restli.examples.typeref.api.IntArrayRef;
import com.linkedin.restli.examples.typeref.api.IntMapRef;
import com.linkedin.restli.examples.typeref.api.IntRef;
import com.linkedin.restli.examples.typeref.api.LongRef;
import com.linkedin.restli.examples.typeref.api.Point;
import com.linkedin.restli.examples.typeref.api.PointRef;
import com.linkedin.restli.examples.typeref.api.StringRef;
import com.linkedin.restli.examples.typeref.api.TyperefRecord;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

/**
 * Test for typeref param and return types in actions.
 *
 * @author slim
 */
@RestLiCollection(name="typeref",
                  namespace = "com.linkedin.restli.examples.typeref.client")
public class TyperefTestResource extends CollectionResourceTemplate<Long,TyperefRecord>
{
  public TyperefTestResource()
  {
  }

  @Action(name = "intFunc", returnTyperef=IntRef.class)
  public int intFunc(@ActionParam(value="arg1", typeref=IntRef.class) int arg1)
  {
    return 100;
  }

  @Action(name = "intFunc2", returnTyperef=IntRef.class)
  public Integer IntegerFunc(@ActionParam(value="arg1", typeref=IntRef.class) Integer arg1)
  {
    return 100;
  }

  @Action(name = "longFunc", returnTyperef=LongRef.class)
  public long longFunc(@ActionParam(value="arg1", typeref=LongRef.class) long arg1)
  {
    return 100L;
  }

  @Action(name = "longFunc2", returnTyperef=LongRef.class)
  public Long LongFunc(@ActionParam(value="arg1", typeref=LongRef.class) Long arg1)
  {
    return 100L;
  }

  @Action(name = "floatFunc", returnTyperef=FloatRef.class)
  public float floatFunc(@ActionParam(value="arg1", typeref=FloatRef.class) float arg1)
  {
    return 100.0f;
  }

  @Action(name = "floatFunc2", returnTyperef=FloatRef.class)
  public Float FloatFunc(@ActionParam(value="arg1", typeref=FloatRef.class) Float arg1)
  {
    return 100.0f;
  }

  @Action(name = "doubleFunc", returnTyperef=DoubleRef.class)
  public double doubleFunc(@ActionParam(value="arg1", typeref=DoubleRef.class) double arg1)
  {
    return 100.0f;
  }

  @Action(name = "doubleFunc2", returnTyperef=DoubleRef.class)
  public Double DoubleFunc(@ActionParam(value="arg1", typeref=DoubleRef.class) Double arg1)
  {
    return 100.0;
  }

  @Action(name = "booleanFunc", returnTyperef=BooleanRef.class)
  public boolean booleanFunc(@ActionParam(value="arg1", typeref=BooleanRef.class) boolean arg1)
  {
    return true;
  }

  @Action(name = "booleanFunc2", returnTyperef=BooleanRef.class)
  public Boolean BooleanFunc(@ActionParam(value="arg1", typeref=BooleanRef.class) Boolean arg1)
  {
    return true;
  }

  @Action(name = "StringFunc", returnTyperef=StringRef.class)
  public String StringFunc(@ActionParam(value="arg1", typeref=StringRef.class) String arg1)
  {
    return "";
  }

  @Action(name = "BytesFunc", returnTyperef=BytesRef.class)
  public ByteString bytesFunc(@ActionParam(value="arg1", typeref=BytesRef.class) ByteString arg1)
  {
    return ByteString.copyAvroString("", false);
  }

  @Action(name = "IntArrayFunc", returnTyperef=IntArrayRef.class)
  public IntegerArray IntArrayFunc(@ActionParam(value="arg1", typeref=IntArrayRef.class) IntegerArray arg1)
  {
    return new IntegerArray();
  }

  @Action(name = "IntMapFunc", returnTyperef=IntMapRef.class)
  public IntegerMap IntMapFunc(@ActionParam(value="arg1", typeref=IntMapRef.class) IntegerMap arg1)
  {
    return new IntegerMap();
  }

  @Action(name = "FruitsRef", returnTyperef=FruitsRef.class)
  public Fruits FruitsFunc(@ActionParam(value="arg1", typeref=FruitsRef.class) Fruits arg1)
  {
    return Fruits.APPLE;
  }

  @Action(name = "PointRef", returnTyperef=PointRef.class)
  public Point PointFunc(@ActionParam(value="arg1", typeref=PointRef.class) Point arg1)
  {
    return new Point();
  }

  @Action(name = "CustomNonNegativeLongRef", returnTyperef=CustomNonNegativeLongRef.class)
  public CustomNonNegativeLong CustomNonNegativeLong(@ActionParam(value="arg1", typeref=CustomNonNegativeLongRef.class) CustomNonNegativeLong arg1)
  {
    return new CustomNonNegativeLong(0L);
  }
}
