'use strict'

const httpClient = yojaWebApi.httpClient

const fetch = await httpClient.fetch('/file_1.txt')
const result = await fetch.text()

ywAssert.assertEquals(200, fetch.status)
ywAssert.assertEquals(true, fetch.bodyUsed)
ywAssert.assertEquals('hello world', result)
