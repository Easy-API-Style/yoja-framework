'use strict'

const webSocketService = yojaWebApi.webSocketService 

const webSocket = webSocketService.webSocket('/websocket/test')
webSocket.close(3010, 'why not')