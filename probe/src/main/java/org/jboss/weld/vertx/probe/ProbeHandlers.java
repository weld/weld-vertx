/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.vertx.probe;

import static org.jboss.weld.probe.Strings.FILTERS;
import static org.jboss.weld.probe.Strings.ID;
import static org.jboss.weld.probe.Strings.PAGE;
import static org.jboss.weld.probe.Strings.PAGE_SIZE;
import static org.jboss.weld.probe.Strings.PARAM_TRANSIENT_DEPENDENCIES;
import static org.jboss.weld.probe.Strings.PARAM_TRANSIENT_DEPENDENTS;
import static org.jboss.weld.probe.Strings.REPRESENTATION;

import java.io.IOException;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;

import org.jboss.weld.probe.JsonDataProvider;
import org.jboss.weld.vertx.web.WebRoute;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * The handlers copy the logic from <tt>org.jboss.weld.probe.Resource</tt> which is currently Servlet-based and declared as package-private. See also
 * <a href="https://issues.jboss.org/browse/WELD-2317">WELD-2317</a>.
 * 
 * @author Martin Kouba
 */
@Vetoed
public class ProbeHandlers {

    static final String BASE = "/weld-probe";
    static final String FILE_CLIENT_HTML = "probe.html";
    static final String PATH_META_INF_CLIENT = "/META-INF/client/";
    static final String RESOURCE = "resource";

    // Internet media types
    static final String APPLICATION_JSON = "application/json";
    static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";
    static final String APPLICATION_JAVASCRIPT = "application/javascript";
    // otf, ttf fonts
    static final String APPLICATION_FONT_SFNT = "application/font-sfnt";
    static final String APPLICATION_FONT_WOFF = "application/font-woff";
    // eot
    static final String APPLICATION_FONT_MS = "application/vnd.ms-fontobject";
    static final String TEXT_JAVASCRIPT = "text/javascript";
    static final String TEXT_CSS = "text/css";
    static final String TEXT_HTML = "text/html";
    static final String TEXT_PLAIN = "text/plain";
    static final String IMG_PNG = "image/png";
    static final String IMG_SVG = "image/svg+xml";
    static final String IMG_ICO = " image/x-icon";

    static final String ENCODING_UTF8 = "UTF-8";

    static final String SUFFIX_HTML = "html";
    static final String SUFFIX_CSS = "css";
    static final String SUFFIX_JS = "js";
    static final String SUFFIX_PNG = "png";
    static final String SUFFIX_TTF = "ttf";
    static final String SUFFIX_OTF = "otf";
    static final String SUFFIX_EOT = "eot";
    static final String SUFFIX_SVG = "svg";
    static final String SUFFIX_WOFF = "woff";
    static final String SUFFIX_ICO = "ico";

