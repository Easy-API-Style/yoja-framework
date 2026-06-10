'use strict'
  
const cssLoader = await import(yojaWeb.path('/yoja/loader/cssLoader.js'))

const load = await cssLoader.load(yojaWeb.path('/home.css'))
//const split = await cssService.splitCss(yojaWeb.path('/home.css'))

export function test_cssSheet_01() {
    ywAssert.assertEquals('/home.css', load.rootPath)
    ywAssert.assertEquals(8, load.sheetEntities.length)
    
    ywAssert.assertEquals('screen    and (orientation: landscape)', load.sheetEntities[0].media)
    ywAssert.assertEquals('/main_1.css', load.sheetEntities[0].path)
    
    ywAssert.assertEquals('print', load.sheetEntities[1].media)
    ywAssert.assertEquals('/main_2.css', load.sheetEntities[1].path)
    
    ywAssert.assertEquals('speech', load.sheetEntities[2].media)
    ywAssert.assertEquals('/main_3.css', load.sheetEntities[2].path)
    
    ywAssert.assertEquals(null, load.sheetEntities[3].media)
    ywAssert.assertEquals('/main_4.css', load.sheetEntities[3].path)
    
    ywAssert.assertEquals('(orientation: landscape)', load.sheetEntities[4].media)
    ywAssert.assertEquals('/main_5.css', load.sheetEntities[4].path)
    
    ywAssert.assertEquals('screen', load.sheetEntities[5].media)
    ywAssert.assertEquals('/main_6.css', load.sheetEntities[5].path)
    
    ywAssert.assertEquals('screen                               and (orientation: landscape)', load.sheetEntities[6].media)
    ywAssert.assertEquals('/main_7.css', load.sheetEntities[6].path)
    
    ywAssert.assertEquals(null, load.sheetEntities[7].media)
    ywAssert.assertEquals('/home.css', load.sheetEntities[7].path)
}

//export function test_cssSheet_02() {
//    ywAssert.assertEquals(7, split.importStatements.length)
//    ywAssert.assertEquals(125, split.css.length)
//}
