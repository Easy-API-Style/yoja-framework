'use strict'

export default class ArticleControler {

    #section;
    #label;
    #number;
    
    constructor(section) {
        this.#section = section
        this.#label = this.#section.firstTag("label")
        this.#number = this.#section.firstTag("input")
    }
    
    get section() {
        return this.#section
    }
    
    get label() {
        return this.#label.innerHTML
    }
    
    set label(value) {
        this.#label.innerHTML = value
    }
    
    get number() {
        return this.#number.value
    }
    
    set number(value) {
        this.#number.value = value
    }
    
}