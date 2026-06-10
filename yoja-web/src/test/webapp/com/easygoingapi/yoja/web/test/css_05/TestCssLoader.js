'use strict'
  
const tool = await import('/tool/externalService.js')
const $ = tool.jQuery

const cssLoader = await import(yojaWeb.path('/yoja/loader/cssLoader.js'))

const load = await cssLoader.load(yojaWeb.path('/file_1.css'))

export function test_cssSheet_01() {
    ywAssert.assertEquals(8, load.sheetEntities.length)
    
    const expectedOrder = [
        '/css_2/file_4.css',
        '/css_2/file_5.css',
        '/css_1/file_2.css',
        '/css_3/file_6.css',
        '/file_8.css',
        '/css_3/file_7.css',
        '/css_1/file_3.css', 
        '/file_1.css']
        
    const actualOrder = []
    for (const sheetEntity of load.sheetEntities) {
        actualOrder.push(sheetEntity.path)
    }
    ywAssert.assertEquals('/file_1.css', load.rootPath)
    ywAssert.assertArrayEquals(expectedOrder, actualOrder)
}

export function test_cssSheet_02() {
    ywAssert.assertEquals('rgb(255, 255, 0)', $(yojaWeb.firstTag('h1')).css('background-color'))
    ywAssert.assertEquals('rgb(0, 0, 255)', $(yojaWeb.firstTag('h2')).css('background-color'))
    ywAssert.assertEquals('rgb(255, 192, 203)', $(yojaWeb.firstTag('h3')).css('background-color'))
    ywAssert.assertEquals('rgb(0, 255, 0)', $(yojaWeb.firstTag('h4')).css('background-color'))
    ywAssert.assertEquals('rgb(0, 128, 0)', $(yojaWeb.firstTag('h5')).css('background-color'))
    ywAssert.assertEquals('rgb(255, 165, 0)', $(yojaWeb.firstTag('h6')).css('background-color'))
    ywAssert.assertEquals('rgb(255, 0, 0)', $(yojaWeb.firstTag('h7')).css('background-color'))
    ywAssert.assertEquals('rgb(165, 42, 42)', $(yojaWeb.firstTag('h8')).css('background-color'))
}
