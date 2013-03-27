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
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.validator.AbstractValidator;
import com.linkedin.data.schema.validator.ValidatorContext;


/**
 * Validate if the data is empty.
 *
 * @author Keren Jin
 */
public class EmptyRecordValidator extends AbstractValidator
{
  public EmptyRecordValidator(DataMap config)
  {
	super(config);
  }

  @Override
  public void validate(ValidatorContext context)
  {
    final Object dataObject = context.dataElement().getValue();
    if (!(dataObject instanceof DataMap))
    {
      context.addResult(new Message(context.dataElement().path(),
                                    "%1$s expects data to be a DataMap, data is %2$s",
                                    getClass().getName(), dataObject));
    }
    else if (!((DataMap) dataObject).isEmpty())
    {
      context.addResult(new Message(context.dataElement().path(),
                                    "Data %1$s is expected to be empty",
                                    dataObject));
    }
  }
}
