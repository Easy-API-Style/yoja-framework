'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.post('/post_01', new Blob([JSON.stringify({key_01: 'value_01', key_02: 'value_02'})]))  

const response = await result.body.text()
ywAssert.assertEquals('application/octet-stream', result.body.type)

ywAssert.assertEquals('{"key_01":"value_01","key_02":"value_02"}', response)
ywAssert.assertEquals('application/blob', result.headers.get('content-type'))


