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

package com.linkedin.data.avro;

/**
 * This Options is used for translating from {@link com.linkedin.data.DataMap} to Avro record
 * Used in {@link DataTranslator}
 * Option used for translating from {@link com.linkedin.data.DataMap} to Avro record
 * Used in {@link DataTranslator}
 */
public class DataMapToAvroRecordTranslationOptions extends DataTranslationOptions
{
  public static final PegasusToAvroDefaultFieldTranslationMode DEFAULT_DEFAULTFIELD_DATA_TRANS_MODE =
      PegasusToAvroDefaultFieldTranslationMode.TRANSLATE;

  /**
   * Default constructor
   * Sets default field's data translation mode to the default "Translate as default" mode
   */
  public DataMapToAvroRecordTranslationOptions()
  {
    this(DEFAULT_DEFAULTFIELD_DATA_TRANS_MODE);
  }

  /**
   * Constructor with default value data translation mode as argument
   * @param defaultFieldDataTranslationMode
   */
  public DataMapToAvroRecordTranslationOptions(
      PegasusToAvroDefaultFieldTranslationMode defaultFieldDataTranslationMode)
  {
    _defaultFieldDataTranslationMode = defaultFieldDataTranslationMode;
  }

  /**
   * Getter method for default field translation mode
   * @return defaultFieldDataTranslationMode in current settings
   */
  public PegasusToAvroDefaultFieldTranslationMode getDefaultFieldDataTranslationMode()
  {
    return _defaultFieldDataTranslationMode;
  }

  /**
   * Setter for default field translation mode
   * @param defaultFieldDataTranslationMode
   */
  public void setDefaultFieldDataTranslationMode(
      PegasusToAvroDefaultFieldTranslationMode defaultFieldDataTranslationMode)
  {
    _defaultFieldDataTranslationMode = defaultFieldDataTranslationMode;
  }


  private PegasusToAvroDefaultFieldTranslationMode _defaultFieldDataTranslationMode;
}
