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

package com.linkedin.d2.discovery.stores.glu;

// TODO this should be rewritten and moved to the com.linkedin.cluster.discovery.balancer.glu

/**
 * GluContainerDynamicDiscoveryClient is an GLU console implementation of a
 * DynamicDiscoveryClient. It periodically polls the GLU console's HTTP JSON interface to
 * get the latest state of the system.
 *
 * @author criccomini
 *
 */
public class GluContainerDiscoveryClient
{
  // private static final Logger _log = LoggerFactory.getLogger(GluContainerDiscoveryClient.class);
  //
  // private URL _gluUrl;
  // private String _gluUsername;
  // private String _gluPassword;
  // private boolean _trusting;
  // private Timer _refreshTimer;
  //
  // public GluContainerDiscoveryClient(URL gluUrl, long refreshPeriod)
  // {
  // this(gluUrl, null, null, false, refreshPeriod);
  // }
  //
  // public GluContainerDiscoveryClient(URL gluUrl,
  // String gluUsername,
  // String gluPassword,
  // long refreshPeriod)
  // {
  // this(gluUrl, gluUsername, gluPassword, false, refreshPeriod);
  // }
  //
  // /**
  // *
  // * @param gluUrl
  // * The URL to the GLU HTTP JSON page.
  // * http://HOST:PORT/console/rest/v1/FABRIC/system/live
  // * @param gluUsername
  // * The username to log into GLU with.
  // * @param gluPassword
  // * The password to log into GLU with.
  // * @param trusting
  // * If false, the TrustingSocketFactory will be used, which will disable SSL
  // * certificate verification.
  // * @param refreshPeriod
  // * How often the GLU HTTP JSON page should be queried in milliseconds.
  // */
  // public GluContainerDiscoveryClient(URL gluUrl,
  // String gluUsername,
  // String gluPassword,
  // boolean trusting,
  // long refreshPeriod)
  // {
  // _gluUrl = gluUrl;
  // _gluUsername = gluUsername;
  // _gluPassword = gluPassword;
  // _trusting = trusting;
  // _refreshTimer = new Timer();
  // _refreshTimer.scheduleAtFixedRate(new RefreshTask(), 0, refreshPeriod);
  // }
  //
  // public class RefreshTask extends TimerTask
  // {
  // @Override
  // public void run()
  // {
  // refresh();
  // }
  // }
  //
  // /**
  // * Refresh the current view of the system by querying GLU's HTTP JSON page. Any
  // changes
  // * that have occurred since the last refresh will be thrown as events to all
  // registered
  // * listeners.
  // */
  // public void refresh()
  // {
  // Map<String, GluProperties> endpoints = new HashMap<String, GluProperties>();
  // HttpClient client = new HttpClient();
  //
  // _log.info("refreshing GLU service list");
  //
  // // ignore https exceptions
  // if (_trusting)
  // {
  // @SuppressWarnings("deprecation")
  // Protocol easyhttps =
  // new Protocol(_gluUrl.getProtocol(),
  // new TrustingSocketFactory(),
  // _gluUrl.getPort());
  //
  // client.getHostConfiguration().setHost(_gluUrl.getHost(),
  // _gluUrl.getPort(),
  // easyhttps);
  // }
  //
  // // use username and password to log into glu
  // if (_gluUsername != null && _gluPassword != null)
  // {
  // Credentials defaultCreds =
  // new UsernamePasswordCredentials(_gluUsername, _gluPassword);
  // AuthScope authScope =
  // new AuthScope(_gluUrl.getHost(), _gluUrl.getPort(), AuthScope.ANY_REALM);
  //
  // client.getState().setCredentials(authScope, defaultCreds);
  // client.getParams().setAuthenticationPreemptive(true);
  // }
  //
  // GetMethod httpGet = new GetMethod(_gluUrl.getPath());
  // String body = null;
  //
  // try
  // {
  // client.executeMethod(httpGet);
  // body = httpGet.getResponseBodyAsString();
  // }
  // catch (Exception e)
  // {
  // _log.warn(e);
  // }
  // finally
  // {
  // httpGet.releaseConnection();
  // }
  //
  // // parse the glu response and load in memory dynamic discovery
  // try
  // {
  // JSONObject glueData = new JSONObject(body);
  // JSONArray entries = glueData.getJSONArray("entries");
  // Map<String, Set<URI>> uris = new HashMap<String, Set<URI>>();
  //
  // for (int i = 0; i < entries.length(); ++i)
  // {
  // try
  // {
  // JSONObject entry = entries.getJSONObject(i);
  // JSONObject entryMetadata = entry.getJSONObject("metadata");
  // JSONObject entryMetadataContainer = entryMetadata.getJSONObject("container");
  // JSONObject entryMetadataScriptState =
  // entryMetadata.getJSONObject("scriptState");
  // JSONObject entryMetadataScriptStateScript =
  // entryMetadataScriptState.getJSONObject("script");
  //
  // String name = entryMetadataContainer.getString("name");
  // String host = entry.getString("agent");
  // int port = entryMetadataScriptStateScript.getInt("port");
  // String protocol = "http"; // TODO howto get this in glu
  // String context = "/"; // TODO howto get this in glu
  // String status = entryMetadata.getString("currentState");
  //
  // if ("running".equals(status))
  // {
  // Set<URI> serviceEndpoints = uris.get(name);
  //
  // if (serviceEndpoints == null)
  // {
  // serviceEndpoints = new HashSet<URI>();
  // uris.put(name, serviceEndpoints);
  // }
  //
  // serviceEndpoints.add(new URI(protocol + "://" + host + ":" + port + context));
  // }
  // }
  // catch (JSONException e)
  // {
  // _log.warn("skipped: " + entries.getJSONObject(i) + "\nbecause: "
  // + e.getMessage());
  // }
  // }
  //
  // // TODO if we get bad json data, endpoints could be empty,
  // // and we'd remove all services from the list. that's bad news.
  //
  // for (Map.Entry<String, Set<URI>> endpointSet : uris.entrySet())
  // {
  // endpoints.put(endpointSet.getKey(), new GluProperties(endpointSet.getKey(),
  // endpointSet.getValue()));
  // }
  //
  // replace(endpoints);
  // }
  // catch (Exception e)
  // {
  // e.printStackTrace();
  // }
  // }
}
