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

graph.addLink('bbb', '111')
graph.addLink('bbb', '222')
graph.addLink('bbb', '333')

graph.addLink('ccc', 'yyy')
graph.addLink('ccc', 'zzz')
graph.addLink('ccc', 'xxx')

graph.addLink('xxx', 'aaa')
graph.addLink('xxx', 'aaa')

export function test_01() {
    const expectd = ["bbb",
                     "111",
                     "222",
                     "333",
                     "ccc",
                     "yyy",
                     "zzz",
                     "xxx",
                     "aaa",
                     "ddd",
                     "eee"]
    
    const actual = []
    graph.walkChildren('aaa', h => actual.push(h.node))
    ywAssert.assertArrayEquals(expectd, actual)
}

export function test_02() {
    const expectd = ["xxx",
                     "ccc",
                     "aaa"]

    const actual = []
    graph.walkParents('aaa', h => actual.push(h.node))
    ywAssert.assertArrayEquals(expectd, actual)
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
