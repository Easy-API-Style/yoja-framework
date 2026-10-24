'use strict';

const Graph = await import('/yoja/util/Graph.js');

// a link's own data: told with it when walked, given by links() -- a node linked twice keeps each link's
export function test_01() {
    const graph = Graph.newInstance();
    graph.addLink('a.css', {id: 'x.css', path: 'x.css'}, {media: 'screen'});
    graph.addLink('b.css', {id: 'x.css', path: 'x.css'}, {media: 'print'});
    graph.addLink('root', 'a.css');
    graph.addLink('root', 'b.css');
    ywAssert.assertEquals('screen', graph.links('a.css')[0].data.media);
    ywAssert.assertEquals('print', graph.links('b.css')[0].data.media);
    const told = [];
    graph.walkChildren('root', h => {
        told.push(graph.id(h.node) + ':' + (h.data ? h.data.media : '-'));
    });
    ywAssert.assertEquals('a.css:-,x.css:screen,b.css:-,x.css:print', told.join(','));
}

// a node with no string id is refused, not kept under "null"; any string is an id ("__proto__" too)
export function test_02() {
    const graph = Graph.newInstance();
    let refused = false;
    try {
        graph.addLink('a', {path: 'no id'});
    }
    catch (error) {
        refused = true;
    }
    ywAssert.assertEquals(true, refused);
    ywAssert.assertEquals(0, graph.nodes().length);
    graph.addLink('__proto__', 'constructor');
    ywAssert.assertEquals('constructor', graph.children('__proto__')[0]);
    ywAssert.assertEquals('__proto__', graph.parents('constructor')[0]);
}

// hasAncestor, isRecursive: up from the node, each node once
export function test_03() {
    const graph = Graph.newInstance();
    graph.addLink('a', 'b');
    graph.addLink('b', 'c');
    graph.addLink('c', 'a');
    graph.addLink('c', 'd');
    ywAssert.assertEquals(true, graph.hasAncestor('d', 'a'));
    ywAssert.assertEquals(false, graph.hasAncestor('a', 'd'));
    ywAssert.assertEquals(true, graph.isRecursive('b'));
    ywAssert.assertEquals(false, graph.isRecursive('d'));
}

// isRecursiveNode: the node that links back to one of its ancestors; a node's first object kept
export function test_04() {
    const graph = Graph.newInstance();
    graph.addLink('a', 'b');
    graph.addLink('b', 'a');
    const recursive = graph.newRecursiveService('a');
    ywAssert.assertEquals(true, recursive.isRecursive('b', 'a'));
    ywAssert.assertEquals(true, recursive.isRecursiveNode('b'));
    ywAssert.assertEquals(false, recursive.isRecursiveNode('a'));
    graph.addNode({id: 'n', value: 1});
    graph.addNode({id: 'n', value: 2});
    ywAssert.assertEquals(1, graph.nodes().find(node => graph.id(node) === 'n').value);
}
