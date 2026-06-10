'use strict'

const urlParameterService = yojaWebApi.urlParameterService 
const navigationService = yojaWebApi.navigationService 

export default function test(args, resolve, rejet) {
    urlParameterService.onChange(h => {
        if (h.event === 'before-replace') {
            ywAssert.assertEquals('/page_1.html', navigationService.path())
            ywAssert.assertEquals(0, navigationService.urlParameter().size())
            ywAssert.assertFalse(urlParameterService.isUpdated())
        }
        else if (h.event === 'after-replace') {
            ywAssert.assertEquals('/page_1.html', navigationService.path())
            ywAssert.assertArrayEquals([{key: 'key_1', value: 'value_11'}, 
                                      {key: 'key_1', value: 'value_12'}], 
                                     navigationService.urlParameter().entries())
            ywAssert.assertTrue(urlParameterService.isUpdated())
        }
        else if (h.event === 'after-push') {
            ywAssert.assertEquals(0, navigationService.urlParameter().size())
            resolve()
        }
    })
   urlParameterService.set('key_1', 'value_11')
   urlParameterService.append('key_1', 'value_12')
   ywAssert.assertEquals('/page_1.html', navigationService.path())
   ywAssert.assertEquals(0, navigationService.urlParameter().size())
   urlParameterService.replace()
   urlParameterService.clear()
   urlParameterService.push()
}