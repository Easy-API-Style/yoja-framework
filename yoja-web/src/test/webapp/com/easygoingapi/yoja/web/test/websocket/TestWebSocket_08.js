'use strict'



const webSocketService = yojaWebApi.webSocketService 

const webSocket = webSocketService.webSocket('/websocket/test')
ywAssert.assertEquals(false, webSocket.isOpen())
webSocket.open()
         .then(() => webSocket.send('hi!'))
