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

package com.linkedin.data.schema.validator;


import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.it.IterationOrder;
import com.linkedin.data.it.ObjectIterator;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.DataTemplate;
import java.util.Collections;
import java.util.List;

/**
 * Use {@link com.linkedin.data.schema.validation.ValidateDataAgainstSchema} instead of this class.
 */
@Deprecated
public class ValidateWithValidator
{
  private ValidateWithValidator()
  {
  }

  @Deprecated
  public static ValidationResult validate(Object value, DataSchema schema, Validator validator)
  {
    return validate(new SimpleDataElement(value, schema), validator);
  }

  @Deprecated
  public static ValidationResult validate(DataTemplate value, Validator validator)
  {
    return validate(value.data(), value.schema(), validator);
  }

  @Deprecated
  public static ValidationResult validate(DataElement element, Validator validator)
  {
    ObjectIterator iterator = new ObjectIterator(element, IterationOrder.POST_ORDER);
    Result result = new Result();
    result._fixed = element.getValue();
    Context ctx = new Context(result);
    for (DataElement dataElement = iterator.next(); dataElement != null; dataElement = iterator.next())
    {
      if (dataElement.getSchema() != null)
      {
        ctx._dataElement = dataElement;
        validator.validate(ctx);
      }
    }
    return result;
  }

  private static class Context implements ValidatorContext
  {
    private DataElement _dataElement;
    private final Result _result;
    private final ValidationOptions _options = new ValidationOptions();

    Context(Result result)
    {
      _result = result;
    }

    @Override
    public DataElement dataElement()
    {
      return _dataElement;
    }

    @Override
    public void addResult(Message message)
    {
      _result._messages.add(message);
      if (message.isError())
        _result._valid = false;
    }

    @Override
    public ValidationOptions validationOptions()
    {
      return _options;
    }

    @Override
    public void setHasFix(boolean value)
    {
      _result._hasFix = value;
    }

    @Override
    public void setHasFixupReadOnlyError(boolean value)
    {
      _result._hasFixupReadOnlyError = value;
    }
  }

  private static class Result implements ValidationResult
  {
    private final List<Message> _messages = new MessageList();

    private boolean _valid = true;
    private Object _fixed = null;
    private boolean _hasFix = false;
    private boolean _hasFixupReadOnlyError = false;

    @Override
    public boolean hasFix()
    {
      return _hasFix;
    }

    @Override
    public boolean hasFixupReadOnlyError()
    {
      return _hasFixupReadOnlyError;
    }

    @Override
    public Object getFixed()
    {
      return _fixed;
    }

    @Override
    public boolean isValid()
    {
      return _valid;
    }

    @Override
    public List<Message> getMessages()
    {
      return Collections.unmodifiableList(_messages);
    }
  }
}