    @WebRoute(value = BASE + "/deployment", methods = HttpMethod.GET)
    public static class DeploymentHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            end(ctx.response(), jsonDataProvider.receiveDeployment());
        }

    }

    @WebRoute(value = BASE + "/beans", methods = HttpMethod.GET)
    public static class BeansHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            end(ctx.response(), jsonDataProvider.receiveBeans(getPage(ctx.request()), getPageSize(ctx.request()), ctx.request().getParam(FILTERS),
                    ctx.request().getParam(REPRESENTATION)));
        }

    }

    @WebRoute(value = BASE + "/beans/:id", methods = HttpMethod.GET)
    public static class BeanHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            endIfFound(ctx.response(),
                    jsonDataProvider.receiveBean(ctx.request().getParam(ID), Boolean.valueOf(ctx.request().getParam(PARAM_TRANSIENT_DEPENDENCIES)),
                            Boolean.valueOf(ctx.request().getParam(PARAM_TRANSIENT_DEPENDENTS))));
        }

    }

    @WebRoute(value = BASE + "/beans/:id/instance", methods = HttpMethod.GET)
    public static class BeanInstanceHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            endIfFound(ctx.response(), jsonDataProvider.receiveBeanInstance(ctx.request().getParam(ID)));
        }

    }

    @WebRoute(value = BASE + "/observers", methods = HttpMethod.GET)
    public static class ObserversHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            end(ctx.response(), jsonDataProvider.receiveObservers(getPage(ctx.request()), getPageSize(ctx.request()), ctx.request().getParam(FILTERS),
                    ctx.request().getParam(REPRESENTATION)));
        }

    }

    @WebRoute(value = BASE + "/observers/:id", methods = HttpMethod.GET)
    public static class ObserverHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            endIfFound(ctx.response(), jsonDataProvider.receiveObserver(ctx.request().getParam(ID)));
        }

    }

    @WebRoute(value = BASE + "/contexts", methods = HttpMethod.GET)
    public static class ContextsHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            end(ctx.response(), jsonDataProvider.receiveContexts());
        }

    }

    @WebRoute(value = BASE + "/contexts/:id", methods = HttpMethod.GET)
    public static class ContextHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            endIfFound(ctx.response(), jsonDataProvider.receiveContext(ctx.request().getParam(ID)));
        }

    }

    @WebRoute(value = BASE + "/invocations", methods = { HttpMethod.GET, HttpMethod.DELETE })
    public static class InvocationsHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            switch (ctx.request().method()) {
                case GET:
                    end(ctx.response(), jsonDataProvider.receiveInvocations(getPage(ctx.request()), getPageSize(ctx.request()), ctx.request().getParam(FILTERS),
                            ctx.request().getParam(REPRESENTATION)));
                    break;
                case DELETE:
                    end(ctx.response(), jsonDataProvider.clearInvocations());
                default:
                    ctx.response().setStatusCode(405).end();
            }
        }

    }

    @WebRoute(value = BASE + "/invocations/:id", methods = HttpMethod.GET)
    public static class InvocationHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            endIfFound(ctx.response(), jsonDataProvider.receiveInvocation(ctx.request().getParam(ID)));
        }

    }

    @WebRoute(value = BASE + "/events", methods = { HttpMethod.GET, HttpMethod.DELETE })
    public static class EventsHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            switch (ctx.request().method()) {
                case GET:
                    end(ctx.response(), jsonDataProvider.receiveEvents(getPage(ctx.request()), getPageSize(ctx.request()), ctx.request().getParam(FILTERS)));
                    break;
                case DELETE:
                    end(ctx.response(), jsonDataProvider.clearEvents());
                default:
                    ctx.response().setStatusCode(405).end();
            }
        }

    }

    @WebRoute(value = BASE + "/monitoring", methods = HttpMethod.GET)
    public static class MonitoringHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            end(ctx.response(), jsonDataProvider.receiveMonitoringStats());
        }

    }

    @WebRoute(value = BASE + "/availableBeans", methods = HttpMethod.GET)
    public static class AvailableBeansHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            end(ctx.response(), jsonDataProvider.receiveAvailableBeans(getPage(ctx.request()), getPageSize(ctx.request()), ctx.request().getParam(FILTERS),
                    ctx.request().getParam(REPRESENTATION)));
        }

    }

    @WebRoute(value = BASE + "/client/:" + RESOURCE, methods = HttpMethod.GET)
    public static class ClientResourceHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            handleResource(ctx, PATH_META_INF_CLIENT + ctx.request().getParam(RESOURCE));
        }

    }

    @WebRoute(value = BASE + "/export", methods = HttpMethod.GET)
    public static class ExportHandler implements Handler<RoutingContext> {

        @Inject
        JsonDataProvider jsonDataProvider;

        @Override
        public void handle(RoutingContext ctx) {
            setHeaders(ctx.response(), "application/zip");
            ctx.response().putHeader("Content-disposition", "attachment; filename=\"weld-probe-export.zip\"");
            try {
                ctx.response().write(Buffer.buffer(Exports.exportJsonData(jsonDataProvider)));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to export data", e);
            }
        }

    }

    @WebRoute(value = BASE, methods = HttpMethod.GET)
    public static class RootHandler implements Handler<RoutingContext> {

        @Override
        public void handle(RoutingContext ctx) {
            handleResource(ctx, PATH_META_INF_CLIENT + FILE_CLIENT_HTML);
        }

    }

    static void setHeaders(HttpServerResponse resp, String contentType) {
        resp.putHeader("Content-type", contentType);
        setCorsHeaders(resp);
    }

    static void setCorsHeaders(HttpServerResponse resp) {
        // Support cross-site HTTP requests - we want to support external HTML5 clients
        // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS
        resp.putHeader("Access-Control-Allow-Origin", "*");
        resp.putHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
    }

    static int getPage(HttpServerRequest req) {
        String pageParam = req.getParam(PAGE);
        if (pageParam == null) {
            return 1;
        }
        try {
            return Integer.valueOf(pageParam);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    static int getPageSize(HttpServerRequest req) {
        String pageSizeParam = req.getParam(PAGE_SIZE);
        if (pageSizeParam == null) {
            return 50;
        } else {
            try {
                int result = Integer.valueOf(pageSizeParam);
                return result > 0 ? result : 0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    static void end(HttpServerResponse response, String content) {
        setHeaders(response, APPLICATION_JSON_UTF8);
        response.end(content);
    }

    static void endIfFound(HttpServerResponse response, String content) {
        setHeaders(response, APPLICATION_JSON_UTF8);
        if (content == null) {
            response.setStatusCode(404).end();
            return;
        }
        response.end(content);
    }

    static String detectContentType(String resourceName) {
        if (resourceName.endsWith(SUFFIX_HTML)) {
            return TEXT_HTML;
        } else if (resourceName.endsWith(SUFFIX_CSS)) {
            return TEXT_CSS;
        } else if (resourceName.endsWith(SUFFIX_JS)) {
            return TEXT_JAVASCRIPT;
        } else if (resourceName.endsWith(SUFFIX_PNG)) {
            return IMG_PNG;
        } else if (resourceName.endsWith(SUFFIX_TTF) || resourceName.endsWith(SUFFIX_OTF)) {
            return APPLICATION_FONT_SFNT;
        } else if (resourceName.endsWith(SUFFIX_EOT)) {
            return APPLICATION_FONT_MS;
        } else if (resourceName.endsWith(SUFFIX_WOFF)) {
            return APPLICATION_FONT_WOFF;
        } else if (resourceName.endsWith(SUFFIX_SVG)) {
            return IMG_SVG;
        } else if (resourceName.endsWith(SUFFIX_ICO)) {
            return IMG_ICO;
        } else {
            return TEXT_PLAIN;
        }
    }

    static boolean isCachableContentType(String contentType) {
        return TEXT_CSS.equals(contentType) || TEXT_JAVASCRIPT.equals(contentType) || IMG_ICO.equals(contentType) || IMG_PNG.equals(contentType)
                || IMG_SVG.equals(contentType);
    }

    static boolean isTextBasedContenType(String contentType) {
        return !(IMG_PNG.equals(contentType) || IMG_ICO.equals(contentType) || APPLICATION_FONT_SFNT.equals(contentType)
                || APPLICATION_FONT_WOFF.equals(contentType) || APPLICATION_FONT_MS.equals(contentType));
    }

    static void handleResource(RoutingContext ctx, String resourceName) {

        String contentType = detectContentType(resourceName);
        setHeaders(ctx.response(), contentType);

        if (isCachableContentType(contentType)) {
            // Set Cache-Control header - 24 hours
            ctx.response().putHeader("Cache-Control", "max-age=86400");
        }

        if (isTextBasedContenType(contentType)) {
            String content = IOUtils.getResourceAsString(resourceName);
            if (content == null) {
                ctx.response().setStatusCode(404).end();
                return;
            }
            content = content.replace("${contextPath}", BASE + "/");
            ctx.response().end(content);
        } else {
            Buffer buffer = Buffer.buffer();
            if (IOUtils.writeResource(resourceName, buffer)) {
                ctx.response().end(buffer);
            } else {
                ctx.response().setStatusCode(404).end();
            }
        }
    }

}
