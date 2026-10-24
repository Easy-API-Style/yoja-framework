'use strict';

// restarted when the server closes it: its map's listeners on the new one, 'restart' told once open
const webSocketService = yojaWebApi.webSocketService;

window.restarted = 0;
window.lastMessage = null;
webSocketService.autoRestart('/websocket/test');
webSocketService.putRestart('/websocket/test', 'test', () => window.restarted++);
webSocketService.putMessage('/websocket/test', 'test', event => window.lastMessage = event.data);
