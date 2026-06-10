'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.fetch('/file_1.json')

ywAssert.assertEquals(200, result.status)
ywAssert.assertEquals(false, result.bodyUsed)
