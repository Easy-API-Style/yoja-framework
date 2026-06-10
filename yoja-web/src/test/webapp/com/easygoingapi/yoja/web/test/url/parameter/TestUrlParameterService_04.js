'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    urlParameterService.onChange(h => {
        if (h.event === 'remove') {
            ywAssert.assertEquals('key_1', h.key)
            ywAssert.assertEquals('value_1', h.value)
            ywAssert.assertArrayEquals(['value_2'], urlParameterService.getAll('key_1'))
            resolve()
        }
    })
    urlParameterService.set('key_1', 'value_1')
    urlParameterService.append('key_1', 'value_2')
    ywAssert.assertArrayEquals(['value_1', 'value_2'], urlParameterService.getAll('key_1'))
    ywAssert.assertEquals('value_1', urlParameterService.get('key_1'))
    
    urlParameterService.remove('key_1', 'value_1')
}