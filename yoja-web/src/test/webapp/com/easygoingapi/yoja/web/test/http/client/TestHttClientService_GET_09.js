'use strict'

const httpClient = yojaWebApi.httpClient

const result = await httpClient.get({'url': '/file_1.txt', 
                                     'fetchAs': 'blob',
                                     'headers': {mode: 'blob'}})
                                     
const result_text = await result.body.text()
ywAssert.assertEquals('text/plain', result.body.type)

ywAssert.assertEquals(true, result.bodyUsed)
ywAssert.assertEquals('hello world', result_text)
