'use strict'

const tool = await import('/tool/externalService.js')

const $ = tool.jQuery
const sectionService = yojaWebApi.sectionService
const cssSheetService = yojaWebApi.cssSheetService

export function test_cssStyleSheet_01() {
    const properties = cssSheetService.getProperties('--theme-color')
    
    ywAssert.assertEquals(5, properties.length)
    
    const property_1 = properties[0]
    ywAssert.assertTrue(property_1.sheet instanceof CSSStyleSheet)
    ywAssert.assertTrue(property_1.cssRule instanceof CSSStyleRule)
    ywAssert.assertEquals('user-section', property_1.section.tag.getAttribute('class'))
    ywAssert.assertEquals([':host(*)'], property_1.selectors)
    ywAssert.assertEquals('gray', property_1.getPropertyValue())
    
    cssSheetService.updateProperties('--theme-color', 'green')
    ywAssert.assertEquals('green', property_1.getPropertyValue())
    
    const section = sectionService.first(document, s => s.tag.getAttribute('class') === 'user-section')
    const actualColor = $(section.firstTag('label')).css('background-color')
    ywAssert.assertEquals('rgb(0, 128, 0)', actualColor)
}

export function test_cssStyleSheet_02() {
    const properties = cssSheetService.getProperties('--label-color')
    ywAssert.assertEquals(1, properties.length)
    
    const property_1 = properties[0]
    ywAssert.assertTrue(property_1.sheet instanceof CSSStyleSheet)
    ywAssert.assertTrue(property_1.cssRule instanceof CSSStyleRule)
    ywAssert.assertEquals('address-section', property_1.section.tag.getAttribute('class'))
    ywAssert.assertEquals(['label', 'input'], property_1.selectors)
    ywAssert.assertEquals('pink', property_1.getPropertyValue())
    
    cssSheetService.updateProperties('--label-color', 'blue')
    ywAssert.assertEquals('blue', property_1.getPropertyValue())
    
    const userSection = sectionService.first(document, s => s.tag.getAttribute('class') === 'user-section')
    const actualUserColor = $(userSection.firstTag('label')).css('background-color')
    ywAssert.assertEquals('rgb(0, 128, 0)', actualUserColor)
    
    const addressSection = sectionService.first(document, s => s.tag.getAttribute('class') === 'address-section')
    const actualAddressColor = $(addressSection.firstTag('label')).css('background-color')
    ywAssert.assertEquals('rgb(0, 0, 255)', actualAddressColor)
}

export function test_cssStyleSheet_03() {
    const cssRules = []
    cssSheetService.walkCssRule(h => cssRules.push(h))
    ywAssert.assertEquals(40, cssRules.length)
    
}

export function test_cssStyleSheet_04() {
    const cssSheets = []
    cssSheetService.walkCssSheet(h => cssSheets.push(h))
    ywAssert.assertEquals(11, cssSheets.length)
}

