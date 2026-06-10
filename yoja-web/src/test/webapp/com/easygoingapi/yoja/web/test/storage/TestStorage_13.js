'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.on('local', 'set', 'test', v => {
        ywAssert.assertEquals('value13', v)
        resolve()
    })
    storageService.setLocalItem('test', 'value13')
}