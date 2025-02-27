// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.CoreConfig;
import dan200.computercraft.core.apis.http.*;
import dan200.computercraft.core.apis.http.request.HttpRequest;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.core.apis.TableHelper.*;

/**
 * Placeholder description, please ignore.
 *
 * @cc.module http
 * @hidden
 */
public class HTTPAPI implements ILuaAPI {
    private final IAPIEnvironment apiEnvironment;

    private final ResourceGroup<CheckUrl> checkUrls = new ResourceGroup<>(() -> ResourceGroup.DEFAULT_LIMIT);
    private final ResourceGroup<HttpRequest> requests = new ResourceQueue<>(() -> CoreConfig.httpMaxRequests);
    private final ResourceGroup<Websocket> websockets = new ResourceGroup<>(() -> CoreConfig.httpMaxWebsockets);

    public HTTPAPI(IAPIEnvironment environment) {
        apiEnvironment = environment;
    }

    @Override
    public String[] getNames() {
        return new String[]{ "http" };
    }

    @Override
    public void startup() {
        checkUrls.startup();
        requests.startup();
        websockets.startup();
    }

    @Override
    public void shutdown() {
        checkUrls.shutdown();
        requests.shutdown();
        websockets.shutdown();
    }

    @Override
    public void update() {
        // It's rather ugly to run this here, but we need to clean up
        // resources as often as possible to reduce blocking.
        Resource.cleanup();
    }

    @LuaFunction
    public final Object[] request(IArguments args) throws LuaException {
        String address, postString, requestMethod;
        Map<?, ?> headerTable;
        boolean binary, redirect;

        if (args.get(0) instanceof Map) {
            var options = args.getTable(0);
            address = getStringField(options, "url");
            postString = optStringField(options, "body", null);
            headerTable = optTableField(options, "headers", Collections.emptyMap());
            binary = optBooleanField(options, "binary", false);
            requestMethod = optStringField(options, "method", null);
            redirect = optBooleanField(options, "redirect", true);

        } else {
            // Get URL and post information
            address = args.getString(0);
            postString = args.optString(1, null);
            headerTable = args.optTable(2, Collections.emptyMap());
            binary = args.optBoolean(3, false);
            requestMethod = null;
            redirect = true;
        }

        var headers = getHeaders(headerTable);

        HttpMethod httpMethod;
        if (requestMethod == null) {
            httpMethod = postString == null ? HttpMethod.GET : HttpMethod.POST;
        } else {
            httpMethod = HttpMethod.valueOf(requestMethod.toUpperCase(Locale.ROOT));
            if (httpMethod == null || requestMethod.equalsIgnoreCase("CONNECT")) {
                throw new LuaException("Unsupported HTTP method");
            }
        }

        try {
            var uri = HttpRequest.checkUri(address);
            var request = new HttpRequest(requests, apiEnvironment, address, postString, headers, binary, redirect);

            // Make the request
            if (!request.queue(r -> r.request(uri, httpMethod))) {
                throw new LuaException("Too many ongoing HTTP requests");
            }

            return new Object[]{ true };
        } catch (HTTPRequestException e) {
            return new Object[]{ false, e.getMessage() };
        }
    }

    @LuaFunction
    public final Object[] checkURL(String address) throws LuaException {
        try {
            var uri = HttpRequest.checkUri(address);
            if (!new CheckUrl(checkUrls, apiEnvironment, address, uri).queue(CheckUrl::run)) {
                throw new LuaException("Too many ongoing checkUrl calls");
            }

            return new Object[]{ true };
        } catch (HTTPRequestException e) {
            return new Object[]{ false, e.getMessage() };
        }
    }

    @LuaFunction
    public final Object[] websocket(String address, Optional<Map<?, ?>> headerTbl) throws LuaException {
        if (!CoreConfig.httpWebsocketEnabled) {
            throw new LuaException("Websocket connections are disabled");
        }

        var headers = getHeaders(headerTbl.orElse(Collections.emptyMap()));

        try {
            var uri = Websocket.checkUri(address);
            if (!new Websocket(websockets, apiEnvironment, uri, address, headers).queue(Websocket::connect)) {
                throw new LuaException("Too many websockets already open");
            }

            return new Object[]{ true };
        } catch (HTTPRequestException e) {
            return new Object[]{ false, e.getMessage() };
        }
    }

    private HttpHeaders getHeaders(Map<?, ?> headerTable) throws LuaException {
        HttpHeaders headers = new DefaultHttpHeaders();
        for (Map.Entry<?, ?> entry : headerTable.entrySet()) {
            var value = entry.getValue();
            if (entry.getKey() instanceof String && value instanceof String) {
                try {
                    headers.add((String) entry.getKey(), value);
                } catch (IllegalArgumentException e) {
                    throw new LuaException(e.getMessage());
                }
            }
        }

        if (!headers.contains(HttpHeaderNames.USER_AGENT)) {
            headers.set(HttpHeaderNames.USER_AGENT, apiEnvironment.getGlobalEnvironment().getUserAgent());
        }
        return headers;
    }
}
