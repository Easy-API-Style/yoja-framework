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
const {Listeners} = await import(yojaWeb.path('../util/listenerUtil.js'));

const SCOPES = ['local', 'session'];
const METHODS = ['get', 'set', 'has', 'remove'];

class StorageService {
    
    #itemKeyPrefix = 'yojaWebItemKey__';
    #itemKeysPrefix = 'yojaWebItemKeys__';
    
    // the application's actions -- each on(...) gives back its off (listenerUtil) --: 'scope method key' -> those
    // of an item, and those of every event
    #onItem = new Map();
    #onEvent = new Listeners('storageService event');
    
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
        this.#onEvent.run(scope, 'clear');
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
    /** @return its off */
    onEvent(action) {
        return this.#onEvent.add(action);
    }
    
    /**
     * @return its off
     * @throws TypeError for a scope or a method it does not have: said, not an action never told
     */
    on(scope, method, key, action) {
        if (!SCOPES.includes(scope) || !METHODS.includes(method)) {
            throw new TypeError('storageService: on(scope [local | session], method [get | set | has | remove], '
                                + 'key, action), not ' + scope + ' ' + method);
        }
        const itemKey = scope + ' ' + method + ' ' + key;
        let listeners = this.#onItem.get(itemKey);
        if (!listeners) {
            listeners = new Listeners('storageService ' + itemKey);
            this.#onItem.set(itemKey, listeners);
        }
        return listeners.add(action);
    }
    
    /** Tells the item's actions, then those of every event. */
    #tell(scope, method, event, key, value) {
        this.#onItem.get(scope + ' ' + method + ' ' + key)?.run(value);
        this.#onEvent.run(scope, event, key, value);
    }
    
    /*
         LOCAL
    */
    getLocalItemKeys(query) {
       return this.#findItemKeys('local', query);
    }

    getLocalItem(key, event) {
        return this.#getItem('local', localStorage, key, event);
    }

    setLocalItem(key, value) {
        this.#setItem('local', localStorage, key, value);
    }

    removeLocalItem(key) {
        this.#removeItem('local', localStorage, key);
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
        return this.#getItem('session', sessionStorage, key, event);
    }

    setSessionItem(key, value) {
        this.#setItem('session', sessionStorage, key, value);
    }

    removeSessionItem(key) {
        this.#removeItem('session', sessionStorage, key);
    }

    clearSession() {
        this.#clearItems('session');
    }
    
    /*
         ITEM
    */
    #getItem(scope, storage, key, event) {
        const value = this.#parse(storage.getItem(this.#toYojaItemKey(key)));
        if (value !== undefined) {
            this.#tell(scope, event === 'get' ? 'get' : 'has', event, key, value);
        }
        return value;
    }

    #setItem(scope, storage, key, value) {
        value = value === undefined ? null : value;
        this.#tell(scope, 'set', 'set', key, value);
        this.#addItemKey(scope, key);
        storage.setItem(this.#toYojaItemKey(key), this.#stringify(value));
    }

    #removeItem(scope, storage, key) {
        const yojaKey = this.#toYojaItemKey(key);
        const value = this.#parse(storage.getItem(yojaKey));
        storage.removeItem(yojaKey);
        this.#removeItemKey(scope, key);
        if (value !== undefined) {
            this.#tell(scope, 'remove', 'remove', key, value);
        }
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

