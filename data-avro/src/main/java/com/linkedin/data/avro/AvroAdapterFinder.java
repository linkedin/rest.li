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

package com.linkedin.data.avro;


import java.lang.reflect.Constructor;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;


/**
 * Finds an instance of {@link AvroAdapter}.
 *
 * <p>
 * There is a one-time initialization that is performed on class instantiation.
 * After initialization, {@link #getAvroAdapter()} always returns the same
 * {@link AvroAdapter}.
 * <p>
 * The one-time initialization looks for the adapter system property
 * (see {@link #AVRO_ADAPTER_PROPERTY}. If the adapter property is present,
 * it will attempt to construct an instance of the class provided by the adapter property.
 * This instance has to be a subclass of {@link AvroAdapter} and it will be returned
 * by calls to {@link #getAvroAdapter()}.
 * <p>
 * If the adapter property is absent, the one-time initialization will look
 * for the chooser property (@see {@link #AVRO_ADAPTER_CHOOSER_PROPERTY}.
 * If the chooser property is present, it will construct an instance of
 * the class provided by the chooser property and invoke
 * {@link com.linkedin.data.avro.AvroAdapterChooser#getAvroAdapter()}
 * on the constructed {@link AvroAdapterChooser} to obtain an {@link AvroAdapter}
 * that will be returned by calls to {@link #getAvroAdapter()}.
 * <p>
 * If neither system property has been specified, then the default chooser
 * built into this class will be used to determine the appropriate
 * builti-in {@link AvroAdapter} to use.
 *
 * @deprecated use {@link com.linkedin.avro.compatibility.AvroCompatibilityHelper} instead.
 */
@Deprecated
public class AvroAdapterFinder
{
  public static final String AVRO_ADAPTER_CHOOSER_PROPERTY = "com.linkedin.data.avro.AvroAdapterChooser";
  public static final String AVRO_ADAPTER_PROPERTY = "com.linkedin.data.avro.AvroAdapter";

  private static final AvroAdapter _avroAdapter = avroAdapter();

  // package scope to facilitate unit testing
  static AvroAdapter avroAdapter()
  {
    AvroAdapter avroAdapter;
    String adapterClassName = System.getProperty(AVRO_ADAPTER_PROPERTY);
    if (adapterClassName != null)
    {
      avroAdapter = newInstance(adapterClassName);
    }
    else
    {
      String chooserClassName = System.getProperty(AVRO_ADAPTER_CHOOSER_PROPERTY);
      AvroAdapterChooser resolver;
      if (chooserClassName == null)
      {
        resolver = new DefaultAvroAdapterChooser();
      }
      else
      {
        resolver = newInstance(chooserClassName);
      }
      avroAdapter = resolver.getAvroAdapter();
    }
    return avroAdapter;
  }

  /**
   * Get {@link AvroAdapter} determined when class was initialized
   *
   * @return the {@link AvroAdapter} determined when class was initialized.
   */
  public static AvroAdapter getAvroAdapter()
  {
    return _avroAdapter;
  }

  /**
   * The default {@link AvroAdapterChooser} when no system property has
   * been specified for chooser or adapter.
   * <p>
   * This chooser can choose between Avro 1.4 and Avro 1.6. It chooses
   * Avro 1.4 if {@link GenericData.EnumSymbol} has a constructor with
   * one parameter and the parameter is a String. It chooses Avro 1.6
   * if {@link GenericData.EnumSymbol} has a constructor with two
   * parameters and the 1st parameter is an {@link Schema} and the
   * 2nd parameter is a String. If both constructors are present, then
   * it throws an {@link IllegalStateException}.
   */
  private static class DefaultAvroAdapterChooser implements AvroAdapterChooser
  {
    @Override
    public AvroAdapter getAvroAdapter()
    {
      Class<GenericData.EnumSymbol> enumSymbolClass = GenericData.EnumSymbol.class;
      Constructor<?>[] enumSymbolConstructors = enumSymbolClass.getConstructors();
      boolean isAvro_1_4 = false;
      boolean isAvro_1_6 = false;
      for (Constructor<?> constructor : enumSymbolConstructors)
      {
        Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0] == String.class)
        {
          isAvro_1_4 = true;
        }
        else if (params.length == 2 && params[0] == Schema.class && params[1] == String.class)
        {
          isAvro_1_6 = true;
        }
      }
      if (isAvro_1_6 && isAvro_1_4)
      {
        throw new IllegalStateException("Both one and two arg constructor are present, cannot determine which AvroAdapter to choose");
      }
      String adapterName = isAvro_1_4 ? "AvroAdapter_1_4" : "AvroAdapter_1_6";
      String adapterFullName = getClass().getPackage().getName() + "." + adapterName;
      return newInstance(adapterFullName);
    }
  };

  protected static <T> T newInstance(String fullName)
  {
    try
    {
      Class<?> clazz = Class.forName(fullName);
      @SuppressWarnings("unchecked")
      T result = (T) clazz.newInstance();
      return result;
    }
    catch (ClassNotFoundException e)
    {
      throw new IllegalStateException("Unable to construct " + fullName, e);
    }
    catch (InstantiationException e)
    {
      throw new IllegalStateException("Unable to construct " + fullName, e);
    }
    catch (IllegalAccessException e)
    {
      throw new IllegalStateException("Unable to construct " + fullName, e);
    }
  }
}
