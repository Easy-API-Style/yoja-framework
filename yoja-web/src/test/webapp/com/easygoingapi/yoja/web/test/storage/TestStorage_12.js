'use strict'

const storageService = yojaWebApi.storageService 

storageService.setLocalItem('test', 'value12')

export default function test(args, resolve, rejet) {
    storageService.onEvent((scope, method, key, value) => {
        ywAssert.assertEquals('local', scope)
        ywAssert.assertEquals('remove', method)
        ywAssert.assertEquals('test', key)
        ywAssert.assertEquals('value12', value)
        resolve()
    })
    storageService.removeLocalItem('test')
}