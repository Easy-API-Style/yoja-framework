'use strict'

// the socket the server closed is over: webSocket(url) would give a new one, so the first one is looked at
export default function test(args, resolve, repeat) {
    const webSocket = window.firstWebSocket
    if (webSocket && webSocket.is('CLOSED') && webSocket.state() === 3) {
        resolve() 
    }
}
