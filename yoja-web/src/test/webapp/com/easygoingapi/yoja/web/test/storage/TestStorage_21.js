'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, rejet) {
    storageService.setLocalItem('test', new Date('August 19, 1975 23:15:30 UTC'))
    const value = storageService.getLocalItem('test')
    ywAssert.assertTrue(new Date('August 19, 1975 23:15:30 UTC').toJSON() === value.toJSON())
    resolve()
}