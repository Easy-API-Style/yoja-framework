'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.post({url: '/post_01', fetchAs: 'text'}, 20100)  

ywAssert.assertEquals('20100', result.body)
ywAssert.assertEquals('text/plain', result.headers.get('content-type'))
