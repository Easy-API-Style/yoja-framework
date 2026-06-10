'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setLocalItem('test', 'value20')
    storageService.clearLocal()
    const value = storageService.getLocalItem('test')
    ywAssert.assertEquals(undefined, value)
    resolve()
}