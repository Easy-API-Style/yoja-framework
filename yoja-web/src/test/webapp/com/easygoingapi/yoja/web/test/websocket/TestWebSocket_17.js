'use strict';

const webSocketService = yojaWebApi.webSocketService;

// connected again: the very same socket, open, 'restart' told, its cut not ('close')
export default function test(args, resolve, repeat) {
    const webSocket = webSocketService.webSocket('/websocket/test');
    if (window.restarted === 1 && webSocket === window.firstWebSocket && webSocket.isOpen() && window.closes === 0) {
        resolve();
    }
}
