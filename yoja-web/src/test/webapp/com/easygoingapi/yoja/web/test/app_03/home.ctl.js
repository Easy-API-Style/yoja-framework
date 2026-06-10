'use strict'

const languageService = yojaWebApi.languageService  
            
export default class HomeControler {

    #section;
    
    constructor(section) {
        this.#section = section
        languageService.setLanguage('fr')
    }
    
    get section() {
        return this.#section
    }
    
    get languageService() {
        return languageService
    }
    
}