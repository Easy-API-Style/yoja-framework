'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    urlParameterService.set('key_1', 'value_11')
    urlParameterService.append('key_1', 'value_12')
    urlParameterService.set('key_1', 'value_13')
    ywAssert.assertEquals('value_13', urlParameterService.get('key_1'))
    resolve()
}