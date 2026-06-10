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

graph.addLink('yyy', '444')
graph.addLink('zzz', '555')
graph.addLink('xxx', '777')

export function test_01() {
    const expectd = ["bbb",
                     "111",
                     "222",
                     "333",
                     "ccc",
                     "ddd",
                     "eee"]
    
    const actual = []
    graph.walkChildren('aaa', h => actual.push(h.node))
    ywAssert.assertArrayEquals(expectd, actual)
}

export function test_02() {
    const expectd = []

    const actual = []
    graph.walkParents('aaa', h => actual.push(h))
    ywAssert.assertArrayEquals(expectd, actual)
}

export function test_03() {
    ywAssert.assertTrue(graph.hasAncestor('333', 'bbb'))
}

export function test_04() {
    ywAssert.assertFalse(graph.hasAncestor('333', 'yyy'))
}

export function test_05() {
    ywAssert.assertTrue(graph.hasAncestor('333', 'aaa'))
}

export function test_06() {
    const expectd = ["444"]
    
    const actual = []
    graph.walkChildren('yyy', h => actual.push(h.node))
    ywAssert.assertArrayEquals(expectd, actual)
}

export function test_07() {
    const expectd = []

    const actual = []
    graph.walkParents('yyy', h => actual.push(h))
    ywAssert.assertArrayEquals(expectd, actual)
}

export function test_08() {
    const expectd = ["bbb",
                     "aaa"]

    const actual = []
    graph.walkParents('333', h => actual.push(h.node))
    ywAssert.assertArrayEquals(expectd, actual)
}

export function test_09() {
    ywAssert.assertFalse(graph.hasAncestor('aaa', 'bbb'))
}

export function test_10() {
    ywAssert.assertFalse(graph.hasAncestor('aaa', 'yyy'))
}

export function test_11() {
    ywAssert.assertFalse(graph.hasAncestor('aaa', 'aaa'))
}

export function test_12() {
    ywAssert.assertTrue(graph.hasAncestor('444', 'yyy'))
}

export function test_13() {
    ywAssert.assertFalse(graph.hasAncestor('555', 'yyy'))
}
