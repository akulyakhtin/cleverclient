package io.github.sashirestela.cleverclient.sender;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.stream.Stream;

import io.github.sashirestela.cleverclient.support.CleverClientSSE;
import io.github.sashirestela.cleverclient.util.JsonUtil;

public class HttpAsyncStreamSender extends HttpSender {

    @Override
    public <S, T> Object sendRequest(HttpClient httpClient, HttpRequest httpRequest, Class<T> responseClass,
            Class<S> genericClass) {

        var httpResponseFuture = httpClient.sendAsync(httpRequest, BodyHandlers.ofLines());

        return httpResponseFuture.thenApply(response -> {

            throwExceptionIfErrorIsPresent(response, Stream.class);

            final var lineRecord = new CleverClientSSE.LineRecord();

            return response.body()
                    .peek(line -> {
                        logger.debug("Response : {}", line);
                        lineRecord.updateWith(line);
                    })
                    .map(line -> new CleverClientSSE(lineRecord))
                    .filter(CleverClientSSE::isActualData)
                    .map(item -> JsonUtil.jsonToObject(item.getActualData(), responseClass));
        });
    }

}