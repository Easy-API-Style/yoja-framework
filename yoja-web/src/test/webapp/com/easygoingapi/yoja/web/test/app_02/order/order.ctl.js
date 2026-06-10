'use strict'

export default class OrderControler {

    #section;
    #order;
    
    constructor(section) {
        this.#section = section
        this.#order = this.#section.firstTag("fieldset")
    }
    
    get section() {
        return this.#section
    }
    
    getArticles() {
        return null
    }

    addArticle(label, number) {
        
    }
    
}