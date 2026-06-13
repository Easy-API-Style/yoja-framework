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
'use strict'
const urlParameterUtil = await import(yojaWeb.path('../util/urlParameterUtil.js'));
const javascriptUtil = await import(yojaWeb.path('../util/javascriptUtil.js'));

const LOG = false;

class HttpClient {
    
    #fetchModes = ['json', 'text', 'blob', 'base64', 'arrayBuffer'];
    
    #jsonContentTypes = ['application/json', 'application/array-json'];
    #textContentTypes = ['text/plain', 'text/html', 'text/css', 
                         'application/xml', 'application/javascript'];
    #blobContentTypes = ['application/blob'];
    #base64ContentTypes = ['application/base64'];
    
    #host = window.location.hostname;
    #port = window.location.port;
    #protocol = window.location.protocol;
    
    #onRequestActions = [];
    #onResponseActions = [];
    #onFetchActions = [];
    
    #onOnlineActions = []
    #onOfflineActions = []
   
    constructor() {
        this.addContentTypes(yojaWeb.config);
        window.addEventListener('online', event => {
            for (const action of this.#onOnlineActions) {
                action(event);
            }
        });
        window.addEventListener('offline', event => {
            for (const action of this.#onOfflineActions) {
                action(event);
            }
        });
    }
    
    addContentTypes(contentTypes) {
        if (contentTypes instanceof Object) {
            if (contentTypes.jsonContentTypes) {
                for (const contentType of contentTypes.jsonContentTypes) {
                    this.#jsonContentTypes.push(contentType);
                }
            }
            if (contentTypes.textContentTypes) {
                for (const contentType of contentTypes.textContentTypes) {
                    this.#textContentTypes.push(contentType);
                }
            }
            if (contentTypes.blobContentTypes) {
                for (const contentType of contentTypes.blobContentTypes) {
                    this.#blobContentTypes.push(contentType);
                }
            }
            if (contentTypes.base64ContentTypes) {
                for (const contentType of contentTypes.base64ContentTypes) {
                    this.#base64ContentTypes.push(contentType);
                }
            }
        }
    }
        
    onRequest(action) {
        this.#onRequestActions.push(action);
    }
    
    onResponse(action) {
        this.#onResponseActions.push(action);
    }
    
    onFetch(action) {
        this.#onFetchActions.push(action);
    }
    
    isOnline() {
        return navigator.onLine;
    }
    
    onOnline(action) {
        this.#onOnlineActions.push(action);
    }
    
    onOffline(action) {
        this.#onOfflineActions.push(action);
    }
    
    get(request, callback) {
        javascriptUtil.requireValue(request);
        const _request = this.#toObjectRequest(request);
        _request['method'] = 'GET';
        this.#applyOnResquest(_request);
        return this.#response(_request, callback);
    }
    
    /*
     *
     *   String, Number, Date -> text/plain
     *   Array -> application/array-json
     *   Object -> application/json
     *   Blob -> application/blob
     *       
     */
    async post(request, body, callback) {
        javascriptUtil.requireValue(request);
        javascriptUtil.requireValue(body);
        const _request = this.#toObjectRequest(request);
        _request['method'] = 'POST';
        
        // Content-Type
        if (_request['contentType']) {
            const contentType = _request['contentType'];
            this.#setHeader(_request, 'content-type', contentType);
        }
        if (!this.#hasHeader(_request, 'content-type')) {
           if (body instanceof Blob) {
               _request['body'] = await javascriptUtil.blobTobase64(body);
               this.#setHeader(_request, 'content-type', 'application/blob');
           }
           else {
                _request['body'] = javascriptUtil.stringify(body);
                let contentType = 'text/plain';
                if (javascriptUtil.isStringArray( _request['body'])) {
                    contentType = 'application/array-json';
                }
                else if (javascriptUtil.isStringJson( _request['body'])) {
                    contentType = 'application/json';
                }
                this.#setHeader(_request, 'content-type', contentType);
           }
        }
        else {
            if (body instanceof Blob) {
                _request['body'] = await javascriptUtil.blobTobase64(body);
            }
            else {
                _request['body'] = javascriptUtil.stringify(body);
            }
        }
        this.#applyOnResquest(_request);
        return this.#response(_request, callback);
    }
    
    load(request, callback) {
        javascriptUtil.requireValue(request);
        const _request = this.#toObjectRequest(request);
        _request.fetchAs = 'text';
        _request['method'] = 'GET';
        this.#applyOnResquest(_request);
        return this.#response(_request, callback);
    }
    
    async fetch(request, callback) {
        javascriptUtil.requireValue(request);
        const _request = this.#toObjectRequest(request);
        this.#applyOnResquest(_request);
        return this.#doFetch(_request)
                   .then(response => this.#log(_request, response))
                   .then(response => {
                        if (callback) {
                            callback(response);
                        }
                        return response;
                    })
                    .catch(error => {
                        this.#error(request.url, error);
                        throw new Error('fetch url failed: ' + request.url, {cause: error});
                    })
    }
    
    async #doFetch(request) {
        const url = urlParameterUtil.toUrl(request.url, request.parameters);
        return window.fetch(url, request)
                     .then(response => this.#applyOnFetch(request, response));
    }
    
    #setHeader(request, name, value) {
        if (request.headers) {
            const headers = request.headers;
            if (headers instanceof Headers) {
                headers.set(name, value);
            }
            else if (typeof headers === 'object') {
                headers[name] = value;
            }
        }
        else {
            request.headers = new Headers();
            request.headers.set(name, value);
        }
    }
    
    #getHeader(request, name) {
       let result = null;
       if (request.headers) {
           const headers = request.headers;
           if (headers instanceof Headers) {
               result = headers.get(name);
           }
           else if (typeof headers === 'object') {
               if (typeof headers[name] !== 'undefined') {
                    result = headers[name];
               }
           }
       }
       return result;
    }
    
    #hasHeader(request, name) {
        return this.#getHeader(request, name) != null;
    }
    
    #toObjectRequest(request) {
        let result;
        if (typeof request === 'object') {
            result = {...request};
        }
        else {
            result = {};
            result['url'] = request;
        }
        return result;
    }
    
    #getFetchMode(request, response) {
        let result;
        const fetchAs = request['fetchAs'];
        if (this.#fetchModes.includes(fetchAs)) {
            result = fetchAs;
        }
        else {
            const contentType = response.headers.get('Content-Type');
            if (this.#containsContentType(this.#jsonContentTypes, contentType)) {
                result = 'json';
            }
            else if (this.#containsContentType(this.#textContentTypes, contentType)) {
                result = 'text';
            }
            else if (this.#containsContentType(this.#blobContentTypes, contentType)) {
                result = 'blob';
            }
            else if (this.#containsContentType(this.#base64ContentTypes, contentType)) {
                result = 'base64';
            }
            else {
                result = 'arrayBuffer';
            }
        }
        return result;
    }
    
    #containsContentType(contentTypes, contentType) {
        let result = false;
        if (contentType) {
            const values = [];
            if (contentType.includes(';')) {
                const splitContentTypes = contentType.split(';');
                for (const splitContentType of splitContentTypes) {
                    values.push(splitContentType.trim().toLowerCase());
                }
            }
            else {
                values.push(contentType.trim().toLowerCase());
            }
            for (const value of values) {
                for (const contentTypeValue of contentTypes) {
                    if (contentTypeValue.trim().toLowerCase() == value) {
                        result = true;
                        break;
                    }
                }
                if (result) {
                    break;
                }
            }
        }
        return result;
    }
    
    async #response(request, callback) {
        let result;
        if (request.responseWith) {
            return this.#responseWith(request, callback)
                       .then(response => this.#log(request, response));
        }
        else if (this.isOnline()) {
            result = this.#responseOnline(request, callback)
                         .then(response => this.#log(request, response));
        }
        else {
            result = this.#responseOffline(request, callback)
                         .then(response => this.#log(request, response));
        }
        return result;
    }
    
    #responseWith(request, callback) {
        return new Promise((resolve, reject) => {
            try {
                const response = request.responseWith;
                this.#applyOnResponse(request, response);
                if (callback) {
                    callback(response);
                }
                resolve(response);
            }
            catch(error) {
                reject({error: error, 
                        request: request});
            }
        })
    }
    
    #responseOffline(request, callback) {
        return new Promise((resolve, reject) => {
            try {
                const response = {};
                response.headers = new Headers();
                response.bodyUsed = true;
                response.status = 470;
                this.#applyOnResponse(request, response);
                if (callback) {
                    callback(response);
                }
                resolve(response);
            }
            catch(error) {
                reject({error: error, 
                        request: request});
            }
        })
    }
    
    #responseOnline(request, callback) {
        return new Promise((resolve, reject) => {
            this.#doFetch(request)
                .then(response => {
                    if (response.ok) {
                        let contentType = null;
                        const fetchMode = this.#getFetchMode(request, response);
                        let responseFuture;
                        if (fetchMode == 'json') {
                            responseFuture = response.json();
                        }
                        else if (fetchMode == 'text') {
                            responseFuture = response.text();
                        }
                        else if (fetchMode == 'base64') {
                            responseFuture = response.text()
                                                     .then(v => { 
                                                        const blob = javascriptUtil.base64ToBlob(v);
                                                        if (blob.type) {
                                                            contentType = blob.type;
                                                        }
                                                        return blob.arrayBuffer();
                                                     }) 
                        }
                        else if (fetchMode == 'blob') {
                            responseFuture = response.text()
                                                     .then(v => javascriptUtil.base64ToBlob(v));
                        }
                        else if (fetchMode == 'arrayBuffer') {
                            if (!response.headers
                                   || !response.headers.has('content-type')) {
                                contentType = 'application/octet-stream';
                            }
                            responseFuture = response.arrayBuffer();
                        }
                        responseFuture.then(body => {
                            const result = this.#toResult(response, body, contentType);
                            this.#applyOnResponse(request, result);
                            if (callback) {
                                callback(result);
                            }
                            resolve(result);
                        })
                    }
                    else {
                        this.#fail(response);
                        const result = this.#toResult(response);
                        this.#applyOnResponse(request, result);
                        if (callback) {
                            callback(result);
                        }
                        resolve(result);
                    }
                })
                .catch(error => {
                    reject({error: error, 
                            request: request});
                })
        })
    }
    
    #applyOnResquest(request) {
        for (const action of this.#onRequestActions) {
            action(request);
        }
    }
    
    #applyOnResponse(request, response) {
        for (const action of this.#onResponseActions) {
            action(request, response);
        }
        return response
    }
    
    #applyOnFetch(request, response) {
        for (const action of this.#onFetchActions) {
            action(request, response);
        }
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
    
    #fail(response) {
        console.debug('not fetch: ', response?.url, response?.status);
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

export function onRequest(callback) {
    httpClient.onRequest(callback);
}

export function onResponse(callback) {
    httpClient.onResponse(callback);
}

//export function onFetch(callback) {
//    httpClient.onFetch(callback)
//}

export function isOffline() {
    return !httpClient.isOnline();
}

export function onOnline(callback) {
    httpClient.onOnline(callback);
}

export function onOffline(callback) {
    httpClient.onOffline(callback);
}
