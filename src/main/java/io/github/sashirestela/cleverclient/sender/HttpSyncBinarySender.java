package io.github.sashirestela.cleverclient.sender;

import io.github.sashirestela.cleverclient.support.CleverClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

public class HttpSyncBinarySender extends HttpSender {

    @Override
    public <S, T> Object sendRequest(HttpClient httpClient, HttpRequest httpRequest, Class<T> responseClass,
            Class<S> genericClass) {
        try {

            var httpResponse = httpClient.send(httpRequest, BodyHandlers.ofInputStream());

            throwExceptionIfErrorIsPresent(httpResponse, InputStream.class);

            var rawData = httpResponse.body();

            logger.debug("Response : {}", rawData);

            return rawData;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CleverClientException(e.getMessage(), null, e);
        }
    }

}
