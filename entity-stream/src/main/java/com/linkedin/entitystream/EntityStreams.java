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
 * A class consists exclusively of static methods to deal with {@link EntityStream}.
 *
 * @author Zhenkai Zhu
 */
public final class EntityStreams
{
  private EntityStreams() {}

  public static <T> EntityStream<T> emptyStream()
  {
    return newEntityStream(new Writer<T>()
    {
      private WriteHandle<? super T> _wh;

      @Override
      public void onInit(WriteHandle<? super T> wh)
      {
        _wh = wh;
      }

      @Override
      public void onWritePossible()
      {
        _wh.done();
      }

      @Override
      public void onAbort(Throwable e)
      {
        // do nothing
      }
    });
  }

  /**
   * The method to create a new EntityStream with a writer for the stream.
   *
   * @param writer the writer for the stream who would provide the data
   * @return an instance of EntityStream
   */
  public static <T> EntityStream<T> newEntityStream(Writer<? extends T> writer)
  {
    return new EntityStreamImpl<T>(writer);
  }
}
