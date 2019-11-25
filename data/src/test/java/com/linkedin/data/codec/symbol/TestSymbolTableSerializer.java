/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.data.codec.symbol;

import com.linkedin.data.ByteString;
import com.linkedin.data.codec.BsonDataCodec;
import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.JacksonLICORDataCodec;
import com.linkedin.data.codec.JacksonSmileDataCodec;
import com.linkedin.data.codec.ProtobufDataCodec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestSymbolTableSerializer
{
  @DataProvider
  public static Object[][] data()
  {
    List<Object[]> list = new ArrayList<>();
    List<DataCodec> codecs = new ArrayList<>();
    codecs.add(new BsonDataCodec());
    codecs.add(new JacksonDataCodec());
    codecs.add(new JacksonSmileDataCodec());
    codecs.add(new JacksonLICORDataCodec(false));
    codecs.add(new JacksonLICORDataCodec(true));
    codecs.add(new ProtobufDataCodec());

    for (DataCodec codec : codecs)
    {
      list.add(new Object[] {codec, true});
      list.add(new Object[] {codec, false});
    }

    return list.toArray(new Object[][] {});
  }

  @Test(dataProvider = "data")
  public void testRoundtrip(DataCodec codec, boolean useRenamer) throws IOException
  {
    SymbolTable symbolTable = new InMemorySymbolTable("TestName",
        Collections.unmodifiableList(Arrays.asList("Haha", "Hehe", "Hoho")));
    ByteString serialized = SymbolTableSerializer.toByteString(codec, symbolTable);
    Function<String, String> renamer = useRenamer ? TestSymbolTableSerializer::rename : null;
    SymbolTable deserialized = SymbolTableSerializer.fromByteString(serialized, codec, renamer);

    if (renamer != null)
    {
      Assert.assertEquals(deserialized.getName(), renamer.apply(symbolTable.getName()));
      Assert.assertEquals(deserialized.getSymbolId("Haha"), 0);
      Assert.assertEquals(deserialized.getSymbolId("Hehe"), 1);
      Assert.assertEquals(deserialized.getSymbolId("Hoho"), 2);
    }
    else
    {
      Assert.assertEquals(deserialized, symbolTable);
    }
  }

  private static String rename(String input)
  {
    return input + "renamed";
  }
}
