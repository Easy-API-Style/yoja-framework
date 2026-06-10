'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    urlParameterService.onChange(h => {
        ywAssert.assertEquals('set', h.event)
        ywAssert.assertEquals('key_1', h.key)
        ywAssert.assertEquals('value_1', h.value)
        resolve()
    })
    urlParameterService.set('key_1', 'value_1')
}