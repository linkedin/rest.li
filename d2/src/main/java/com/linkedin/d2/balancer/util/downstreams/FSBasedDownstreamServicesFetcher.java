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

package com.linkedin.d2.balancer.util.downstreams;

import com.linkedin.common.callback.SuccessCallback;
import com.linkedin.d2.balancer.util.FileSystemDirectory;
import java.util.List;


/**
 * It relies on the internal FileStore, which keeps a list of the called services in the previous runs.
 * As a consequence, if the service has not run previously on the current machine, no service will be returned.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class FSBasedDownstreamServicesFetcher implements DownstreamServicesFetcher
{
  private final String _d2FsPath;
  private final String _d2ServicePath;

  public FSBasedDownstreamServicesFetcher(String d2FsPath, String d2ServicePath)
  {
    _d2FsPath = d2FsPath;
    _d2ServicePath = d2ServicePath;
  }

  @Override
  public void getServiceNames(SuccessCallback<List<String>> callback)
  {
    FileSystemDirectory fsDirectory = new FileSystemDirectory(_d2FsPath, _d2ServicePath);
    callback.onSuccess(fsDirectory.getServiceNames());
  }
}
