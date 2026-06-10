'use strict'



const webSocketService = yojaWebApi.webSocketService 

const webSocket = webSocketService.webSocket('/websocket/test')
ywAssert.assertEquals(true, webSocket.isOpen())
webSocket.open()
         .then(() => webSocket.send('yo!'))