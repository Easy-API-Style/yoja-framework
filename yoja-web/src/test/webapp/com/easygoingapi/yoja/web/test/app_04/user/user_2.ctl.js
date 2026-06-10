'use strict'
            
export default class UserControler_2 {

    #section;
    #familyName;
    #firstName;
    
    #street
    #city;
    #postcode;
    
    constructor(section) {
        this.#section = section
        this.#familyName = this.#section.firstTag("input[name=familyName]")
        this.#firstName = this.#section.firstTag("input[name=firstName]")
        
        this.#street = this.#section.deepFirstTag("input[name=street]")
        this.#city = this.#section.deepFirstTag("input[name=city]")
        this.#postcode = this.#section.deepFirstTag("input[name=postcode]")
    }
    
    get section() {
        return this.#section
    }
    
    get languageService() {
        return languageService
    }
    
    get familyName() {
        return this.#familyName.value
    }
    
    set familyName(value) {
        this.#familyName.value = value
    }
    
    get firstName() {
        return this.#firstName.value
    }
    
    set firstName(value) {
        this.#firstName.value = value
    }
    
    get street() {
        return this.#street.value
    }

    set street(value) {
        this.#street.value = value
    }
    
    get city() {
        return this.#city.value
    }
    
    set city(value) {
        this.#city.value = value
    }
    
    get postcode() {
        return this.#postcode.value
    }
    
    set postcode(value) {
        this.#postcode.value = value
    }
    
}