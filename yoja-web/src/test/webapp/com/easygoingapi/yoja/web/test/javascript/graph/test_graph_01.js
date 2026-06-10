'use strict'

const Graph = await import('/yoja/util/Graph.js')

const graph = Graph.newInstance()

graph.addLink('aaa', 'bbb')
graph.addLink('aaa', 'ccc')
graph.addLink('aaa', 'ddd')
graph.addLink('aaa', 'eee')

graph.addLink('bbb', '111')
graph.addLink('bbb', '222')
graph.addLink('bbb', '333')

graph.addLink('ccc', 'yyy')
graph.addLink('ccc', 'zzz')
graph.addLink('ccc', 'xxx')

graph.addLink('xxx', 'aaa')


export function test_01() {
    const expected = [{"parent":"aaa","parentPath":["aaa"],"deep":1,"node":"bbb","recursive":false},
                      {"parent":"bbb","parentPath":["aaa","bbb"],"deep":2,"node":"111","recursive":false},
                      {"parent":"bbb","parentPath":["aaa","bbb"],"deep":2,"node":"222","recursive":false},
                      {"parent":"bbb","parentPath":["aaa","bbb"],"deep":2,"node":"333","recursive":false},
                      {"parent":"aaa","parentPath":["aaa"],"deep":1,"node":"ccc","recursive":false},
                      {"parent":"ccc","parentPath":["aaa","ccc"],"deep":2,"node":"yyy","recursive":false},
                      {"parent":"ccc","parentPath":["aaa","ccc"],"deep":2,"node":"zzz","recursive":false},
                      {"parent":"ccc","parentPath":["aaa","ccc"],"deep":2,"node":"xxx","recursive":false},
                      {"parent":"xxx","parentPath":["aaa","ccc","xxx"],"deep":3,"node":"aaa","recursive":true},
                      {"parent":"aaa","parentPath":["aaa"],"deep":1,"node":"ddd","recursive":false},
                      {"parent":"aaa","parentPath":["aaa"],"deep":1,"node":"eee","recursive":false}]
    const actual = []
    graph.walkChildren('aaa', h => actual.push(h))
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_02() {
    const expected = [{"child":"aaa","childPath":["aaa"],"deep":1,"node":"xxx","recursive":false},
                      {"child":"xxx","childPath":["aaa","xxx"],"deep":2,"node":"ccc","recursive":false},
                      {"child":"ccc","childPath":["aaa","xxx","ccc"],"deep":3,"node":"aaa","recursive":true}]

    const actual = []
    graph.walkParents('aaa', h => actual.push(h))
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_03() {
    ywAssert.assertTrue(graph.hasAncestor('xxx', 'aaa'))
}

export function test_04() {
    ywAssert.assertTrue(graph.hasAncestor('ccc', 'xxx'))
}

export function test_05() {
    ywAssert.assertTrue(graph.hasAncestor('111', 'ccc'))
}

export function test_06() {
    ywAssert.assertTrue(graph.isRecursive('aaa'))
}

export function test_07() {
    ywAssert.assertFalse(graph.isRecursive('333'))
}

export function test_08() {
    const expected = [{"parent":"xxx","parentPath":["xxx"],"deep":1,"node":"aaa","recursive":false},
                      {"parent":"aaa","parentPath":["xxx","aaa"],"deep":2,"node":"bbb","recursive":false},
                      {"parent":"bbb","parentPath":["xxx","aaa","bbb"],"deep":3,"node":"111","recursive":false},
                      {"parent":"bbb","parentPath":["xxx","aaa","bbb"],"deep":3,"node":"222","recursive":false},
                      {"parent":"bbb","parentPath":["xxx","aaa","bbb"],"deep":3,"node":"333","recursive":false},
                      {"parent":"aaa","parentPath":["xxx","aaa"],"deep":2,"node":"ccc","recursive":false},
                      {"parent":"ccc","parentPath":["xxx","aaa","ccc"],"deep":3,"node":"yyy","recursive":false},
                      {"parent":"ccc","parentPath":["xxx","aaa","ccc"],"deep":3,"node":"zzz","recursive":false},
                      {"parent":"ccc","parentPath":["xxx","aaa","ccc"],"deep":3,"node":"xxx","recursive":true},
                      {"parent":"aaa","parentPath":["xxx","aaa"],"deep":2,"node":"ddd","recursive":false},
                      {"parent":"aaa","parentPath":["xxx","aaa"],"deep":2,"node":"eee","recursive":false}]

    const actual = []
    graph.walkChildren('xxx', h => actual.push(h))
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_09() {
    const expected = ["bbb","ccc","ddd","eee"]

    const actual = graph.children('aaa')
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_10() {
    const expected = ["xxx"]

    const actual = graph.parents('aaa')
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_11() {
    const expected = ['bbb']

    const actual = graph.parents('222')
    ywAssert.assertArrayEquals(expected, actual)
}

