'use strict'

const storageService = yojaWebApi.storageService 

export function test_01() {
    storageService.setLocalItem('aaa', 1001)
    const value = storageService.getLocalItem('aaa')
    ywAssert.assertTrue(1001 === value)
}

export function test_02() {
    storageService.setLocalItem('bbbxxx', 1002)
    const value = storageService.getLocalItem('bbbxxx')
    ywAssert.assertTrue(1002 === value)
}

export function test_03() {
    storageService.setLocalItem('ccc', 1002)
    const value = storageService.getLocalItem('ccc')
    ywAssert.assertTrue(1002 === value)
}

export function test_04() {
    const values = storageService.getLocalItemKeys()
    ywAssert.assertArrayEquals(['aaa', 'bbbxxx', 'ccc'], values)
}

export function test_05() {
    const values = storageService.getLocalItemKeys('aaa')
    ywAssert.assertArrayEquals(['aaa'], values)
}

export function test_06() {
    const values = storageService.getLocalItemKeys('aa')
    ywAssert.assertArrayEquals([], values)
}

export function test_07() {
    const values = storageService.getLocalItemKeys({startsWith: 'bbb'})
    ywAssert.assertArrayEquals(['bbbxxx'], values)
}

export function test_08() {
    const values = storageService.getLocalItemKeys({endsWith: 'xxx'})
    ywAssert.assertArrayEquals(['bbbxxx'], values)
}

export function test_09() {
    const values = storageService.getLocalItemKeys(/c{3}/g)
    ywAssert.assertArrayEquals(['ccc'], values)
}

export function test_10() {
    const values = storageService.getLocalItemKeys(new RegExp('c{3}'))
    ywAssert.assertArrayEquals(['ccc'], values)
}

export function test_11() {
    const values = storageService.getLocalItemKeys({endsWith: 'xxx', matches: 'c{3}'})
    ywAssert.assertArrayEquals(['bbbxxx', 'ccc'], values)
}

export function test_12() {
    const values = storageService.getLocalItemKeys({endsWith: 'xxx', matches: new RegExp('c{3}')})
    ywAssert.assertArrayEquals(['bbbxxx', 'ccc'], values)
}

export function test_13() {
    const values = storageService.getLocalItemKeys({contains: 'x', equals: ['ccc']})
    ywAssert.assertArrayEquals(['bbbxxx', 'ccc'], values)
}

export function test_14() {
    const values = storageService.getLocalItemKeys()
    ywAssert.assertArrayEquals(['aaa', 'bbbxxx', 'ccc'], values)
}

export function test_15() {
    storageService.setLocalItem('bbb', new Date())
    const values = storageService.getLocalItemKeys()
    ywAssert.assertArrayEquals(['aaa', 'bbb', 'bbbxxx', 'ccc'], values)
}



