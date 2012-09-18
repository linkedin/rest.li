/**
 * <p>Documentation generator for Rest.li server.</p>
 *
 * <p>When passing an implementation of {@link com.linkedin.restli.server.RestLiDocumentationRequestHandler} to
 * {@link com.linkedin.restli.server.RestLiServer} through {@link com.linkedin.restli.server.RestLiConfig},
 * the server will respond to special URLs with documentation content such as HTML page or JSON object.</p>
 *
 * <p>The default implementation {@link com.linkedin.restli.docgen.DefaultDocumentationRequestHandler} renders
 * both HTML and JSON documentation.</p>
 *
 * <p>It also provides an OPTIONS http method alias to the JSON documentation content.</p>
 */
package com.linkedin.restli.docgen;