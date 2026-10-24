'use strict';

// closed by the application: for good, not restarted
const webSocketService = yojaWebApi.webSocketService;

webSocketService.close('/websocket/test', 3000, 'bye');
