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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.annotations.Test;


public class TestRecordTemplatePerformance
{
  @Test
  public void MeasureRecordTemplatePutPerformance() throws InterruptedException
  {
    List<Integer> threadCounts = Arrays.asList(1, 2, 4, 8, 16, 32);

    double averagePut = 0;
    final int testCount = 1000000;
    final int sampleCount = 5;

    //Warm-up
    for (int i= 0; i < testCount; ++i)
    {
      Foo foo = new Foo();
      Bar bar = new Bar().setInt(54);
      foo.setRecord(bar);
    }

    //Multi-threaded latency measurement based on different thread counts
    for (Integer numThreads : threadCounts)
    {
      // Try out some number of samples to get a better average.
      for (int k=0; k < sampleCount; ++k)
      {
        final AtomicLong durationPut = new AtomicLong(0);
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i=0; i < numThreads; ++i)
        {
          Thread thread = new Thread(new Runnable(){

            public void run()
            {
              long duration = 0;
              for (int i= 0; i < testCount; ++i)
              {
                Foo foo = new Foo();
                Bar bar = new Bar().setInt(54);
                long startPut = System.currentTimeMillis();
                foo.setRecord(bar);
                duration += System.currentTimeMillis() - startPut;
              }

              durationPut.addAndGet(duration);
              latch.countDown();
            }

          });

          thread.start();
        }

        latch.await();
        averagePut += (double)durationPut.get() / (double)(testCount * numThreads);
      }

      System.out.println("Number of Threads: " + numThreads);
      System.out.println("Average Put (Microseconds): " + (averagePut / sampleCount) * 1000);
    }

    System.out.flush();
  }


  public static class Foo extends RecordTemplate
  {
    public static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema(
        "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [\n" +
            "{ \"name\" : \"record\", \"type\" : { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] } } \n" +
            "] }");
    private static final RecordDataSchema.Field FIELD_record = SCHEMA.getField("record");

    public Foo()
    {
      super(new DataMap(), SCHEMA);
    }

    public Bar getRecord()
    {
      return obtainWrapped(FIELD_record, Bar.class, GetMode.DEFAULT);
    }

    public void setRecord(Bar value)
    {
      putWrapped(FIELD_record, Bar.class, value);
    }
  }

  public static class Bar extends RecordTemplate
  {
    public static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema
        (
            "{ \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] }"
        );
    private static final RecordDataSchema.Field FIELD_int = SCHEMA.getField("int");

    public Bar()
    {
      super(new DataMap(), SCHEMA);
    }

    public Integer getInt()
    {
      return obtainDirect(FIELD_int, Integer.TYPE, GetMode.STRICT);
    }

    public Bar setInt(int value)
    {
      putDirect(FIELD_int, Integer.class, Integer.class, value, SetMode.DISALLOW_NULL);
      return this;
    }
  }
}