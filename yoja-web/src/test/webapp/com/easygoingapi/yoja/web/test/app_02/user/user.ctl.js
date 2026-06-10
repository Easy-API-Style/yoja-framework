'use strict'
            
export default class UserControler {

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
    
    #addOrderTag() {
        const tag = this.#section.tag
        const path = import.meta.resolve(yojaWeb.path('../order/order.cpt.html'))
        return yojaWeb.append(tag, path)
    }
    
    #addArticleTag(label, number) {
        const tag = this.#section.deepFirstTag('.order-section fieldset')
        const path = import.meta.resolve(yojaWeb.path('../order/article/article.cpt.html'))
        return yojaWeb.append(tag, path)
                      .then(tags => {
                         const div = tags[0]
                         yojaWeb.firstTag('label', div).innerHTML = label
                         yojaWeb.firstTag('input', div).value = number
                         return div
                      })
    }
    
    addArticle(label, number) {
        if (!this.#section.deepFirstTag('.order-section')) {
            return this.#addOrderTag()
                       .then(() => this.#addArticleTag(label, number))
        }
        else {
            return this.#addArticleTag(label, number)
        }
    }
    
}