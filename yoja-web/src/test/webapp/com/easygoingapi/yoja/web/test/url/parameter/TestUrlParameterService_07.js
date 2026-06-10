'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    urlParameterService.set('key_1', 'value_11')
    urlParameterService.append('key_1', 'value_12')
    urlParameterService.append('key_2', 'value_22')
    ywAssert.assertArrayEquals(['key_1', 'key_2'], urlParameterService.keys())
    resolve()
}