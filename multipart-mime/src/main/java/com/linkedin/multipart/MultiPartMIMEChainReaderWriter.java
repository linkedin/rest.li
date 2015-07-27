/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.multipart;


import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;


/**
 * The writer to consume parts from a {@link com.linkedin.multipart.MultiPartMIMEDataSourceIterator} when
 * chaining the entire {@link com.linkedin.multipart.MultiPartMIMEDataSourceIterator} itself as a non-nested data source.
 * This will read each part from the {@link com.linkedin.multipart.MultiPartMIMEDataSourceIterator} and append that part serially
 * when invoked by R2.
 *
 * @author Karim Vidhani
 */
final class MultiPartMIMEChainReaderWriter implements Writer
{
  private final MultiPartMIMEDataSourceIterator _multiPartMIMEDataSourceIterator;
  private final byte[] _normalEncapsulationBoundary;
  private WriteHandle _writeHandle;
  private MultiPartMIMEChainReaderCallback _multiPartMIMEChainReaderCallback = null;

  MultiPartMIMEChainReaderWriter(final MultiPartMIMEDataSourceIterator multiPartMIMEDataSourceIterator,
                                 final byte[] normalEncapsulationBoundary)
  {
    _multiPartMIMEDataSourceIterator = multiPartMIMEDataSourceIterator;
    _normalEncapsulationBoundary = normalEncapsulationBoundary;
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _writeHandle = wh;
  }

  @Override
  public void onWritePossible()
  {
    if (_multiPartMIMEChainReaderCallback == null)
    {
      _multiPartMIMEChainReaderCallback =
          new MultiPartMIMEChainReaderCallback(_writeHandle, _normalEncapsulationBoundary);
      //Since this is not a MultiPartMIMEDataSourceWriter we can't use the regular mechanism for reading data.
      //Instead of create a new callback that will use to write to the writeHandle using the SinglePartMIMEReader.

      _multiPartMIMEDataSourceIterator.registerDataSourceReaderCallback(_multiPartMIMEChainReaderCallback);
      //Note that by registering here, this will eventually lead to onNewDataSource() which will then requestPartData()
      //which will eventually lead to onPartDataAvailable() which will then write to the writeHandle thereby
      //honoring the original request here to write data. This initial write here will write out the boundary that this
      //writer is using followed by the headers.
    }
    else
    {
      //R2 asked us to read after initial setup is done.
      _multiPartMIMEChainReaderCallback.getCurrentDataSource().onWritePossible();
    }
  }

  @Override
  public void onAbort(Throwable e)
  {
    //If this happens, this means that there was a call on
    //{@link com.linkedin.r2.message.stream.entitystream.CompositeWriter.onAbort} which caused it to walk through
    //each of its writers and call onAbort() on each one of them. This class happened to be one of those writers.
    //
    //In terms of the original call made to the CompositeWriter to abort, this could arise due to the following:
    //1. This can be invoked by R2 if it tells the composite writer to abort.
    //2. This can also be invoked if there is a functional need to abort all data sources.

    //Regardless of how it was called we need to completely drain and drop all bytes to the ground. We can't
    //leave these bytes in the MultiPartMIMEDataSourceIterator untouched.
    _multiPartMIMEDataSourceIterator.abortAllDataSources();
  }
}