/*
   Copyright (c) 2018 LinkedIn Corp.

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
package com.linkedin.entitystream;

/**
 * This exception is used and only used to notify the {@link Writer} and {@link Observer}s that the {@link Reader} has
 * cancelled reading. When {@link Reader} signals its intention to cancel reading
 * by invoking {@link ReadHandle#cancel()}, {@link Writer#onAbort(Throwable)} and {@link Observer#onError(Throwable)}
 * will be invoked with an AbortedException.
 *
 * @author Zhenkai Zhu
 */
public class AbortedException extends Exception
{
  static final long serialVersionUID = 0L;

  public AbortedException() {
    super();
  }

  public AbortedException(String message) {
    super(message);
  }

  public AbortedException(String message, Throwable cause) {
    super(message, cause);
  }

  public AbortedException(Throwable cause) {
    super(cause);
  }
}
