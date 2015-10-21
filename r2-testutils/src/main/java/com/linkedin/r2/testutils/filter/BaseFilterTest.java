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
package com.linkedin.r2.testutils.filter;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.message.rest.RestFilter;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class BaseFilterTest
{
    private RestFilter _filter;
    private RestCountFilter _beforeFilter;
    private RestCountFilter _afterFilter;
    private FilterChain _fc;

    public void setUp() throws Exception
    {
        _filter = getFilter();
        _beforeFilter = new RestCountFilter();
        _afterFilter = new RestCountFilter();
        _fc = FilterChains.createRestChain(_beforeFilter, _filter, _afterFilter);
    }

    protected FilterChain getFilterChain()
    {
        return _fc;
    }

    protected RestCountFilter getBeforeFilter()
    {
        return _beforeFilter;
    }

    protected RestCountFilter getAfterFilter()
    {
        return _afterFilter;
    }

    protected abstract RestFilter getFilter();
}