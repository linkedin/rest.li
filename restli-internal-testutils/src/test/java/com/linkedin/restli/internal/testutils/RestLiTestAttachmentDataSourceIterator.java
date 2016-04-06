/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.internal.testutils;


import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;
import com.linkedin.restli.common.attachments.RestLiDataSourceIteratorCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Test utility class to represent multiple attachments for rest.li attachment streaming.
 *
 * @author Karim Vidhani
 */
public class RestLiTestAttachmentDataSourceIterator implements RestLiDataSourceIterator
{
  private final List<RestLiTestAttachmentDataSource> _dataSources;
  private int _currentDataSourceIndex;
  private final Exception _exceptionToThrowOnAbandon;
  private RestLiDataSourceIteratorCallback _restLiDataSourceIteratorCallback;
  private boolean _abandoned = false;

  public RestLiTestAttachmentDataSourceIterator(final List<RestLiTestAttachmentDataSource> dataSources,
                                                final Exception exceptionToThrowOnAbandon)
  {
    if (dataSources.size() == 0)
    {
      throw new IllegalArgumentException("Must provide at least one data source");
    }

    _dataSources = Collections.unmodifiableList(new ArrayList<>(dataSources));
    _currentDataSourceIndex = 0;
    _exceptionToThrowOnAbandon = exceptionToThrowOnAbandon;
  }

  @Override
  public void abandonAllDataSources()
  {
    _abandoned = true;
    for (final RestLiTestAttachmentDataSource dataSource : _dataSources)
    {
      dataSource.onAbort(_exceptionToThrowOnAbandon);
    }
  }

  @Override
  public void registerDataSourceReaderCallback(RestLiDataSourceIteratorCallback callback)
  {
    _restLiDataSourceIteratorCallback = callback;
    _restLiDataSourceIteratorCallback.onNewDataSourceWriter(_dataSources.get(_currentDataSourceIndex++));
  }

  void moveToNextDataSource()
  {
    if (_currentDataSourceIndex == _dataSources.size())
    {
      _restLiDataSourceIteratorCallback.onFinished();
      return;
    }
    _restLiDataSourceIteratorCallback.onNewDataSourceWriter(_dataSources.get(_currentDataSourceIndex++));
  }

  public boolean dataSourceIteratorAbandoned()
  {
    return _abandoned;
  }
}