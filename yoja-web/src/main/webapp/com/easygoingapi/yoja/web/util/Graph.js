/*
 * Copyright 2026 easy api <easy.api.contact@gmail.com>
 * https://easygoingapi.com
 * https://github.com/Easy-API-Style/yoja-framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

/**
 * The links of a graph that go back up its paths -- found once, from a node: a link parent -> child is one when
 * the child is on the path already walked to its parent (an inclusion that includes itself, at some depth).
 */
class RecursiveService {

    #graph;
    #recursiveLinks = new Set();      // 'parentId__childId'
    #recursiveParents = new Set();    // the ids of the parents of those links

    constructor(graph, fromNode) {
        this.#graph = graph;
        graph.walkChildren(fromNode, h => {
            if (h.recursive) {
                const parentId = graph.id(h.parent);
                this.#recursiveLinks.add(parentId + '__' + graph.id(h.node));
                this.#recursiveParents.add(parentId);
            }
        });
    }

    /** @return whether the link parentNode -> childNode goes back up a path */
    isRecursive(parentNode, childNode) {
        const parentId = this.#graph.id(parentNode);
        const childId = this.#graph.id(childNode);
        return Boolean(parentId && childId && this.#recursiveLinks.has(parentId + '__' + childId));
    }

    /**
     * @return whether one of the node's links goes back up a path: it is the one that links to one of its
     *         ancestors -- not merely a node on a cycle
     */
    isRecursiveNode(node) {
        return this.#recursiveParents.has(this.#graph.id(node));
    }

}

/**
 * A directed graph -- nodes and their links, parent -> child --, walked down from a node to its children or up to
 * its parents.
 *
 * A node is a string, its own id, or an object holding a string {@code id}; the first one given for an id is the
 * one kept. A link may carry data of its own ({@code addLink(parent, child, data)}: an import's media...), told
 * with it when walked.
 *
 * A walk tells every path: a node is told once for each path that leads to it, and a link that goes back up its
 * path is told {@code recursive}, not walked beyond. The consumer returning {@code false} does not walk below the
 * node told -- the others are walked all the same.
 */
class Graph {

    #nodes = new Map();          // id -> node
    #parents = new Map();        // id -> [parent ids], in the order linked
    #children = new Map();       // id -> [child ids], in the order linked
    #links = new Map();          // 'parentId__childId' -> the link's data

