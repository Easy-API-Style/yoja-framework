'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.post('/post_01', ['value_01', 'value_02'])   

ywAssert.assertEquals('["value_01","value_02"]', JSON.stringify(result.body))
ywAssert.assertEquals('application/array-json', result.headers.get('content-type'))

