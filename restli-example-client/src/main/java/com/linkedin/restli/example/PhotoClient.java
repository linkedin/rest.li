package com.linkedin.restli.example;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.linkedin.common.callback.Callback;
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
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.example.photos.AlbumEntryBuilders;
import com.linkedin.restli.example.photos.AlbumsBuilders;
import com.linkedin.restli.example.photos.PhotosBuilders;

/**
 * @author kjin
 */
public class PhotoClient
{
  public static final int  PORT = 7279; // Must match that in restli-examples-server
                                        // RestLiExamplesServer
  private final RestClient _restClient;

  /*
   * This is a stand-alone app to demo the use of client-side Pegasus API. To run in,
   * com.linkedin.restli.example.RestLiExamplesServer has to be running.
   *
   * The only argument is the path to the resource on the photo server, e.g. /album/1
   */
  public static void main(String[] args)
  {
    // create HTTP Netty client with default properties
    final TransportClient transClient =
        new HttpClientFactory().getClient(Collections.<String, String> emptyMap());
    // create an abstraction layer over the actual client, which supports both REST and
    // RPC
    final Client r2Client = new TransportClientAdapter(transClient);
    // REST client wrapper that simplifies the interface
    RestClient restClient = new RestClient(r2Client, "http://localhost:" + PORT);

    PhotoClient photoClient = new PhotoClient(restClient);

    photoClient.processGet(args[0], new PrintWriter(System.out));
  }



  public PhotoClient(RestClient restClient)
  {
    this._restClient = restClient;
  }



