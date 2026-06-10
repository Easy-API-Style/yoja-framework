'use strict'

const storageService = yojaWebApi.storageService 

storageService.setSessionItem('test', 'value10')

export default function test(args, resolve, rejet) {
    storageService.onEvent((scope, method, key, value) => {
        ywAssert.assertEquals('session', scope)
        ywAssert.assertEquals('remove', method)
        ywAssert.assertEquals('test', key)
        ywAssert.assertEquals('value10', value)
        resolve()
    })
    storageService.removeSessionItem('test')
}