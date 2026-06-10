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

const urlParameterUtil = await import(yojaWeb.path('../util/urlParameterUtil.js'));

const originBack = window.history.back;
const originForward = window.history.forward;
const originGo = window.history.go;
const originHistory = window.history;

class HistoryState {
    
    #data;
    #urlParameter;
    #url;
    
    constructor(data, urlParameter, url) {
        this.#data = data;
        this.#urlParameter = urlParameter;
        this.#url = url;
    }
    
    get data() {
        return this.#data;
    }
    
    get url() {
        return this.#url;
    }
    
    get urlParameter() {
        return this.#urlParameter;
    }
    
    clone() {
        return new HistoryEntity(this.#data, 
                                 this.#urlParameter.clone(),
                                 this.#url);
    }
    
    toJson() {
        return {data: this.#data, 
                urlParameter: this.#urlParameter.toJson(),
                url: this.#url};
    }

    toString() {
        return JSON.stringify(this.toJson());
    }
    
}

class UrlParameterService {
    
    #urlParameter = null;
    #onChangeActions = [];
    
    constructor() {
        this.#updateUrlParameter();
        const url = urlParameterUtil.toUrl(window.location.pathname, this.#urlParameter);
        const historyState = new HistoryState(undefined, 
                                              this.#urlParameter.clone(),
                                              url);
        window.history.replaceState(historyState.toJson(), '', url);
        this.#applyOnChange({event: 'load'});
        
        window.addEventListener('popstate', () => {
             this.#updateUrlParameter();
             this.#applyOnChange({event: 'pop'});
        });
    }
    
    #emptyToNull(value) {
        let result = value;
        if (result === null 
             || result === undefined 
             || result == '') {
            result = null;
        }
        return result;
    }
    
    has(key, value) {
        return this.#urlParameter.has(key, value);
    }

    keys() {
        return this.#urlParameter.keys();
    }

    get(key) {
        return this.#urlParameter.get(key);
    }
    
    getAll(key) {
        return this.#urlParameter.getAll(key);
    }

    entries() {
        return this.#urlParameter.entries();
    }

    urlParameterEquals(urlParameter) {
        return this.#urlParameter.equals(urlParameter);
    }
    
    clear() {
        const entries = this.entries();
        this.#urlParameter.clear();
        this.#applyOnChange({event: 'clear', entries: entries});
    }

    append(key, value) {
        const _value = this.#emptyToNull(value);
        this.#urlParameter.append(key, _value);
        this.#applyOnChange({event: 'append', key: key, value: _value});
    }

    set(key, value) {
        const _value = this.#emptyToNull(value);
        this.#urlParameter.set(key, _value);
        this.#applyOnChange({event: 'set', key: key, value: _value});
    }

    remove(key, value) {
        const _value = this.#emptyToNull(value);
        const removeAll = value === undefined
        let event;
        if (removeAll) {
            event = {event: 'remove', key: key, value: this.getAll(key)};
            this.#urlParameter.remove(key);
        }
        else {
            event = {event: 'remove', key: key, value: _value};
            this.#urlParameter.remove(key, _value);
        }
        this.#applyOnChange(event);
    }
    
    toUrlQuery() {
        return this.#urlParameter.toUrlQuery();
    }

    #applyOnChange(handler) {
        let result = true;
        for (const action of this.#onChangeActions) {
            const v = action(handler);
            if (v === false) {
                result = false;
            }
            else if (v === true) {
                result = true;
            }
        }
        return result;
    }
    
    onChange(callback) {
        this.#onChangeActions.push(callback);
    }
    
    // action [ push | replace ]
    #history(handler) {
        if (handler.action === 'push') {
            // before
            const doPush = this.#applyOnChange({event: 'before-push'});
            if (doPush === true) {
                // after
                const urlQuery = this.toUrlQuery();
                let url = window.location.pathname;
                if (urlQuery) {
                    url = url + '?' + urlQuery;
                }
                const historyState = new HistoryState(handler.data,
                                                      this.#urlParameter.clone(),
                                                      url);
                window.history.pushState(historyState.toJson(), '', url);
                this.#applyOnChange({event: 'after-push'});
            }
            else {
                // cancel
                this.#applyOnChange({event: 'cancel-push'});
            }
        }
        else if (handler.action === 'replace') {
            // before
            const doReplace = this.#applyOnChange({event: 'before-replace'});
            if (doReplace === true) {
                // after
                const urlQuery = this.toUrlQuery();
                let url = window.location.pathname;
                if (urlQuery) {
                    url = url + '?' + urlQuery;
                }
                const historyState = new HistoryState(handler.data,
                                                      this.#urlParameter.clone(),
                                                      url);
                window.history.replaceState(historyState.toJson(), '', url);
                this.#applyOnChange({event: 'after-replace'});
            }
            else {
                // cancel
                this.#applyOnChange({event: 'cancel-replace'});
            }
        }
        else if (handler.action === 'back') {
            // before
            const doBack = this.#applyOnChange({event: 'before-back'});
            if (doBack === true) {
                // after
                this.#originBack();
            }
            else {
                // cancel
                this.#applyOnChange({event: 'cancel-back'});
            }
        }
        else if (handler.action === 'forward') {
            // before
            const doForward = this.#applyOnChange({event: 'before-forward'});
            // after
            if (doForward === true) {
                // after
                this.#originForward();
            }
            else {
                // cancel
                this.#applyOnChange({event: 'cancel-forward'});
            }
        }
        else if (handler.action === 'go') {
            // before
            const doGo = this.#applyOnChange({event: 'before-go',
                                              delta: handler.delta});
            // after
            if (doGo === true) {
                // after
                this.#originGo(handler.delta);
            }
            else {
                // cancel
                this.#applyOnChange({event: 'cancel-go',
                                     delta: handler.delta});
            }
        }
    }
    
    #updateUrlParameter() {
        this.#urlParameter = urlParameterUtil.parseUrlParameter(window.location.search);
    }
    
    state() {
        return window.history.state;
    }
    
    back() {
       this.#history({action: 'back'});
    }
    
    forward() {
        this.#history({action: 'forward'});
    }
    
    go(delta) {
        this.#history({action: 'go', delta: delta});
    }
    
    replace(data) {
        this.#history({action: 'replace', data: data});
    }
    
    push(data) {
        this.#history({action: 'push', data: data});
    }
    
    #originGo(delta) {
        originGo.apply(originHistory, delta);
    }

    #originBack() {
        originBack.apply(originHistory);
    }

    #originForward() {
        originForward.apply(originHistory);
    }
    
}