  public void processGet(String pathInfo, PrintWriter respWriter) // throws IOException
  {
    /*
     * Supported URLs /fail just fail /album/<album_id> display album and the photos /
     * make photo, get photo, purge photos
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
          final Long newPhotoId = createPhoto(respWriter);
          final CountDownLatch latch = new CountDownLatch(1);
          createPhotoAsync(respWriter, latch, newPhotoId);
          getPhoto(respWriter, newPhotoId);
          findPhoto(respWriter);
          purgeAllPhotos(respWriter);
          try
          {
            latch.await();
          }
          catch (InterruptedException e)
          {
            respWriter.println(e.getMessage());
          }

          final Long id = createPhoto(respWriter);
          partialUpdatePhoto(respWriter, id);
          // photos and albums have IDs starting from 1
          getAlbumSummary(respWriter, (long) new Random().nextInt(10) + 1);
        }
      }
    }
    catch (RemoteInvocationException e)
    {
      respWriter.println("RemoteInvocationException: " + e.getMessage());
    }
    respWriter.flush();
    respWriter.close();
  }

  private Long createPhoto(PrintWriter respWriter) throws RemoteInvocationException
  {
    // 1) make create photo request and send with the rest client asynchronously
    // response of create request does not have body, therefore use EmptyRecord as
    // template

    // create an instance of photo pragmatically
    // this resembles to photo-create.json
    final LatLong newLatLong =
        new LatLong().setLatitude(37.42394f).setLongitude(-122.0708f);
    final EXIF newExif = new EXIF().setLocation(newLatLong);
    final Photo newPhoto =
        new Photo().setTitle("New Photo").setFormat(PhotoFormats.PNG).setExif(newExif);

    final Request<EmptyRecord> createReq1 =
        _photoBuilders.create().input(newPhoto).build();
    final ResponseFuture<EmptyRecord> createFuture1 = _restClient.sendRequest(createReq1);
    // Future.getResource() blocks until server responds
    final Response<EmptyRecord> createResp1 = createFuture1.getResponse();

    // HTTP header Location also shows the relative URI of the created resource
    final Long newPhotoId = Long.parseLong(createResp1.getId());
    respWriter.println("New photo ID: " + newPhotoId);

    return newPhotoId;
  }

  private void createPhotoAsync(final PrintWriter respWriter,
                                       final CountDownLatch latch,
                                       final Long newPhotoId)
  {
    // 2) update photo with different data

    // this resembles to photo-create-id.json
    final LatLong newLatLong = new LatLong().setLatitude(40.725f).setLongitude(-74.005f);
    final EXIF newExif = new EXIF().setIsFlash(false).setLocation(newLatLong);
    final Photo newPhoto =
        new Photo().setId(123)
                   .setTitle("Updated Photo")
                   .setFormat(PhotoFormats.JPG)
                   .setExif(newExif);

    final Request<EmptyRecord> createReq2 =
        _photoBuilders.update().id(newPhotoId).input(newPhoto).build();

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
   * Retrieve the album information and each photo in the album. The photos are retrieved
   * in parallel.
   */
  private void getAlbum(PrintWriter respWriter, Long albumId) throws RemoteInvocationException
  {
    // get the album
    final Request<Album> getAlbumReq = _albumBuilders.get().id(albumId).build();
    final ResponseFuture<Album> getAlbumFuture = _restClient.sendRequest(getAlbumReq);
    final Response<Album> getResp = getAlbumFuture.getResponse();
    final Album album = getResp.getEntity();

    respWriter.println("<!DOCTYPE html>");
    respWriter.println("<html><head><title>");
    respWriter.println(album.getTitle());
    respWriter.println("</title><body>");
    respWriter.println("<h1>" + album.getTitle() + "</h1>");
    respWriter.println("Created on " + new Date(album.getCreationTime()));

    // get the album's entries
    final FindRequest<AlbumEntry> searchReq =
        _albumEntryBuilders.findBySearch().albumIdParam(albumId).build();
    final ResponseFuture<CollectionResponse<AlbumEntry>> responseFuture =
        _restClient.sendRequest(searchReq);
    final Response<CollectionResponse<AlbumEntry>> response =
        responseFuture.getResponse();
    final List<AlbumEntry> entries =
        new ArrayList<AlbumEntry>(response.getEntity().getElements());

    // intentional fail
    entries.add(new AlbumEntry().setAlbumId(1337).setPhotoId(42));

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
          latch.countDown();
        }
      });
    }

    try
    {
      // wait for all requests to finish
      latch.await(2, TimeUnit.SECONDS);
      if (latch.getCount() > 0)
        respWriter.println("Failed to retrieve some photo(s)");
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
        respWriter.println("<h2>Failed to load photo " + entry.getPhotoId() + "</h2>");
        respWriter.println("<pre>");
        ((Throwable) val).printStackTrace(respWriter);
        respWriter.println("</pre>");
      }
      else if (val instanceof Photo)
      {
        final Photo photo = (Photo) val;
        respWriter.println("<h2>" + photo.getTitle() + "</h2>");
        respWriter.println("<pre>");
        respWriter.println(photo);
        respWriter.println("</pre>");
        respWriter.println("Added on " + new Date(entry.getAddTime()));
      }
      else
        throw new AssertionError("expected photo or exception");
    }
    respWriter.println("</body>");
    respWriter.println("</html>");
  }

  private void getPhoto(PrintWriter respWriter, Long newPhotoId) throws RemoteInvocationException
  {
    // 3) get request to retrieve created photo
    final Request<Photo> getReq = _photoBuilders.get().id(newPhotoId).build();
    final ResponseFuture<Photo> getFuture = _restClient.sendRequest(getReq);
    final Response<Photo> getResp = getFuture.getResponse();
    respWriter.println("Photo: " + getResp.getEntity().toString());
  }

  private void purgeAllPhotos(PrintWriter respWriter) throws RemoteInvocationException
  {
    // 4) call action purge to delete all photos on server
    final Request<Integer> purgeReq = _photoBuilders.actionPurge().build();
    final ResponseFuture<Integer> purgeFuture = _restClient.sendRequest(purgeReq);
    final Response<Integer> purgeResp = purgeFuture.getResponse();
    respWriter.println("Purged: " + purgeResp.getEntity());
  }

  private void getAlbumSummary(PrintWriter respWriter, Long newAlbumId) throws RemoteInvocationException
  {
    // 5) get request to retrieve created photo
    final Request<Album> getReq = _albumBuilders.get().id(newAlbumId).build();
    final ResponseFuture<Album> getFuture = _restClient.sendRequest(getReq);
    final Response<Album> getResp = getFuture.getResponse();
    respWriter.println("Album: " + getResp.getEntity().toString());
  }

  private void getNonPhoto() throws RemoteInvocationException
  {
    // *) failed request that try to access non-existing photo and throw RestException
    final Request<Photo> failReq = _photoBuilders.get().id(-1L).build();
    final ResponseFuture<Photo> failFuture = _restClient.sendRequest(failReq);
    final Response<Photo> failResponse = failFuture.getResponse();
  }

  private void partialUpdatePhoto(PrintWriter respWriter, Long photoId) throws RemoteInvocationException
  {
    Request<Photo> getReq = _photoBuilders.get().id(photoId).build();
    ResponseFuture<Photo> getFuture = _restClient.sendRequest(getReq);
    Response<Photo> getResp = getFuture.getResponse();
    Photo originalPhoto = getResp.getEntity();

    final Photo updatedPhoto = new Photo().setTitle("Partially Updated Photo");
    PatchRequest<Photo> patch = PatchGenerator.diff(originalPhoto, updatedPhoto);

    Request<EmptyRecord> partialUpdateRequest =
        _photoBuilders.partialUpdate().id(photoId).input(patch).build();
    int status = _restClient.sendRequest(partialUpdateRequest).getResponse().getStatus();
    respWriter.println("Partial update photo is successful: " + (status == 202));

  }

  private void findPhoto(PrintWriter respWriter) throws RemoteInvocationException
  {
    final Long newPhotoId = createPhoto(respWriter);
    createPhoto(respWriter);
    createPhoto(respWriter);

    Request<Photo> getReq = _photoBuilders.get().id(newPhotoId).build();
    ResponseFuture<Photo> getFuture = _restClient.sendRequest(getReq);
    Response<Photo> getResp = getFuture.getResponse();
    Photo photo = getResp.getEntity();

    FindRequest<Photo> findReq =
        _photoBuilders.findByTitleAndOrFormat()
                      .titleParam(photo.getTitle())
                      .formatParam(photo.getFormat())
                      .build();

    CollectionResponse<Photo> crPhotos =
        _restClient.sendRequest(findReq).getResponse().getEntity();
    List<Photo> photos = crPhotos.getElements();

    respWriter.println("Found " + photos.size() + " photos with title "
        + photo.getTitle());
  }

  // builder is convenient way to generate rest.li request
  private static final PhotosBuilders     _photoBuilders      = new PhotosBuilders();
  private static final AlbumsBuilders     _albumBuilders      = new AlbumsBuilders();
  private static final AlbumEntryBuilders _albumEntryBuilders = new AlbumEntryBuilders();

}
