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

const httpClient = await import(yojaWeb.path('../api/httpClient.js'));
const Graph = await import(yojaWeb.path('../util/Graph.js'));
const domUtil = await import(yojaWeb.path('../util/domUtil.js'));

/**
 * Loads a language file -- its {@code <yw-text>} -- and the files it imports ({@code <yw-import path="...">}): an
 * import is an include, unrolled in its place before the file's own texts, so that what comes later wins -- a file
 * always overrides what it includes, a later include an earlier one. The texts are merged in that order
 * ({@code paths}): the true value of a key is its last one (languageService).
 *
 * A file is fetched and parsed once, however many load it -- at the same time too; an import that comes back to a
 * file on its own path is not followed again.
 */
class LanguageLoader {

    // the files and their imports: path -> paths
    #fileGraph = Graph.newInstance();
    // path -> a promise of the file read: {document, importPaths}
    #files = new Map();
    // path -> the file, once read: {document (parsed once, its imports out), importPaths}
    #loaded = new Map();

    /** @return the texts of the files read, by path (as strings) */
    get xmlDocuments() {
        const result = {};
        for (const [path, file] of this.#loaded) {
            result[path] = domUtil.serializeXml(file.document);
        }
        return result;
    }

    /**
     * @return the files to merge, in their include order: each import unrolled before its file -- a file found
     *         twice is so (where it is included); an import back on its own path is not
     */
    #importPaths(xmlPath) {
        const result = [xmlPath];
        const recursiveService = this.#fileGraph.newRecursiveService(xmlPath);
        this.#importPathsRecursively(recursiveService, xmlPath, result);
        result.reverse();
        return result;
    }

    #importPathsRecursively(recursiveService, path, result) {
        const childrenPaths = [...this.#fileGraph.children(path)];
        childrenPaths.reverse();
        for (const childPath of childrenPaths) {
            if (!recursiveService.isRecursive(path, childPath)) {
                result.push(childPath);
                this.#importPathsRecursively(recursiveService, childPath, result);
            }
        }
    }

    /** @return the absolute paths of a file's imports -- a relative one from the file's folder, '.' and '..' resolved */
    #toImportPaths(xmlPath, importPaths) {
        const folder = xmlPath.substring(0, xmlPath.lastIndexOf('/') + 1);
        return importPaths.map(importPath => {
            const url = new URL(importPath.startsWith('/') ? importPath : folder + importPath, window.location.origin);
            return url.pathname + url.search;
        });
    }

    /**
     * @return one {@code <yw-language>} holding the files' texts, in their order -- copies: each file is parsed
     *         once, its document kept for the next merge
     */
    #merge(paths) {
        const merged = domUtil.parseXml('<yw-language></yw-language>');
        const ywLanguageTag = merged.querySelector('yw-language');
        for (const path of paths) {
            for (const ywTextTag of this.#loaded.get(path).document.querySelectorAll('yw-text')) {
                ywLanguageTag.appendChild(merged.importNode(ywTextTag, true));
            }
        }
        return ywLanguageTag;
    }

    /**
     * Reads a language file: its imports taken out of it.
     *
     * @param xmlAsString the file -- empty: no text, no import
     * @param path        its path, told when it is not well formed
     * @return {importPaths, dom: the file without its imports, as a string}
     */
    searchImport(xmlAsString, path) {
        const read = this.#read(xmlAsString, path);
        return {importPaths: read.importPaths, dom: domUtil.serializeXml(read.document)};
    }

    /**
     * Parses a language file -- once --, its imports taken out of it.
     *
     * @return {importPaths, document: the file without its imports} -- an empty one, or one not well formed: no text
     */
    #read(xmlAsString, path) {
        const importPaths = [];
        const empty = () => ({importPaths: importPaths, document: domUtil.parseXml('<yw-language></yw-language>')});
        if (!xmlAsString || !xmlAsString.trim()) {
            return empty();
        }
        const xmlDocument = domUtil.parseXml(xmlAsString);
        if (xmlDocument.querySelector('parsererror')) {
            // DOMParser refuses nothing: the file's texts would be missing, unsaid
            console.error('language file not well formed: ' + (path || ''), xmlDocument.querySelector('parsererror').textContent);
            return empty();
        }
        for (const importTag of xmlDocument.querySelectorAll('yw-import')) {
            const importPath = importTag.getAttribute('path');
            importTag.remove();
            if (importPath) {
                importPaths.push(importPath);
            }
        }
        return {importPaths: importPaths, document: xmlDocument};
    }

    /**
     * @return a promise of the file and its imports merged: {xml (one {@code <yw-language>}), graph, paths (the
     *         include order), rootPath}
     */
    load(xmlPath) {
        return this.#loadTree(xmlPath, new Set()).then(() => {
            const xmlPaths = this.#importPaths(xmlPath);
            return {xml: this.#merge(xmlPaths),
                    graph: this.#fileGraph,
                    paths: xmlPaths,
                    rootPath: xmlPath};
        });
    }

    /**
     * Reads a file, then its imports -- not one already on its path (an import coming back to it).
     *
     * @param path      the file
     * @param ancestors the files on its path
     */
    #loadTree(path, ancestors) {
        return this.#loadFile(path).then(file => {
            const onPath = new Set(ancestors).add(path);
            const children = [];
            for (const childPath of file.importPaths) {
                this.#fileGraph.addLink(path, childPath);
                if (!onPath.has(childPath)) {
                    children.push(this.#loadTree(childPath, onPath));
                }
            }
            return Promise.all(children);
        });
    }

    /** @return a promise of the file read -- fetched and parsed once; a failure not kept: read again next time */
    #loadFile(xmlPath) {
        let file = this.#files.get(xmlPath);
        if (!file) {
            file = httpClient.get({url: yojaWeb.path(xmlPath), fetchAs: 'text'})
                             .then(xmlResponse => {
                                 if (xmlResponse.status !== 200) {
                                     throw new Error('load yojaWeb xml failed: [' + xmlResponse.status + '] ' + xmlPath,
                                                     {cause: {xmlPath: xmlPath,
                                                              httpStatus: xmlResponse.status,
                                                              httpBody: xmlResponse.body}});
                                 }
                                 const parsed = this.#read(xmlResponse.body, xmlPath);
                                 const read = {document: parsed.document,
                                               importPaths: this.#toImportPaths(xmlPath, parsed.importPaths)};
                                 this.#loaded.set(xmlPath, read);
                                 return read;
                             })
                             .catch(error => {
                                 this.#files.delete(xmlPath);
                                 console.error('load yojaWeb xml failed: ' + xmlPath, {cause: error});
                                 throw error;
                             });
            this.#files.set(xmlPath, file);
        }
        return file;
    }

}

const languageLoader = new LanguageLoader();

export function load(path) {
    return languageLoader.load(path);
}