const urlParameterService = new UrlParameterService();

//window.history.back = () => {
//    urlParameterService.back()
//}
//
//window.history.forward = () => {
//    urlParameterService.forward()
//}
//
//window.history.go = (...args) => {
//    if (args.length > 0 
//           && (args[0] instanceof Number 
//                 || typeof args[0] === 'number' )) {
//        urlParameterService.go(args[0])
//    }
//}

export function clear() {
    return urlParameterService.clear();
}

export function has(key, value) {
    return urlParameterService.has(key, value);
}

export function keys() {
    return urlParameterService.keys();
}

export function get(key) {
    return urlParameterService.get(key);
}

export function getAll(key) {
    return urlParameterService.getAll(key);
}

export function entries() {
    return urlParameterService.entries();
}

export function set(key, value) {
    urlParameterService.set(key, value);
}

export function append(key, value) {
    urlParameterService.append(key, value);
}

export function remove(key, value) {
    urlParameterService.remove(key, value);
}

export function currentUrlParameter() {
    return urlParameterUtil.parseUrlParameter(window.location.search);
}

export function isUpdated() {
    return urlParameterService.urlParameterEquals(currentUrlParameter());
}

export function replace(data) {
    urlParameterService.replace(data);
}

export function push(data) {
    urlParameterService.push(data);
}

export function state() {
    return urlParameterService.state();
}

export function toUrlQuery() {
    return urlParameterService.toUrlQuery();
}

/*
 *  {event: String}
 *
 *  values: clear 
 *           | append
 *           | set
 *           | remove
 *           | before-replace
 *           | after-replace
 *           | before-push
 *           | after-push
 *           | pop
 *           | load
 * 
 */
export function onChange(callback) {
    urlParameterService.onChange(callback);
}


