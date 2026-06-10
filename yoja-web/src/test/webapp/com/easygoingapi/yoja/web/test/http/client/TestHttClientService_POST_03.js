'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.post('/post_01', {key_01: 'value_01', key_02: 'value_02'})   

ywAssert.assertEquals('{"key_01":"value_01","key_02":"value_02"}', JSON.stringify(result.body))
ywAssert.assertEquals('application/json', result.headers.get('content-type'))
