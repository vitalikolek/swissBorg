package me.yukitale.cryptoexchange.exchange.security.xss;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XSSFilter implements Filter {

    private static final List<String> EXCLUSION_FILTERS = Arrays.asList(
            "/api/admin-panel/settings/email",
            "/api/admin-panel/settings/presets",
            "/api/admin-panel/settings/legals",
            "/api/admin-panel/settings/errors",
            "/api/admin-panel/user-edit/errors",
            "/api/admin-panel/user-edit/alert",
            "/api/supporter-panel/settings/presets",
            "/api/supporter-panel/user-edit/alert",
            "/api/supporter-panel/user-edit/errors",
            "/api/worker-panel/settings/legals",
            "/api/worker-panel/settings/presets",
            "/api/worker-panel/settings/errors",
            "/api/worker-panel/user-edit/errors",
            "/api/worker-panel/user-edit/alert"
    );


    private static boolean isExclusionUrl(String url) {
        return EXCLUSION_FILTERS.contains(url.toLowerCase().split("\\?")[0]);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (isExclusionUrl(((HttpServletRequest) request).getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String enctype = request.getContentType();
        if (StringUtils.isNotBlank(enctype) && enctype.toLowerCase().contains("multipart/form-data")) {
            //todo: comments fixed encoding (iso/utf-8)
            //StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
            //MultipartHttpServletRequest multipartHttpServletRequest = multipartResolver.resolveMultipart((HttpServletRequest) request);
            //chain.doFilter(new XSSRequestWrapper(multipartHttpServletRequest), response);
            chain.doFilter(request, response);
        } else {
            XSSRequestWrapper wrappedRequest = new XSSRequestWrapper ((HttpServletRequest) request);

            String body = IOUtils.toString(wrappedRequest.getReader());
            if (!body.isBlank()) {
                body = XSSUtils.stripXSS(body);
                wrappedRequest.resetInputStream(body.getBytes());
            }

            chain.doFilter(wrappedRequest, response);
        }
    }
}