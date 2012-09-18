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

package com.linkedin.data.performance;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.WrappingArrayTemplate;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


public class TestCloudPerformance
{
  private static int _numElements = 25000;
  private static int _numIterations = 1;
  private static boolean _testOnly = true;

  private static final PrintStream out = System.out;

  public static void main(String args[]) throws IOException
  {
    TestCloudPerformance test = new TestCloudPerformance();
    _numIterations = 100;
    _testOnly = false;
    test.run();
  }

  @Test
  public void run() throws IOException
  {
    DataCodec codecs[] = {
      new JacksonDataCodec(),
      new PsonDataCodec().setOptions(new PsonDataCodec.Options().setEncodeContainerCount(false).setEncodeStringLength(false)),
      new PsonDataCodec().setOptions(new PsonDataCodec.Options().setEncodeContainerCount(false).setEncodeStringLength(true)),
      new PsonDataCodec().setOptions(new PsonDataCodec.Options().setEncodeContainerCount(true).setEncodeStringLength(false)),
      new PsonDataCodec().setOptions(new PsonDataCodec.Options().setEncodeContainerCount(true).setEncodeStringLength(true)),
    };

    if (_testOnly == false)
    {
      out.println("Number of elements " + _numElements);
      out.println("Number of iterations " + _numIterations);
    }

    for (DataCodec codec : codecs)
    {
      testSerializeDataMap(codec);
      testDeserializeDataMap(codec);
    }
    testEdgeListAddAll();
    testEdgeListEfficientAddAll();
  }

  public void testSerializeDataMap(DataCodec codec) throws IOException
  {
    DataMap map = cloudDataMap(_numElements);

    byte[] bytes = null;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < _numIterations; i++)
    {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      codec.writeMap(map, os);
      bytes = os.toByteArray();
      os.close();
    }
    long endTime = System.currentTimeMillis();
    long elapsedTime = endTime - startTime;

    assertTrue(Arrays.equals(bytes, cloudBytes(codec, _numElements)));
    if (_testOnly == false)
    {
      out.println(codec + " DataMap serialize " +
                  perIterationTime(elapsedTime) + " ms " + bytes.length + " bytes");
    }
  }

  public void testDeserializeDataMap(DataCodec codec) throws IOException
  {
    byte[] bytes = cloudBytes(codec, _numElements);

    DataMap map = null;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < _numIterations; i++)
    {
      ByteArrayInputStream is = new ByteArrayInputStream(bytes);
      map = codec.readMap(is);
      is.close();
    }
    long endTime = System.currentTimeMillis();
    long elapsedTime = endTime - startTime;

    assertEquals(map, cloudDataMap(_numElements));
    if (_testOnly == false)
    {
      out.println(codec + " DataMap deserialize " +
                  perIterationTime(elapsedTime) + " ms " + bytes.length + " bytes");
      // TestUtil.dumpBytes(out, bytes); out.println();
    }
  }

  public void testEdgeListAddAll() throws IOException
  {
    List<Edge> input = cloudEdgesList(_numElements);

    EdgeList edgeList = null;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < _numIterations; i++)
    {
      edgeList = new EdgeList();
      edgeList.addAll(input);
    }
    long endTime = System.currentTimeMillis();
    long elapsedTime = endTime - startTime;

    assertEquals(edgeList, input);
    if (_testOnly == false)
    {
      out.println("EdgeList addAll " + perIterationTime(elapsedTime) + " ms");
    }
  }

  public void testEdgeListEfficientAddAll() throws IOException
  {
    List<Edge> input = cloudEdgesList(_numElements);

    EdgeList edgeList = null;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < _numIterations; i++)
    {
      edgeList = new EdgeList();
      for (Edge edge : input)
      {
         edgeList.data().add(edge.data());
      }
    }
    long endTime = System.currentTimeMillis();
    long elapsedTime = endTime - startTime;

    assertEquals(edgeList, input);
    if (_testOnly == false)
    {
      out.println("EdgeList efficient addAll " + perIterationTime(elapsedTime) + " ms" );
    }
  }

  private double perIterationTime(long elapsedTime)
  {
    return elapsedTime / (double) _numIterations;
  }

  private static final String EDGE_SCHEMA = "{ \"type\" : \"record\", \"name\" : \"Edge\", \"fields\" : [] }";
  private static final String EDGE_LIST_SCHEMA = "{ \"type\" : \"array\", \"items\" : " + EDGE_SCHEMA + " }";

  public static class Edge extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema(EDGE_SCHEMA);

    public Edge(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class EdgeList extends WrappingArrayTemplate<Edge>
  {
    private static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema(EDGE_LIST_SCHEMA);

    public EdgeList()
    {
      super(new DataList(), SCHEMA, Edge.class);
    }
  }

  private byte[] cloudBytes(DataCodec codec, int numElements) throws IOException
  {
    DataMap map = cloudDataMap(numElements);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    codec.writeMap(map, os);
    byte[] bytes = os.toByteArray();
    return bytes;
  }

  private DataMap cloudDataMap(int numElements)
  {
    DataMap map = new DataMap();

    map.put("elements", cloudEdgesDataList(numElements));

    DataMap paging = new DataMap();
    paging.put("count", 10);
    paging.put("links", new DataMap());
    paging.put("start", 0);
    map.put("paging", paging);

    return map;
  }

  private DataList cloudEdgesDataList(int numElements)
  {
    DataList edgeList = new DataList(numElements);
    for (int i = 0; i < numElements; i++)
    {
      edgeList.add(cloudEdgeDataMap());
    }
    return edgeList;
  }

  private DataMap cloudEdgeDataMap()
  {
    DataMap e = new DataMap();
    e.put("creationTime", 1124804967000L);
    e.put("destId", 12345678);
    e.put("edgeType", "MEMBER_TO_MEMBER");
    e.put("metadata", new DataMap());
    e.put("score", 23);
    e.put("srcId", 123456789);
    return e;
  }

  private List<Edge> cloudEdgesList(int numElements)
  {
    List<Edge> edgeList = new ArrayList<Edge>(numElements);
    for (int i = 0; i < numElements; i++)
    {
      edgeList.add(cloudEdge());
    }
    return edgeList;
  }

  private Edge cloudEdge()
  {
    return new Edge(cloudEdgeDataMap());
  }
}
