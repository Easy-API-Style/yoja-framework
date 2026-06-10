'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.onEvent((scope, method, key, value) => {
        ywAssert.assertEquals('local', scope)
        ywAssert.assertEquals('set', method)
        ywAssert.assertEquals('test', key)
        ywAssert.assertEquals('value17', value)
        resolve()
    })
    storageService.setLocalItem('test', 'value17')
}