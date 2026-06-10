'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setSessionItem('test', undefined)
    const value = storageService.getSessionItem('test')
    ywAssert.assertEquals(null, value)
    resolve()
}