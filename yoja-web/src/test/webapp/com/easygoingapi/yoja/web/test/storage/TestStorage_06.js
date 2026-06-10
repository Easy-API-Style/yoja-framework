'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.onEvent((scope, method, key, value) => {
        ywAssert.assertEquals('session', scope)
        ywAssert.assertEquals('set', method)
        ywAssert.assertEquals('test', key)
        ywAssert.assertEquals(null, value)
        resolve()
    })
    storageService.setSessionItem('test', undefined)
}