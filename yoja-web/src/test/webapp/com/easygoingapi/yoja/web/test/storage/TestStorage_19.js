'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setLocalItem('test', undefined)
    const value = storageService.getLocalItem('test')
    ywAssert.assertEquals(null, value)
    resolve()
}