'use strict';

const webSocketService = yojaWebApi.webSocketService;

// once 'restart' told, the socket the url has is the new one, open (asked only then: none opened for asking)
export default function test(args, resolve, repeat) {
    if (window.restarted !== 1) {
        return;
    }
    const webSocket = webSocketService.webSocket('/websocket/test');
    if (webSocket !== window.firstWebSocket && webSocket.isOpen()) {
        resolve();
    }
}
