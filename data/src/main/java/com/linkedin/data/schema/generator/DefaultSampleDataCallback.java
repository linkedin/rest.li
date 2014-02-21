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

package com.linkedin.data.schema.generator;


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Default implementation of {@link SampleDataCallback}.
 *
 * @author Keren Jin
 */
public class DefaultSampleDataCallback implements SampleDataCallback
{
  /**
   * @param stringPool map from field name regular expression to candidate strings.
   *                   note that no precedence is maintained on the regular expressions
   * @param defaultStrings candidate strings when all regular expressions fail to match
   */
  public DefaultSampleDataCallback(Map<String, String[]> stringPool, String[] defaultStrings)
  {
    _stringPool = stringPool;
    _defaultStrings = defaultStrings;

    compilePatterns(_stringPool.keySet());
  }

  private int nonNegative(int max)
  {
    return Math.abs(_random.nextInt(max)) + 1;
  }

  private <T> T valueFromArray(T[] array)
  {
    return array[nonNegative(array.length - 1)];
  }

  @Override
  public boolean getBoolean(String fieldName)
  {
    return _random.nextBoolean();
  }

  @Override
  public int getInteger(String fieldName)
  {
    return nonNegative(10);
  }

  @Override
  public long getLong(String fieldName)
  {
    return nonNegative(10);
  }

  private final Float[] _floats = { 3.14f, 2.71f, 1.41f, 1.61f };

  @Override
  public float getFloat(String fieldName)
  {
    return valueFromArray(_floats);
  }

  @Override
  public double getDouble(String fieldName)
  {
    return valueFromArray(_floats);
  }

  @Override
  public ByteString getBytes(String fieldName)
  {
    return ByteString.copy(getString(fieldName).getBytes(Data.UTF_8_CHARSET));
  }

  @Override
  public String getString(String fieldName)
  {
    String[] candidateStrings = null;

    if (fieldName != null)
    {
      for (Map.Entry<String, String[]> poolPair : _stringPool.entrySet())
      {
        if (_compiledPatterns.get(poolPair.getKey()).matcher(fieldName).find())
        {
          candidateStrings = poolPair.getValue();
          break;
        }
      }
    }

    if (candidateStrings == null)
    {
      candidateStrings = _defaultStrings;
    }

    return valueFromArray(candidateStrings);
  }

  @Override
  public ByteString getFixed(String fieldName, FixedDataSchema schema)
  {
    final byte[] bytes = new byte[schema.getSize()];
    _random.nextBytes(bytes);
    return ByteString.copy(bytes);
  }

  @Override
  public String getEnum(String fieldName, EnumDataSchema enumDataSchema)
  {
    List<String> symbols = enumDataSchema.getSymbols();
    return symbols.get(nonNegative(symbols.size() - 1));
  }

  private DefaultSampleDataCallback()
  {
    _stringPool = new HashMap<String, String[]>();
    _stringPool.put("url|link", new String[] {"http://www.example.com", "http://rest.li"});
    _stringPool.put("name", new String[] {"John", "Doe"});
    _stringPool.put("email|emailAddress|email_address", new String[] {"foo@example.com", "bar@rest.li"});
    _stringPool.put("description|summary", new String[] {
        "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
        // http://hipsteripsum.me/
        "Aesthetic sustainable raw denim messenger bag narwhal 8-bit, ethnic vegan craft beer quinoa selvage authentic dolor.",
        "Vegan commodo kogi twee, consectetur single-origin coffee readymade swag.",
        "Organic american apparel eiusmod, high life craft beer mollit polaroid lo-fi sed culpa.",
        "Lo-fi vinyl 3 wolf moon hoodie PBR eiusmod farm-to-table next level, est aliqua sriracha pour-over raw denim"
    });
    _defaultStrings = new String[] {"foo", "bar", "baz"};

    compilePatterns(_stringPool.keySet());
  }

  private void compilePatterns(Set<String> fieldNameRegexs)
  {
    for (String regex : fieldNameRegexs)
    {
      _compiledPatterns.put(regex, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }
  }

  public static final SampleDataCallback INSTANCE = new DefaultSampleDataCallback();
  private static final Random _random = new Random();

  private final Map<String, Pattern> _compiledPatterns = new HashMap<String, Pattern>();
  private final Map<String, String[]> _stringPool;
  private final String[] _defaultStrings;
}
