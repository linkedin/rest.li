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


import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;


/**
 * Used to chain a {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader} as a data source.
 *
 * @author Karim Vidhani
 */
final class SinglePartMIMEChainReaderCallback implements SinglePartMIMEReaderCallback
{
  private final WriteHandle _writeHandle;
  private final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;

  @Override
  public void onPartDataAvailable(ByteString partData)
  {
    _writeHandle.write(partData);
    if (_writeHandle.remaining() > 0)
    {
      //No danger of a stack overflow due to the iterative invocation technique in MultiPartMIMEReader
      _singlePartMIMEReader.requestPartData();
    }
  }

  @Override
  public void onFinished()
  {
    _writeHandle.done();
  }

  @Override
  public void onAbandoned()
  {
    //This can happen if the SinglePartMIMEReader this callback was registered with was used as a data source and it was
    //told to abandon and the abandon finished.
    //We don't need to take any action here.
  }

  @Override
  public void onStreamError(Throwable throwable)
  {
    //If there was an error while this single part was being read then we notify the writeHandle.
    //Note that the MultiPartMIMEReader and SinglePartMIMEReader have already been rendered
    //inoperable due to this. We just need to let the writeHandle know of this problem.
    //Also note that the MultiPartMIMEReaderCallback that was associated with this SinglePartMIMEReader
    //has also been informed of this. Therefore if an application developer had chosen to send just
    //this part further down they could recover as well.
    _writeHandle.error(throwable);
  }

  SinglePartMIMEChainReaderCallback(final WriteHandle writeHandle,
                                    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
  {
    _singlePartMIMEReader = singlePartMIMEReader;
    _writeHandle = writeHandle;
  }
}