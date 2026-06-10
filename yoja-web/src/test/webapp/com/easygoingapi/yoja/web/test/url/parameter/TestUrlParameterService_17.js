'use strict'

window.yojaWebTest = []

yojaWebTest.appendParameter = function(key, value, action) {
    yojaWebApi.urlParameterService.append(key, value)
    if (action === 'push') {
        yojaWebApi.urlParameterService.push({key: key, value: value})
    }
    else {
        yojaWebApi.urlParameterService.replace({key: key, value: value})
    }
}
   
yojaWebApi.urlParameterService.onChange(e => {
    const result = {event: e,
                    state: yojaWebApi.urlParameterService.state()}
    yojaWebTest.push(result)
})
