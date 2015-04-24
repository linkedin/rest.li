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
package test.r2.caprep;

import com.linkedin.r2.caprep.db.TransientDb;
import com.linkedin.r2.filter.Filter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;
import org.testng.annotations.BeforeMethod;
import com.linkedin.r2.testutils.filter.BaseFilterTest;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class AbstractCapRepTest extends BaseFilterTest
{
  private TransientDb _db;
  private Filter _filter;

  @BeforeMethod
  public void setUp() throws Exception
  {
    _db = new TransientDb();
    _filter = createFilter(_db);
    super.setUp();
  }

  protected TransientDb getDb()
  {
    return _db;
  }

  @Override
  protected Filter getFilter()
  {
    return _filter;
  }

  protected abstract Filter createFilter(TransientDb db);

  protected abstract Request request();

  protected abstract Response response();
}
