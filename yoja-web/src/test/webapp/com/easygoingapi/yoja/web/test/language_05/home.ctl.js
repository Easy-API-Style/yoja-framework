'use strict'
const storageService = yojaWebApi.storageService 

export default class HomeControler {

    #section;
    
    constructor(section) {
        this.#section = section
    }
    
    get section() {
        return this.#section
    }
    
    get storageService() {
        return storageService
    }
    
}