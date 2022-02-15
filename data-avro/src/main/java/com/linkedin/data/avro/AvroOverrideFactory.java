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


import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.Name;


/**
 * Abstract class for factory that creates {@link AvroOverride}.
 *
 * Depending on how it is used, subclass must override {@link #emitMessage}.
 * @see AvroOverride
 */
/* package scoped */
abstract class AvroOverrideFactory
{
  private boolean _instantiateCustomDataTranslator;

  abstract void emitMessage(String format, Object... args);

  protected void setInstantiateCustomDataTranslator(boolean instantiateCustomDataTranslator)
  {
    _instantiateCustomDataTranslator = instantiateCustomDataTranslator;
  }

  AvroOverride createFromDataSchema(DataSchema schema)
  {
    AvroOverride avroOverride = null;
    Map<String, Object> properties = schema.getProperties();
    Object avro = properties.get("avro");
    if (avro != null)
    {
      boolean ok = true;
      Object avroTranslator = null;
      Object avroSchema = null;
      if (avro.getClass() != DataMap.class)
      {
        emitMessage("%1$s has \"avro\" property whose value is not a JSON object", schema);
        ok = false;
      }
      else
      {
        DataMap avroMap = (DataMap) avro;
        avroTranslator = avroMap.get("translator");
        avroSchema = avroMap.get("schema");
      }

      // process "schema" property

      String avroSchemaFullName = null;
      DataMap avroSchemaDataMap = null;
      if (avroSchema != null)
      {
        if (avroSchema.getClass() != DataMap.class)
        {
          emitMessage("\"schema\" property is not a JSON object, value is %1$s", avroSchema);
          ok = false;
        }
        else
        {
          avroSchemaDataMap = (DataMap) avroSchema;
          Object name = avroSchemaDataMap.get("name");
          if (name == null)
          {
            emitMessage("\"name\" property of \"schema\" property is required");
            ok = false;
          }
          else if (name.getClass() != String.class)
          {
            emitMessage("\"name\" property of \"schema\" property is not a string, value is %1$s", name);
            ok = false;
          }
          else
          {
            Object namespace = avroSchemaDataMap.get("namespace");
            if (namespace != null)
            {
              if (namespace.getClass() != String.class)
              {
                emitMessage("\"namespace\" property of \"schema\" property is not a string, value is %1$s", namespace);
                ok = false;
              }
              else if (Name.isValidNamespace((String) namespace) == false)
              {
                emitMessage("%1$s is not a valid namespace", namespace);
                ok = false;
              }
            }
            avroSchemaFullName = namespace == null ? (String) name : namespace + "." + name;
          }
        }
      }

      // process "translator" property

      String customDataTranslatorClassName = null;
      CustomDataTranslator customDataTranslator = null;
      if (avroTranslator != null)
      {
        if (avroTranslator.getClass() != DataMap.class)
        {
          emitMessage("\"translator\" property is not a JSON object, value is %1$s", avroTranslator);
          ok = false;
        }
        else
        {
          DataMap avroTranslatorMap = (DataMap) avroTranslator;
          Object avroTranslatorClass = avroTranslatorMap.get("class");
          if (avroTranslatorClass == null)
          {
            emitMessage("\"class\" property of \"translator\" property is required");
            ok = false;
          }
          else if (avroTranslatorClass.getClass() != String.class)
          {
            emitMessage("\"class\" property of \"translator\" property is not a string, value is %1$s", avroTranslatorClass);
            ok = false;
          }
          else
          {
            customDataTranslatorClassName = (String) avroTranslatorClass;
            if (_instantiateCustomDataTranslator)
            {
              try
              {
                Class<?> translatorClass = Class.forName(customDataTranslatorClassName, true, Thread.currentThread().getContextClassLoader());
                customDataTranslator = (CustomDataTranslator) translatorClass.getDeclaredConstructor().newInstance();
              }
              catch (ClassCastException e)
              {
                emitMessage("%1$s is not a %2$s", customDataTranslatorClassName, CustomDataTranslator.class.getName());
                ok = false;
              }
              catch (ClassNotFoundException e)
              {
                emitMessage("%1$s class not found", customDataTranslatorClassName);
                ok = false;
              }
              catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e)
              {
                emitMessage("%1$s cannot be instantiated due to %2$s", customDataTranslatorClassName, e.getClass().getName());
                ok = false;
              }
            }
          }
        }
      }

      if (ok)
      {
        // make both "schema" and "translator" are present if either is present
        if ((avroSchemaFullName != null && customDataTranslatorClassName == null) ||
            (avroSchemaFullName == null && customDataTranslatorClassName != null))
        {
          emitMessage("both \"translator\" and \"schema\" properties of \"avro\" are required if either is present");
          ok = false;
        }
        if (schema.getType() != DataSchema.Type.RECORD)
        {
          emitMessage("%1$s has \"avro\" properties \"translator\" and \"schema\" that are only applicable to records", schema);
          ok = false;
        }
      }
      if (ok)
      {
        avroOverride = new AvroOverride(avroSchemaFullName, avroSchemaDataMap, customDataTranslatorClassName, customDataTranslator);
      }
    }
    return avroOverride;
  }
}
