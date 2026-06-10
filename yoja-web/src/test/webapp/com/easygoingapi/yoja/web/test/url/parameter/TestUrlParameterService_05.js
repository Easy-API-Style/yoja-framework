'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    urlParameterService.onChange(h => {
        if (h.event === 'append') {
            ywAssert.assertEquals('key_1', h.key)
            ywAssert.assertEquals('value_12', h.value)
            ywAssert.assertEquals('value_11', urlParameterService.get('key_1'))
            resolve()
        }
    })
    urlParameterService.set('key_1', 'value_11')
    urlParameterService.append('key_1', 'value_12')
}