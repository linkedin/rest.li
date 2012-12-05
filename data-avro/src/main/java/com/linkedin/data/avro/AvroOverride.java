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


import com.linkedin.data.DataMap;


/**
 * Avro schema and data translation overrides.
 *
 * A custom Avro schema is provided via as follows:
 * <pre>
 *   {
 *     "type" : "record",
 *     "name" : "AnyRecord",
 *     "fields" : [ ... ],
 *     ...
 *     "avro" : {
 *       "schema" : {
 *         "type" : "record",
 *         "name" : "AnyRecord",
 *         "fields" : [
 *           {
 *             "name" : "type",
 *             "type" : "string"
 *           },
 *           {
 *             "name" : "value",
 *             "type" : "string"
 *           }
 *         ]
 *       },
 *       "translator" : {
 *         "class" : "com.foo.bar.AnyRecordTranslator"
 *       }
 *     }
 *   }
 * </pre>
 *
 * If the "avro" property is present, it provides overrides that
 * override the default schema and data translation. The "schema"
 * property provides the override Avro schema. The "translator"
 * property provides the class for that will be used to translate
 * from the to and from Pegasus and Avro data representations.
 * Both of these properties are required if either is present.
 *
 * If an override Avro schema is specified, the schema translation
 * inlines the value of the "schema" property into the translated
 * Avro schema.
 *
 * If a translator class is specified, the data translator will
 * construct an instance of this class and invoke this instance
 * to translate the data between Pegasus and Avro representations.
 */
/* package scoped */
class AvroOverride
{
  private final String _avroSchemaFullName;
  private final DataMap _avroSchemaDataMap;
  private final String _customDataTranslatorClassName;
  private final CustomDataTranslator _customDataTranslator;
  private int _accessCount;

  AvroOverride(String avroSchemaFullName, DataMap avroSchemaDataMap, String customDataTranslatorClassName, CustomDataTranslator customDataTranslator)
  {
    _avroSchemaFullName = avroSchemaFullName;
    _avroSchemaDataMap = avroSchemaDataMap;
    _customDataTranslatorClassName = customDataTranslatorClassName;
    _customDataTranslator = customDataTranslator;
    _accessCount = 0;
  }

  String getAvroSchemaFullName()
  {
    return _avroSchemaFullName;
  }

  DataMap getAvroSchemaDataMap()
  {
    return _avroSchemaDataMap;
  }

  String getCustomDataTranslatorClassName()
  {
    return _customDataTranslatorClassName;
  }

  CustomDataTranslator getCustomDataTranslator()
  {
    return _customDataTranslator;
  }

  int getAccessCount()
  {
    return _accessCount;
  }

  void incrementAccessCount()
  {
    _accessCount++;
  }
}
