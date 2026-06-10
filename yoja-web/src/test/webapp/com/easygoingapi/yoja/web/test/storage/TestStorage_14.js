'use strict'

const storageService = yojaWebApi.storageService 

storageService.setLocalItem('test', 'value14')

export default function test(args, resolve, rejet) {
    storageService.on('local', 'get', 'test', v => {
        ywAssert.assertEquals('value14', v)
        resolve()
    })
    storageService.getLocalItem('test')
}