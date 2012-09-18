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
 *   Renamed UriBuilderImpl->UriBuilder, added fromUri() factory method for use independent of JAX-RS
 *   Added fromPath() factory method
 *   Removed dependency on javax.ws.rs interfaces
 *   Added JavaDoc documentation to conform to Pegasus style guidelines
 */
package com.linkedin.jersey.api.uri;

import com.linkedin.jersey.core.util.MultivaluedMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * A utility class for building URIs from their components
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class UriBuilder
{
  /**
   * initialize and return a UriBuilder based on the given uri
   * @param uri base uri
   * @return a UriBuilder
   */
    public static UriBuilder fromUri(URI uri)
    {
      UriBuilder result = new UriBuilder();
      result.uri(uri);
      return result;
    }

  /**
   * initialize and return a UriBuilder based on the given path
   * @param path base path
   * @return a UriBuilder
   * @throws IllegalArgumentException
   */
    public static UriBuilder fromPath(String path) throws IllegalArgumentException
    {
      UriBuilder result = new UriBuilder();
      result.path(path);
      return result;
    }

    // All fields should be in the percent-encoded form

    private String scheme;

    private String ssp;

    private String authority;

    private String userInfo;

    private String host;

    private int port = -1;

    private final StringBuilder path;

    private MultivaluedMap matrixParams;

    private final StringBuilder query;

    private MultivaluedMap queryParams;

    private String fragment;

  /**
   * initialize and return a basic UriBuilder
   */
    public UriBuilder() {
        path = new StringBuilder();
        query = new StringBuilder();
    }

    private UriBuilder(UriBuilder that) {
        this.scheme = that.scheme;
        this.ssp = that.ssp;
        this.userInfo = that.userInfo;
        this.host = that.host;
        this.port = that.port;
        this.path = new StringBuilder(that.path);
        this.query = new StringBuilder(that.query);
        this.fragment = that.fragment;
    }

    @Override
    public UriBuilder clone() {
        return new UriBuilder(this);
    }

  /**
   * Set the uri in the UriBuilder
   * @param uri a uri
   * @return this
   */
    public UriBuilder uri(URI uri) {
        if (uri == null)
            throw new IllegalArgumentException("URI parameter is null");

        if (uri.getRawFragment() != null) fragment = uri.getRawFragment();

        if (uri.isOpaque()) {
            scheme = uri.getScheme();
            ssp = uri.getRawSchemeSpecificPart();
            return this;
        }

        if (uri.getScheme() == null) {
            if (ssp != null) {
                if (uri.getRawSchemeSpecificPart() != null) {
                    ssp = uri.getRawSchemeSpecificPart();
                    return this;
                }
            }
        } else {
            scheme = uri.getScheme();
        }

        ssp = null;
        if (uri.getRawAuthority() != null) {
            if (uri.getRawUserInfo() == null && uri.getHost() == null && uri.getPort() == -1) {
                authority = uri.getRawAuthority();
                userInfo = null;
                host = null;
                port = -1;
            } else {
                authority = null;
                if (uri.getRawUserInfo() != null) userInfo = uri.getRawUserInfo();
                if (uri.getHost() != null) host = uri.getHost();
                if (uri.getPort() != -1) port = uri.getPort();
            }
        }

        if (uri.getRawPath() != null && uri.getRawPath().length() > 0) {
            path.setLength(0);
            path.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null && uri.getRawQuery().length() > 0) {
            query.setLength(0);
            query.append(uri.getRawQuery());

        }

        return this;
    }

  /**
   * Set the scheme of the UriBuilder
   * @param scheme the scheme
   * @return this
   */
    public UriBuilder scheme(String scheme) {
        if (scheme != null) {
            this.scheme = scheme;
            UriComponent.validate(scheme, UriComponent.Type.SCHEME, true);
        } else {
            this.scheme = null;
        }
        return this;
    }

  /**
   * Set the shceme specific part of the UriBuilder
   * @param ssp the scheme specific part
   * @return this
   */
    public UriBuilder schemeSpecificPart(String ssp) {
        if (ssp == null)
            throw new IllegalArgumentException("Scheme specific part parameter is null");

        // TODO encode or validate scheme specific part
        // This will not work for template variables present in the spp
        StringBuilder sb = new StringBuilder();
        if (scheme != null) sb.append(scheme).append(':');
        if (ssp != null)
            sb.append(ssp);
        if (fragment != null && fragment.length() > 0) sb.append('#').append(fragment);
        URI uri = createURI(sb.toString());

        if (uri.getRawSchemeSpecificPart() != null && uri.getRawPath() == null) {
            this.ssp = uri.getRawSchemeSpecificPart();
        } else {
            this.ssp = null;

            if (uri.getRawAuthority() != null) {
                if (uri.getRawUserInfo() == null && uri.getHost() == null && uri.getPort() == -1) {
                    authority = uri.getRawAuthority();
                    userInfo = null;
                    host = null;
                    port = -1;
                } else {
                    authority = null;
                    userInfo = uri.getRawUserInfo();
                    host = uri.getHost();
                    port = uri.getPort();
                }
            }

            path.setLength(0);
            path.append(replaceNull(uri.getRawPath()));

            query.setLength(0);
            query.append(replaceNull(uri.getRawQuery()));
        }
        return this;
    }

  /**
   * set the user info of the UriBuilder
   * @param ui the user info
   * @return this
   */
    public UriBuilder userInfo(String ui) {
        checkSsp();
        this.userInfo = (ui != null) ?
            encode(ui, UriComponent.Type.USER_INFO) : null;
        return this;
    }

  /**
   * set the host of the UriBuilder
   * null will reset the host setting
   * @param host the host
   * @return this
   */
    public UriBuilder host(String host) {
        checkSsp();
        if(host != null) {
            if(host.length() == 0) // null is used to reset host setting
                throw new IllegalArgumentException("Invalid host name");
            this.host = encode(host, UriComponent.Type.HOST);
        } else {
            this.host = null;
        }
        return this;
    }

  /**
   * Set the port of the UriBuilder
   * @param port port number; must be a postive integer.
   * @return this
   */
    public UriBuilder port(int port) {
        checkSsp();
        if(port < -1) // -1 is used to reset port setting and since URI allows
                      // as port any positive integer, so do we.
            throw new IllegalArgumentException("Invalid port value");
        this.port = port;
        return this;
    }

  /**
   * Replace or set the path of the UriBuilder
   * @param path path value
   * @return this
   */
    public UriBuilder replacePath(String path) {
        checkSsp();
        this.path.setLength(0);
        if(path != null)
            appendPath(path);
        return this;
    }

  /**
   * Append to the path of the UriBuilder
   * @param path path fragment
   * @return this
   */
    public UriBuilder path(String path) {
        checkSsp();
        appendPath(path);
        return this;
    }

  /**
   * Append path segments to path
   * @param segments path segments
   * @return this
   * @throws IllegalArgumentException if any path segments are null
   */
    public UriBuilder segment(String... segments) throws IllegalArgumentException {
        checkSsp();
        if (segments == null)
            throw new IllegalArgumentException("Segments parameter is null");

        for (String segment: segments)
            appendPath(segment, true);
        return this;
    }

    /**
     * Replace or append matrix param to UriBuilder
     * @param matrix matrix param name
     * @return this
     */
    public UriBuilder replaceMatrix(String matrix) {
        checkSsp();
        int i = path.lastIndexOf("/");
        if (i != -1) i = 0;
        i = path.indexOf(";", i);
        if (i != -1) {
            path.setLength(i + 1);
        } else {
            path.append(';');
        }

        if (matrix != null)
            path.append(encode(matrix, UriComponent.Type.PATH));
        return this;
    }

    /**
     * Set the matrix param of the UriBuilder to values
     * @param name name of the matrix param
     * @param values values of the matrix param
     * @return this
     */
    public UriBuilder matrixParam(String name, Object... values) {
        checkSsp();
        if (name == null)
            throw new IllegalArgumentException("Name parameter is null");
        if (values == null)
            throw new IllegalArgumentException("Value parameter is null");
        if (values.length == 0)
            return this;

        name = encode(name, UriComponent.Type.MATRIX_PARAM);
        if (matrixParams == null) {
            for (Object value : values) {
                path.append(';').append(name);

                if (value == null)
                    throw new IllegalArgumentException("One or more of matrix value parameters are null");

                final String stringValue = value.toString();
                if (stringValue.length() > 0)
                    path.append('=').append(encode(stringValue, UriComponent.Type.MATRIX_PARAM));
            }
        } else {
            for (Object value : values) {
                if (value == null)
                    throw new IllegalArgumentException("One or more of matrix value parameters are null");

                matrixParams.add(name, encode(value.toString(), UriComponent.Type.MATRIX_PARAM));
            }
        }
        return this;
    }

    /**
     * Replace matrix param of name with values
     * @param name name of the matrix param
     * @param values values of the matrix param
     * @return this
     */
    public UriBuilder replaceMatrixParam(String name, Object... values) {
        checkSsp();

        if (matrixParams == null) {
            int i = path.lastIndexOf("/");
            if (i != -1) i = 0;
            matrixParams = UriComponent.decodeMatrix((i != -1) ? path.substring(i) : "", false);
            i = path.indexOf(";", i);
            if (i != -1) path.setLength(i);
        }

        name = encode(name, UriComponent.Type.MATRIX_PARAM);
        matrixParams.remove(name);
        for (Object value : values) {
            if (value == null)
                throw new IllegalArgumentException("One or more of matrix value parameters are null");

            matrixParams.add(name, encode(value.toString(), UriComponent.Type.MATRIX_PARAM));
        }
        return this;
    }

    /**
     * set or replace the query in the UriBuilder
     * @param query the name of the query
     * @return this
     */
    public UriBuilder replaceQuery(String query) {
        checkSsp();
        this.query.setLength(0);
        if (query != null)
            this.query.append(encode(query, UriComponent.Type.QUERY));
        return this;
    }

    /**
     * add the given queryParam and its value(s) to the UriBuilder
     * @param name name of the queryParam
     * @param values values of the queryParam
     * @return this
     */
    public UriBuilder queryParam(String name, Object... values) {
        checkSsp();
        if (name == null)
            throw new IllegalArgumentException("Name parameter is null");
        if (values == null)
            throw new IllegalArgumentException("Value parameter is null");
        if (values.length == 0)
            return this;

        name = encode(name, UriComponent.Type.QUERY_PARAM);
        if (queryParams == null) {
            for (Object value : values) {
                if (query.length() > 0) query.append('&');
                query.append(name);

                if (value == null)
                    throw new IllegalArgumentException("One or more of query value parameters are null");

                final String stringValue = value.toString();
                if (stringValue.length() > 0)
                    query.append('=').append(encode(stringValue, UriComponent.Type.QUERY_PARAM));
            }
        } else {
            for (Object value : values) {
                if (value == null)
                    throw new IllegalArgumentException("One or more of query value parameters are null");

                queryParams.add(name, encode(value.toString(), UriComponent.Type.QUERY_PARAM));
            }
        }
        return this;
    }

    /**
     * replace the values of the given queryParam with new ones
     * @param name name of the queryParam
     * @param values value(s) of the queryParam
     * @return this
     */
    public UriBuilder replaceQueryParam(String name, Object... values) {
        checkSsp();

        if (queryParams == null) {
            queryParams = UriComponent.decodeQuery(query.toString(), false);
            query.setLength(0);
        }

        name = encode(name, UriComponent.Type.QUERY_PARAM);
        queryParams.remove(name);

        if (values == null) return this;

        for (Object value : values) {
            if (value == null)
                throw new IllegalArgumentException("One or more of query value parameters are null");

            queryParams.add(name, encode(value.toString(), UriComponent.Type.QUERY_PARAM));
        }
        return this;
    }

    /**
     * set the fragment
     * @param fragment the URI fragment component
     * @return this
     */
    public UriBuilder fragment(String fragment) {
        this.fragment = (fragment != null) ?
            encode(fragment, UriComponent.Type.FRAGMENT) :
            null;
        return this;
    }

    private void checkSsp() {
        if (ssp != null)
            throw new IllegalArgumentException("Schema specific part is opaque");
    }

    private void appendPath(String path) {
        appendPath(path, false);
    }

    private void appendPath(String segments, boolean isSegment) {
        if (segments == null)
            throw new IllegalArgumentException("Path segment is null");
        if (segments.length() == 0)
            return;

        // Encode matrix parameters on current path segment
        encodeMatrix();

        segments = encode(segments,
                (isSegment) ? UriComponent.Type.PATH_SEGMENT : UriComponent.Type.PATH);

        final boolean pathEndsInSlash = path.length() > 0 && path.charAt(path.length() - 1) == '/';
        final boolean segmentStartsWithSlash = segments.charAt(0) == '/';

        if (path.length() > 0 && !pathEndsInSlash && !segmentStartsWithSlash) {
            path.append('/');
        } else if (pathEndsInSlash && segmentStartsWithSlash) {
            segments = segments.substring(1);
            if (segments.length() == 0)
                return;
        }

        path.append(segments);
    }

    private void encodeMatrix() {
        if (matrixParams == null || matrixParams.isEmpty())
            return;

        for (Map.Entry<String, List<String>> e : matrixParams.entrySet()) {
            String name = e.getKey();

            for (String value : e.getValue()) {
                path.append(';').append(name);
                if (value.length() > 0)
                    path.append('=').append(value);
            }
        }
        matrixParams = null;
    }

    private void encodeQuery() {
        if (queryParams == null || queryParams.isEmpty())
            return;

        for (Map.Entry<String, List<String>> e : queryParams.entrySet()) {
            String name = e.getKey();

            for (String value : e.getValue()) {
                if (query.length() > 0) query.append('&');
                query.append(name);

                if (value.length() > 0)
                    query.append('=').append(value);
            }
        }
        queryParams = null;
    }

    private String encode(String s, UriComponent.Type type) {
        return UriComponent.contextualEncode(s, type, true);
    }

    /**
     * Build a uri off the UriBuilder and the given values
     * @param values the template variable to unencoded values map
     * @return a URI
     */
    public URI buildFromMap(Map<String, ? extends Object> values) {
        return _buildFromMap(true, values);
    }

    /**
     * Build a uri off of the UriBuilder and the given values
     * @param values the template variable to encoded values map
     * @return a URI
     * @throws IllegalArgumentException if schema specific part is opaque
     * @throws URISyntaxException
     */
    public URI buildFromEncodedMap(Map<String, ? extends Object> values) throws IllegalArgumentException, URISyntaxException {
        return _buildFromMap(false, values);
    }

    private URI _buildFromMap(boolean encode, Map<String, ? extends Object> values) {
        if (ssp != null)
            throw new IllegalArgumentException("Schema specific part is opaque");

        encodeMatrix();
        encodeQuery();

        String uri = UriTemplate.createURI(
                scheme, authority,
                userInfo, host, (port != -1) ? String.valueOf(port) : null,
                path.toString(), query.toString(), fragment, values, encode);
        return createURI(uri);
    }

    /**
     * Build a uri off of the UriBuilder and given values
     * @param values array of unencoded template values
     * @return a URI
     */
    public URI build(Object... values) {
        return _build(true, values);
    }

    /**
     * Build an uri off of the UriBuilder and the given values
     * @param values array of encoded template values
     * @return a URI
     */
    public URI buildFromEncoded(Object... values) {
        return _build(false, values);
    }

    private URI _build(boolean encode, Object... values) {
        if (values == null || values.length == 0)
            return createURI(create());

        if (ssp != null)
            throw new IllegalArgumentException("Schema specific part is opaque");

        encodeMatrix();
        encodeQuery();

        String uri = UriTemplate.createURI(
                scheme, authority,
                userInfo, host, (port != -1) ? String.valueOf(port) : null,
                path.toString(), query.toString(), fragment, values, encode);
        return createURI(uri);
    }

    private String create() {
        encodeMatrix();
        encodeQuery();

        StringBuilder sb = new StringBuilder();

        if (scheme != null) sb.append(scheme).append(':');

        if (ssp != null) {
            sb.append(ssp);
        } else {
            if (userInfo != null || host != null || port != -1) {
                sb.append("//");

                if (userInfo != null && userInfo.length() > 0)
                    sb.append(userInfo).append('@');

                if (host != null) {
                    // TODO check IPv6 address
                    sb.append(host);
                }

                if (port != -1) sb.append(':').append(port);
            } else if (authority != null) {
                sb.append("//").append(authority);
            }

            if (path.length() > 0) {
                if (sb.length() > 0 && path.charAt(0) != '/') sb.append("/");
                sb.append(path);
            }

            if (query.length() > 0) sb.append('?').append(query);
        }

        if (fragment != null && fragment.length() > 0) sb.append('#').append(fragment);

        return UriComponent.encodeTemplateNames(sb.toString());
    }

    private URI createURI(String uri) {
        return URI.create(uri);
    }

    private String replaceNull(String s) {
        return (s != null) ? s : "";
    }
}