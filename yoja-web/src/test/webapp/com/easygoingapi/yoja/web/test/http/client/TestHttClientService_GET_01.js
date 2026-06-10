'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.get('/file_1.txt')

const propertyNames = []
for (const property in result) {
    propertyNames.push(property)
}
ywAssert.assertEquals(4, propertyNames.length)
ywAssert.assertEquals(200, result.status)
ywAssert.assertEquals('hello world', result.body)
ywAssert.assertEquals(true, result.bodyUsed)

ywAssert.assertEquals('no-store', result.headers.get('cache-control'))
ywAssert.assertEquals('11', result.headers.get('content-length'))
ywAssert.assertEquals('text/plain', result.headers.get('content-type'))
