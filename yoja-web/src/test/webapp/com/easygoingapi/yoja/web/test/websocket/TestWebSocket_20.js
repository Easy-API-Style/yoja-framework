'use strict';

// no more restart, cut at the moment: closed for good, 'close' told
const webSocketService = yojaWebApi.webSocketService;

webSocketService.stopAutoRestart('/websocket/test');
