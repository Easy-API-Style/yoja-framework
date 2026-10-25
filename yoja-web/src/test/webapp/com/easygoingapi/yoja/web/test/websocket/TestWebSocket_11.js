'use strict';

// the listeners in their order: one taken off before any message, one failing (the others are told all the same),
// one to take off later (TestWebSocket_13)
const webSocketService = yojaWebApi.webSocketService;

window.wsLog = [];
const webSocket = webSocketService.webSocket('/websocket/test');
webSocket.onMessage(event => window.wsLog.push('list:' + event.data));
const offFirst = webSocket.onMessage(() => window.wsLog.push('a1'));
offFirst();
window.offB = webSocket.onMessage(event => window.wsLog.push('b:' + event.data));
webSocket.onMessage(() => {
    throw new Error('a listener failing');
});
webSocket.onMessage(event => window.wsLog.push('a2:' + event.data));
webSocket.send('ready');
