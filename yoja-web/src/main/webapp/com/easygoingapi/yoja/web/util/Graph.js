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
'use strict'


class RecursiveService {
    
    #graph;
    #recursiveKeys = [];
    #recursiveNodes = {};
    
    constructor(graph, fromNode) {
        this.#graph = graph;
        graph.walkChildren(fromNode, h => {
            if (h.recursive) {
                const key = graph.id(h.parent) + '__' + graph.id(h.node);
                this.#recursiveKeys.push(key);
                this.#recursiveNodes[key] = {parent: h.parent,
                                             node: h.node,
                                             parentPath: h.parentPath};
            }
        })
    }
    
    isRecursive(parentNode, childNode) {
        let result = false;
        const parentId = this.#graph.id(parentNode);
        const childId = this.#graph.id(childNode);
        if (parentId && childId) {
            result = this.#recursiveKeys.includes(parentId + '__' + childId);
        }
        return result;
    }
    
    isRecursiveNode(node) {
        let result = false;
        const nodeId = this.#graph.id(node);
        for (const recursiveNode of Object.values(this.#recursiveNodes)) {
            if (nodeId === this.#graph.id(recursiveNode.parent)) {
                result = true;
                break;
            }
        }
        return result;
    }
        
}

class Graph {
    
    #nodes = {};
    #nodeParents = {};
    #nodeChildren = {};
    
    constructor() {
        
    }
    
    nodes() {
        const ids = [];
        for (const id of Object.keys(this.#nodes)) {
            ids.push(id);
        }
        ids.sort((a, b) => a > b ? 1 : -1);
        const result = [];
        for (const id of ids) {
            result.push(this.#nodes[id]);
        }
        return result;
    }
    
    hasNode(node) {
        const id = this.id(node);
        return id && this.#nodes[id] ? true : false;
    }
    
    id(node) {
        let result = null;
        if (node) {
            if (this.#isString(node.id)) {
                result = node.id;
            }
            else if (this.#isString(node)) {
                result = node;
            }
        }
        return result;
    }
    
    #node(id) {
        return this.#nodes[id];
    }
    
    #isString(value) {
        return  value instanceof String || typeof value === 'string';
    }
    
    children(node) {
        const result = [];
        const id = this.id(node);
        if (id && this.#nodeChildren[id]) {
           for (const child of this.#nodeChildren[id]) {
               result.push(this.#node(child));
           }
        }
        return result;
    }
    
    parents(node) {
        const result = [];
        const id = this.id(node);
        if (id && this.#nodeParents[id]) {
            for (const parent of this.#nodeParents[id]) {
                result.push(this.#node(parent));
            }
        }
        return result;
    }
    
    addNode(node) {
        const nodeId = this.id(node);
        if (nodeId) {
            this.#nodes[nodeId] = node;
        }
    }

    addLink(nodeParent, nodeChild) {
        if (nodeParent && nodeChild) {
            const nodeChildId = this.id(nodeChild);
            const nodeParentId = this.id(nodeParent);
            
            if (nodeParent && nodeChild) {
                // nodes 
                if (!Object.keys(this.#nodes).includes(nodeChildId)) {
                    this.#nodes[nodeChildId] = nodeChild;
                }
                if (!Object.keys(this.#nodes).includes(nodeParentId)) {
                    this.#nodes[nodeParentId] = nodeParent;
                }
                // parents
                let nodeParents = this.#nodeParents[nodeChildId];
                if (!nodeParents) {
                    nodeParents = [];
                    this.#nodeParents[nodeChildId] = nodeParents;
                }
                if (!nodeParents.includes(nodeParentId)) {
                    nodeParents.push(nodeParentId);
                }
                // children
                let nodeChildren = this.#nodeChildren[nodeParentId]
                if (!nodeChildren) {
                    nodeChildren = [];
                    this.#nodeChildren[nodeParentId] = nodeChildren;
                }
                if (!nodeChildren.includes(nodeChildId)) {
                    nodeChildren.push(nodeChildId);
                }
            }
        }
    }
    
    walkChildren(node, consumer) {
        const nodeId = this.id(node);
        const parentPath = [];
        if (this.#isString(node)) {
            parentPath.push(this.#node(node));
        }
        else {
            parentPath.push(node);
        }
        this.#walkChildrenRecursively(nodeId, consumer, parentPath);
    }
    
    #walkChildrenRecursively(nodeId, consumer, parentPath) {
        const nodeChildrenIds = this.#nodeChildren[nodeId];
        if (nodeChildrenIds) {
            for (const nodeChildId of nodeChildrenIds) {
                const parentPathIds = [];
                for (const parent of parentPath) {
                    parentPathIds.push(this.id(parent));
                }
                const recursive = parentPathIds.includes(nodeChildId);
                const keepOn = consumer({parent: this.#node(nodeId),
                                         parentPath: parentPath,
                                         deep: parentPath.length,
                                         node: this.#node(nodeChildId),
                                         recursive: recursive});
                
                if (!recursive) {
                    if (this.#toBoolean(keepOn)) {
                        const _parentPath = [...parentPath];
                        _parentPath.push(this.#node(nodeChildId));
                        this.#walkChildrenRecursively(nodeChildId, consumer, _parentPath);
                    }
                }
//                else {
//                    const warnParentPath = [...parentPath]
//                    warnParentPath.push(this.#node(nodeChildId))
//                    console.warn('recursive parentPath: ', warnParentPath)
//                }
            }
        }
    }
    
    hasAncestor(node, ancestorNode) {
        let result = false;
        this.walkParents(node, h => {
            if (this.id(h.node) === this.id(ancestorNode)) {
                result = true;
                return false;
            }
        }) 
        return result;
    }
    
    isRecursive(node) {
        let result = false;
        const nodeId = this.id(node);
        this.walkParents(node, h => {
            if (this.id(h.node) === nodeId) {
                result = true;
                return false;
            }
        })
        return result;
    }
    
    walkParents(node, consumer) {
        const nodeId = this.id(node);
        const childPath = []
        if (this.#isString(node)) {
            childPath.push(this.#node(node));
        }
        else {
            childPath.push(node);
        }
        this.#walkParentsRecursively(nodeId, consumer, childPath);
    }

    #walkParentsRecursively(nodeId, consumer, childPath) {
        const nodeParentIds = this.#nodeParents[nodeId];
        if (nodeParentIds) {
            for (const nodeParentId of nodeParentIds) {
                const childPathIds = [];
                for (const child of childPath) {
                    childPathIds.push(this.id(child));
                }
                const recursive = childPathIds.includes(nodeParentId);
                const keepOn = consumer({child: this.#node(nodeId),
                                         childPath: childPath,
                                         deep: childPath.length,
                                         node: this.#node(nodeParentId),
                                         recursive: recursive});
                
                if (!recursive) {
                    if (this.#toBoolean(keepOn)) {
                        const _childPath = [...childPath];
                        _childPath.push(this.#node(nodeParentId));
                        this.#walkParentsRecursively(nodeParentId, consumer, _childPath);
                    }
                }
//                else {
//                    const warnCildPath = [...childPath]
//                    warnCildPath.push(this.#node(nodeParentId))
//                    console.warn('recursive childPath', warnCildPath)
//                }
            }
        }
    }
    
    #toBoolean(value) {
        let result = true;
        if (value === true || value === false) {
            result = value;
        }
        return result;
    }
    
    newRecursiveService(node) {
        return new RecursiveService(this, node);
    }
    
}

export function newInstance() {
    return new Graph();
}