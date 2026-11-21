package com.olimpiici.arena.web.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * High-precedence servlet filter that turns routine malformed/scanner client
 * requests into clean 4xx responses without flooding server logs with stack traces:
 * <ul>
 *   <li>Spring Security {@link RequestRejectedException} (malformed URLs such as
 *       {@code //}, {@code ..}, semicolons, encoded slashes) -&gt; 400 Bad Request</li>
 *   <li>Unparseable {@code Accept} headers (e.g. scanner/Log4Shell probes) -&gt; 406 Not Acceptable</li>
 *   <li>Undertow client-aborted multipart parsing ({@code UT000036}) -&gt; 400 Bad Request</li>
 * </ul>
 */
public class ClientRequestExceptionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ClientRequestExceptionFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!hasValidAcceptHeaders(request)) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (RequestRejectedException ex) {
            log.debug("Rejected malformed request: {}", ex.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (RuntimeException ex) {
            if (isClientAbortedMultipart(ex)) {
                log.debug("Client disconnected while parsing multipart request: {}", ex.getMessage());
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                return;
            }
            throw ex;
        }
    }

    private boolean hasValidAcceptHeaders(HttpServletRequest request) {
        Enumeration<String> acceptHeaders = request.getHeaders(HttpHeaders.ACCEPT);
        if (acceptHeaders == null) {
            return true;
        }
        while (acceptHeaders.hasMoreElements()) {
            String header = acceptHeaders.nextElement();
            if (!StringUtils.hasText(header)) {
                continue;
            }
            try {
                MediaType.parseMediaTypes(header);
            } catch (InvalidMediaTypeException | InvalidMimeTypeException ex) {
                log.debug("Rejected request with invalid Accept header: {}", ex.getMessage());
                return false;
            }
        }
        return true;
    }

    private boolean isClientAbortedMultipart(Throwable ex) {
        Throwable current = ex.getCause();
        for (int depth = 0; current != null && depth < 5; depth++) {
            if (current instanceof IOException) {
                String msg = current.getMessage();
                if (msg != null && (msg.contains("UT000036")
                        || msg.contains("Connection terminated parsing multipart data"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
