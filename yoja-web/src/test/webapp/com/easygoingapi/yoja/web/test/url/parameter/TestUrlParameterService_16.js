'use strict'

const urlParameterService = yojaWebApi.urlParameterService 

export default function test(args, resolve, rejet) {
    urlParameterService.set('key_1', 'value 11')
    urlParameterService.push()
    ywAssert.assertArrayEquals(['value 11'], urlParameterService.getAll('key_1'))
    
    urlParameterService.append('key_1', 'value_12_àùé')
    urlParameterService.push()
    ywAssert.assertArrayEquals(['value 11', 'value_12_àùé'], urlParameterService.getAll('key_1'))
    
    urlParameterService.append('key_1')
    urlParameterService.replace()
    ywAssert.assertArrayEquals(['value 11', 'value_12_àùé', null], urlParameterService.getAll('key_1'))
    
    ywAssert.assertEquals('key_1=value%2011&key_1=value_12_%C3%A0%C3%B9%C3%A9&key_1', urlParameterService.toUrlQuery())
    ywAssert.assertArrayEquals([{key: 'key_1', value: 'value 11'}, 
                                {key: 'key_1', value: 'value_12_àùé'}, 
                                {key: 'key_1', value: null}], 
                               urlParameterService.currentUrlParameter().entries())
    
    resolve()
}