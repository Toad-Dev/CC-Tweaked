-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Make HTTP requests, sending and receiving data to a remote web server.

@module http
@since 1.1
@see local_ips To allow accessing servers running on your local network.
]]

local expect = dofile("rom/modules/main/cc/expect.lua").expect

local native = http
local nativeHTTPRequest = http.request

local methods = {
    GET = true, POST = true, HEAD = true,
    OPTIONS = true, PUT = true, DELETE = true,
    PATCH = true, TRACE = true,
}

local function checkKey(options, key, ty, opt)
    local value = options[key]
    local valueTy = type(value)

    if (value ~= nil or not opt) and valueTy ~= ty then
        error(("bad field '%s' (%s expected, got %s"):format(key, ty, valueTy), 4)
    end
end

local function checkOptions(options, body)
    checkKey(options, "url", "string")
    if body == false then
        checkKey(options, "body", "nil")
    else
        checkKey(options, "body", "string", not body)
    end
    checkKey(options, "headers", "table", true)
    checkKey(options, "method", "string", true)
    checkKey(options, "redirect", "boolean", true)

    if options.method and not methods[options.method] then
        error("Unsupported HTTP method", 3)
    end
end

local function wrapRequest(_url, ...)
    local ok, err = nativeHTTPRequest(...)
    if ok then
        while true do
            local event, param1, param2, param3 = os.pullEvent()
            if event == "http_success" and param1 == _url then
                return param2
            elseif event == "http_failure" and param1 == _url then
                return nil, param2, param3
            end
        end
    end
    return nil, err
end

