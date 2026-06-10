'use strict'

const languageService = yojaWebApi.languageService  
const storageService = yojaWebApi.storageService 

export default class HomeControler {

    #section;
    
    constructor(section) {
        this.#section = section
    }
    
    get section() {
        return this.#section
    }
    
    get languageService() {
        return languageService
    }
    
    get storageService() {
        return storageService
    }
    
    appendAddress() {
        const path = yojaWeb.path('/address.html')
        const body = yojaWeb.firstTag('body')
        return yojaWeb.append(body, path)
    }
    
}