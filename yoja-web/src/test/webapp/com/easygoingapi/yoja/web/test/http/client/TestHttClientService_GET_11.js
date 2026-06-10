'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.get({'url': '/file_1.txt', 
                                     'headers': {mode: 'base64_bis'}})

const decoder = new TextDecoder();       
ywAssert.assertEquals(true, result.bodyUsed)
ywAssert.assertEquals('hello world', decoder.decode(result.body))
