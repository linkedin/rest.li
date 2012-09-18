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

/* $Id$ */
package com.linkedin.r2.caprep.db;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Useful for tests.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TransientDb implements DbSource, DbSink
{
  private final ConcurrentMap<Request, Response> _db = new ConcurrentHashMap<Request, Response>();

  @Override
  public void record(Request req, Response res)
  {
    _db.put(canonicalize(req), res);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Response> T replay(Request req)
  {
    return (T)_db.get(canonicalize(req));
  }

  private Request canonicalize(Request req)
  {
    return req.requestBuilder().buildCanonical();
  }
}
