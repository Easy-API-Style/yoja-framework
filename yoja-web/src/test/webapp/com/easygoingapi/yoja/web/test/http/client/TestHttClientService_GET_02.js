'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.get('/file_1.json')

const propertyNames = []
for (const property in result) {
    propertyNames.push(property)
}

ywAssert.assertEquals(4, propertyNames.length)
ywAssert.assertEquals(200, result.status)
ywAssert.assertEquals('{"name":"Bill","age":12}', JSON.stringify(result.body))
ywAssert.assertEquals(true, result.bodyUsed)
ywAssert.assertEquals('Bill', result.body.name)
ywAssert.assertEquals(12, result.body.age)

ywAssert.assertEquals('no-store', result.headers.get('cache-control'))
ywAssert.assertEquals('38', result.headers.get('content-length'))
ywAssert.assertEquals('application/json', result.headers.get('content-type'))
