'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setSessionItem('test', 'value8')
    storageService.clearSession()
    const value = storageService.getSessionItem('test')
    ywAssert.assertEquals(undefined, value)
    resolve()
}