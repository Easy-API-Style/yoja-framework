'use strict';

// to restart after a cut: its listeners kept on the same socket; a cut not seen
const webSocketService = yojaWebApi.webSocketService;

window.restarted = 0;
window.closes = 0;
window.listMessage = null;
window.otherMessage = null;
webSocketService.autoRestart('/websocket/test');
webSocketService.onMessage('/websocket/test', event => window.listMessage = event.data);
webSocketService.onMessage('/websocket/test', event => window.otherMessage = event.data);
webSocketService.onRestart('/websocket/test', () => window.restarted++);
webSocketService.onClose('/websocket/test', () => window.closes++);
