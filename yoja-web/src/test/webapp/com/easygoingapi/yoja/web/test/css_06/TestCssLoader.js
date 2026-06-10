'use strict'
  
const tool = await import('/tool/externalService.js')
const $ = tool.jQuery

const cssLoader = await import(yojaWeb.path('/yoja/loader/cssLoader.js'))

const load = await cssLoader.load(yojaWeb.path('/file_1.css'))

export function test_cssSheet_01() {
    ywAssert.assertEquals(4, load.sheetEntities.length)
       
       const expectedOrder = [
           '/file_4.css',
           '/file_3.css',
           '/file_2.css', 
           '/file_1.css']
           
       const actualOrder = []
       for (const sheetEntity of load.sheetEntities) {
           actualOrder.push(sheetEntity.path)
       }
       ywAssert.assertEquals('/file_1.css', load.rootPath)
       ywAssert.assertArrayEquals(expectedOrder, actualOrder)
}

export function test_cssSheet_02() {
    ywAssert.assertEquals('rgb(0, 0, 255)', $(yojaWeb.firstTag('h1')).css('background-color'))
}

