'use strict'

const webSocketService = yojaWebApi.webSocketService 

export default function test(args, resolve, repeat) {
    const webSocket = webSocketService.webSocket('/websocket/test')
    window.firstWebSocket = window.firstWebSocket || webSocket   // the one opened first (TestWebSocket_07)
    if (webSocket.is('OPEN') && webSocket.state() === 1) {
        resolve()
    }
}
