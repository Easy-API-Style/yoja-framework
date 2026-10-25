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
const {Listeners} = await import(yojaWeb.path('../util/listenerUtil.js'));

/*
 * One web socket per url of the application -- its own host and port, the url as is (its parameters, if any,
 * written in it): the url is its key.
 *
 * A socket is listened to by on(eventType, callback) -- onOpen, onMessage...: each gives back its off (listenerUtil),
 * called the callback is no more told; a controler gives it to its section (section.track(off)), off once the
 * section has left the page. Each event is told to the listeners in the order they were added; a listener failing
 * does not keep the others from being told.
 *
 * A socket closed -- by close(), or by the server, the network, its opening timeout -- is over: its listeners are
 * dropped, and webSocket(url) gives a new one, from zero.
 *
 * Unless its url is to restart (autoRestart(url)): cut otherwise than by close(), the very same socket connects
 * again, unseen -- its listeners kept; no 'close', no 'error', no second 'open' told -- after 1s,
 * 2s, 4s... up to 30s, at once when the network comes back or the page is shown again, never while offline or
 * hidden; send() and open() meanwhile wait for it, within their timeout. Once connected again it tells 'restart':
 * what was sent meanwhile is lost, the application reads its data again. stopAutoRestart(url) or close() stop it.
 */

// the events a socket tells: the WebSocket's own, and 'restart' -- connected again after a cut (autoRestart)
const EVENT_TYPES = ['open', 'close', 'message', 'error', 'restart'];

const FIRST_DELAY = 1000;   // the first wait before connecting again (ms), doubled at each try...
const LAST_DELAY = 30000;   // ...up to this one

class WebSocketWrapper {

    #timeout = 5000;
    #url;                    // its ws(s) url
    #webSocket;              // the WebSocket it holds: another one after each cut, when it restarts
    #listeners = {};         // eventType -> its Listeners: on(...)
    #restart = false;        // connected again after a cut (autoRestart)
    #cut = false;            // cut, connecting again
    #delay = FIRST_DELAY;    // the wait before the next try
    #timer = null;           // the next try, when it waits for it
    #waiting = [];           // open() asked meanwhile: [{resolve, reject}], settled once open, or over
    #closeAsked = false;     // close() asked: over, not restarted
    #over = false;           // closed for good: its listeners dropped, a new one to ask for
    #onEnd;                  // (wrapper, closeAsked) => the service told it is over
    #tryNow = () => this.#connectNow();

    /**
     * @param url     its ws(s) url
     * @param restart whether it connects again after a cut
     * @param onEnd   told once it is over -- whether close() asked it
     */
    constructor(url, restart, onEnd) {
        this.#url = url;
        this.#onEnd = onEnd;
        for (const eventType of EVENT_TYPES) {
            this.#listeners[eventType] = new Listeners('websocket ' + eventType + ' ' + url);
        }
        this.#connect();
        this.restart(restart);
    }

    // ============================================================
    //  OPEN / SEND / CLOSE
    // ============================================================

