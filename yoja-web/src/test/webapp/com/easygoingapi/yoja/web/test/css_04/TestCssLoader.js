'use strict'
  
const tool = await import('/tool/externalService.js')
const $ = tool.jQuery

const cssLoader = await import(yojaWeb.path('/yoja/loader/cssLoader.js'))

const load = await cssLoader.load(yojaWeb.path('/file_1.css'))

export function test_cssSheet_01() {
    ywAssert.assertEquals('/file_1.css', load.rootPath)
    ywAssert.assertEquals(3, load.sheetEntities.length)
    ywAssert.assertEquals('/file_3.css', load.sheetEntities[0].path)
    ywAssert.assertEquals(null, load.sheetEntities[0].media)
    ywAssert.assertEquals('/file_2.css', load.sheetEntities[1].path)
    ywAssert.assertEquals(null, load.sheetEntities[1].media)
    ywAssert.assertEquals('/file_1.css', load.sheetEntities[2].path)
    ywAssert.assertEquals(null, load.sheetEntities[2].media)
}

export function test_cssSheet_02() {
    ywAssert.assertEquals('rgb(0, 0, 255)', $(yojaWeb.firstTag('label')).css('background-color'))
}

