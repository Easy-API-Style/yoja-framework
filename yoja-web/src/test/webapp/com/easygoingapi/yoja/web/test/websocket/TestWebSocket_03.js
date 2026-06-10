'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, repeat) {
    const message = storageService.getSessionItem('onMessage')
    const onCloseCode = storageService.getSessionItem('onCloseCode')
    const onCloseReason = storageService.getSessionItem('onCloseReason')
    if (message === 'hello'
          && onCloseCode === 200
          && onCloseReason === 'close websocket') {
        resolve()
    }
}