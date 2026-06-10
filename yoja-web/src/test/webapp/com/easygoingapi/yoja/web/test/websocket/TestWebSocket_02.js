'use strict'

const storageService = yojaWebApi.storageService 
const webSocketService = yojaWebApi.webSocketService 

const webSocket = webSocketService.webSocket('/websocket/test')
webSocket.onMessage(v => {
    storageService.setSessionItem('onMessage', v.data)
})
webSocket.onClose(v => {
    storageService.setSessionItem('onCloseCode', v.code)
    storageService.setSessionItem('onCloseReason', v.reason)
})
webSocket.send('bye')