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
 * Writer is the producer of data for an {@link EntityStream}. It writes data through the provided {@link WriteHandle}
 * when data is requested through {@link #onWritePossible} method.
 *
 * It is expected that the methods {@link #onInit}, {@link #onWritePossible} and {@link #onAbort} are invoked in a
 * thread-safe manner, usually by EntityStream.
 *
 * @author Zhenkai Zhu
 */
public interface Writer<T>
{
  /**
   * This is called when a Reader is set for the EntityStream.
   *
   * @param wh the handle to write data to the EntityStream.
   */
  void onInit(final WriteHandle<? super T> wh);

  /**
   * Invoked when it it possible to write data.
   *
   * This method will be invoked the first time as soon as data can be written to the WriteHandle.
   * Subsequent invocations will only occur if a call to {@link WriteHandle#remaining()} has returned 0
   * and it has since become possible to write data.
   */
  void onWritePossible();

  /**
   * Invoked when the entity stream is aborted.
   * Usually writer could do clean up to release any resource it has acquired.
   *
   * @param e The throwable that caused the entity stream to abort. If the abortion is caused by Reader cancelling
   *          reading, this Throwable should be an {@link AbortedException}.
   *
   */
  void onAbort(Throwable e);
}
