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

public final class DataMapToAvroRecordTranslationOptionsBuilder {
  private PegasusToAvroDefaultFieldTranslationMode _defaultFieldDataTranslationMode
      = DataMapToAvroRecordTranslationOptions.DEFAULT_DEFAULTFIELD_DATA_TRANS_MODE;

  public DataMapToAvroRecordTranslationOptionsBuilder() {
  }

  public static DataMapToAvroRecordTranslationOptionsBuilder aDataMapToAvroRecordTranslationOptions() {
    return new DataMapToAvroRecordTranslationOptionsBuilder();
  }

  public DataMapToAvroRecordTranslationOptionsBuilder defaultFieldDataTranslationMode(
      PegasusToAvroDefaultFieldTranslationMode defaultFieldDataTranslationMode) {
    this._defaultFieldDataTranslationMode = defaultFieldDataTranslationMode;
    return this;
  }

  public DataMapToAvroRecordTranslationOptions build() {
    DataMapToAvroRecordTranslationOptions dataMapToAvroRecordTranslationOptions =
        new DataMapToAvroRecordTranslationOptions();
    dataMapToAvroRecordTranslationOptions.setDefaultFieldDataTranslationMode(_defaultFieldDataTranslationMode);
    return dataMapToAvroRecordTranslationOptions;
  }
}
