'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.post({url: '/post_01', contentType: 'text/plain'}, 'hello')   

ywAssert.assertEquals('hello', result.body)
ywAssert.assertEquals('text/plain', result.headers.get('content-type'))

