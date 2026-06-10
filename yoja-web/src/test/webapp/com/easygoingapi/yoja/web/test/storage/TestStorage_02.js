'use strict'

const storageService = yojaWebApi.storageService 

storageService.setSessionItem('test', 'value2')

export default function test(args, resolve, rejet) {
    storageService.on('session', 'get', 'test', v => {
        ywAssert.assertEquals('value2', v)
        resolve()
    })
    storageService.getSessionItem('test')
}