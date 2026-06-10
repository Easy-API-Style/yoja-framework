'use strict'

const storageService = yojaWebApi.storageService 
const languageService = yojaWebApi.languageService  
            
export default class HomeControler {

    #section;
    
    constructor(section) {
        this.#section = section
        yojaWeb.onTagReady(tags => {
            // check event TagReady
            let tagReadyClassNames = storageService.getSessionItem('tagReadyClassNames')
            if (!tagReadyClassNames) {
                tagReadyClassNames = []
            }
            const className = tags[0].getAttribute("class")
            tagReadyClassNames.push(className)
            storageService.setSessionItem('tagReadyClassNames', tagReadyClassNames)
        })
        yojaWeb.onDocumentReady(() => {
            // check event DocumentReady
            const documentReadyDone = storageService.getLocalItem('documentReadyDone')
            if (documentReadyDone !== undefined) {
                storageService.setLocalItem('documentReadyDone', false)
            }
            else {
                storageService.setLocalItem('documentReadyDone', true)
            }
        })
    }
    
    get section() {
        return this.#section
    }
    
    get languageService() {
        return languageService
    }
    
}