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

package com.linkedin.restli.example;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.example.photos.AlbumEntryRequestBuilders;
import com.linkedin.restli.example.photos.AlbumsRequestBuilders;
import com.linkedin.restli.example.photos.PhotosRequestBuilders;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * @author Keren Jin
 */
public class RestLiExampleBasicClient
{
  /**
   * This is a stand-alone app to demo the use of client-side Pegasus API. To run in,
   * com.linkedin.restli.example.RestLiExamplesServer has to be running.
   *
   * The only argument is the path to the resource on the photo server, e.g. /photos/1
   */
  public static void main(String[] args) throws Exception
  {
    // create HTTP Netty client with default properties
    final HttpClientFactory http = new HttpClientFactory();
    final TransportClient transportClient = http.getClient(Collections.<String, String>emptyMap());
    // create an abstraction layer over the actual client, which supports both REST and RPC
    final Client r2Client = new TransportClientAdapter(transportClient);
    // REST client wrapper that simplifies the interface
    final StringBuilder serverUrlBuilder = new StringBuilder("http://").append(SERVER_HOSTNAME).append(":").append(SERVER_PORT).append("/");
    final RestClient restClient = new RestClient(r2Client, serverUrlBuilder.toString());
    final RestLiExampleBasicClient photoClient = new RestLiExampleBasicClient(restClient);

    String pathInfo = args.length == 0 ? "" : args[0];
    photoClient.sendRequest(pathInfo, new PrintWriter(System.out));
    photoClient.shutdown();
    http.shutdown(new FutureCallback<None>());
  }

  public RestLiExampleBasicClient(RestClient restClient)
  {
    _restClient = restClient;
  }

  public void sendRequest(String pathInfo, PrintWriter respWriter)
  {
    /* Supported URLs
     *  /fail: just fail
     *  /album/<album_id>: display album
     *  all others: make photos, get photos, purge photos
     */
    try
    {
      if (pathInfo.equals("/fail"))
      {
        getNonPhoto();
      }
      else
      {
        if (pathInfo.startsWith("/album/"))
        {
          getAlbum(respWriter, Long.parseLong(pathInfo.substring("/album/".length())));
        }
        else
        {
          // this track does not make any assumption on server
          // we need to create photo first, find it and clean them up
          // we use both sync and async approaches for creating

          final long newPhotoId = createPhoto(respWriter);
          final CountDownLatch latch = new CountDownLatch(1);
          createPhotoAsync(respWriter, latch, newPhotoId);
          getPhoto(respWriter, newPhotoId);
          findPhoto(respWriter);
          partialUpdatePhoto(respWriter, newPhotoId);
          // photos and albums have IDs starting from 1
          getAlbumSummary(respWriter, (long) new Random().nextInt(10) + 1);
          purgeAllPhotos(respWriter);
          try
          {
            latch.await();
          }
          catch (InterruptedException e)
          {
            respWriter.println(e.getMessage());
          }
        }
      }
    }
    catch (RemoteInvocationException e)
    {
      respWriter.println("Error in example client: " + e.getMessage());
    }
    respWriter.flush();
  }

