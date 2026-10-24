'use strict';

// 'b' removed: told no more
const webSocketService = yojaWebApi.webSocketService;

webSocketService.remove('message', '/websocket/test', 'b');
window.wsLog = [];
