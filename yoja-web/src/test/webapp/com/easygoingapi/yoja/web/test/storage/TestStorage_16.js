'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setLocalItem('test', 'value16')
    storageService.removeLocalItem('test')
    const value_1 = storageService.getLocalItem('test')
    ywAssert.assertEquals(undefined, value_1)
    storageService.setLocalItem('test', 'vvv16')
    const value_2 = storageService.getLocalItem('test')
    ywAssert.assertEquals('vvv16', value_2)
    resolve()
}