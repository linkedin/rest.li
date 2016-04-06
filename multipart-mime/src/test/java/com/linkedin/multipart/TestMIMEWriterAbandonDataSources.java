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


import org.testng.annotations.Test;

import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


/**
 * This test verifies that upon invocation of {@link MultiPartMIMEWriter#abortAllDataSources(java.lang.Throwable)},
 * that each type of data source is abandoned appropriately.
 *
 * @author Karim Vidhani
 */
public class TestMIMEWriterAbandonDataSources extends AbstractMIMEUnitTest
{
  @Test
  public void abandonAllTypesDataSource() throws Exception
  {
    //Create a MultiPartMIMEDataSourceIterator, SinglePartMIMEReader and custom data source all as data sources for a writer.
    final MultiPartMIMEDataSourceIterator multiPartMIMEReader = mock(MultiPartMIMEDataSourceIterator.class);
    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader = mock(MultiPartMIMEReader.SinglePartMIMEReader.class);
    final MultiPartMIMEDataSourceWriter multiPartMIMEDataSourceWriter = mock(MultiPartMIMEDataSourceWriter.class);

    Mockito.doCallRealMethod().when(singlePartMIMEReader).onAbort(any(Throwable.class));

    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder("abc", "123")
        .appendDataSource(singlePartMIMEReader)
        .appendDataSourceIterator(multiPartMIMEReader)
        .appendDataSource(multiPartMIMEDataSourceWriter)
        .build();

    final Throwable throwable = new NullPointerException("Some exception");
    writer.abortAllDataSources(throwable);

    //The MultiPartMIMEReader should have been abandoned.
    verify(multiPartMIMEReader, times(1)).abandonAllDataSources();

    //The SinglePartMIMEReader should also have its part drained.
    verify(singlePartMIMEReader, times(1)).drainPart();
    verify(singlePartMIMEReader, times(1)).onAbort(throwable);
    verify(singlePartMIMEReader, times(1)).dataSourceHeaders();

    //The custom data source should have been told to abort.
    verify(multiPartMIMEDataSourceWriter, times(1)).onAbort(throwable);
    verify(multiPartMIMEDataSourceWriter, times(1)).dataSourceHeaders();

    verifyNoMoreInteractions(multiPartMIMEReader);
    verifyNoMoreInteractions(singlePartMIMEReader);
    verifyNoMoreInteractions(multiPartMIMEDataSourceWriter);
  }
}
