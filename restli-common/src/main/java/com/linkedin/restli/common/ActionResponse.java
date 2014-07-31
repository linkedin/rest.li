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

package com.linkedin.restli.common;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;


/**
 * A response from an action. Has a single value field, of template or primitive type.
 *
 * @author Eran Leshem
 */
public final class ActionResponse<T> extends DynamicRecordTemplate
{
  public static final String VALUE_NAME = "value";

  private final FieldDef<T> _valueFieldDef;

  /**
   * Initialize an ActionResponse based on the data returned from the RestLi
   * server, the expected return type of the Action, and the RecordDataSchema for the
   * Action return.
   *
   * @param data DataMap of the returned data
   * @param valueFieldDef the {@link FieldDef} representing the action return.
   * @param schema the {@link RecordDataSchema} of the Action return
   */
  public ActionResponse(DataMap data, FieldDef<T> valueFieldDef, RecordDataSchema schema)
  {
    super(data, schema);
    _valueFieldDef = valueFieldDef;
  }

  /**
   * Initialize an ActionResponse based on the expected return type of the action
   *
   * @param valueFieldDef the {@link FieldDef} representing the action return.
   * @param schema the {@link RecordDataSchema} of the Action return
   */
  public ActionResponse(FieldDef<T> valueFieldDef, RecordDataSchema schema)
  {
    this(new DataMap(), valueFieldDef, schema);
  }

  /**
   * Initialize an ActionResponse based on the value result of the Action.
   *
   * @param value Class of the type that the Action is expected to return
   * @param valueFieldDef the {@link FieldDef} representing the action return.
   * @param schema the {@link RecordDataSchema} of the Action return
   */
  public ActionResponse(T value, FieldDef<T> valueFieldDef, RecordDataSchema schema)
  {
    this (valueFieldDef, schema);
    setValue(value);
  }

  /**
   * Get the value result of the Action.
   *
   * @return value of type T
   */
  public T getValue()
  {
    return getValue(_valueFieldDef);
  }

  /**
   * Set the value result of the Action.
   *
   * @param value value of type T
   */
  public void setValue(T value)
  {
    setValue(_valueFieldDef, value);
  }
}
