'use strict'

const storageService = yojaWebApi.storageService 

storageService.setLocalItem('test', 'value15')

export default function test(args, resolve, rejet) {
    storageService.on('local', 'remove', 'test', v => {
        ywAssert.assertEquals('value15', v)
        resolve()
    })
    storageService.removeLocalItem('test')
}