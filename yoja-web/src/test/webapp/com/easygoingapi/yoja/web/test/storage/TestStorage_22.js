'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setLocalItem('test', 1001)
    const value = storageService.getLocalItem('test')
    ywAssert.assertTrue(1001 === value)
    resolve()
}