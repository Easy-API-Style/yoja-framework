'use strict'
const languageService = yojaWebApi.languageService  
const storageService = yojaWebApi.storageService 

export default class AddressControler {

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
    
}