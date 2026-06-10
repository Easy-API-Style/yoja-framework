'use strict'

const storageService = yojaWebApi.storageService 

storageService.setSessionItem('test', 'value3')

export default function test(args, resolve, rejet) {
    storageService.on('session', 'remove', 'test', v => {
        ywAssert.assertEquals('value3', v)
        resolve()
    })
    storageService.removeSessionItem('test')
}