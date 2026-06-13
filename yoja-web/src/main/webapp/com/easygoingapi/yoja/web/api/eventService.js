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

class YojaAction {
    
    #label;
    #action;
    
    constructor(label, action) {
        this.#label = label;
        this.#action = action;
    }

    get label() {
        return this.#label;
    }

    get action() {
        return this.#action;
    }
    
    apply(parameter) {
        this.#action(parameter);
    }
    
    toJson() {
        return {label: this.#label};
    }

    toString() {
        return JSON.stringify(this.toJson());
    }
    
}

class YojaEvent {
    
    #id;
    #active = true;
    #yojaActions = [];
    
    constructor(id) {
        this.#id = id;
    }
    
    get id() {
        return this.#id;
    }
    
    get actions() {
        return [...this.#yojaActions];
    }
    
    isActive() {
        return this.#active;
    }
    
    active(state) {
        if (jsUtil.isBoolean(state)) {
            this.#active = state ;
        }
    }
    
    size() {
        return this.#yojaActions.length
    }
    
    trigger(parameter) {
        for (const yojaAction of this.#yojaActions) {
            yojaAction.action(parameter);
        }
    }
    
    hasAction(position) {
        return jsUtil.isNumber(position)
                 && position > 0
                 && position <= this.#yojaActions.length;
    }
    
    /*
    
          action: Function
                      | YojaAction
                      | { label: String,
                          action: Function,
                          position: Number }
                      | { action: YojaAction,
                          position: Number }
    
     */
    addAction(action) {
        let result = null;
        if (action instanceof Function) {
            result = new YojaAction(undefined, action);
            this.#yojaActions.push(result);
        }
        else if (action instanceof YojaAction) {
            result = action;
            this.#yojaActions.push(result);
        }
        else if (action instanceof Object) {
            if (action.action instanceof Function) {
                result = new YojaAction(action.label, action.action);
            }
            else if (action.action instanceof YojaAction) {
                result = action.action;
            }
            if (this.hasAction(action.position)) {
                this.#yojaActions.splice(action.position - 1, 0, result);
            }
            else if (jsUtil.isNumber(action.position)) {
                if (action.position > 0) {
                    this.#yojaActions.push(result);
                }
            }
            else {
                this.#yojaActions.push(result);
            }
        }
        return result
    }
    
    getAction(position) {
        let result = null
        if (this.hasAction(position)) {
            result = this.#yojaActions[position - 1];
        }
        return result;
    }
    
    removeAction(position) {
        let result = null;
        if (this.hasAction(position)) {
            result = this.#yojaActions.splice(position - 1, 1)[0];
        }
        return result;
    }
    
    moveAction(fromPosition, toPosition) {
        if (this.hasAction(fromPosition)
               && toPosition > 0) {
            const yojaAction = this.removeAction(fromPosition);
            this.addAction({position: toPosition, 
                            action: yojaAction});
        }
    }

    clearAction() {
        this.#yojaActions = [];
    }
    
    toJson() {
        const actions = [];
        let position = 0;
        for (const yojaAction of this.#yojaActions) {
            position++;
            actions.push({position: position, 
                          label: yojaAction.label});
        }
        return {id: this.#id, 
                active: this.#active,
                actions: actions};
    }
    
    toString() {
        return JSON.stringify(this.toJson());
    }
    
}

class YojaEventService {
    
    #yojaEvents = {};
    
    #find(query) {
        const result = [];
        if (query === undefined) {
            for (const id in this.#yojaEvents) {
                result.push(this.#yojaEvents[id]);
            }
        }
        else {
            const ids = []
            for (const id in this.#yojaEvents) {
                ids.push(id);
            }
            const resultIds = jsUtil.filterStringArray(ids, query);
            for (const id of resultIds) {
                result.push(this.#yojaEvents[id]);
            }
        }
        return result;
    }
    
    has(query) {
        return this.#find(query).length > 0;
    }
    
    events(query) {
        const result = [];
        for (const yojaEvent of this.#find(query)) {
            result.push(yojaEvent);
        }
        result.sort((a, b) => a.id.localeCompare(b.id));
        return result;
    }
    
    actions(query) {
        const result = [];
        for (const yojaEvent of events(query)) {
            let position = 0;
            for (const yojaAction of yojaEvent.actions) {
                position++;
                const action = {eventId: yojaEvent.id, 
                                isActive: function() {
                                    return yojaEvent.isActive()
                                }, 
                                position: position,
                                label: yojaAction.label,
                                apply: function(parameter) {
                                    yojaAction.apply(parameter)
                                }};
                result.push(action);
            }
        }
        return result;
    }

    on(eventId, action) {
        if (action) {
            if (jsUtil.isString(eventId)
                  && (jsUtil.isFunction(action) || jsUtil.isFunction(action.action))) {
                const yojaEvent = this.#yojaEvents[eventId];
                if (yojaEvent) {
                    yojaEvent.addAction(action);
                }
                else {
                    const yojaEvent = new YojaEvent(eventId);
                    yojaEvent.addAction(action);
                    this.#yojaEvents[eventId] = yojaEvent;
                }
            }
        }
    }
    
    trigger(query, parameter) {
        const yojaEvents = this.events(query);
        for (const yojaEvent of yojaEvents) {
            if (yojaEvent.isActive()) {
                yojaEvent.trigger(parameter);
            }
        }
    }
    
    pause(query) {
        const yojaEvents = this.#find(query);
        for (const yojaEvent of yojaEvents) {
            yojaEvent.active(false);
        }
    }

    activate(query) {
        const yojaEvents = this.#find(query);
        for (const yojaEvent of yojaEvents) {
            yojaEvent.active(true);
        }
    }
    
    count(query) {
        return this.#find(query).length;
    }
    
    remove(query) {
        const yojaEvents = this.#find(query);
        for (const yojaEvent of yojaEvents) {
            yojaEvent.active(false);
            delete this.#yojaEvents[yojaEvent.id];
        }
    }
    
    countAction(query) {
        let result = 0;
        const yojaEvents = this.#find(query);
        for (const yojaEvent of yojaEvents) {
            result = result + yojaEvent.actions.length;
        }
        return result;
    }
        
}

const yojaEventService = new YojaEventService();

/*
 *  query is applied on YojaEvent.id
 * 
 *  query: String 
 *             | RegExp
 *             | { startsWith: String | String[], 
 *                 endsWith: String | String[], 
 *                 contains: String | String[], 
 *                 equals:String | String[],
 *                 matches: String | RegExp } 
 * 
 */

export function has(query) {
    return yojaEventService.has(query);
}

export function pause(query) {
    yojaEventService.pause(query);
}

export function activate(query) {
    yojaEventService.activate(query);
}

export function events(query) {
    return yojaEventService.events(query);
}

export function event(query) {
    const result = yojaEventService.events(query);
    return result.length ? result[0] : null;
}

export function actions(query) {
    return yojaEventService.actions(query);
}

export function count(query) {
    return yojaEventService.count(query);
}

export function countAction(query) {
    return yojaEventService.countAction(query);
}

export function remove(query) {
    yojaEventService.remove(query);
}

/*
 *  eventId: String
 *  action: Function 
 *           | { label: String, 
 *               position: Integer
 *               action: Function } 
 */
export function on(eventId, action) {
    yojaEventService.on(eventId, action);
}

export function trigger(query, parameter) {
    yojaEventService.trigger(query, parameter);
}


