'use strict'

const urlParameterService = yojaWebApi.urlParameterService 
const navigationService = yojaWebApi.navigationService 

export default function test(args, resolve, rejet) {
    let count = 0
    const events = []
    urlParameterService.onChange(h => {
        count++
        events.push(h.event)
        if (count == 7) {
            ywAssert.assertArrayEquals(['value_11'], urlParameterService.getAll('key_1'))
        }
        if (count == 8) {
            ywAssert.assertEquals('pop', h.event)
            ywAssert.assertArrayEquals(['set', 
                                        'before-push', 
                                        'after-push',
                                        'append',
                                        'before-push',
                                        'after-push',
                                        'pop',
                                        'pop'],
                                        events)
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