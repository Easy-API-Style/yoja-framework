'use strict';

// the list and the map: a list's listener, three under keys -- 'a' put again (it replaces its first one, at its
// place), 'broken' failing (the others are told all the same)
const webSocketService = yojaWebApi.webSocketService;

window.wsLog = [];
const webSocket = webSocketService.webSocket('/websocket/test');
webSocket.onMessage(event => window.wsLog.push('list:' + event.data));
webSocket.putMessage('a', () => window.wsLog.push('a1'));
webSocket.putMessage('b', event => window.wsLog.push('b:' + event.data));
webSocket.putMessage('broken', () => {
    throw new Error('a listener failing');
});
webSocket.putMessage('a', event => window.wsLog.push('a2:' + event.data));
webSocket.send('ready');
