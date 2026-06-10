'use strict'

const storageService = yojaWebApi.storageService 

storageService.setSessionItem('test', 'value9')

export default function test(args, resolve, rejet) {
    storageService.onEvent((scope, method, key, value) => {
        ywAssert.assertEquals('session', scope)
        ywAssert.assertEquals('get', method)
        ywAssert.assertEquals('test', key)
        ywAssert.assertEquals('value9', value)
        resolve()
    })
    storageService.getSessionItem('test')
}