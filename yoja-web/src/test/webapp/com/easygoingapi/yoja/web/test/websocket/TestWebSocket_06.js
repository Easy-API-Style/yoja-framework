'use strict'

const storageService = yojaWebApi.storageService 
const webSocketService = yojaWebApi.webSocketService 

webSocketService.onMessage('/websocket/test', v => {
    storageService.setSessionItem('onMessage', v.data)
})
webSocketService.onClose('/websocket/test', v => {
    storageService.setSessionItem('onCloseCode', v.code)
    storageService.setSessionItem('onCloseReason', v.reason)
})
webSocketService.send('/websocket/test', 'bye')