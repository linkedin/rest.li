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

/**
 * $id$
 */
package com.linkedin.data.transform;


import com.linkedin.data.DataMap;


/**
 * @deprecated Please use {@link com.linkedin.data.transform.DataComplexProcessor} instead
 *
 * @author jodzga
 *
 */
@Deprecated
public class DataMapProcessor extends DataComplexProcessor
{
  public DataMapProcessor(InstructionScheduler instructionScheduler, Interpreter interpreter, DataMap program, DataMap data)
  {
    super(instructionScheduler, interpreter, program, data);
  }

  public DataMapProcessor(Interpreter interpreter, DataMap program, DataMap data)
  {
    super(interpreter, program, data);
  }
}
