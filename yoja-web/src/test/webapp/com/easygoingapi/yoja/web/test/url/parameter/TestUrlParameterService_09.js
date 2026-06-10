'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    urlParameterService.set('key_1', 'value_11')
    urlParameterService.append('key_1', 'value_12')
    urlParameterService.append('key_2', 'value_22')
    
    ywAssert.assertTrue(urlParameterService.has('key_1', 'value_12'))
    ywAssert.assertTrue(urlParameterService.has('key_1'))
    ywAssert.assertFalse(urlParameterService.has('key_2', 'value_12'))
    ywAssert.assertFalse(urlParameterService.has('key_3', 'value_12'))
    ywAssert.assertEquals('key_1=value_11&key_1=value_12&key_2=value_22', urlParameterService.toUrlQuery())
    resolve()
}