/*
   Copyright (c) 2014 LinkedIn Corp.

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


package com.example.d2.config;

import com.linkedin.d2.discovery.util.D2Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Map;

/**
 * Read D2 Topology configuration at /config/src/main/d2Config/d2Config.json.
 * This configuration will tell D2 what Services and Clusters exist in our system. In
 * this example we have 2 clusters and 3 services.
 *
 * For more information about what configuration you can modify please check
 * the D2 documentation.
 *
 * @author Oby Sumampouw (osumampo@linkedin.com)
 */
public class ConfigRunner
{
  public static void main(String[] args)
      throws Exception
  {
    //get server configuration
    String path = new File(new File(".").getAbsolutePath()).getCanonicalPath() +
        "/src/main/d2Config/d2Config.json";
    JSONParser parser = new JSONParser();
    Object object = parser.parse(new FileReader(path));
    JSONObject json = (JSONObject) object;
    System.out.println("Finished parsing d2 topology config");

    String zkConnectString = (String)json.get("zkConnectString");
    int zkSessionTimeout = ((Long)json.get("zkSessionTimeout")).intValue();
    String zkBasePath = (String)json.get("zkBasePath");
    int zkRetryLimit = ((Long)json.get("zkRetryLimit")).intValue();

    Map<String,Object> serviceDefaults = (Map<String, Object>)json.get(
        "defaultServiceProperties");

    //this contains the topology of our system
    Map<String,Object> clusterServiceConfigurations =
        (Map<String, Object>)json.get("d2Clusters");
    // 'comment' has no special meaning in json...
    clusterServiceConfigurations.remove("comment");

    System.out.println("Populating zookeeper with d2 configuration");

    //d2Config is the utility class for populating zookeeper with our topology
    //some the params are not needed for this simple example so we will just use
    //default value by passing an empty map
    D2Config d2Config = new D2Config(zkConnectString, zkSessionTimeout, zkBasePath,
                                     zkSessionTimeout, zkRetryLimit,
                                     (Map<String, Object>)Collections.EMPTY_MAP,
                                     serviceDefaults,
                                     clusterServiceConfigurations,
                                     (Map<String, Object>)Collections.EMPTY_MAP,
                                     (Map<String, Object>)Collections.EMPTY_MAP);

    //populate zookeeper
    d2Config.configure();
    System.out.println("Finished populating zookeeper with d2 configuration");
  }
}
