'use strict';

const webSocketService = yojaWebApi.webSocketService;

// the list first, then the map in its order -- 'a' replaced at its place, 'broken' failing told last
export default function test(args, resolve, repeat) {
    const expected = ['list:one', 'a2:one', 'b:one'];
    if (JSON.stringify(window.wsLog) === JSON.stringify(expected)
          && webSocketService.has('message', '/websocket/test', 'a')
          && webSocketService.has('message', '/websocket/test', 'broken')
          && !webSocketService.has('message', '/websocket/test', 'none')) {
        resolve();
    }
}
