'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.post('/post_01', 20100)  

ywAssert.assertEquals('20100', result.body)
ywAssert.assertEquals('text/plain', result.headers.get('content-type'))