    /**
     * @return a promise of the WebSocket, once open -- connected again, when cut; rejected when it is over, or
     *         is not open within its timeout
     */
    open() {
        if (this.#over) {
            return Promise.reject(new Error('the websocket ' + this.#url + ' is closed: webSocket(url) gives a new one'));
        }
        if (this.#webSocket.readyState === WebSocket.OPEN) {
            return Promise.resolve(this.#webSocket);
        }
        return new Promise((resolve, reject) => {
            const waiter = {resolve: resolve, reject: reject};
            waiter.timer = setTimeout(() => {
                this.#waiting = this.#waiting.filter(candidate => candidate !== waiter);
                reject(new Error('opening websocket failed ' + this.#url + ', timeout: ' + this.#timeout + 'ms'));
                if (!this.#restart && this.#webSocket.readyState === WebSocket.CONNECTING) {
                    this.#webSocket.close();   // given up: over, not left connecting
                }
            }, this.#timeout);
            this.#waiting.push(waiter);
        });
    }

    /** @return a promise settled once sent: rejected when it is over, or is not open within its timeout */
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
        this.#stopRestart();
        if (this.#over) {
            return Promise.resolve();
        }
        if (this.#webSocket.readyState === WebSocket.CLOSED) {
            // cut, between two tries: closed already
            this.#tell('close', {type: 'close', code: code, reason: reason});
            this.#end();
            return Promise.resolve();
        }
        return new Promise((resolve, reject) => {
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

    /**
     * Connects again after a cut -- or no more: cut at the moment, it is closed for good ('close' told).
     *
     * @param restart whether it does
     */
    restart(restart) {
        if (this.#over) {
            return;
        }
        this.#restart = restart;
        if (restart) {
            window.addEventListener('online', this.#tryNow);
            window.addEventListener('pageshow', this.#tryNow);
            document.addEventListener('visibilitychange', this.#tryNow);
        }
        else {
            const cut = this.#cut;
            this.#stopRestart();
            if (cut) {
                this.#tell('close', {type: 'close', code: 1006, reason: 'cut'});
                this.#end();
            }
        }
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
    //  LISTENERS: on(...)
    // ============================================================

    /**
     * eventType [ open | close | message | error | restart ]
     *
     * @return its off
     * @throws TypeError for an event it does not tell
     */
    on(eventType, callback) {
        const listeners = this.#listeners[eventType];
        if (!listeners) {
            throw new TypeError('websocket: on(eventType [' + EVENT_TYPES.join(' | ') + '], callback), not '
                                + eventType);
        }
        return listeners.add(callback);
    }

    onOpen(callback) {
        return this.on('open', callback);
    }

    onMessage(callback) {
        return this.on('message', callback);
    }

    onClose(callback) {
        return this.on('close', callback);
    }

    onError(callback) {
        return this.on('error', callback);
    }

    onRestart(callback) {
        return this.on('restart', callback);
    }

    // ============================================================
    //  THE WEBSOCKET IT HOLDS
    // ============================================================

    /** A WebSocket: its events told -- the first time; after a cut, its opening told as 'restart' only. */
    #connect() {
        const webSocket = new WebSocket(this.#url);
        this.#webSocket = webSocket;
        webSocket.addEventListener('open', event => {
            const restarted = this.#cut;
            this.#cut = false;
            this.#delay = FIRST_DELAY;
            for (const waiter of this.#waiting.splice(0)) {
                clearTimeout(waiter.timer);
                waiter.resolve(webSocket);
            }
            this.#tell(restarted ? 'restart' : 'open', restarted ? {type: 'restart', url: this.#url} : event);
        });
        webSocket.addEventListener('message', event => this.#tell('message', event));
        webSocket.addEventListener('error', event => {
            if (!this.#willRestart()) {
                this.#tell('error', event);   // a cut to restart from is not seen
            }
        });
        webSocket.addEventListener('close', event => {
            if (this.#willRestart()) {
                this.#cut = true;
                this.#schedule();
            }
            else {
                this.#tell('close', event);
                this.#end();
            }
        });
    }

    #willRestart() {
        return this.#restart && !this.#closeAsked;
    }

    /** The next try, once it is time -- offline or hidden: once the network is back, the page shown again. */
    #schedule() {
        clearTimeout(this.#timer);
        this.#timer = null;
        if (!this.#canTry()) {
            return;
        }
        const delay = this.#delay * (0.75 + Math.random() * 0.5);   // not every page at the same second
        this.#delay = Math.min(this.#delay * 2, LAST_DELAY);
        this.#timer = setTimeout(() => {
            this.#timer = null;
            this.#connect();
        }, delay);
    }

    /** The network back, the page shown again: cut, it tries at once. */
    #connectNow() {
        if (this.#cut && !this.#over && this.#canTry() && this.#webSocket.readyState === WebSocket.CLOSED) {
            clearTimeout(this.#timer);
            this.#timer = null;
            this.#delay = FIRST_DELAY;
            this.#connect();
        }
    }

    #canTry() {
        return navigator.onLine !== false && document.visibilityState !== 'hidden';
    }

    #stopRestart() {
        this.#restart = false;
        this.#cut = false;
        clearTimeout(this.#timer);
        this.#timer = null;
        window.removeEventListener('online', this.#tryNow);
        window.removeEventListener('pageshow', this.#tryNow);
        document.removeEventListener('visibilitychange', this.#tryNow);
    }

    /** Tells an event to the list, then to the map; a listener failing does not keep the others from it. */
    #tell(eventType, event) {
        this.#listeners[eventType].run(event);
    }

    /** Over -- closed for good, its listeners told: they are dropped, open() refused, the service told. */
    #end() {
        if (this.#over) {
            return;
        }
        this.#over = true;
        this.#stopRestart();
        for (const eventType of EVENT_TYPES) {
            this.#listeners[eventType].clear();
        }
        for (const waiter of this.#waiting.splice(0)) {
            clearTimeout(waiter.timer);
            waiter.reject(new Error('the websocket ' + this.#url + ' is closed'));
        }
        this.#onEnd(this, this.#closeAsked);
    }

}

class WebSocketService {

    #host = window.location.hostname;
    #port = window.location.port;
    #protocolWebSocket = window.location.protocol === 'https:' ? 'wss' : 'ws';

    #webSockets = new Map();   // url -> its socket, until it is over
    #restarts = new Set();     // the urls whose socket connects again after a cut (autoRestart)

    /** @return the socket of that url -- a new one when it has none, or the one it had is over */
    webSocket(url) {
        let webSocket = this.#webSockets.get(url);
        if (!webSocket) {
            webSocket = new WebSocketWrapper(this.#protocolWebSocket + '://' + this.#host + ':' + this.#port + url,
                                             this.#restarts.has(url),
                                             (ended, closeAsked) => this.#onEnd(url, ended, closeAsked));
            this.#webSockets.set(url, webSocket);
        }
        return webSocket;
    }

    /**
     * Closes the socket of that url, for good -- its restart stopped.
     *
     * @return a promise settled once it is closed
     */
    close(url, code, reason) {
        this.#restarts.delete(url);
        const webSocket = this.#webSockets.get(url);
        return webSocket ? webSocket.close(code, reason) : Promise.resolve();
    }

    /** The socket of that url connects again after a cut, unseen -- its own, or the next one it is given. */
    autoRestart(url) {
        this.#restarts.add(url);
        this.#webSockets.get(url)?.restart(true);
    }

    /** No more: cut at the moment, it is closed for good. */
    stopAutoRestart(url) {
        this.#restarts.delete(url);
        this.#webSockets.get(url)?.restart(false);
    }

    /** A socket over: forgotten -- closed by close(), not restarted any more. */
    #onEnd(url, webSocket, closeAsked) {
        if (this.#webSockets.get(url) === webSocket) {
            this.#webSockets.delete(url);
        }
        if (closeAsked) {
            this.#restarts.delete(url);
        }
    }

}

const webSocketService = new WebSocketService();

export function webSocket(url) {
    return webSocketService.webSocket(url);
}

export function autoRestart(url) {
    webSocketService.autoRestart(url);
}

export function stopAutoRestart(url) {
    webSocketService.stopAutoRestart(url);
}

export function send(url, data) {
    return webSocket(url).send(data);
}

export function close(url, code, reason) {
    return webSocketService.close(url, code, reason);
}

// eventType [ open | message | close | error | restart ]
export function on(eventType, url, callback) {
    return webSocket(url).on(eventType, callback);
}

export function onOpen(url, callback) {
    return webSocket(url).onOpen(callback);
}

export function onMessage(url, callback) {
    return webSocket(url).onMessage(callback);
}

export function onClose(url, callback) {
    return webSocket(url).onClose(callback);
}

export function onError(url, callback) {
    return webSocket(url).onError(callback);
}

export function onRestart(url, callback) {
    return webSocket(url).onRestart(callback);
}








