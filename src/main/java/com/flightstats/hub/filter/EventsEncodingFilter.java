package com.flightstats.hub.filter;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This exists to prevent gzip & deflate encodings for event-streams.
 */
@SuppressWarnings("WeakerAccess")
@PreMatching
public final class EventsEncodingFilter implements ContainerResponseFilter {

    private final static Logger logger = LoggerFactory.getLogger(EventsEncodingFilter.class);

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        MediaType contentType = (MediaType) response.getHeaders().getFirst("Content-Type");
        if (contentType != null && StringUtils.contains(contentType.getSubtype(), "event-stream")) {
            handleEncoding(request);
        }
    }

    private void handleEncoding(ContainerRequestContext request) {
        List<String> acceptEncoding = request.getHeaders().get(HttpHeaders.ACCEPT_ENCODING);
        logger.trace("acceptEncoding {}", acceptEncoding);
        List<String> allowedEncoding = new ArrayList<>();
        if (acceptEncoding != null) {
            for (String encoding : acceptEncoding) {
                List<String> innerEncoding = new ArrayList<>();
                for (String token : encoding.split(",")) {
                    token = token.trim();
                    if (!FilterUtils.removedEncodings.contains(token)) {
                        innerEncoding.add(token);
                    }
                }
                allowedEncoding.add(Joiner.on(",").join(innerEncoding));
            }
            logger.debug("removing from events {} ", allowedEncoding);
            request.getHeaders().put(HttpHeaders.ACCEPT_ENCODING, allowedEncoding);
        }
    }


}
