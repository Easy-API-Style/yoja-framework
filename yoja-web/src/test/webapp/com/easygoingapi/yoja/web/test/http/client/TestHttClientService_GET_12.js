'use strict'

const httpClient = yojaWebApi.httpClient

httpClient.addContentTypes({base64ContentTypes: ['base64']})    

const result = await httpClient.get({'url': '/file_1.txt', 
                                     'headers': {mode: 'base64_only'}})
         
const decoder = new TextDecoder();                                         
ywAssert.assertEquals(true, result.bodyUsed)
ywAssert.assertEquals('hello world', decoder.decode(result.body))
