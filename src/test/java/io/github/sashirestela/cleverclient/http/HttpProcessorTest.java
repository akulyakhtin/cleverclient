package io.github.sashirestela.cleverclient.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.sashirestela.cleverclient.support.CleverClientException;

@SuppressWarnings("unchecked")
class HttpProcessorTest {

  HttpProcessor httpProcessor;
  HttpClient httpClient = mock(HttpClient.class);
  HttpResponse<String> httpResponse = mock(HttpResponse.class);
  HttpResponse<Stream<String>> httpResponseStream = mock(HttpResponse.class);

  @BeforeEach
  void init() {
    httpProcessor = new HttpProcessor(
        httpClient,
        "https://api.demmo",
        null);
  }

  @Test
  void shouldThownExceptionWhenCallingCreateMethodForNoAnnotedMethod() {
    Exception exception = assertThrows(CleverClientException.class,
        () -> httpProcessor.createProxy(ITest.NotAnnotatedService.class));
    assertTrue(exception.getMessage().contains("Missing HTTP anotation for the method"));
  }

  @Test
  void shouldThownExceptionWhenCallingCreateMethodForBadPathParamMethod() {
    Exception exception = assertThrows(CleverClientException.class,
        () -> httpProcessor.createProxy(ITest.BadPathParamService.class));
    assertTrue(exception.getMessage().contains("Path param demoId in the url cannot find"));
  }

  @Test
  void shouldThownExceptionWhenCallingMethodReturnTypeIsUnsupported() {
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    Exception exception = assertThrows(CleverClientException.class,
        () -> service.unsupportedMethod());
    assertTrue(exception.getMessage().contains("Unsupported return type"));
  }

  @Test
  void shouldReturnAStringSyncWhenMethodReturnTypeIsAString() throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("{\"id\":100,\"description\":\"Description\",\"active\":true}");
    
    var service = httpProcessor.createProxy(ITest.SyncService.class);
    var actualDemo = service.getDemoPlain(100);
    var expectedDemo = "{\"id\":100,\"description\":\"Description\",\"active\":true}";
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAnObjectSyncWhenMethodReturnTypeIsAnObject() throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("{\"id\":100,\"description\":\"Description\",\"active\":true}");
    
    var service = httpProcessor.createProxy(ITest.SyncService.class);
    var actualDemo = service.getDemo(100);
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAGenericSyncWhenMethodReturnTypeIsAnObject() throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("{\"id\":1,\"listDemo\":[{\"id\":100,\"description\":\"Description\",\"active\":true}]}");
    
    var service = httpProcessor.createProxy(ITest.SyncService.class);
    var actualGenericDemo = service.getGenericDemo(1);
    var actualDemo = actualGenericDemo.getListDemo().get(0);
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAListSyncWhenMethodReturnTypeIsAList() throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("[{\"id\":100,\"description\":\"Description\",\"active\":true}]");

    var service = httpProcessor.createProxy(ITest.SyncService.class);
    var actualListDemo = service.getDemos();
    var actualDemo = actualListDemo.get(0);
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAStreamSyncWhenMethodReturnTypeIsAStream() throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofLines().getClass())))
        .thenReturn(httpResponseStream);
    when(httpResponseStream.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponseStream.body()).thenReturn(Stream.of("data: {\"id\":100,\"description\":\"Description\",\"active\":true}"));

    var service = httpProcessor.createProxy(ITest.SyncService.class);
    var actualStreamDemo = service.getDemoStream(new ITest.RequestDemo("Descr", null));
    var actualDemo = actualStreamDemo.findFirst().get();
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAStringAsyncWhenMethodReturnTypeIsAString() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("{\"id\":100,\"description\":\"Description\",\"active\":true}");
    
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var actualDemo = service.getDemoPlain(100).join();
    var expectedDemo = "{\"id\":100,\"description\":\"Description\",\"active\":true}";
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAnObjectAsyncWhenMethodReturnTypeIsAnObject() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("{\"id\":100,\"description\":\"Description\",\"active\":true}");
    
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var actualDemo = service.getDemo(100).join();
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAGenericAsyncWhenMethodReturnTypeIsAnObject() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("{\"id\":1,\"listDemo\":[{\"id\":100,\"description\":\"Description\",\"active\":true}]}");
    
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var actualGenericDemo = service.getGenericDemo(1).join();
    var actualDemo = actualGenericDemo.getListDemo().get(0);
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAListAsyncWhenMethodReturnTypeIsAList() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("[{\"id\":100,\"description\":\"Description\",\"active\":true}]");

    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var actualListDemo = service.getDemos().join();
    var actualDemo = actualListDemo.get(0);
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAStreamAsyncWhenMethodReturnTypeIsAStream() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofLines().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponseStream));
    when(httpResponseStream.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponseStream.body()).thenReturn(Stream.of("data: {\"id\":100,\"description\":\"Description\",\"active\":true}"));

    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var actualStreamDemo = service.getDemoStream(new ITest.RequestDemo("Descr", null)).join();
    var actualDemo = actualStreamDemo.findFirst().get();
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldReturnAnObjectWhenMethodIsAnnotatedWithMultipart() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(httpResponse.body()).thenReturn("{\"id\":100,\"description\":\"Description\",\"active\":true}");
    
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var actualDemo = service.getFile(new ITest.RequestDemo("Descr", Paths.get("src/test/resources/image.png"))).join();
    var expectedDemo = new ITest.Demo(100, "Description", true);
    
    assertEquals(expectedDemo, actualDemo);
  }

  @Test
  void shouldThrownExceptionWhenCallingNoStreamingMethodAndServerRespondsWithError() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
    when(httpResponse.body()).thenReturn(
      "{\"error\": {\"message\": \"The resource does not exist\", \"type\": \"T\", \"param\": \"P\", \"code\": \"C\"}}");
    
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var futureService = service.getDemo(100);
    Exception exception = assertThrows(CompletionException.class,
        () -> futureService.join());
    assertTrue(exception.getMessage().contains("The resource does not exist"));
  }

  @Test
  void shouldThrownExceptionWhenCallingStreamingMethodAndServerRespondsWithError() {
    when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofLines().getClass())))
        .thenReturn(CompletableFuture.completedFuture(httpResponseStream));
    when(httpResponseStream.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
    when(httpResponseStream.body()).thenReturn(Stream.of(
      "{\"error\": {\"message\": \"The resource does not exist\", \"type\": \"T\", \"param\": \"P\", \"code\": \"C\"}}"));
    
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var futureService = service.getDemoStream(new ITest.RequestDemo("Descr", null));
    Exception exception = assertThrows(CompletionException.class,
        () -> futureService.join());
    assertTrue(exception.getMessage().contains("The resource does not exist"));
  }

  @Test
  void shouldExecuteDefaultMethodWhenItIsCalled() {
    var service = httpProcessor.createProxy(ITest.AsyncService.class);
    var actualValue = service.defaultMethod("Test");
    var expectedValue = "Hello Test";

    assertEquals(expectedValue, actualValue);
  }
}