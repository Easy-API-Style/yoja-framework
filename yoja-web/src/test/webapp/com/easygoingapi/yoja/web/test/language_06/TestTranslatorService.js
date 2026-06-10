'use strict'

const languageService = yojaWebApi.languageService  
const translator = await languageService.loadTranslator(yojaWeb.path('/lang.xml'))

export function test_translator_01() {
    ywAssert.assertEquals('Nom De Famille', translator('familyName'))
    ywAssert.assertEquals('Nom De Famille', translator('familyName', 'fr'))
    ywAssert.assertEquals('Family Name', translator('familyName', 'en'))
}

export function test_translator_02() {
    ywAssert.assertEquals('Adresse', translator('address'))
    ywAssert.assertEquals('Adresse', translator('address', 'fr'))
    ywAssert.assertEquals('Address', translator('address', 'en'))
}

export function test_translator_03() {
    ywAssert.assertEquals(null, translator('address', 'be'))
}