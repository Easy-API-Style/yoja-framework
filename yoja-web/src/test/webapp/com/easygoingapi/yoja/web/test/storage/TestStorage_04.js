'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setSessionItem('test', 'value4')
    storageService.removeSessionItem('test')
    const value_1 = storageService.getSessionItem('test')
    ywAssert.assertEquals(undefined, value_1)
    storageService.setSessionItem('test', 'vvv4')
    const value_2 = storageService.getSessionItem('test')
    ywAssert.assertEquals('vvv4', value_2)
    resolve()
}