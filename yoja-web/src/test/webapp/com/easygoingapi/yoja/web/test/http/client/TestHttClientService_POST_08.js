'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.post('/post_01', new Date('August 19, 1975 23:15:30 UTC'))   

ywAssert.assertEquals('1975-08-19T23:15:30.000Z', result.body)
ywAssert.assertEquals('text/plain', result.headers.get('content-type'))


