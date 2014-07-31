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

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/**
 * LinkedIn elects to include this software in this distribution under the CDDL license.
 *
 * Modifications:
 *   Repackaged original source under com.linkedin.jersey package.
 *   Removed dependency on javax.ws.rs interfaces
 *   Added JavaDoc documentation to conform to Pegasus style guidelines
 */

/**
 * Core utilities for Uri-related classes
 */
package com.linkedin.jersey.core.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link MultivaluedMap} where keys and values are
 * instances of String.
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class MultivaluedMap
        extends HashMap<String, List<String>>
{

    static final long serialVersionUID = -6052320403766368902L;

    /**
     * initialize and return an empty MultivaluedMap
     */
    public MultivaluedMap() { }

    /**
     * Copy constructor for MultivaluedMap
     * @param that MultivaluedMap to copy
     */
    @SuppressWarnings("unchecked")
    public MultivaluedMap(MultivaluedMap that) {
        for (Map.Entry<String, List<String>> e : that.entrySet()) {
            this.put(e.getKey(), new ArrayList(e.getValue()));
        }
    }

    // MultivaluedMap

    /**
     * Replace all existing values for key with the single given value
     * @param key the key in the map
     * @param value the value replace key's values with
     */
    public final void putSingle(String key, String value) {
        List<String> l = getList(key);

        l.clear();
        if (value != null)
            l.add(value);
        else
            l.add("");
    }

    /**
     * Add a single value to the list of values associated with key in this
     * MultiValuedMap
     * @param key the key
     * @param value the value to add to the key's values
     */
    public final void add(String key, String value) {
        List<String> l = getList(key);

        if (value != null)
            l.add(value);
        else
            l.add("");
    }

    /**
     * Get the first value associated with the given key
     * @param key the key
     * @return a String
     */
    public final String getFirst(String key) {
        List<String> values = get(key);
        if (values != null && values.size() > 0)
            return values.get(0);
        else
            return null;
    }

    /**
     * Add the given value to the front of the list of values for key
     * in this MultivaluedMap
     *
     * @param key the key
     * @param value value to add
     */
    public final void addFirst(String key, String value) {
        List<String> l = getList(key);

        if (value != null)
            l.add(0, value);
        else
            l.add(0, "");
    }

    /**
     * Construct and return a list of the given typeof all the values
     * associated with the given key
     * @param key the key
     * @param type the type to construct from the Strings associated with key
     * @param <A> any type with a string constructor
     * @return a List of items of type /<A/>
     */
    public final <A> List<A> get(String key, Class<A> type) {
        Constructor<A> c = null;
        try {
            c = type.getConstructor(String.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(type.getName()+" has no String constructor", ex);
        }

        ArrayList<A> l = null;
        List<String> values = get(key);
        if (values != null) {
            l = new ArrayList<A>();
            for (String value: values) {
                try {
                    l.add(c.newInstance(value));
                } catch (Exception ex) {
                    l.add(null);
                }
            }
        }
        return l;
    }

    /**
     * Replace all keys associated with key in this Multivalued Map
     * with the given value.
     * <p>
     * Insert the given value into the MultivaluedMap as a String.
     * @param key the key
     * @param value the value to replace key's values with
     */
    public final void putSingle(String key, Object value) {
        List<String> l = getList(key);

        l.clear();
        if (value != null)
            l.add(value.toString());
        else
            l.add("");
    }

    /**
     * Add the value to the end of the list of values associated with key
     * <p>
     * Inserts the given value into the MultivaluedMap as a String.
     * @param key the key
     * @param value the value to insert
     */
    public final void add(String key, Object value) {
        List<String> l = getList(key);

        if (value != null)
            l.add(value.toString());
        else
            l.add("");
    }

    /**
     * Return the list of Strings associated with the given key.
     * If this key is not in the map, returns and empty list.
     * @param key the key
     * @return List of Strings
     */
    private List<String> getList(String key) {
        List<String> l = get(key);
        if (l == null) {
            l = new LinkedList<String>();
            put(key, l);
        }
        return l;
    }

    /**
     * Return the first item in the List of items associated with the given
     * key in this MultivaluedMap as an item of type /<A/>, or null if the
     * key is not in the MultivaluedMap.
     * @param key the key
     * @param type The class of the return type
     * @param <A> a type with a string constructor
     * @return an object of type /<A/>
     */
    public final <A> A getFirst(String key, Class<A> type) {
        String value = getFirst(key);
        if (value == null)
            return null;
        Constructor<A> c = null;
        try {
            c = type.getConstructor(String.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(type.getName()+" has no String constructor", ex);
        }
        A retVal = null;
        try {
            retVal = c.newInstance(value);
        } catch (Exception ex) {
        }
        return retVal;
    }

    /**
     * Attempt to get the first value associated with the given key in this
     * MultivaluedMap, cast as an item of type /<A/>.
     * <p>
     * If this key is not in the map, return defaultValue.
     * @param key the key value
     * @param defaultValue the value to return if the key is not in the map.
     * @param <A> the type of the value that should be returned.  Should
     *           have a string constructor.
     * @return an object of type /<A/>
     */
    @SuppressWarnings("unchecked")
    public final <A> A getFirst(String key, A defaultValue) {
        String value = getFirst(key);
        if (value == null)
            return defaultValue;

        Class<A> type = (Class<A>)defaultValue.getClass();

        Constructor<A> c = null;
        try {
            c = type.getConstructor(String.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(type.getName()+" has no String constructor", ex);
        }
        A retVal = defaultValue;
        try {
            retVal = c.newInstance(value);
        } catch (Exception ex) {
        }
        return retVal;
    }
}
