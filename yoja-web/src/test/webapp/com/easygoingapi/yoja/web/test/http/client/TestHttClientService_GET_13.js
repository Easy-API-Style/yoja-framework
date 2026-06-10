'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.get({'url': '/file_1.bi', 
                                     'fetchAs':'arrayBuffer'})

ywAssert.assertEquals(true, result.bodyUsed)
const decoder = new TextDecoder();
ywAssert.assertEquals('hello world', decoder.decode(result.body))
ywAssert.assertEquals('application/octet-stream', result.headers.get('content-type'))

