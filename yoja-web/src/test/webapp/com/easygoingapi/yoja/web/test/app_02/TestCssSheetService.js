'use strict'

const tool = await import('/tool/externalService.js')

const $ = tool.jQuery
const cssSheetService = yojaWebApi.cssSheetService
const sectionService = yojaWebApi.sectionService

export function test_cssStyleSheet_01() {
    const properties = cssSheetService.getProperties('--theme-color')
    ywAssert.assertEquals(5, properties.length)
    
    const property_1 = properties[0]
    ywAssert.assertTrue(property_1.sheet instanceof CSSStyleSheet)
    ywAssert.assertTrue(property_1.cssRule instanceof CSSStyleRule)
    ywAssert.assertEquals('user-section', property_1.section.tag.getAttribute('class'))
    ywAssert.assertEquals([':host'], property_1.selectors)
    ywAssert.assertEquals('gray', property_1.getPropertyValue())
    
    cssSheetService.updateProperties('--theme-color', 'green')
    ywAssert.assertEquals('green', property_1.getPropertyValue())
    
    const section = sectionService.first(document, s => s.tag.getAttribute('class') === 'user-section')
    const actualColor = $(section.firstTag('label')).css('background-color')
    ywAssert.assertEquals('rgb(0, 128, 0)', actualColor)
}

export function test_cssStyleSheet_02() {
    const properties = cssSheetService.getProperties('--theme-color')
    ywAssert.assertEquals(5, properties.length)
    
    const property_1 = properties[4]
    ywAssert.assertTrue(property_1.sheet instanceof CSSStyleSheet)
    ywAssert.assertTrue(property_1.cssRule instanceof CSSStyleRule)
    ywAssert.assertEquals('article-section', property_1.section.tag.getAttribute('class'))
    ywAssert.assertEquals([':host'], property_1.selectors)
    ywAssert.assertEquals('green', property_1.getPropertyValue())
    
    cssSheetService.updateProperties('--theme-color', 'blue')
    ywAssert.assertEquals('blue', property_1.getPropertyValue())
    
    const userSection = sectionService.first(document, s => s.tag.getAttribute('class') === 'article-section')
    const actualUserColor = $(userSection.firstTag('label')).css('background-color')
    ywAssert.assertEquals('rgb(0, 0, 255)', actualUserColor)
    
    const addressSection = sectionService.first(document, s => s.tag.getAttribute('class') === 'address-section')
    const actualAddressColor = $(addressSection.firstTag('label')).css('background-color')
    ywAssert.assertEquals('rgb(0, 0, 255)', actualAddressColor)
}

export function test_cssStyleSheet_03() {
    const cssRules = []
    cssSheetService.walkCssRule(h => cssRules.push(h))
    ywAssert.assertEquals(38, cssRules.length)
    
}

export function test_cssStyleSheet_04() {
    const cssSheets = []
    cssSheetService.walkCssSheet(h => cssSheets.push(h))
    ywAssert.assertEquals(16, cssSheets.length)
}
