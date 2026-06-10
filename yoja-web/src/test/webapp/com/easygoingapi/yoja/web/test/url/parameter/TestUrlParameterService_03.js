'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    
    urlParameterService.onChange(h => {
        if (h.event === 'remove') {
            ywAssert.assertEquals('key_1', h.key)
            ywAssert.assertArrayEquals(['value_1', 'value_2'], h.value)
            resolve()
        }
    })
    urlParameterService.set('key_1', 'value_1')
    urlParameterService.append('key_1', 'value_2')
    urlParameterService.remove('key_1')
}