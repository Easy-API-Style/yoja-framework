'use strict';

const webSocketService = yojaWebApi.webSocketService;

// closed for good: told once, over -- webSocket(url) would give a new one
export default function test(args, resolve, repeat) {
    if (window.closes === 1 && window.firstWebSocket.is('CLOSED')) {
        resolve();
    }
}
