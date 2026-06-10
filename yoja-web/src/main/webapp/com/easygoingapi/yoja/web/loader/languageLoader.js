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

const httpClient = await import(yojaWeb.path('../api/httpClient.js'));
const Graph = await import(yojaWeb.path('../util/Graph.js'));
const domUtil = await import(yojaWeb.path('../util/domUtil.js'));

class LanguageLoader {
    
    // node: path 
    #fileGraph = Graph.newInstance();
    // key: constains path; 
    // value: constains xml document
    #xmlDocuments = {};
    // list of loaded xml paths
    #loadedXmlFiles = [];
    
    constructor() {
    }
    
    get xmlDocuments() {
        return this.#xmlDocuments;
    }

    #importPaths(xmlPath) {
        const result = [];
        result.push(xmlPath);
        const recursiveService = this.#fileGraph.newRecursiveService(xmlPath);
        this.#importPathsRecursively(recursiveService, xmlPath, result);
        result.reverse();
        return result;
    }

    #importPathsRecursively(recursiveService, path, result) {
        if (this.#fileGraph.hasNode(path)) {
            const childrenPaths = [...this.#fileGraph.children(path)];
            childrenPaths.reverse();
            for (const childPath of childrenPaths) {
                if (!recursiveService.isRecursive(path, childPath)) {
                    result.push(childPath);
                    this.#importPathsRecursively(recursiveService, childPath, result);
                }
            }
        }
    }
    
    #parentFolder(path) {
        const splitPath = path.split('/');
        const pathAsArray = [];
        for (let i = 0; i < (splitPath.length - 1); i++) {
            pathAsArray.push(splitPath[i]);
        }
        return pathAsArray.join('/');
    }

    #toImportPaths(parentPath, importPaths) {
        const result = [];
        for (const importPath of importPaths) {
              let path = importPath;
              if (!path.startsWith('/')) {
                  path = parentPath + '/' + path;
              }
              const url = new URL(import.meta.resolve(path));
              path = url.pathname;
              if (url.search) {
                  path = path + url.search;
              }
              result.push(path);
        }
        return result;
    }
    
    #merge(doms) {
        const ywLanguageTag = domUtil.parseXml('<yw-language></yw-language>')
                                     .querySelector('yw-language');
        for (const dom of doms) {
            const ywTextTags = dom.querySelectorAll('yw-text');
            ywTextTags.forEach(ywTextTag => ywLanguageTag.appendChild(ywTextTag));
        }
        return ywLanguageTag;
    }

    searchImport(xmlAsString) {
        const importPaths = [];
        let xmlAsDom = null;
        if (xmlAsString && xmlAsString.trim()) {
            xmlAsDom = domUtil.parseXml(xmlAsString);
            const importTags = xmlAsDom.querySelectorAll('yw-import');
            if (importTags) {
                for (const importTag of importTags) {
                    const path = importTag.getAttribute('path');
                    importTag.remove();
                    if (path) {
                        importPaths.push(path);
                    }
                }
            }
        }
        return {importPaths: importPaths,
                dom: domUtil.serializeXml(xmlAsDom)};
    }
    
   async load(xmlPath) {
        return this.#loadXmlFileRecursively(xmlPath).then(() => {
            const xmlPaths = this.#importPaths(xmlPath);
            const doms = [];
            for (const path of xmlPaths) {
                doms.push(domUtil.parseXml(this.#xmlDocuments[path]));
            }
            return {
                xml: this.#merge(doms),
                graph: this.#fileGraph,
                paths: xmlPaths,
                rootPath: xmlPath
            };
        });
    }

    #loadChildrenXmlFiles(xmlPath, childrenXmlPaths) {
        const futures = [];
        for (const childXmlPath of childrenXmlPaths) {
            if (!this.#loadedXmlFiles.includes(childXmlPath)) {
                this.#fileGraph.addLink(xmlPath, childXmlPath);
                futures.push(this.#loadXmlFileRecursively(childXmlPath));
            }
            else {
                this.#fileGraph.addLink(xmlPath, childXmlPath);
            }
        }
        return Promise.all(futures);
    }

    #loadXmlFileRecursively(xmlPath) {
       return new Promise((resolve, reject) => {
        this.#loadXmlFile(xmlPath)
            .then(childrenXmlPaths => {
                this.#loadChildrenXmlFiles(xmlPath, childrenXmlPaths)
                    .then(() => resolve())
                    .catch(e => reject(e));
            })
            .catch(e => reject(e));
       });
    }

    #loadXmlFile(xmlPath) {
        const formatedXmlPath = yojaWeb.path(xmlPath);
        return httpClient.get({url: formatedXmlPath, fetchAs: 'text'})
                         .then(xmlResponse => {
                              if (xmlResponse.status === 200) {
                                  this.#loadedXmlFiles.push(xmlPath);
                                  const searchResult = this.searchImport(xmlResponse.body);
                                  this.#xmlDocuments[xmlPath] = searchResult.dom;
                                  const childrenXmlPaths = this.#toImportPaths(this.#parentFolder(xmlPath),
                                                                               searchResult.importPaths);
                                  return childrenXmlPaths;
                              }
                              else {
                                  throw new Error('load yojaWeb xml failed: [' + xmlResponse.status + '] ' + xmlPath, 
                                                   {xmlPath: xmlPath,
                                                    httpStatus: xmlResponse.status,
                                                    httpBody: xmlResponse.body});
                              }
                          })
                          .catch(error => { 
                              console.error('load yojaWeb xml failed: ' + xmlPath, {cause: error})
                              throw new Error('load yojaWeb xml failed: ' + xmlPath, {cause: error});
                          });
    }
    
}

const languageLoader = new LanguageLoader();

export function load(path) {
    return languageLoader.load(path);
} 
