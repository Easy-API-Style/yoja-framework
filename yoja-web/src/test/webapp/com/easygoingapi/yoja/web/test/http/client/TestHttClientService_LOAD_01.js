'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.load('/file_1.json')

const propertyNames = []
for (const property in result) {
    propertyNames.push(property)
}
ywAssert.assertEquals(4, propertyNames.length)
ywAssert.assertEquals(200, result.status)
ywAssert.assertEquals('{\n    "name": "Bill",\n     "age": 12\n}', result.body)
ywAssert.assertEquals(true, result.bodyUsed)

ywAssert.assertEquals('no-store', result.headers.get('cache-control'))
ywAssert.assertEquals('38', result.headers.get('content-length'))
ywAssert.assertEquals('application/json', result.headers.get('content-type'))
