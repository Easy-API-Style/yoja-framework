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

class CssLoader {
    
    // node.id: path 
    // node: contains imports paths with media rules: [{id: '...', path: '...', media: '...'})]
    #fileGraph = Graph.newInstance();
    // key: constains css path; 
    // value: constains CSSStyleSheet object
    #cssStyleSheets = {};
    // list of loaded css paths
    #loadedCssFiles = [];
    
    constructor() {
    }
    
    get cssStyleSheets() {
        return this.#cssStyleSheets;
    }
    
    #importPaths(cssPath) {
        const result = [];
        result.push({path: cssPath, media: null});
        const recursiveService = this.#fileGraph.newRecursiveService(cssPath);
        this.#importPathsRecursively(recursiveService, cssPath, result);
        result.reverse();
        return result;
    }

    #importPathsRecursively(recursiveService, path, result) {
        if (this.#fileGraph.hasNode(path)) {
            const children = [...this.#fileGraph.children(path)];
            children.reverse();
            for (const child of children) {
                if (!recursiveService.isRecursive(path, child)) {
                    result.push({path: child.path, media: child.media});
                    this.#importPathsRecursively(recursiveService, child.path, result);
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

    #toImportPaths(parentPath, importStatements) {
        const result = [];
        for (const importStatement of importStatements) {
              let path = importStatement.path;
              if (!path.startsWith('/')) {
                  path = parentPath + '/' + path;
              }
              const url = new URL(import.meta.resolve(path));
              path = url.pathname;
              if (url.search) {
                  path = path + url.search;
              }
              result.push({path: path,
                           media: importStatement.media});
        }
        return result
    }

    searchCssImport(css) {
        const importStatements = [];
        if (css && css.trim()) {
           // remove comments
           css = css.replace(/(\/\*)[^]*?(\*\/)/g, ' ');
           // import statements
           const importFullStatements = css.match(/(@import)([^;]*?);/g);
           const importCleanedStatements = [];
           if (importFullStatements) {
               // clean import statements 
               for (let importFullStatement of importFullStatements) {
                   importFullStatement = importFullStatement.replace(/;/g, '');
                   importFullStatement = importFullStatement.replace(/\n/g, ' ');
                   importFullStatement = importFullStatement.replace(/\r/g, ' ');
                   importFullStatement = importFullStatement.trim();
                   importCleanedStatements.push(importFullStatement);
               }
               // remove import statements 
               css = css.replace(/(@import)([^;]*?);/g, ' ');
           }
           // split path and media
           for (const importCleanedStatement of importCleanedStatements) {
               const match = this.#match(importCleanedStatement);
               let path = match[0];
               path = path.substring(1, path.length - 1);
               
               let media = importCleanedStatement.substring(match.index + match[0].length).trim();
               if (media.startsWith(')')) {
                   media = media.substring(1).trim();
               }
               media = media ? media : null;
               importStatements.push({path: path, 
                                      media: media});
           }
        }
        return {importStatements: importStatements, 
                css: css};
    }
    
    #match(value) {
        let regEx;
        for (let i = 0; i < value.length; i++) {
             if (value[i] === "'") {
                regEx = /'.*'/;
                break;
             }
             else if (value[i] === '"') {
                regEx = /".*"/;
                break;
             }
        }
        return value.match(regEx);
    }

    async load(cssPath) {
        return this.#loadCssFileRecursively(cssPath).then(() => {
            const allCssSheetEntities = this.#importPaths(cssPath);
            const sheetEntities = [];
            for (const cssSheetEntity of allCssSheetEntities) {
                const sheet = this.#cssStyleSheets[cssSheetEntity.path];
                sheetEntities.push({path: cssSheetEntity.path,
                                    media: cssSheetEntity.media,
                                    sheet: sheet});
            }
            return {rootPath: cssPath,
                    graph: this.#fileGraph,
                    sheetEntities: sheetEntities};
        });
    }
    
    #loadChildrenCssFiles(cssPath, childrenCssEntities) {
        const futures = [];
        for (const childCssEntity of childrenCssEntities) {
            const childPath = childCssEntity.path
            if (!this.#loadedCssFiles.includes(childPath)) {
                this.#fileGraph.addLink(cssPath, {id: childPath,
                                                  path: childPath,
                                                  media: childCssEntity.media});
                futures.push(this.#loadCssFileRecursively(childPath));
            }
            else {
                this.#fileGraph.addLink(cssPath, {id: childPath,
                                                  path: childPath,
                                                  media: childCssEntity.media});
            }
        }
        return Promise.all(futures);
    }

    #loadCssFileRecursively(cssPath) {
       return new Promise((resolve, reject) => {
        this.#loadCssFile(cssPath)
            .then(childrenCssEntities => {
                this.#loadChildrenCssFiles(cssPath, childrenCssEntities)
                    .then(() => resolve())
                    .catch(e => reject(e));
            })
            .catch(e => reject(e));
       });
    }

    #loadCssFile(cssPath) {
        const formatedCssPath = yojaWeb.path(cssPath);
        return httpClient.get({url: formatedCssPath, fetchAs: 'text'})
                         .then(cssResponse => {
                              if (cssResponse.status === 200) {
                                  this.#loadedCssFiles.push(cssPath);
                                  const searchResult = this.searchCssImport(cssResponse.body);
                                  
                                  const cssStyleSheet = new CSSStyleSheet();
                                  cssStyleSheet.replaceSync(searchResult.css);
                                  this.#cssStyleSheets[cssPath] = cssStyleSheet;
                                  const resultCssEntities = this.#toImportPaths(this.#parentFolder(cssPath), 
                                                                                searchResult.importStatements);
                                  for (const resultCssEntity of resultCssEntities) {
                                       this.#fileGraph.addNode({id: resultCssEntity.path,
                                                                path: resultCssEntity.path,
                                                                media: resultCssEntity.media});
                                  }
                                  return resultCssEntities;
                              }
                              else {
                                  throw new Error('load yojaWeb css failed: [' + cssResponse.status + '] ' + cssPath, 
                                                   {cssPath: cssPath,
                                                    httpStatus: cssResponse.status,
                                                    httpBody: cssResponse.body});
                              }
                          })
                          .catch(error => { 
                              console.error('load yojaWeb css failed: ' + cssPath, {cause: error});
                              throw new Error('load yojaWeb css failed: ' + cssPath, {cause: error});
                          });
    }
    
}

const cssLoader = new CssLoader();

export function load(cssPath) {
    return cssLoader.load(cssPath);
} 
