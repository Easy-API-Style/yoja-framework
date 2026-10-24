'use strict';

// to restart after a cut: its listeners, list and map, kept on the same socket; a cut not seen
const webSocketService = yojaWebApi.webSocketService;

window.restarted = 0;
window.closes = 0;
window.listMessage = null;
window.mapMessage = null;
webSocketService.autoRestart('/websocket/test');
webSocketService.onMessage('/websocket/test', event => window.listMessage = event.data);
webSocketService.putMessage('/websocket/test', 'test', event => window.mapMessage = event.data);
webSocketService.putRestart('/websocket/test', 'test', () => window.restarted++);
webSocketService.onClose('/websocket/test', () => window.closes++);
