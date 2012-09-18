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

/**
 * $Id: $
 */

package com.linkedin.common.callback;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * An exception representing multiple exceptions. Useful for example to deliver multiple
 * exceptions to a callback interface which only accepts a single Exception.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class MultiException extends Exception
{
  private static final long serialVersionUID = 1L;

  private final Collection<? extends Throwable> _causes;

  public MultiException(String message, Collection<? extends Throwable> causes)
  {
    super(message);
    _causes = (causes == null ? Collections.<Throwable>emptyList() : causes);

    // Let the first exception be the cause so at least something shows up in the stack
    // trace; we can't just override printStackTrace() because OUR printStackTrace() is
    // not invoked when this exception is the cause of another.
    Iterator<? extends Throwable> i = _causes.iterator();
    if (i.hasNext())
    {
      initCause(i.next());
    }
  }
  public MultiException(Collection<? extends Throwable> causes)
  {
    this(null, causes);
  }

  public Collection<? extends Throwable> getCauses()
  {
    return _causes;
  }

  @Override
  public String toString()
  {
    // MultiException: Xyz failed (multiple causes follow; only first is shown in stack
    // trace): [java.lang.FooException: bar, java.lang.BazException: quux]
    return super.toString()
        + " (multiple causes follow; only first is shown in stack trace): " + _causes;
  }
}
