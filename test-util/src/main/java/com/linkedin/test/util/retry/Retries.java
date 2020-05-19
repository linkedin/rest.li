/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.test.util.retry;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;


/**
 * Allows N retries for a given test method. Subclass implementations must specify the value of N.
 *
 * Note that the same instance is used for all iterations of a test method, meaning that even if there are multiple
 * iterations (e.g. data provider provides multiple sets of input) only N retries will be allowed.
 *
 * @author Evan Williams
 */
public abstract class Retries implements IRetryAnalyzer
{
  private final int _allowedRetries;
  private int _numRetries;

  protected Retries(int allowedRetries)
  {
    _allowedRetries = allowedRetries;
    _numRetries = 0;
  }

  @Override
  public boolean retry(ITestResult result)
  {
    return _numRetries++ < _allowedRetries;
  }
}
