'use strict'

const languageLoader = await import(yojaWeb.path('/yoja/loader/languageLoader.js'))

const languageEntity = await languageLoader.load(yojaWeb.path('/lang_01.xml'))

export function test_01() {
    
    const firstName = languageEntity.xml.querySelectorAll('[key="firstName"]')
    const lastIndex = firstName.length - 1
    const firstName_fr = firstName[lastIndex].querySelector('fr')
    const firstName_en = firstName[lastIndex].querySelector('en')
    
    ywAssert.assertEquals('Prénom', firstName_fr.innerHTML)
    ywAssert.assertEquals('First Name', firstName_en.innerHTML)
}

export function test_02() {
    const expectedOrder = ["/lang_04.xml",
                           "/lang_02.xml",
                           "/lang_04.xml",
                           "/lang_02.xml",
                           "/lang_05.xml",
                           "/lang_06.xml",
                           "/lang_03.xml",
                           "/lang_01.xml"]
    ywAssert.assertEquals(expectedOrder, languageEntity.paths)
}

export function test_03() {
    const firstName = languageEntity.xml.querySelectorAll('[key="postcode"]')
    const lastIndex = firstName.length - 1
    const firstName_fr = firstName[lastIndex].querySelector('fr')
    const firstName_en = firstName[lastIndex].querySelector('en')
    
    ywAssert.assertEquals('Code Postal', firstName_fr.innerHTML)
    ywAssert.assertEquals('Postcode', firstName_en.innerHTML)
}

export function test_04() {
    const firstName = languageEntity.xml.querySelectorAll('[key="city"]')
    const lastIndex = firstName.length - 1
    const firstName_fr = firstName[lastIndex].querySelector('fr')
    const firstName_en = firstName[lastIndex].querySelector('en')
    
    ywAssert.assertEquals('Ville', firstName_fr.innerHTML)
    ywAssert.assertEquals('City', firstName_en.innerHTML)
}
