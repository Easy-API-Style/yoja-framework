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

/*
 * One web socket per url of the application -- its own host and port, the url as is (its parameters, if any,
 * written in it): the url is its key.
 *
 * A socket is listened to in two ways:
 *  - a list: on(eventType, callback) -- only added to, for the listeners set once (a module, a service);
 *  - a map: put(eventType, key, callback), remove(eventType, key), has(eventType, key) -- a key the application
 *    chooses (a screen's name): put again under the same key, a listener replaces the one it had, so that a
 *    screen built again does not add one more.
 * Each event is told to the list, then to the map, in the order they were added; a listener failing does not
 * keep the others from being told.
 *
 * A socket closed -- by close(), or by the server, the network, a timeout -- is over: its listeners are dropped,
 * and webSocket(url) gives a new one, from zero.
 *
 * autoRestart(url) restarts a socket closed otherwise than by close(): from zero, its map's listeners put on the
 * new one (not its list's), after 1s, 2s, 4s... up to 30s -- at once when the network comes back or the page is
 * shown again, never while offline or hidden. Once open again, it tells 'restart': what was sent meanwhile is
 * lost, the application reads its data again.
 */

// the events a socket tells: the WebSocket's own, and 'restart' -- told by a socket restarted (autoRestart)
const EVENT_TYPES = ['open', 'close', 'message', 'error', 'restart'];
const SOCKET_EVENT_TYPES = ['open', 'close', 'message', 'error'];

class WebSocketWrapper {

    #timeout = 5000;
    #url;                    // its ws(s) url
    #webSocket;
    #lists = {};             // eventType -> [callback]: on(...)
    #maps = {};              // eventType -> Map(key -> callback): put(...)
    #closeAsked = false;     // close() asked: it is not restarted
    #over = false;           // closed: its listeners dropped, a new one to ask for
    #onOver;                 // (wrapper, {asked, maps}) => the service told it is over

    /**
     * @param url    its ws(s) url
     * @param onOver told once it is over: whether close() asked it, and its map's listeners
     */
    constructor(url, onOver) {
        this.#url = url;
        this.#onOver = onOver;
        for (const eventType of EVENT_TYPES) {
            this.#lists[eventType] = [];
            this.#maps[eventType] = new Map();
        }
        this.#webSocket = new WebSocket(url);
        for (const eventType of SOCKET_EVENT_TYPES) {
            this.#webSocket.addEventListener(eventType, event => this.#tell(eventType, event));
        }
        // told to its listeners first (the listener above), then over
        this.#webSocket.addEventListener('close', () => this.#end());
    }

    // ============================================================
    //  OPEN / SEND / CLOSE
    // ============================================================

