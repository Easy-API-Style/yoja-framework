'use strict'

const Graph = await import('/yoja/util/Graph.js')

const graph = Graph.newInstance()

graph.addLink({id: 'aaa', value: 'hello'}, {id: 'bbb', value: 'hello'})
graph.addLink({id: 'aaa', value: 'hello'}, {id: 'bbb', value: 'hello'})
graph.addLink({id: 'aaa', value: 'hello'}, {id: 'ccc', value: 'hello'})
graph.addLink({id: 'aaa', value: 'hello'}, {id: 'ddd', value: 'hello'})
graph.addLink({id: 'aaa', value: 'hello'}, {id: 'eee', value: 'hello'})

graph.addLink({id: 'bbb', value: 'hello'}, {id: '111', value: 'hello'})
graph.addLink({id: 'bbb', value: 'hello'}, {id: '222', value: 'hello'})
graph.addLink({id: 'bbb', value: 'hello'}, {id: '333', value: 'hello'})

graph.addLink({id: 'yyy', value: 'hello'}, {id: '444', value: 'hello'})
graph.addLink({id: 'zzz', value: 'hello'}, {id: '555', value: 'hello'})
graph.addLink({id: 'xxx', value: 'hello'}, {id: '666', value: 'hello'})

export function test_01() {
    const expected = [{"parent":{"id":"aaa","value":"hello"},"parentPath":[{"id":"aaa","value":"hello"}],"deep":1,"node":{"id":"bbb","value":"hello"},"recursive":false},
                      {"parent":{"id":"bbb","value":"hello"},"parentPath":[{"id":"aaa","value":"hello"},{"id":"bbb","value":"hello"}],"deep":2,"node":{"id":"111","value":"hello"},"recursive":false},
                      {"parent":{"id":"bbb","value":"hello"},"parentPath":[{"id":"aaa","value":"hello"},{"id":"bbb","value":"hello"}],"deep":2,"node":{"id":"222","value":"hello"},"recursive":false},
                      {"parent":{"id":"bbb","value":"hello"},"parentPath":[{"id":"aaa","value":"hello"},{"id":"bbb","value":"hello"}],"deep":2,"node":{"id":"333","value":"hello"},"recursive":false},
                      {"parent":{"id":"aaa","value":"hello"},"parentPath":[{"id":"aaa","value":"hello"}],"deep":1,"node":{"id":"ccc","value":"hello"},"recursive":false},
                      {"parent":{"id":"aaa","value":"hello"},"parentPath":[{"id":"aaa","value":"hello"}],"deep":1,"node":{"id":"ddd","value":"hello"},"recursive":false},
                      {"parent":{"id":"aaa","value":"hello"},"parentPath":[{"id":"aaa","value":"hello"}],"deep":1,"node":{"id":"eee","value":"hello"},"recursive":false}]
 
    const actual = []
    graph.walkChildren('aaa', h => actual.push(h))
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_02() {
    const expected = []

    const actual = []
    graph.walkParents('aaa', h => actual.push(h))
    ywAssert.assertArrayEquals(expected, actual)
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
    const expected = [{"parent":{"id":"yyy","value":"hello"},
                      "parentPath":[{"id":"yyy","value":"hello"}],
                      "deep":1,
                      "node":{"id":"444","value":"hello"},
                      "recursive":false}]
    
    const actual = []
    graph.walkChildren('yyy', h => actual.push(h))
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_07() {
    const expected = []

    const actual = []
    graph.walkParents('yyy', h => actual.push(h))
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_08() {
    const expected = [{id: 'bbb', value: 'hello'},
                     {id: 'aaa', value: 'hello'}]

    const actual = []
    graph.walkParents('333', h => actual.push(h.node))
    ywAssert.assertArrayEquals(expected, actual)
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

export function test_14() {
    const expected = [{"id":"bbb","value":"hello"},
                      {"id":"ccc","value":"hello"},
                      {"id":"ddd","value":"hello"},
                      {"id":"eee","value":"hello"}]
                      
    const actual = graph.children('aaa')
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_15() {
    const expected = []
    const actual = graph.parents('aaa')
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_16() {
    const expected = [{"id":"bbb","value":"hello"}]
    const actual = graph.parents('222')
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_17() {
    const expected = [{"id":"111","value":"hello"},
                      {"id":"222","value":"hello"},
                      {"id":"333","value":"hello"},
                      {"id":"444","value":"hello"},
                      {"id":"555","value":"hello"},
                      {"id":"666","value":"hello"},
                      {"id":"aaa","value":"hello"},
                      {"id":"bbb","value":"hello"},
                      {"id":"ccc","value":"hello"},
                      {"id":"ddd","value":"hello"},
                      {"id":"eee","value":"hello"},
                      {"id":"xxx","value":"hello"},
                      {"id":"yyy","value":"hello"},
                      {"id":"zzz","value":"hello"}]
    const actual = graph.nodes()
    ywAssert.assertArrayEquals(expected, actual)
}

export function test_18() {
    ywAssert.assertTrue(graph.hasNode("bbb"))
}

export function test_19() {
    ywAssert.assertTrue(graph.hasNode({"id":"ddd","value":"hello"}))
}

export function test_20() {
    ywAssert.assertFalse(graph.hasNode({"id":"OOO","value":"hello"}))
}