--[[- Make a HTTP GET request to the given url.

@tparam string url   The url to request
@tparam[opt] { [string] = string } headers Additional headers to send as part
of this request.
@tparam[opt] boolean binary Whether to make a binary HTTP request. If true,
the body will not be UTF-8 encoded, and the received response will not be
decoded.

@tparam[2] {
  url = string, headers? = { [string] = string },
  binary? = boolean, method? = string, redirect? = boolean,
} request Options for the request. See @{http.request} for details on how
these options behave.

@treturn Response The resulting http response, which can be read from.
@treturn[2] nil When the http request failed, such as in the event of a 404
error or connection timeout.
@treturn string A message detailing why the request failed.
@treturn Response|nil The failing http response, if available.

@changed 1.63 Added argument for headers.
@changed 1.80pr1 Response handles are now returned on error if available.
@changed 1.80pr1 Added argument for binary handles.
@changed 1.80pr1.6 Added support for table argument.
@changed 1.86.0 Added PATCH and TRACE methods.

@usage Make a request to [example.tweaked.cc](https://example.tweaked.cc),
and print the returned page.

```lua
local request = http.get("https://example.tweaked.cc")
print(request.readAll())
-- => HTTP is working!
request.close()
```
]]
function get(_url, _headers, _binary)
    if type(_url) == "table" then
        checkOptions(_url, false)
        return wrapRequest(_url.url, _url)
    end

    expect(1, _url, "string")
    expect(2, _headers, "table", "nil")
    expect(3, _binary, "boolean", "nil")
    return wrapRequest(_url, _url, nil, _headers, _binary)
end

--[[- Make a HTTP POST request to the given url.

@tparam string url   The url to request
@tparam string body  The body of the POST request.
@tparam[opt] { [string] = string } headers Additional headers to send as part
of this request.
@tparam[opt] boolean binary Whether to make a binary HTTP request. If true,
the body will not be UTF-8 encoded, and the received response will not be
decoded.

@tparam[2] {
  url = string, body? = string, headers? = { [string] = string },
  binary? = boolean, method? = string, redirect? = boolean,
} request Options for the request. See @{http.request} for details on how
these options behave.

@treturn Response The resulting http response, which can be read from.
@treturn[2] nil When the http request failed, such as in the event of a 404
error or connection timeout.
@treturn string A message detailing why the request failed.
@treturn Response|nil The failing http response, if available.

@since 1.31
@changed 1.63 Added argument for headers.
@changed 1.80pr1 Response handles are now returned on error if available.
@changed 1.80pr1 Added argument for binary handles.
@changed 1.80pr1.6 Added support for table argument.
@changed 1.86.0 Added PATCH and TRACE methods.
]]
function post(_url, _post, _headers, _binary)
    if type(_url) == "table" then
        checkOptions(_url, true)
        return wrapRequest(_url.url, _url)
    end

    expect(1, _url, "string")
    expect(2, _post, "string")
    expect(3, _headers, "table", "nil")
    expect(4, _binary, "boolean", "nil")
    return wrapRequest(_url, _url, _post, _headers, _binary)
end

--[[- Asynchronously make a HTTP request to the given url.

This returns immediately, a @{http_success} or @{http_failure} will be queued
once the request has completed.

@tparam      string url   The url to request
@tparam[opt] string body  An optional string containing the body of the
request. If specified, a `POST` request will be made instead.
@tparam[opt] { [string] = string } headers Additional headers to send as part
of this request.
@tparam[opt] boolean binary Whether to make a binary HTTP request. If true,
the body will not be UTF-8 encoded, and the received response will not be
decoded.

@tparam[2] {
  url = string, body? = string, headers? = { [string] = string },
  binary? = boolean, method? = string, redirect? = boolean,
} request Options for the request.

This table form is an expanded version of the previous syntax. All arguments
from above are passed in as fields instead (for instance,
`http.request("https://example.com")` becomes `http.request { url =
"https://example.com" }`).
 This table also accepts several additional options:

 - `method`: Which HTTP method to use, for instance `"PATCH"` or `"DELETE"`.
 - `redirect`: Whether to follow HTTP redirects. Defaults to true.

@see http.get  For a synchronous way to make GET requests.
@see http.post For a synchronous way to make POST requests.

@changed 1.63 Added argument for headers.
@changed 1.80pr1 Added argument for binary handles.
@changed 1.80pr1.6 Added support for table argument.
@changed 1.86.0 Added PATCH and TRACE methods.
]]
function request(_url, _post, _headers, _binary)
    local url
    if type(_url) == "table" then
        checkOptions(_url)
        url = _url.url
    else
        expect(1, _url, "string")
        expect(2, _post, "string", "nil")
        expect(3, _headers, "table", "nil")
        expect(4, _binary, "boolean", "nil")
        url = _url
    end

    local ok, err = nativeHTTPRequest(_url, _post, _headers, _binary)
    if not ok then
        os.queueEvent("http_failure", url, err)
    end

    -- Return true/false for legacy reasons. Undocumented, as it shouldn't be relied on.
    return ok, err
end

local nativeCheckURL = native.checkURL

--[[- Asynchronously determine whether a URL can be requested.

If this returns `true`, one should also listen for @{http_check} which will
container further information about whether the URL is allowed or not.

@tparam string url The URL to check.
@treturn true When this url is not invalid. This does not imply that it is
allowed - see the comment above.
@treturn[2] false When this url is invalid.
@treturn string A reason why this URL is not valid (for instance, if it is
malformed, or blocked).

@see http.checkURL For a synchronous version.
]]
checkURLAsync = nativeCheckURL

--[[- Determine whether a URL can be requested.

If this returns `true`, one should also listen for @{http_check} which will
container further information about whether the URL is allowed or not.

@tparam string url The URL to check.
@treturn true When this url is valid and can be requested via @{http.request}.
@treturn[2] false When this url is invalid.
@treturn string A reason why this URL is not valid (for instance, if it is
malformed, or blocked).

@see http.checkURLAsync For an asynchronous version.

@usage
```lua
print(http.checkURL("https://example.tweaked.cc/"))
-- => true
print(http.checkURL("http://localhost/"))
-- => false Domain not permitted
print(http.checkURL("not a url"))
-- => false URL malformed
```
]]
function checkURL(_url)
    expect(1, _url, "string")
    local ok, err = nativeCheckURL(_url)
    if not ok then return ok, err end

    while true do
        local _, url, ok, err = os.pullEvent("http_check")
        if url == _url then return ok, err end
    end
end

local nativeWebsocket = native.websocket


--[[- Asynchronously open a websocket.

This returns immediately, a @{websocket_success} or @{websocket_failure}
will be queued once the request has completed.

@tparam string url The websocket url to connect to. This should have the
`ws://` or `wss://` protocol.
@tparam[opt] { [string] = string } headers Additional headers to send as part
of the initial websocket connection.
@since 1.80pr1.3
@changed 1.95.3 Added User-Agent to default headers.
]]
function websocketAsync(url, headers)
    expect(1, url, "string")
    expect(2, headers, "table", "nil")

    local ok, err = nativeWebsocket(url, headers)
    if not ok then
        os.queueEvent("websocket_failure", url, err)
    end

    -- Return true/false for legacy reasons. Undocumented, as it shouldn't be relied on.
    return ok, err
end

--[[- Open a websocket.

@tparam string url The websocket url to connect to. This should have the
`ws://` or `wss://` protocol.
@tparam[opt] { [string] = string } headers Additional headers to send as part
of the initial websocket connection.

@treturn Websocket The websocket connection.
@treturn[2] false If the websocket connection failed.
@treturn string An error message describing why the connection failed.
@since 1.80pr1.1
@changed 1.80pr1.3 No longer asynchronous.
@changed 1.95.3 Added User-Agent to default headers.
]]
function websocket(_url, _headers)
    expect(1, _url, "string")
    expect(2, _headers, "table", "nil")

    local ok, err = nativeWebsocket(_url, _headers)
    if not ok then return ok, err end

    while true do
        local event, url, param = os.pullEvent( )
        if event == "websocket_success" and url == _url then
            return param
        elseif event == "websocket_failure" and url == _url then
            return false, param
        end
    end
end
