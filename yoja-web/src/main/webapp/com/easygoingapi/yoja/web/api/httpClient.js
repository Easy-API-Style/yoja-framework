/*
 * Copyright 2026 easy api <easy.api.contact@gmail.com>
 * https://easygoingapi.com
 * https://github.com/Easy-API-Style/yoja-framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';
const urlParameterUtil = await import(yojaWeb.path('../util/urlParameterUtil.js'));
const javascriptUtil = await import(yojaWeb.path('../util/javascriptUtil.js'));
const {Listeners} = await import(yojaWeb.path('../util/listenerUtil.js'));

const LOG = false;

/**
 * @return an Error -- its stack, its message told -- its cause the error met; {@code error} and {@code request}
 *         kept on it, as the rejections held them
 */
function httpError(message, error, request) {
    const result = new Error(message, {cause: error});
    result.error = error;
    result.request = request;
    return result;
}

class HttpClient {

    #fetchModes = ['json', 'text', 'blob', 'base64', 'arrayBuffer'];

    #jsonContentTypes = ['application/json', 'application/array-json'];
    #textContentTypes = ['text/plain', 'text/html', 'text/css',
                         'application/xml', 'application/javascript'];
    #blobContentTypes = ['application/blob'];
    #base64ContentTypes = ['application/base64'];

    // the application's actions: each onXXX(action) gives back its off (listenerUtil)
    #onRequestActions = new Listeners('httpClient request');
    #onResponseActions = new Listeners('httpClient response');
    #onFetchActions = new Listeners('httpClient fetch');

    #onOnlineActions = new Listeners('httpClient online');
    #onOfflineActions = new Listeners('httpClient offline');

    constructor() {
        this.addContentTypes(yojaWeb.config);
        window.addEventListener('online', event => this.#onOnlineActions.run(event));
        window.addEventListener('offline', event => this.#onOfflineActions.run(event));
    }

    addContentTypes(contentTypes) {
        if (contentTypes instanceof Object) {
            this.#jsonContentTypes.push(...(contentTypes.jsonContentTypes || []));
            this.#textContentTypes.push(...(contentTypes.textContentTypes || []));
            this.#blobContentTypes.push(...(contentTypes.blobContentTypes || []));
            this.#base64ContentTypes.push(...(contentTypes.base64ContentTypes || []));
        }
    }

    onRequest(action) {
        return this.#onRequestActions.add(action);
    }

    onResponse(action) {
        return this.#onResponseActions.add(action);
    }

    onFetch(action) {
        return this.#onFetchActions.add(action);
    }

    isOnline() {
        return navigator.onLine;
    }

    onOnline(action) {
        return this.#onOnlineActions.add(action);
    }

    onOffline(action) {
        return this.#onOfflineActions.add(action);
    }

    get(request, callback) {
        javascriptUtil.requireValue(request);
        const _request = this.#toObjectRequest(request);
        _request['method'] = 'GET';
        this.#applyOnRequest(_request);
        return this.#response(_request, callback);
    }

    /*
     * The body's content type, when the request gives none -- from what the body is:
     *
     *   String, Number, Boolean, Date -> text/plain
     *   Array -> application/array-json
     *   Object -> application/json
     *   Blob -> application/blob (sent as base64)
     *   none (null, undefined) -> no body
     */
    async post(request, body, callback) {
        javascriptUtil.requireValue(request);
        const _request = this.#toObjectRequest(request);
        _request['method'] = 'POST';
        if (_request['contentType']) {
            this.#setHeader(_request, 'content-type', _request['contentType']);
        }
        if (body !== undefined && body !== null) {
            _request['body'] = body instanceof Blob ? await javascriptUtil.blobTobase64(body)
                                                    : javascriptUtil.stringify(body);
            if (!this.#hasHeader(_request, 'content-type')) {
                this.#setHeader(_request, 'content-type', this.#contentTypeOf(body));
            }
        }
        this.#applyOnRequest(_request);
        return this.#response(_request, callback);
    }

    /** @return the content type of a body, from what it is -- not from what its text looks like */
    #contentTypeOf(body) {
        if (body instanceof Blob) {
            return 'application/blob';
        }
        if (Array.isArray(body)) {
            return 'application/array-json';
        }
        if (typeof body === 'string' || typeof body === 'number' || typeof body === 'boolean'
              || body instanceof String || body instanceof Number || body instanceof Boolean || body instanceof Date) {
            return 'text/plain';
        }
        return 'application/json';
    }

    load(request, callback) {
        javascriptUtil.requireValue(request);
        const _request = this.#toObjectRequest(request);
        _request.fetchAs = 'text';
        _request['method'] = 'GET';
        this.#applyOnRequest(_request);
        return this.#response(_request, callback);
    }

    fetch(request, callback) {
        javascriptUtil.requireValue(request);
        const _request = this.#toObjectRequest(request);
        this.#applyOnRequest(_request);
        return this.#doFetch(_request)
                   .then(response => this.#log(_request, response))
                   .then(response => {
                        if (callback) {
                            callback(response);
                        }
                        return response;
                    })
                    .catch(error => {
                        this.#error(_request.url, error);
                        throw httpError('fetch url failed: ' + _request.url, error, _request);
                    });
    }

    #doFetch(request) {
        const url = urlParameterUtil.toUrl(request.url, request.parameters);
        return window.fetch(url, request)
                     .then(response => this.#applyOnFetch(request, response));
    }

    /** @return the request as an object -- its own, the one given left as it is */
    #toObjectRequest(request) {
        return typeof request === 'object' ? {...request} : {url: request};
    }

    /**
     * @return the key a header has in headers given as an object, whatever its case ('Content-Type' is
     *         'content-type'), or null
     */
    #headerKey(headers, name) {
        const lower = name.toLowerCase();
        return Object.keys(headers).find(key => key.toLowerCase() === lower) ?? null;
    }

    /** Sets a header -- headers given as an object: the key it has, whatever its case, replaced (not one more). */
    #setHeader(request, name, value) {
        if (!request.headers) {
            request.headers = new Headers();
        }
        const headers = request.headers;
        if (headers instanceof Headers) {
            headers.set(name, value);
        }
        else if (typeof headers === 'object') {
            headers[this.#headerKey(headers, name) ?? name] = value;
        }
    }

    #getHeader(request, name) {
        const headers = request.headers;
        if (headers instanceof Headers) {
            return headers.get(name);
        }
        if (headers && typeof headers === 'object') {
            const key = this.#headerKey(headers, name);
            return key === null ? null : headers[key];
        }
        return null;
    }

    #hasHeader(request, name) {
        return this.#getHeader(request, name) != null;
    }

    /**
     * @return how the body is read: as the request asks; else from the response's content type -- the ones listed
     *         (addContentTypes), then any json (+json), any text (text/*, +xml); else an array buffer
     */
    #getFetchMode(request, response) {
        const fetchAs = request['fetchAs'];
        if (this.#fetchModes.includes(fetchAs)) {
            return fetchAs;
        }
        const contentType = response.headers.get('Content-Type');
        const mime = contentType ? contentType.split(';')[0].trim().toLowerCase() : '';
        const listed = contentTypes => contentTypes.some(listedType => listedType.trim().toLowerCase() === mime);
        if (!mime) {
            return 'arrayBuffer';
        }
        if (listed(this.#jsonContentTypes)) {
            return 'json';
        }
        if (listed(this.#textContentTypes)) {
            return 'text';
        }
        if (listed(this.#blobContentTypes)) {
            return 'blob';
        }
        if (listed(this.#base64ContentTypes)) {
            return 'base64';
        }
        if (mime.endsWith('+json')) {
            return 'json';
        }
        if (mime.startsWith('text/') || mime.endsWith('+xml') || mime === 'text/xml') {
            return 'text';
        }
        return 'arrayBuffer';
    }

    #response(request, callback) {
        let result;
        if (request.responseWith) {
            result = this.#responseWith(request, callback);
        }
        else if (this.isOnline()) {
            result = this.#responseOnline(request, callback);
        }
        else {
            result = this.#responseOffline(request, callback);
        }
        return result.then(response => this.#log(request, response));
    }

    /** @return a promise of the response given -- its callback failing, rejected */
    #answer(request, response, callback) {
        this.#applyOnResponse(request, response);
        if (callback) {
            callback(response);
        }
        return response;
    }

    #responseWith(request, callback) {
        return Promise.resolve()
                      .then(() => this.#answer(request, request.responseWith, callback))
                      .catch(error => {
                          throw httpError('http response failed: ' + request.url, error, request);
                      });
    }

    #responseOffline(request, callback) {
        return Promise.resolve()
                      .then(() => {
                          const response = {};
                          response.headers = new Headers();
                          response.bodyUsed = true;
                          response.status = 470;
                          return this.#answer(request, response, callback);
                      })
                      .catch(error => {
                          throw httpError('http response failed (offline): ' + request.url, error, request);
                      });
    }

    /**
     * @return a promise of the response read -- rejected, not left pending, when it cannot be: a json body not one
     *         (an empty one is null), its callback failing
     */
    #responseOnline(request, callback) {
        let contentType = null;
        return this.#doFetch(request)
                   .then(response => {
                       const fetchMode = this.#getFetchMode(request, response);
                       let body;
                       if (fetchMode === 'json') {
                           body = response.text().then(text => text.trim() ? JSON.parse(text) : null);
                       }
                       else if (fetchMode === 'text') {
                           body = response.text();
                       }
                       else if (fetchMode === 'base64') {
                           body = response.text()
                                          .then(text => {
                                              const blob = javascriptUtil.base64ToBlob(text);
                                              if (blob.type) {
                                                  contentType = blob.type;
                                              }
                                              return blob.arrayBuffer();
                                          });
                       }
                       else if (fetchMode === 'blob') {
                           body = response.text()
                                          .then(text => javascriptUtil.base64ToBlob(text));
                       }
                       else {
                           if (!response.headers || !response.headers.has('content-type')) {
                               contentType = 'application/octet-stream';
                           }
                           body = response.arrayBuffer();
                       }
                       return body.then(value => this.#toResult(response, value, contentType));
                   })
                   .then(result => this.#answer(request, result, callback))
                   .catch(error => {
                       throw httpError('http request failed: ' + request.method + ' ' + request.url, error, request);
                   });
    }

    #applyOnRequest(request) {
        this.#onRequestActions.run(request);
    }

    #applyOnResponse(request, response) {
        this.#onResponseActions.run(request, response);
        return response;
    }

    #applyOnFetch(request, response) {
        this.#onFetchActions.run(request, response);
        return response;
    }

    #toResult(response, body, contentType) {
        let headers = response.headers;
        if (contentType) {
            headers = response.headers ? new Headers(response.headers) : new Headers();
            headers.set('content-type', contentType);
        }
        return { status: response.status,
                 headers: headers,
                 bodyUsed: true,
                 body: body };
    }

    #log(request, response) {
        if (LOG) {
            let message = request.method + ' ' + request.url + ' status:' + response.status;
            if (response.headers && response.headers.has('Content-Type')) {
                message = message + ' contentType: ' + response.headers.get('Content-Type');
            }
            if (request['fetchAs']) {
                message = message + ' fetchAs:' + request['fetchAs'];
            }
            console.info(message);
        }
        return response;
    }

    #error(url, error) {
        console.error('error fetch: ', url, error);
    }

}

const httpClient = new HttpClient();

export function addContentTypes(contentTypes) {
    httpClient.addContentTypes(contentTypes);
}

export function load(request, callback) {
   return httpClient.load(request, callback);
}

export function get(request, callback) {
   return httpClient.get(request, callback);
}

export function post(request, body, callback) {
   return httpClient.post(request, body, callback);
}

export function fetch(request, callback) {
   return httpClient.fetch(request, callback);
}

/** @return its off */
export function onRequest(callback) {
    return httpClient.onRequest(callback);
}

/** @return its off */
export function onResponse(callback) {
    return httpClient.onResponse(callback);
}

export function isOffline() {
    return !httpClient.isOnline();
}

/** @return its off */
export function onOnline(callback) {
    return httpClient.onOnline(callback);
}

/** @return its off */
export function onOffline(callback) {
    return httpClient.onOffline(callback);
}