    /** @return the nodes, by their id's order */
    nodes() {
        return [...this.#nodes.keys()].sort((a, b) => a.localeCompare(b))
                                       .map(id => this.#nodes.get(id));
    }

    hasNode(node) {
        const id = this.id(node);
        return id !== null && this.#nodes.has(id);
    }

    /** @return the node's id: the node itself when it is a string, else its string {@code id}; null otherwise */
    id(node) {
        if (isString(node?.id)) {
            return String(node.id);
        }
        if (isString(node)) {
            return String(node);
        }
        return null;
    }

    children(node) {
        return (this.#children.get(this.id(node)) || []).map(id => this.#nodes.get(id));
    }

    parents(node) {
        return (this.#parents.get(this.id(node)) || []).map(id => this.#nodes.get(id));
    }

    /**
     * @return the links down from the node, in the order linked: [{node, data}] -- each child, and the data of
     *         its link
     */
    links(node) {
        const parentId = this.id(node);
        return (this.#children.get(parentId) || []).map(id => ({node: this.#nodes.get(id),
                                                                data: this.#links.get(parentId + '__' + id)}));
    }

    /** Adds a node -- its id's first node kept. @throws Error when it has no id */
    addNode(node) {
        this.#keep(node);
    }

    /**
     * Links a parent to a child -- each id's first node kept, a link added once (its data, the last given).
     *
     * @param data the link's own data, told with it when walked (optional)
     * @throws Error when one of them has no id
     */
    addLink(nodeParent, nodeChild, data) {
        if (!nodeParent || !nodeChild) {
            return;
        }
        this.#checkId(nodeParent);
        this.#checkId(nodeChild);   // both checked first: a link refused keeps neither of them
        const parentId = this.#keep(nodeParent);
        const childId = this.#keep(nodeChild);
        addOnce(this.#parents, childId, parentId);
        addOnce(this.#children, parentId, childId);
        if (data !== undefined) {
            this.#links.set(parentId + '__' + childId, data);
        }
    }

    /**
     * Walks down from the node, each path: consumer({parent, parentPath, deep, node, recursive, data}) -- data, the
     * link's, when it has some. It returning false: not below that node.
     */
    walkChildren(node, consumer) {
        const nodeId = this.id(node);
        this.#walkChildrenRecursively(nodeId, consumer, [isString(node) ? this.#nodes.get(nodeId) : node]);
    }

    #walkChildrenRecursively(nodeId, consumer, parentPath) {
        const pathIds = parentPath.map(parent => this.id(parent));
        for (const childId of this.#children.get(nodeId) || []) {
            const recursive = pathIds.includes(childId);
            const keepOn = consumer(told({parent: this.#nodes.get(nodeId),
                                          parentPath: [...parentPath],   // its own: a consumer changing it changes no other
                                          deep: parentPath.length,
                                          node: this.#nodes.get(childId),
                                          recursive: recursive},
                                         this.#links.get(nodeId + '__' + childId)));
            if (!recursive && keepOn !== false) {
                this.#walkChildrenRecursively(childId, consumer, [...parentPath, this.#nodes.get(childId)]);
            }
        }
    }

    /**
     * Walks up from the node, each path: consumer({child, childPath, deep, node, recursive, data}) -- data, the
     * link's, when it has some. It returning false: not above that node.
     */
    walkParents(node, consumer) {
        const nodeId = this.id(node);
        this.#walkParentsRecursively(nodeId, consumer, [isString(node) ? this.#nodes.get(nodeId) : node]);
    }

    #walkParentsRecursively(nodeId, consumer, childPath) {
        const pathIds = childPath.map(child => this.id(child));
        for (const parentId of this.#parents.get(nodeId) || []) {
            const recursive = pathIds.includes(parentId);
            const keepOn = consumer(told({child: this.#nodes.get(nodeId),
                                          childPath: [...childPath],
                                          deep: childPath.length,
                                          node: this.#nodes.get(parentId),
                                          recursive: recursive},
                                         this.#links.get(parentId + '__' + nodeId)));
            if (!recursive && keepOn !== false) {
                this.#walkParentsRecursively(parentId, consumer, [...childPath, this.#nodes.get(parentId)]);
            }
        }
    }

    /** @return whether the ancestor is up from the node -- looked for until found, each node once */
    hasAncestor(node, ancestorNode) {
        const ancestorId = this.id(ancestorNode);
        return ancestorId !== null && this.#reachesUp(this.id(node), id => id === ancestorId);
    }

    /** @return whether the node is up from itself: on a cycle -- looked for until found, each node once */
    isRecursive(node) {
        const nodeId = this.id(node);
        return nodeId !== null && this.#reachesUp(nodeId, id => id === nodeId);
    }

    /** @return whether a node up from the one given -- not itself -- is one the predicate is true of */
    #reachesUp(fromId, predicate) {
        const seen = new Set();
        const toVisit = [...(this.#parents.get(fromId) || [])];
        while (toVisit.length) {
            const id = toVisit.pop();
            if (predicate(id)) {
                return true;
            }
            if (!seen.has(id)) {
                seen.add(id);
                toVisit.push(...(this.#parents.get(id) || []));
            }
        }
        return false;
    }

    newRecursiveService(node) {
        return new RecursiveService(this, node);
    }

    /** @return the node's id @throws Error when it has none */
    #checkId(node) {
        const id = this.id(node);
        if (id === null) {
            throw new Error('a node of a graph is a string, or holds a string id: ' + JSON.stringify(node));
        }
        return id;
    }

    /** @return the node's id, its first node kept @throws Error when it has none */
    #keep(node) {
        const id = this.#checkId(node);
        if (!this.#nodes.has(id)) {
            this.#nodes.set(id, node);
        }
        return id;
    }

}

function isString(value) {
    return value instanceof String || typeof value === 'string';
}

/** Adds a value to a key's list, once. */
function addOnce(map, key, value) {
    const values = map.get(key) || [];
    if (!values.includes(value)) {
        values.push(value);
    }
    map.set(key, values);
}

/** @return what a walk tells -- the link's data with it, when it has some */
function told(told, data) {
    if (data !== undefined) {
        told.data = data;
    }
    return told;
}

export function newInstance() {
    return new Graph();
}
