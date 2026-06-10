'use strict'

const webSocketService = yojaWebApi.webSocketService 

export default function test(args, resolve, repeat) {
    const webSocket = webSocketService.webSocket('/websocket/test')
    if (webSocket.is('CLOSED') && webSocket.state() === 3) {
        resolve() 
    }
}