    /** @return a promise of the WebSocket, once open; rejected when it is over, or does not open in time */
    open() {
        if (this.#over) {
            return Promise.reject(new Error('the websocket ' + this.#url + ' is closed: webSocket(url) gives a new one'));
        }
        if (this.#webSocket.readyState === WebSocket.OPEN) {
            return Promise.resolve(this.#webSocket);
        }
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                reject(new Error('opening websocket failed ' + this.#url + ', timeout: ' + this.#timeout + 'ms'));
                this.#webSocket.close();   // given up: over, not left connecting
            }, this.#timeout);
            this.#webSocket.addEventListener('open', () => {
                clearTimeout(timer);
                resolve(this.#webSocket);
            }, {once: true});
            this.#webSocket.addEventListener('close', event => {
                clearTimeout(timer);
                reject(new Error('opening websocket failed ' + this.#url, {cause: event}));
            }, {once: true});
        });
    }

    /** @return a promise settled once sent: rejected when it is over, or does not open */
    send(data) {
        return this.open().then(webSocket => webSocket.send(data));
    }

    /**
     * Closes it -- for good: its listeners dropped once told, not restarted.
     *
     * @return a promise settled once it is closed (closing already: once it is)
     */
    close(code, reason) {
        this.#closeAsked = true;
        return new Promise((resolve, reject) => {
            if (this.#webSocket.readyState === WebSocket.CLOSED) {
                this.#end();
                resolve();
                return;
            }
            const timer = setTimeout(() => reject(new Error('closing websocket failed ' + this.#url + ', timeout: '
                                                            + this.#timeout + 'ms')),
                                     this.#timeout);
            this.#webSocket.addEventListener('close', () => {
                clearTimeout(timer);
                resolve();
            }, {once: true});
            if (this.#webSocket.readyState !== WebSocket.CLOSING) {
                this.#webSocket.close(code, reason);
            }
        });
    }

    // ============================================================
    //  STATE
    // ============================================================

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

    // ============================================================
    //  THE LIST: on(...)
    // ============================================================

    // eventType [ open | close | message | error | restart ]
    on(eventType, callback) {
        this.#lists[eventType].push(callback);
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

    onRestart(callback) {
        this.on('restart', callback);
    }

    // ============================================================
    //  THE MAP: put / remove / has
    // ============================================================

    /** Listens under a key: put again under it, the listener replaces the one it had -- at its place. */
    put(eventType, key, callback) {
        this.#maps[eventType].set(key, callback);
    }

    remove(eventType, key) {
        this.#maps[eventType].delete(key);
    }

    has(eventType, key) {
        return this.#maps[eventType].has(key);
    }

    putOpen(key, callback) {
        this.put('open', key, callback);
    }

    putMessage(key, callback) {
        this.put('message', key, callback);
    }

    putClose(key, callback) {
        this.put('close', key, callback);
    }

    putError(key, callback) {
        this.put('error', key, callback);
    }

    putRestart(key, callback) {
        this.put('restart', key, callback);
    }

    removeOpen(key) {
        this.remove('open', key);
    }

    removeMessage(key) {
        this.remove('message', key);
    }

    removeClose(key) {
        this.remove('close', key);
    }

    removeError(key) {
        this.remove('error', key);
    }

    removeRestart(key) {
        this.remove('restart', key);
    }

    hasOpen(key) {
        return this.has('open', key);
    }

    hasMessage(key) {
        return this.has('message', key);
    }

    hasClose(key) {
        return this.has('close', key);
    }

    hasError(key) {
        return this.has('error', key);
    }

    hasRestart(key) {
        return this.has('restart', key);
    }

    // ============================================================
    //  TELLING
    // ============================================================

    /** Tells 'restart': open again, what was sent meanwhile lost (WebSocketRestart). */
    tellRestart() {
        this.#tell('restart', {type: 'restart', url: this.#url});
    }

    /** Tells an event to the list, then to the map; a listener failing does not keep the others from it. */
    #tell(eventType, event) {
        for (const callback of [...this.#lists[eventType], ...this.#maps[eventType].values()]) {
            try {
                callback(event);
            }
            catch (error) {
                console.error({message: 'a websocket listener failed on ' + eventType + ' ' + this.#url, error: error});
            }
        }
    }

    /** Over -- closed, its listeners told: they are dropped, the service told (its map's kept for a restart). */
    #end() {
        if (this.#over) {
            return;
        }
        this.#over = true;
        const maps = {};
        for (const eventType of EVENT_TYPES) {
            maps[eventType] = new Map(this.#maps[eventType]);
            this.#lists[eventType] = [];
            this.#maps[eventType].clear();
        }
        this.#onOver(this, {asked: this.#closeAsked, maps: maps});
    }

}

/**
 * Restarts a socket closed otherwise than by close(): after 1s, 2s, 4s... up to 30s (a part of it at random,
 * so that the pages do not all come back at the same second) -- at once when the network comes back or the page
 * is shown again; never while offline or hidden. Its map's listeners are put on the new one, which tells
 * 'restart' once open.
 */
class WebSocketRestart {

    static #FIRST_DELAY = 1000;
    static #LAST_DELAY = 30000;

    #service;
    #url;
    #delay = WebSocketRestart.#FIRST_DELAY;
    #timer = null;
    #maps = null;            // the listeners to put on the next one, once one is to restart
    #stopped = false;
    #now = () => this.#tryNow();

    /**
     * @param service the service: it gives the new socket
     * @param url     the socket's url
     */
    constructor(service, url) {
        this.#service = service;
        this.#url = url;
        window.addEventListener('online', this.#now);
        window.addEventListener('pageshow', this.#now);
        document.addEventListener('visibilitychange', this.#now);
    }

    /**
     * A socket closed otherwise than by close(): the next one, once it is time.
     *
     * @param maps its map's listeners, put on the next one
     */
    schedule(maps) {
        if (this.#stopped) {
            return;
        }
        this.#maps = maps;
        clearTimeout(this.#timer);
        this.#timer = null;
        if (!this.#canTry()) {
            return;   // offline or hidden: the network back, the page shown again, will try
        }
        const delay = this.#delay * (0.75 + Math.random() * 0.5);
        this.#delay = Math.min(this.#delay * 2, WebSocketRestart.#LAST_DELAY);
        this.#timer = setTimeout(() => this.#try(), delay);
    }

    /** No more restarts: close() asked, or autoRestart(url, false). */
    stop() {
        this.#stopped = true;
        clearTimeout(this.#timer);
        this.#timer = null;
        window.removeEventListener('online', this.#now);
        window.removeEventListener('pageshow', this.#now);
        document.removeEventListener('visibilitychange', this.#now);
    }

    /** The network back, the page shown again: a socket waiting for its restart tries at once. */
    #tryNow() {
        if (!this.#stopped && this.#maps && this.#canTry()) {
            clearTimeout(this.#timer);
            this.#timer = null;
            this.#delay = WebSocketRestart.#FIRST_DELAY;
            this.#try();
        }
    }

    #canTry() {
        return navigator.onLine !== false && document.visibilityState !== 'hidden';
    }

    /** A new socket, the listeners put on it: open, it tells 'restart'; closed again, its end schedules the next. */
    #try() {
        const maps = this.#maps;
        this.#maps = null;
        this.#timer = null;
        const webSocket = this.#service.webSocket(this.#url);
        for (const eventType of EVENT_TYPES) {
            for (const [key, callback] of maps[eventType]) {
                webSocket.put(eventType, key, callback);
            }
        }
        webSocket.open()
                 .then(() => {
                     this.#delay = WebSocketRestart.#FIRST_DELAY;
                     webSocket.tellRestart();
                 })
                 .catch(() => {
                     // not open: its end schedules the next try
                 });
    }

}

class WebSocketService {

    #host = window.location.hostname;
    #port = window.location.port;
    #protocolWebSocket = window.location.protocol === 'https:' ? 'wss' : 'ws';

    #webSockets = new Map();   // url -> its socket, until it is over
    #restarts = new Map();     // url -> its restart (autoRestart)

    /** @return the socket of that url -- a new one when it has none, or the one it had is over */
    webSocket(url) {
        let webSocket = this.#webSockets.get(url);
        if (!webSocket) {
            webSocket = new WebSocketWrapper(this.#protocolWebSocket + '://' + this.#host + ':' + this.#port + url,
                                             (over, ended) => this.#over(url, over, ended));
            this.#webSockets.set(url, webSocket);
        }
        return webSocket;
    }

    /** @return the socket of that url, or {@code undefined} when it has none (none opened for asking) */
    existing(url) {
        return this.#webSockets.get(url);
    }

    /**
     * Closes the socket of that url, for good -- its restart stopped too, when it has none at the moment.
     *
     * @return a promise settled once it is closed
     */
    close(url, code, reason) {
        const webSocket = this.#webSockets.get(url);
        if (webSocket) {
            return webSocket.close(code, reason);
        }
        this.autoRestart(url, false);
        return Promise.resolve();
    }

    /**
     * Restarts the socket of that url when it is closed otherwise than by close() -- or no more.
     *
     * @param enabled {@code false}: no more restarts
     */
    autoRestart(url, enabled = true) {
        const restart = this.#restarts.get(url);
        if (enabled && !restart) {
            this.#restarts.set(url, new WebSocketRestart(this, url));
        }
        else if (!enabled && restart) {
            restart.stop();
            this.#restarts.delete(url);
        }
    }

    /** A socket over: forgotten -- restarted, if it is to be, unless close() asked it. */
    #over(url, webSocket, ended) {
        if (this.#webSockets.get(url) === webSocket) {
            this.#webSockets.delete(url);
        }
        const restart = this.#restarts.get(url);
        if (!restart) {
            return;
        }
        if (ended.asked) {
            restart.stop();
            this.#restarts.delete(url);
        }
        else {
            restart.schedule(ended.maps);
        }
    }

}

const webSocketService = new WebSocketService();

export function webSocket(url) {
    return webSocketService.webSocket(url);
}

export function autoRestart(url, enabled = true) {
    webSocketService.autoRestart(url, enabled);
}

export function send(url, data) {
    return webSocket(url).send(data);
}

export function close(url, code, reason) {
    return webSocketService.close(url, code, reason);
}

// eventType [ open | message | close | error | restart ]
export function on(eventType, url, callback) {
    webSocket(url).on(eventType, callback);
}

export function onOpen(url, callback) {
    webSocket(url).onOpen(callback);
}

export function onMessage(url, callback) {
    webSocket(url).onMessage(callback);
}

export function onClose(url, callback) {
    webSocket(url).onClose(callback);
}

export function onError(url, callback) {
    webSocket(url).onError(callback);
}

export function onRestart(url, callback) {
    webSocket(url).onRestart(callback);
}

export function put(eventType, url, key, callback) {
    webSocket(url).put(eventType, key, callback);
}

// remove / has: on the socket the url has, none opened for it
export function remove(eventType, url, key) {
    webSocketService.existing(url)?.remove(eventType, key);
}

export function has(eventType, url, key) {
    return webSocketService.existing(url)?.has(eventType, key) ?? false;
}

export function putOpen(url, key, callback) {
    webSocket(url).putOpen(key, callback);
}

export function putMessage(url, key, callback) {
    webSocket(url).putMessage(key, callback);
}

export function putClose(url, key, callback) {
    webSocket(url).putClose(key, callback);
}

export function putError(url, key, callback) {
    webSocket(url).putError(key, callback);
}

export function putRestart(url, key, callback) {
    webSocket(url).putRestart(key, callback);
}
