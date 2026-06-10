'use strict'

const urlParameterService = yojaWebApi.urlParameterService 
const navigationService = yojaWebApi.navigationService 

export default function test(args, resolve, rejet) {
    let count = 0
    urlParameterService.onChange(h => {
        if ('pop' === h.event) {
            count++
        }
        if (count === 2) {
            ywAssert.assertArrayEquals(['value_11', 'value_12'], urlParameterService.getAll('key_1'))
            resolve()
        }
    })
    
    urlParameterService.set('key_1', 'value_11')
    urlParameterService.push()
    ywAssert.assertArrayEquals(['value_11'], urlParameterService.getAll('key_1'))
    
    urlParameterService.append('key_1', 'value_12')
    urlParameterService.push()
    ywAssert.assertArrayEquals(['value_11', 'value_12'], urlParameterService.getAll('key_1'))
    
    navigationService.back()
    navigationService.forward()
}