'use strict'

const storageService = yojaWebApi.storageService 

storageService.setLocalItem('test', 'value11')

export default function test(args, resolve, rejet) {
    storageService.onEvent((scope, method, key, value) => {
        ywAssert.assertEquals('local', scope)
        ywAssert.assertEquals('get', method)
        ywAssert.assertEquals('test', key)
        ywAssert.assertEquals('value11', value)
        resolve()
    })
    storageService.getLocalItem('test')
}