  public void shutdown()
  {
    _restClient.shutdown(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        throw new RuntimeException("Error occurred during example client shutdown.", e);
      }

      @Override
      public void onSuccess(None result)
      {
        System.out.println("Example client is shutdown.");
      }
    });
  }

  private long createPhoto(PrintWriter respWriter) throws RemoteInvocationException
  {
    // make create photo request and send with the rest client synchronously
    // response of create request does not have body, therefore use EmptyRecord as template

    // create an instance of photo pragmatically
    // this resembles to photo-create.json
    final LatLong newLatLong = new LatLong().setLatitude(37.42394f).setLongitude(-122.0708f);
    final EXIF newExif = new EXIF().setLocation(newLatLong);
    final Photo newPhoto = new Photo().setTitle("New Photo").setFormat(PhotoFormats.PNG).setExif(newExif);

    final Request<IdResponse<Long>> createReq1 = _photoBuilders.create().input(newPhoto).build();
    final ResponseFuture<IdResponse<Long>> createFuture1 = _restClient.sendRequest(createReq1);
    // Future.getResource() blocks until server responds
    final Response<IdResponse<Long>> createResp1 = createFuture1.getResponse();
    createResp1.getEntity();

    @SuppressWarnings("unchecked")
    final IdResponse<Long> entity = createResp1.getEntity();
    final long newPhotoId = entity.getId();
    respWriter.println("New photo ID: " + newPhotoId);

    return newPhotoId;
  }

  /**
   * make create photo request and send with the rest client asynchronously
   */
  private void createPhotoAsync(final PrintWriter respWriter, final CountDownLatch latch, final long newPhotoId)
  {
    // this resembles to photo-create-id.json
    final LatLong newLatLong = new LatLong().setLatitude(40.725f).setLongitude(-74.005f);
    final EXIF newExif = new EXIF().setIsFlash(false).setLocation(newLatLong);
    final Photo newPhoto = new Photo().setTitle("Updated Photo").setFormat(PhotoFormats.JPG).setExif(newExif);

    final Request<EmptyRecord> createReq2 = _photoBuilders.update().id(newPhotoId).input(newPhoto).build();

    // send request with callback
    _restClient.sendRequest(createReq2, new Callback<Response<EmptyRecord>>()
    {
      @Override
      public void onError(Throwable e)
      {
        respWriter.println(e.getMessage());
        latch.countDown();
      }

      @Override
      public void onSuccess(Response<EmptyRecord> result)
      {
        respWriter.println("Update photo is successful: " + (result.getStatus() == 204));

        // without a condition variable or CountDownLatch,
        // this callback might not be called before main thread terminates
        latch.countDown();
      }
    });
  }

  /**
   * Retrieve the album information and each photo in the album. The photos are retrieved in parallel.
   */
  private void getAlbum(PrintWriter respWriter, long albumId) throws RemoteInvocationException
  {
    // get the specific album
    final Request<Album> getAlbumReq = _albumBuilders.get().id(albumId).build();
    final ResponseFuture<Album> getAlbumFuture = _restClient.sendRequest(getAlbumReq);
    final Response<Album> getResp = getAlbumFuture.getResponse();
    final Album album = getResp.getEntity();

    respWriter.println(album.getTitle());
    respWriter.println("Created on " + new Date(album.getCreationTime()));

    // get the album's entries
    final FindRequest<AlbumEntry> searchReq = _albumEntryBuilders.findBySearch().albumIdParam(albumId).build();
    final ResponseFuture<CollectionResponse<AlbumEntry>> responseFuture = _restClient.sendRequest(searchReq);
    final Response<CollectionResponse<AlbumEntry>> response = responseFuture.getResponse();
    final List<AlbumEntry> entries = new ArrayList<AlbumEntry>(response.getEntity().getElements());

    entries.add(new AlbumEntry().setAlbumId(-1).setPhotoId(9999));

    // don't return until all photo requests done
    final CountDownLatch latch = new CountDownLatch(entries.size());

    // fetch every photo asynchronously
    // store either a photo or an exception
    final Object[] photos = new Object[entries.size()];
    for (int i = 0; i < entries.size(); i++)
    {
      final int finalI = i; // need final version for callback
      final AlbumEntry entry = entries.get(i);
      final long photoId = entry.getPhotoId();
      final Request<Photo> getPhotoReq = _photoBuilders.get().id(photoId).build();
      _restClient.sendRequest(getPhotoReq, new Callback<Response<Photo>>()
      {
        @Override
        public void onSuccess(Response<Photo> result)
        {
          photos[finalI] = result.getEntity();
          latch.countDown();
        }

        @Override
        public void onError(Throwable e)
        {
          photos[finalI] = e;
        }
      });
    }

    try
    {
      // wait for all requests to finish
      latch.await(2, TimeUnit.SECONDS);
      if (latch.getCount() > 0)
      {
        respWriter.println("Failed to retrieve some photo(s)");
      }
    }
    catch (InterruptedException e)
    {
      e.printStackTrace(respWriter);
    }

    // print photo data
    for (int i = 0; i < entries.size(); i++)
    {
      final Object val = photos[i];
      final AlbumEntry entry = entries.get(i);
      if (val instanceof Throwable)
      {
        respWriter.println("Failed to load photo " + entry.getPhotoId());
        respWriter.println("Stack trace:");
        ((Throwable) val).printStackTrace(respWriter);
        respWriter.println();
      }
      else if (val instanceof Photo)
      {
        final Photo photo = (Photo) val;
        respWriter.println("Photo " + photo.getTitle() + ":");
        respWriter.println(photo);
        respWriter.println("Added on " + new Date(entry.getAddTime()));
      }
      else
      {
        throw new AssertionError("expected photo or exception");
      }
    }
  }

  /**
   * send request to retrieve created photo
   */
  private void getPhoto(PrintWriter respWriter, long newPhotoId) throws RemoteInvocationException
  {
    final Request<Photo> getReq = _photoBuilders.get().id(newPhotoId).build();
    final ResponseFuture<Photo> getFuture = _restClient.sendRequest(getReq);
    final Response<Photo> getResp = getFuture.getResponse();
    respWriter.println("Photo: " + getResp.getEntity().toString());
  }

  /**
   * call action purge to delete all photos on server
   */
  private void purgeAllPhotos(PrintWriter respWriter) throws RemoteInvocationException
  {
    final Request<Integer> purgeReq = _photoBuilders.actionPurge().build();
    final ResponseFuture<Integer> purgeFuture = _restClient.sendRequest(purgeReq);
    final Response<Integer> purgeResp = purgeFuture.getResponse();
    respWriter.println("Purged " + purgeResp.getEntity() + " photos");
  }

  /**
   * retrieve album
   */
  private void getAlbumSummary(PrintWriter respWriter, long newAlbumId) throws RemoteInvocationException
  {
    final Request<Album> getReq = _albumBuilders.get().id(newAlbumId).build();
    final ResponseFuture<Album> getFuture = _restClient.sendRequest(getReq);
    final Response<Album> getResp = getFuture.getResponse();
    respWriter.println("Album: " + getResp.getEntity().toString());
  }

  /**
   * failed request that try to access non-existing photo and throw RestException
   */
  private void getNonPhoto() throws RemoteInvocationException
  {
    final Request<Photo> failReq = _photoBuilders.get().id(-1L).build();
    final ResponseFuture<Photo> failFuture = _restClient.sendRequest(failReq);
    final Response<Photo> failResponse = failFuture.getResponse();
  }

  /**
   *
   */
  private void partialUpdatePhoto(PrintWriter respWriter, long photoId) throws RemoteInvocationException
  {
    final Request<Photo> getReq = _photoBuilders.get().id(photoId).build();
    final ResponseFuture<Photo> getFuture = _restClient.sendRequest(getReq);
    final Response<Photo> getResp = getFuture.getResponse();
    final Photo originalPhoto = getResp.getEntity();

    final Photo updatedPhoto = new Photo().setTitle("Partially Updated Photo");
    final PatchRequest<Photo> patch = PatchGenerator.diff(originalPhoto, updatedPhoto);

    final Request<EmptyRecord> partialUpdateRequest = _photoBuilders.partialUpdate().id(photoId).input(patch).build();
    final int status = _restClient.sendRequest(partialUpdateRequest).getResponse().getStatus();
    respWriter.println("Partial update photo is successful: " + (status == 202));
  }

  /**
   * use Finder to find the photo with some criteria
   */
  private void findPhoto(PrintWriter respWriter) throws RemoteInvocationException
  {
    final long newPhotoId = createPhoto(respWriter);
    createPhoto(respWriter);
    createPhoto(respWriter);

    final Request<Photo> getReq = _photoBuilders.get().id(newPhotoId).build();
    final ResponseFuture<Photo> getFuture = _restClient.sendRequest(getReq);
    final Response<Photo> getResp = getFuture.getResponse();
    final Photo photo = getResp.getEntity();

    final FindRequest<Photo> findReq = _photoBuilders
        .findByTitleAndOrFormat()
        .titleParam(photo.getTitle())
        .formatParam(photo.getFormat())
        .build();

    final CollectionResponse<Photo> crPhotos = _restClient.sendRequest(findReq).getResponse().getEntity();
    final List<Photo> photos = crPhotos.getElements();

    respWriter.println("Found " + photos.size() + " photos with title " + photo.getTitle());
  }

  private static final String SERVER_HOSTNAME = "localhost";
  private static final int SERVER_PORT = 7279;

  // builder is convenient way to generate rest.li request
  private static final PhotosRequestBuilders _photoBuilders          = new PhotosRequestBuilders();
  private static final AlbumsRequestBuilders _albumBuilders          = new AlbumsRequestBuilders();
  private static final AlbumEntryRequestBuilders _albumEntryBuilders = new AlbumEntryRequestBuilders();

  private final RestClient _restClient;
}

