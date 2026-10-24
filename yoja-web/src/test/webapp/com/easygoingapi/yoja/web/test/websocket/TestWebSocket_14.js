'use strict';

const webSocketService = yojaWebApi.webSocketService;

export default function test(args, resolve, repeat) {
    if (JSON.stringify(window.wsLog) === JSON.stringify(['list:two', 'a2:two'])
          && !webSocketService.has('message', '/websocket/test', 'b')) {
        resolve();
    }
}
