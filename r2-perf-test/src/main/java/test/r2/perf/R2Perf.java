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

package test.r2.perf;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;


public class R2Perf implements TestConstants
{

  private static final Logger _log = LoggerFactory.getLogger(R2Perf.class);

  private static final long serialVersionUID = 240L;
  private static final HttpClientFactory FACTORY = new HttpClientFactory();
  public static final Client CLIENT = new TransportClientAdapter(FACTORY.getClient(Collections.<String, String>emptyMap()));

  private String    _host;
  private int      _port;
  private String    _relative_uri;
  private String    _datafile;
  private String    _header;
  private double    _delay;
  private double    _range;
  private int      _req_length;
  private boolean    _req_length_randomize;
  private int      _threads;
  private int      _runs;
  private double    _rampup;

  private byte[]    _entity;
  private URI      _uri;
  private RestRequest  _request;

  public R2Perf()
  {
    setupValues();
  }

  private void setupValues()
  {
    _host = MiscUtil.getString(SERVER_HOST_PROP_NAME, DEFAULT_HOST);
    _port = Integer.parseInt(System.getProperty(SERVER_PORT_PROP_NAME, DEFAULT_PORT));
    _relative_uri = MiscUtil.getString(SERVER_RELATIVE_URI_PROP_NAME, DEFAULT_RELATIVE_URI);
    _datafile = MiscUtil.getString(DATA_FILE_PROP_NAME, DEFAULT_DATAFILE);
    _header = DEFAULT_HEADER;
    _delay = Double.parseDouble(System.getProperty(DELAY_PROP_NAME, DEFAULT_DELAY));
    _range = Double.parseDouble(System.getProperty(RANGE_PROP_NAME, DEFAULT_RANGE));
    _req_length = Integer.parseInt(System.getProperty(REQUEST_LENGTH_PROP_NAME, DEFAULT_REQUEST_LENGTH));
    _req_length_randomize = Boolean.parseBoolean(MiscUtil.getString(REQUEST_LENGTH_RANDOM_PROP_NAME, DEFAULT_REQUEST_LENGTH));
    _threads = Integer.parseInt(System.getProperty(THREADS_NUMBER_PROP_NAME, DEFAULT_THREADS));
    _runs = Integer.parseInt(System.getProperty(RUNS_NUMBER_PROP_NAME, DEFAULT_RUNS));
    _rampup = (Double.parseDouble(System.getProperty(RAMPUP_PROP_NAME, DEFAULT_RAMPUP))/_threads)*1000;

    System.out.println("perf.host:"+_host);
    System.out.println("perf.port:"+_port);
    System.out.println("perf.uri:"+_relative_uri);
    System.out.println("perf.datafile:"+_datafile);
    System.out.println("perf.delay:"+_delay);
    System.out.println("perf.range:"+_range);
    System.out.println("perf.threads:"+_threads);
    System.out.println("perf.runs:"+_runs);
    System.out.println("perf.rampup:"+_rampup);
  }

  public void setupTest()
  {
    System.out.println("Executing setupTest...  threadid:"+Thread.currentThread().getId()+",threadName:"+Thread.currentThread().getName());
    String method = "POST";

    try
    {
      File file = new File(_datafile);
      _entity = MiscUtil.getBytesFromFile(file);
      if (_req_length != -1 && _req_length < _entity.length)
      {
        byte[] arr2 = Arrays.copyOf(_entity, _req_length);
        _entity = Arrays.copyOf(arr2, _req_length);
      }
    }
    catch (Exception e)
    {
      _log.error("Got exception in setupTest. Exception :"+e.getMessage());
      e.printStackTrace();
    }

    _uri = URI.create("http://" + _host + ":" + _port + _relative_uri);
    try
    {
      _request = buildRequest(_uri, method, _entity) ;
    }
    catch (Exception e)
    {
      _log.info(",ERROR,,"+e.toString());
    }
  }

  public void runTest() throws Exception
  {
    System.out.println(
            " _datafile:" + _datafile + " _threads:" + _threads + " _runs:" + _runs + " _delay:" + _delay + " _range:" + _range);
    R2PerfDriver.sendRequest(CLIENT, _uri, _request, _threads, _runs, _delay,
                                                    _range, _host, _relative_uri, _rampup);
  }

  public void teardownTest() throws ExecutionException, TimeoutException, InterruptedException
  {
    shutdown();
  }

  private RestRequest buildRequest(URI uri, String method, byte[] entity) throws IOException
  {
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri).setMethod(method);

    requestBuilder.setHeader(DEFAULT_HEADER, "1");

    if (entity != null)
    {
      requestBuilder.setEntity(entity).setHeader("Content-Type", "application/json");
    }

    return requestBuilder.build();
  }

  private void shutdown() throws ExecutionException, TimeoutException, InterruptedException
  {
    final FutureCallback<None> callback = new FutureCallback<None>();
    CLIENT.shutdown(callback);

    callback.get(30, TimeUnit.SECONDS);
    _log.info("Client shutdown has been completed.");

    final FutureCallback<None> factoryCallback = new FutureCallback<None>();
    FACTORY.shutdown(factoryCallback, 30, TimeUnit.SECONDS);
    factoryCallback.get(30, TimeUnit.SECONDS);
    _log.info("Factory shutdown has been completed.");
  }

  public static void main (String [] args)
  {
    R2Perf test = new R2Perf();
    test.setupTest();
    try
    {
      test.runTest();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      try
      {
        test.teardownTest();
      }
      catch (Exception e)
      {
        _log.error("Error tearing down test", e);
        e.printStackTrace();
      // Even if some non-daemon threads fail to stop, System.exit() should terminate the VM
        System.exit(-1);
      }
    }
  }
}
