'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.on('session', 'set', 'test', v => {
        ywAssert.assertEquals('value1', v)
        resolve()
    })
    storageService.setSessionItem('test', 'value1')
}