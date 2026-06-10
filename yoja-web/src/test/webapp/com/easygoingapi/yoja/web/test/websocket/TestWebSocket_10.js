'use strict'


const webSocketService = yojaWebApi.webSocketService 

export default function test(args, resolve, rejet) {
    const webSocket = webSocketService.webSocket('/websocket/test')
    ywAssert.assertEquals(false, webSocket.isOpen())
    ywAssert.assertEquals(5000, webSocket.timeout)
    webSocket.timeout = 200
    ywAssert.assertEquals(200, webSocket.timeout)
    webSocket.send('hello')
             .catch(e => {
                console.error(e)
                resolve()
             })
         
}