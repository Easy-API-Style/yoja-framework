'use strict'
const languageService = yojaWebApi.languageService  
const storageService = yojaWebApi.storageService 

export default class HomeControler {

    #section;
    #languageLoaded = false
    #languageEvents = [];
    
    constructor(section) {
        this.#section = section
        languageService.onLanguageChange(h => {
          this.#languageLoaded = true
        })  
        languageService.onLanguageTranslate(h => {
            this.#languageEvents.push(JSON.stringify({tag: h.tag.tagName,
                                                      value: h.value,
                                                      attribute: h.attribute,
                                                      type: h.type,
                                                      section: h.section.tag.tagName}))
            if ('Nom De Famille' === h.value
                  && h.key === 'familyName') {
                return 'Nom'
            }
        })
    }
    
    get section() {
        return this.#section
    }
    
    get storageService() {
        return storageService
    }
    
    get languageEvents() {
        return this.#languageEvents
    }

    get languageLoaded() {
        return  this.#languageLoaded
    }
    
}