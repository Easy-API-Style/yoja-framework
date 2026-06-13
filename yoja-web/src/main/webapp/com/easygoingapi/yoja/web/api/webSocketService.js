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
const urlParameterService = await import(yojaWeb.path('../util/urlParameterUtil.js'));

class WebSocketWrapper {
    
    #timeout = 5000;
    #webSocket;
    #request;
    #eventTypes = ['open', 'close', 'message', 'error'];
    #callBackActions = {close: [], 
                        open: [], 
                        message: [],
                        error: []};
    
    constructor(request) {
        this.#request = this.#toUrl(request);
        this.#webSocket = new WebSocket(this.#request);
    }
    
    #toUrl(request) {
        const url = urlParameterService.toUrl(request['url'], request['parameters']);
        return request['protocol'] + '://' + request['host'] + ':' + request['port'] + url;
    }
    
    async open() {
        return new Promise((resolve, reject) => {
            if (this.#webSocket.readyState === WebSocket.OPEN) {
                resolve(this.#webSocket);
            }   
            else if (this.#webSocket.readyState === WebSocket.CONNECTING) {
                const timer = setTimeout(() => reject(new Error('openning websocket failed ' + this.#request + ', timeout: ' + this.#timeout + 'ms')), 
                                         this.#timeout);
                this.#webSocket.addEventListener('open', 
                                                 () => { 
                                                     clearTimeout(timer); 
                                                     resolve(this.#webSocket); 
                                                 }, 
                                                 { once: true });
                this.#webSocket.addEventListener('error', 
                                                 event => { 
                                                    clearTimeout(timer); 
                                                    reject(new Error('openning websocket failed ' + this.#request, {cause: event})); 
                                                  }, 
                                                  { once: true })  
            }     
            else {  
                this.#webSocket = new WebSocket(this.#request);
                for (const eventType of this.#eventTypes) {
                    for (const callbackAction of this.#callBackActions[eventType]) {
                        this.#webSocket.addEventListener(eventType, handler => callbackAction(handler));
                    }
                }
                const timer = setTimeout(() => reject(new Error('openning websocket failed ' + this.#request + ', timeout: ' + this.#timeout + 'ms')), 
                                         this.#timeout);
                this.#webSocket.addEventListener("open", 
                                                 () => {
                                                    clearTimeout(timer); 
                                                    resolve(this.#webSocket);
                                                 },
                                                 { once: true });
                this.#webSocket.addEventListener("error",
                                                 event => {
                                                    clearTimeout(timer); 
                                                    reject(new Error('openning websocket failed ' + this.#request, {cause: event}))
                                                  },
                                                  { once: true });
                
            }
        });
    }
    
    async send(data) {
       return this.open()
                  .then(ws => ws.send(data));
    }
    
    async close(code, reason) {
        return new Promise((resolve, reject) => {
            if (this.#webSocket.readyState === WebSocket.CLOSED
                   || this.#webSocket.readyState === WebSocket.CLOSING) {
                resolve();
            }
            else {
                const timer = setTimeout(() => reject(new Error('closing websocket failed ' + this.#request + ', timeout: ' + this.#timeout + 'ms')), 
                                         this.#timeout);
                this.#webSocket.addEventListener("close", 
                                                 () => {
                                                    clearTimeout(timer); 
                                                    resolve();
                                                 },
                                                 { once: true });
                this.#webSocket.addEventListener("error", 
                                                 event => {
                                                    clearTimeout(timer); 
                                                    reject(new Error('closing websocket failed ' + this.#request, {cause: event}));
                                                 },
                                                 { once: true });
                this.#webSocket.close(code, reason);
            }
        })
    }
    
    get timeout() {
        return this.#timeout;
    }
    
    set timeout(timeout) {
        this.#timeout = timeout;
    }
    
    isOpen() {
        return this.#webSocket.readyState === WebSocket.OPEN;
    }
    
    // state [ CONNECTING | OPEN | CLOSING | CLOSED ]
    is(state) {
        return this.#webSocket.readyState === WebSocket[state];
    }
    
    // state [ CONNECTING == 0 | OPEN == 1 | CLOSING == 2 | CLOSED == 3 ]
    state() {
        return this.#webSocket.readyState;
    }

    onOpen(callback) {
        this.on('open', callback);
    }
    
    onMessage(callback) {
        this.on('message', callback);
    }
    
    onClose(callback) {
        this.on('close', callback);
    }
    
    onError(callback) {
        this.on('error', callback);
    }
    
    // eventType [ open, close, message, error ]
    on(eventType, callback) {
        this.#webSocket.addEventListener(eventType, handler => callback(handler));
        this.#callBackActions[eventType].push(callback);
    }

}

class WebSocketService {
    
    #host = window.location.hostname;
    #port = window.location.port;
    #protocol = window.location.protocol;
    #protocolWebSocket;
    
    #webSockets = {};
    
    constructor() {
        this.#protocolWebSocket = this.#protocol === 'https:' ? 'wss' : 'ws';
    }
    
    webSocket(request) {
        const _request = this.#toObjectRequest(request);
        const url = _request.url;
        let webSocket = this.#webSockets[url];
//        if (!webSocket 
//              || webSocket.is('CLOSING')
//              || webSocket.is('CLOSED')) {
        if (!webSocket) {
            webSocket = new WebSocketWrapper(_request);
            this.#webSockets[url] = webSocket;
        }
        return webSocket;
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
        result['protocol'] = this.#protocolWebSocket;
        result['host'] = this.#host;
        result['port'] = this.#port;
        return result;
    }
    
}

const webSocketService = new WebSocketService();

export function webSocket(request) {
    return webSocketService.webSocket(request);
}

export function send(request, data) {
    return webSocket(request).send(data);
}

export function close(request, code, reason) {
    return webSocket(request).close(code, reason);
}

export function onOpen(request, callback) {
    webSocket(request).onOpen(callback);
}

export function onMessage(request, callback) {
    webSocket(request).onMessage(callback);
}

export function onClose(request, callback) {
    webSocket(request).onClose(callback);
}

export function onError(request, callback) {
    webSocket(request).onError(callback);
}

// eventType [ open | message | close | error ]
export function on(eventType, request, callback) {
    webSocket(request).on(eventType, callback);
}
