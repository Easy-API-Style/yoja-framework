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
const jsUtil = await import(yojaWeb.path('../util/javascriptUtil.js'));

class StorageService {
    
    #itemKeyPrefix = 'yojaWebItemKey__';
    #itemKeysPrefix = 'yojaWebItemKeys__';
    
    #onLocalSet = {};
    #onLocalGet = {};
    #onLocalRemove = {};
    #onLocalHas = {};
    
    #onSessionSet = {};
    #onSessionGet = {};
    #onSessionRemove = {};
    #onSessionHas = {};
    
    #onEvent = [];
    
    constructor() {
        
    }
    
    /*
         STORAGE
    */
    #getStorage(scope) {
        return scope === 'local' ? localStorage : sessionStorage;
    }
    
    /*
         KEY
    */
    #toYojaItemKey(itemKey) {
        return this.#itemKeyPrefix + itemKey;
    }
  
    #getItemKeys(scope) {
        const result = this.#getStorage(scope)
                           .getItem(this.#itemKeysPrefix + scope);
        return result ? JSON.parse(result) : [];
    }
    
    #findItemKeys(scope, query) {
        const itemKeys = this.#getItemKeys(scope);
        const result = query === undefined 
                              ? itemKeys
                              : jsUtil.filterStringArray(itemKeys, query);
        result.sort((a, b) => a.localeCompare(b));                  
        return result;
    }
       
    #addItemKey(scope, itemKey) {
        if (itemKey) {
            const itemKeys = this.#getItemKeys(scope);
            if (!itemKeys.includes(itemKey)) {
                itemKeys.push(itemKey);
                this.#getStorage(scope)
                    .setItem(this.#itemKeysPrefix + scope, JSON.stringify(itemKeys));
            }
        }
    }
    
    #clearItems(scope) {
        const storage = this.#getStorage(scope);
        const itemKeys = this.#getItemKeys(scope);
        for (const itemKey of itemKeys) {
            storage.removeItem(this.#toYojaItemKey(itemKey));
        }
        storage.removeItem(this.#itemKeysPrefix + scope);
        for (let i = 0; i < this.#onEvent.length; i++) {
            this.#onEvent[i](scope, 'clear');
        }
    }
    
    #removeItemKey(scope, itemKey) {
        if (itemKey) {
            const itemKeys = this.#getItemKeys(scope);
            if (itemKeys.includes(itemKey)) {
                const result = [];
                for (const key of itemKeys) {
                    if (key !== itemKey) {
                        result.push(key);
                    }
                }
                this.#getStorage(scope)
                    .setItem(this.#itemKeysPrefix + scope, JSON.stringify(result));
            }
        }
    }

    /*
         VALUE
    */
    #parse(value) {
        if (value) {
            const _value = JSON.parse(value);
            let result = _value.value;
            if (_value.type === 'date') {
                result = new Date(result);
            }
            return result;
        }
        return undefined;
    }
    
    #stringify(value) {
        let result;
        if (value instanceof Date) {
            result = JSON.stringify({date: new Date(), 
                                     type: 'date', 
                                     value: value});
        }
        else if (value instanceof Object
                     && !(value instanceof String
                            || value instanceof Boolean
                            || value instanceof Number)) {
            result = JSON.stringify({date: new Date(), 
                                     type: 'object', 
                                     value: value });
        }
        else {
            result = JSON.stringify({date: new Date(), 
                                     type: 'primitive', 
                                     value: value === undefined ? null : value });
        }
        return result;
    }
    
    /*
         EVENT
    */
    onEvent(action) {
        this.#onEvent.push(action);
    }
    
    on(scope, method, key, action) {
        if (scope === 'local') {
            if (method === 'set') {
                if (!this.#onLocalSet[key]) {
                    this.#onLocalSet[key] = [];
                }
                this.#onLocalSet[key].push(action);
            }
            else if (method === 'get') {
                if (!this.#onLocalGet[key]) {
                    this.#onLocalGet[key] = [];
                }
                this.#onLocalGet[key].push(action);
            }
            else if (method === 'has') {
                if (!this.#onLocalHas[key]) {
                    this.#onLocalHas[key] = [];
                }
                this.#onLocalHas[key].push(action);
            }
            else if (method === 'remove') {
                if (!this.#onLocalRemove[key]) {
                    this.#onLocalRemove[key] = [];
                }
                this.#onLocalRemove[key].push(action);
            }
        }
        else if (scope === 'session') {
            if (method === 'set') {
                if (!this.#onSessionSet[key]) {
                    this.#onSessionSet[key] = [];
                }
                this.#onSessionSet[key].push(action);
            }
            else if (method === 'get') {
                if (!this.#onSessionGet[key]) {
                    this.#onSessionGet[key] = [];
                }
                this.#onSessionGet[key].push(action);
            }
            else if (method === 'has') {
                if (!this.#onSessionHas[key]) {
                    this.#onSessionHas[key] = [];
                }
                this.#onSessionHas[key].push(action);
            }
            else if (method === 'remove') {
                if (!this.#onSessionRemove[key]) {
                    this.#onSessionRemove[key] = [];
                }
                this.#onSessionRemove[key].push(action);
            }
        }
    }
    
    /*
         LOCAL
    */
    getLocalItemKeys(query) {
       return this.#findItemKeys('local', query);
    }

    getLocalItem(key, event) {
        const yojaKey = this.#toYojaItemKey(key);
        const value = this.#parse(localStorage.getItem(yojaKey));
        if (value !== undefined) {
            if (event === 'get') {
                if (this.#onLocalGet[key]) {
                    for (let i = 0;i < this.#onLocalGet[key].length;i++) {
                        this.#onLocalGet[key][i](value);
                    }
                }
            }
            else {
                if (this.#onLocalHas[key]) {
                    for (let i = 0;i < this.#onLocalHas[key].length;i++) {
                        this.#onLocalHas[key][i](value);
                    }
                }

            }
            for (let i = 0;i < this.#onEvent.length;i++) {
                this.#onEvent[i]('local', event, key, value);
            }
        }
        return value;
    }

    setLocalItem(key, value) {
        value = value === undefined ? null : value;
        if (this.#onLocalSet[key]) {
            for (let i = 0; i < this.#onLocalSet[key].length; i++) {
                this.#onLocalSet[key][i](value);
            }
        }
        for (let i = 0; i < this.#onEvent.length; i++) {
            this.#onEvent[i]('local', 'set', key, value);
        }
        this.#addItemKey('local', key);
        const yojaKey = this.#toYojaItemKey(key);
        localStorage.setItem(yojaKey, this.#stringify(value));
    }

    removeLocalItem(key) {
       const yojaKey = this.#toYojaItemKey(key);
       const value = this.#parse(localStorage.getItem(yojaKey));
       localStorage.removeItem(yojaKey);
       this.#removeItemKey('local', key);
       if (value !== undefined) {
            if (this.#onLocalRemove[key]) {
                for (let i = 0; i < this.#onLocalRemove[key].length; i++) {
                   this.#onLocalRemove[key][i](value);
                }
            }
            for (let i = 0; i < this.#onEvent.length; i++) {
                this.#onEvent[i]('local', 'remove', key, value);
            }
        }
    }

    clearLocal() {
        this.#clearItems('local');
    }
    
    /*
         SESSION
    */
    getSessionItemKeys(query) {
        return this.#findItemKeys('session', query);
    }
            
    getSessionItem(key, event) {
        const yojaKey = this.#toYojaItemKey(key);
        const value = this.#parse(sessionStorage.getItem(yojaKey));
        if (value !== undefined) {
            if (event === 'get') {
                if (this.#onSessionGet[key]) {
                    for (let i = 0;i < this.#onSessionGet[key].length;i++) {
                        this.#onSessionGet[key][i](value);
                    }
                }
            }
            else {
                if (this.#onSessionHas[key]) {
                    for (let i = 0;i < this.#onSessionHas[key].length;i++) {
                        this.#onSessionHas[key][i](value);
                    }
                }
            }
            for (let i = 0;i < this.#onEvent.length;i++) {
                this.#onEvent[i]('session', event, key, value);
            }
        }
        return value;
    }

    setSessionItem(key, value) {
        value = value === undefined ? null : value;
        if (this.#onSessionSet[key]) {
            for (let i = 0; i < this.#onSessionSet[key].length; i++) {
                this.#onSessionSet[key][i](value);
            }
        }
        for (let i = 0; i < this.#onEvent.length; i++) {
           this.#onEvent[i]('session', 'set', key, value);
        }
        this.#addItemKey('session', key);
        const yojaKey = this.#toYojaItemKey(key);
        sessionStorage.setItem(yojaKey, this.#stringify(value));
    }

    removeSessionItem(key) {
        const yojaKey = this.#toYojaItemKey(key);
        const value = this.#parse(sessionStorage.getItem(yojaKey));
        sessionStorage.removeItem(yojaKey);
        this.#removeItemKey('session', key);
        if (value !== undefined) {
            if (this.#onSessionRemove[key]) {
                for (let i = 0; i < this.#onSessionRemove[key].length; i++) {
                    this.#onSessionRemove[key][i](value);
                }
            }
            for (let i = 0; i < this.#onEvent.length; i++) {
                this.#onEvent[i]('session', 'remove', key, value);
            }
        }
    }

    clearSession() {
        this.#clearItems('session');
    }
    
}
const storageService = new StorageService();

/*
 *    EVENT
 */
// @scope : [local | session]
// @method : [get | set | has | remove]
// @parameter action : Function
export function on(scope, method, key, action) {
    return storageService.on(scope, method, key, action);
}

// scope : [local | session]
// method : [get | set | has | remove | clear]
// @parameter action : Function(scope, method, ...)
export function onEvent(action) {
    return storageService.onEvent(action);
}

/*
 *    LOCAL
 */
export function getLocalItemKeys(query) {
    return storageService.getLocalItemKeys(query);
}

export function hasLocalItem(key) {
    return storageService.getLocalItem(key, 'has') !== undefined ? true : false;
}

export function getLocalItem(key) {
    return storageService.getLocalItem(key, 'get');
}

export function setLocalItem(key, value) {
    storageService.setLocalItem(key, value);
}

export function removeLocalItem(key) {
    storageService.removeLocalItem(key);
}

export function clearLocal() {
    storageService.clearLocal();
}

/*
 *    SESSION
 */
export function getSessionItemKeys(query) {
     return storageService.getSessionItemKeys(query);
}

export function hasSessionItem(key) {
    return storageService.getSessionItem(key, 'has') !== undefined ? true : false;
}

export function getSessionItem(key) {
    return storageService.getSessionItem(key, 'get');
}

export function setSessionItem(key, value) {
    storageService.setSessionItem(key, value);
}

export function removeSessionItem(key) {
    storageService.removeSessionItem(key);
}

export function clearSession() {
    storageService.clearSession();
}

