/*
   Copyright (c) 2016 LinkedIn Corp.

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

package test.r2.message;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 * @version $Revision$
 */
public class TestMessages
{
  private static final ByteString DATA = ByteString.copy("the quick brown fox".getBytes());

  private static final FullEntityReader ENTITY_VERIFIER = new FullEntityReader(new Callback<ByteString>() {
    @Override
    public void onError(Throwable e) {
      Assert.fail("Failed to construct full entity");
    }

    @Override
    public void onSuccess(ByteString result) {
      // We can assert same here because there is only one chunk to be assembled therefore the
      // reference to that chunk (ByteString) is returned
      Assert.assertSame(result, DATA);
    }
  });

  private static final Map<String, String> WIRE_ATTR = new HashMap<>();

  static {
    WIRE_ATTR.put("key1", "value1");
    WIRE_ATTR.put("key2", "value2");
  }

  @Test
  public void testToStreamTransportCallbackSuccess()
  {
    TransportCallback<RestResponse> restCallback = response -> {
      Assert.assertFalse(response.hasError());
      Assert.assertNotNull(response.getResponse());
      Assert.assertSame(response.getResponse().getEntity(), DATA);
      Assert.assertNotNull(response.getWireAttributes());
      Assert.assertEquals(response.getWireAttributes(), WIRE_ATTR);
    };

    TransportCallback<StreamResponse> streamCallback = Messages.toStreamTransportCallback(restCallback);
    StreamResponseBuilder builder = new StreamResponseBuilder();
    StreamResponse streamResponse = builder.build(EntityStreams.newEntityStream(new ByteStringWriter(DATA)));
    streamCallback.onResponse(TransportResponseImpl.success(streamResponse, WIRE_ATTR));
  }

  @Test
  public void testToStreamTransportCallbackStreamException()
  {
    TransportCallback<RestResponse> restCallback = response -> {
      Assert.assertTrue(response.hasError());
      Assert.assertNotNull(response.getError());
      Assert.assertTrue(response.getError() instanceof RestException);
      Assert.assertNotNull(response.getWireAttributes());
      Assert.assertEquals(response.getWireAttributes(), WIRE_ATTR);
    };

    TransportCallback<StreamResponse> streamCallback = Messages.toStreamTransportCallback(restCallback);
    StreamResponseBuilder builder = new StreamResponseBuilder();
    StreamResponse streamResponse = builder.build(EntityStreams.newEntityStream(new ByteStringWriter(DATA)));
    streamCallback.onResponse(TransportResponseImpl.error(
        new StreamException(streamResponse, new IllegalStateException()), WIRE_ATTR));
  }

  @Test
  public void testToStreamTransportCallbackOtherException()
  {
    TransportCallback<RestResponse> restCallback = response -> {
      Assert.assertTrue(response.hasError());
      Assert.assertNotNull(response.getError());
      Assert.assertTrue(response.getError() instanceof IllegalStateException);
      Assert.assertNotNull(response.getWireAttributes());
      Assert.assertEquals(response.getWireAttributes(), WIRE_ATTR);
    };

    TransportCallback<StreamResponse> streamCallback = Messages.toStreamTransportCallback(restCallback);
    streamCallback.onResponse(TransportResponseImpl.error(new IllegalStateException(), WIRE_ATTR));
  }

  @Test
  public void testToRestTransportCallbackSuccess() {
    TransportCallback<StreamResponse> streamCallback = response -> {
      Assert.assertFalse(response.hasError());
      Assert.assertNotNull(response.getResponse());
      response.getResponse().getEntityStream().setReader(ENTITY_VERIFIER);
      Assert.assertNotNull(response.getWireAttributes());
      Assert.assertEquals(response.getWireAttributes(), WIRE_ATTR);
    };
    TransportCallback<RestResponse> restCallback = Messages.toRestTransportCallback(streamCallback);
    RestResponseBuilder builder = new RestResponseBuilder();
    builder.setEntity(DATA);
    RestResponse restResponse = builder.build();
    restCallback.onResponse(TransportResponseImpl.success(restResponse, WIRE_ATTR));
  }

  @Test
  public void testToRestTransportCallbackRestException() {
    TransportCallback<StreamResponse> streamCallback = response -> {
      Assert.assertTrue(response.hasError());
      Assert.assertNotNull(response.getError());
      Assert.assertTrue(response.getError() instanceof StreamException);
      Assert.assertNotNull(response.getWireAttributes());
      Assert.assertEquals(response.getWireAttributes(), WIRE_ATTR);
    };
    TransportCallback<RestResponse> restCallback = Messages.toRestTransportCallback(streamCallback);
    RestResponseBuilder builder = new RestResponseBuilder();
    builder.setEntity(DATA);
    RestResponse restResponse = builder.build();
    restCallback.onResponse(TransportResponseImpl.error(
        new RestException(restResponse, new IllegalStateException()), WIRE_ATTR));
  }

  @Test
  public void testToRestTransportCallbackOtherException() {
    TransportCallback<StreamResponse> streamCallback = response -> {
      Assert.assertTrue(response.hasError());
      Assert.assertNotNull(response.getError());
      Assert.assertTrue(response.getError() instanceof IllegalStateException);
      Assert.assertNotNull(response.getWireAttributes());
      Assert.assertEquals(response.getWireAttributes(), WIRE_ATTR);
    };
    TransportCallback<RestResponse> restCallback = Messages.toRestTransportCallback(streamCallback);
    restCallback.onResponse(TransportResponseImpl.error(new IllegalStateException(), WIRE_ATTR));
  }
}
