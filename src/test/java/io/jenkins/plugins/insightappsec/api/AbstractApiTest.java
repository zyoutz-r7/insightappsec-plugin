package io.jenkins.plugins.insightappsec.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jenkins.plugins.insightappsec.MappingConfiguration;
import io.jenkins.plugins.insightappsec.exception.APIException;
import io.jenkins.plugins.insightappsec.mock.MockHttpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.jenkins.plugins.insightappsec.api.search.PageModels.aMetadata;
import static io.jenkins.plugins.insightappsec.api.search.PageModels.aPageOf;
import static java.lang.String.format;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AbstractApiTest {

    private static final String X_API_KEY = "x-api-key";
    private static final String API_KEY = UUID.randomUUID().toString();

    private static final String HOST = "test.com";
    private static final Body BODY = new Body("test");
    private static final String PATH = "/test";
    private static final String ID = UUID.randomUUID().toString();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private HttpClient client;

    @InjectMocks
    private TestApi testApi;

    // POST

    @Test
    public void post_201Response() throws IOException {
        // given
        HttpResponse response = MockHttpResponse.create(201, locationHeader());
        given(client.execute(any(HttpPost.class))).willReturn(response);

        // when
        String actualId = testApi.post(PATH, BODY);

        // then
        assertEquals(ID, actualId);

        verifyRequestAttributes();
        verifyRequestContent();
    }

    @Test
    public void post_non201Response() throws IOException {
        // given
        HttpResponse response = MockHttpResponse.create(500);
        given(client.execute(any(HttpPost.class))).willReturn(response);

        exception.expect(APIException.class);
        exception.expectMessage(format("Error occurred during POST of [%s]. Expected status code [%s]. Response was: %n %s",
                                       Body.class.getSimpleName(),
                                       HttpStatus.SC_CREATED,
                                       response));

        // when
        testApi.post(PATH, BODY);

        // then
        // expected exception
    }

    @Test
    public void post_error() throws IOException {
        // given
        given(client.execute(any(HttpPost.class))).willThrow(IOException.class);

        exception.expect(APIException.class);
        exception.expectCause(isA(IOException.class));
        exception.expectMessage(format("Error occurred during POST of [%s]", Body.class.getName()));

        // when
        testApi.post(PATH, BODY);

        // then
        // expected exception
    }

    @Test
    public void postForAll_singlePage() throws IOException {
        // given
        Page<Body> page0 = aPageOf(() -> BODY, 50).metadata(aMetadata().index(0).totalPages(1).build()).build();

        given(client.execute(argThat((req) -> "size=1000&index=0".equals(req.getURI().getQuery()))))
                .willReturn(MockHttpResponse.create(200, page0));

        // when
        List<Body> allResults = testApi.postForAll(PATH, Body.class, BODY);

        // then
        assertEquals(page0.getData(), allResults);
    }

    @Test
    public void postForAll_multiPage() throws IOException {
        // given
        Page<Body> page0 = aPageOf(() -> BODY, 50).metadata(aMetadata().index(0).totalPages(3).build()).build();
        Page<Body> page1 = aPageOf(() -> BODY, 50).metadata(aMetadata().index(1).totalPages(3).build()).build();
        Page<Body> page2 = aPageOf(() -> BODY, 10).metadata(aMetadata().index(2).totalPages(3).build()).build();

        doReturn(MockHttpResponse.create(200, page0)).when(client).execute(argThat((req) -> "size=1000&index=0".equals(req.getURI().getQuery())));
        doReturn(MockHttpResponse.create(200, page1)).when(client).execute(argThat((req) -> "size=1000&index=1".equals(req.getURI().getQuery())));
        doReturn(MockHttpResponse.create(200, page2)).when(client).execute(argThat((req) -> "size=1000&index=2".equals(req.getURI().getQuery())));

        // when
        List<Body> allResults = testApi.postForAll(PATH, Body.class, BODY);

        // then
        List<Body> expected = new ArrayList<>(page0.getData());
        expected.addAll(page1.getData());
        expected.addAll(page2.getData());

        assertEquals(expected, allResults);
    }

    @Test
    public void postForAll_zeroResults() throws IOException {
        // given
        Page<Body> page0 = aPageOf(() -> BODY, 0).metadata(aMetadata().index(0).totalPages(0).build()).build();

        doReturn(MockHttpResponse.create(200, page0)).when(client).execute(argThat((req) -> "size=1000&index=0".equals(req.getURI().getQuery())));

        // when
        List<Body> allResults = testApi.postForAll(PATH, Body.class, BODY);

        // then
        List<Body> expected = new ArrayList<>(page0.getData());

        assertEquals(expected, allResults);
    }

    // PUT

    @Test
    public void put_200Response() throws IOException {
        // given
        HttpResponse response = MockHttpResponse.create(200);
        given(client.execute(any(HttpPut.class))).willReturn(response);

        // when
        testApi.put(PATH, BODY);

        // then
        verifyRequestAttributes();
        verifyRequestContent();
    }

    @Test
    public void put_non200Response() throws IOException {
        // given
        HttpResponse response = MockHttpResponse.create(500);
        given(client.execute(any(HttpPut.class))).willReturn(response);

        exception.expect(APIException.class);
        exception.expectMessage(format("Error occurred during PUT of [%s]. Expected status code [%s]. Response was: %n %s",
                                       Body.class.getSimpleName(),
                                       HttpStatus.SC_OK,
                                       response));

        // when
        testApi.put(PATH, BODY);

        // then
        // expected exception
    }

    @Test
    public void put_error() throws IOException {
        // given
        given(client.execute(any(HttpPut.class))).willThrow(IOException.class);

        exception.expect(APIException.class);
        exception.expectCause(isA(IOException.class));
        exception.expectMessage(format("Error occurred during PUT of [%s]", Body.class.getName()));

        // when
        testApi.put(PATH, BODY);

        // then
        // expected exception
    }

    // GET

    @Test
    public void getById_200Response() throws IOException {
        // given
        HttpResponse response = MockHttpResponse.create(200, BODY);
        given(client.execute(any(HttpGet.class))).willReturn(response);

        // when
        Body body = testApi.getById(PATH, ID, Body.class);

        // then
        assertEquals(BODY, body);

        verifyRequestAttributes();
    }

    @Test
    public void getById_non200Response() throws IOException {
        // given
        HttpResponse response = MockHttpResponse.create(500);
        given(client.execute(any(HttpGet.class))).willReturn(response);

        exception.expect(APIException.class);
        exception.expectMessage(format("Error occurred during GET for [%s] with id [%s]. Expected status code [%s]. Response was: %n %s",
                                       Body.class.getSimpleName(),
                                       ID,
                                       HttpStatus.SC_OK,
                                       response));

        // when
        testApi.getById(PATH, ID, Body.class);

        // then
        // excepted exception
    }

    @Test
    public void getById_error() throws IOException {
        // given
        given(client.execute(any(HttpGet.class))).willThrow(IOException.class);

        exception.expect(APIException.class);
        exception.expectCause(isA(IOException.class));
        exception.expectMessage(format("Error occurred during GET for [%s] with id [%s]",
                                       Body.class.getSimpleName(),
                                       ID));
        // when
        testApi.getById(PATH, ID, Body.class);

        // then
        // excepted exception
    }

    @Test
    public void getForAll_singlePage() throws IOException {
        // given
        Page<Body> page0 = aPageOf(() -> BODY, 50).metadata(aMetadata().index(0).totalPages(1).build()).build();

        given(client.execute(argThat((req) -> "size=1000&index=0".equals(req.getURI().getQuery()))))
              .willReturn(MockHttpResponse.create(200, page0));

        // when
        List<Body> allResults = testApi.getForAll(PATH, Body.class);

        // then
        assertEquals(page0.getData(), allResults);
    }

    @Test
    public void getForAll_multiPage() throws IOException {
        // given
        Page<Body> page0 = aPageOf(() -> BODY, 50).metadata(aMetadata().index(0).totalPages(3).build()).build();
        Page<Body> page1 = aPageOf(() -> BODY, 50).metadata(aMetadata().index(1).totalPages(3).build()).build();
        Page<Body> page2 = aPageOf(() -> BODY, 10).metadata(aMetadata().index(2).totalPages(3).build()).build();

        doReturn(MockHttpResponse.create(200, page0)).when(client).execute(argThat((req) -> "size=1000&index=0".equals(req.getURI().getQuery())));
        doReturn(MockHttpResponse.create(200, page1)).when(client).execute(argThat((req) -> "size=1000&index=1".equals(req.getURI().getQuery())));
        doReturn(MockHttpResponse.create(200, page2)).when(client).execute(argThat((req) -> "size=1000&index=2".equals(req.getURI().getQuery())));

        // when
        List<Body> allResults = testApi.getForAll(PATH, Body.class);

        // then
        List<Body> expected = new ArrayList<>(page0.getData());
        expected.addAll(page1.getData());
        expected.addAll(page2.getData());

        assertEquals(expected, allResults);
    }

    @Test
    public void getForAll_zeroResults() throws IOException {
        // given
        Page<Body> page0 = aPageOf(() -> BODY, 0).metadata(aMetadata().index(0).totalPages(0).build()).build();

        doReturn(MockHttpResponse.create(200, page0)).when(client).execute(argThat((req) -> "size=1000&index=0".equals(req.getURI().getQuery())));

        // when
        List<Body> allResults = testApi.getForAll(PATH, Body.class);

        // then
        List<Body> expected = new ArrayList<>(page0.getData());

        assertEquals(expected, allResults);
    }

    // TEST HELPERS

    /**
     * There is no .equals() method on apache HttpX request classes, to use with given(..)thenReturn(..),
     * Use an argument capture to verify.
     *
     * Attributes common to all requests.
     */
    private void verifyRequestAttributes() throws IOException {
        verify(client).execute(argThat((req) -> expectedUri().equals(req.getURI())));
        verify(client).execute(argThat((req) -> API_KEY.equals(req.getHeaders(X_API_KEY)[0].getValue())));
    }

    /**
     * There is no .equals() method on apache HttpX request classes, to use with given(..)thenReturn(..),
     * Use an argument capture to verify.
     *
     * Attributes common to POST / PUT requests.
     */
    private void verifyRequestContent() throws IOException {
        verify(client).execute(argThat(((request) -> expectedBody().equals(getBody((HttpEntityEnclosingRequestBase) request)))));
        verify(client).execute(argThat(((request) -> ContentType.APPLICATION_JSON.toString().equals(getContentType((HttpEntityEnclosingRequestBase) request)))));
    }

    private String getContentType(HttpEntityEnclosingRequestBase post) {
        return post.getEntity().getContentType().getValue();
    }

    private String getBody(HttpEntityEnclosingRequestBase post) {
        try {
            return IOUtils.toString(post.getEntity().getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URI expectedUri() {
        try {
            return new URIBuilder(String.format("https://%s/ias/v1%s", HOST, PATH)).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String expectedBody() {
        try {
            return MappingConfiguration.OBJECT_MAPPER_INSTANCE.writeValueAsString(BODY);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Header[] locationHeader() {
        return header(HttpHeaders.LOCATION, "http://some.location.com/" + ID);
    }

    private Header[] header(String name,
                            String value) {
        Header[] header = new Header[1];

        header[0] = new BasicHeader(name, value);

        return header;
    }

    // TEST CLASSES

    private static class TestApi extends AbstractApi {

        private TestApi(HttpClient client) {
            super(client, HOST, API_KEY);
        }

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Body {

        private String test;

    }

}