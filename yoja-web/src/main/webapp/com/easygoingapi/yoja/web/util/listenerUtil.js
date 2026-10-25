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

/*
 * The listeners of the api, one model for every onXXX(action): it gives back its off() -- called, the action is no
 * more told (called again: nothing). A controler gives it to its section -- section.track(off) --: off() runs once
 * the section has left the page. Before each event told, the sections gone are forgotten (YojaWeb's sweep): an
 * action of a section no more in the page is never told, even before the next section is added.
 *
 * The actions are told in the order they were added -- each one isolated: one failing is said, the others told all
 * the same; one added or removed while an event is told is so for the next one (removed: not told, if not yet).
 */

// YojaWeb's: forgets the sections gone, their offs run -- then, an action failed: tells its error actions
let sweep = null;
let report = null;

/**
 * YojaWeb's own.
 *
 * @param hooks {sweep: forgets the sections gone, before an event is told; report: told an action's error}
 */
export function hookWith(hooks) {
    sweep = hooks.sweep;
    report = hooks.report;
}

/** Forgets the sections gone -- their offs run --: for an api telling its actions on its own (eventService). */
export function sweepGone() {
    sweep?.();
}

export class Listeners {

    #name;
    #report;
    #entries = [];   // [{action, removed}]

    /**
     * @param name    what they listen to, said when one fails
     * @param options {report: whether an action's error is told to YojaWeb's error actions (true by default) --
     *                not theirs: one failing is not told to them again}
     */
    constructor(name, options) {
        this.#name = name;
        this.#report = options?.report ?? true;
    }

    /**
     * @return its off: called, the action is no more told
     * @throws TypeError when the action is not a function: said at once, not when the event comes
     */
    add(action) {
        if (typeof action !== 'function') {
            throw new TypeError(this.#name + ': an action is a function, not ' + typeof action + ': ' + action);
        }
        const entry = {action: action, removed: false};
        this.#entries.push(entry);
        return () => {
            if (!entry.removed) {
                entry.removed = true;
                this.#entries.splice(this.#entries.indexOf(entry), 1);
            }
        };
    }

    get size() {
        return this.#entries.length;
    }

    clear() {
        for (const entry of this.#entries) {
            entry.removed = true;
        }
        this.#entries = [];
    }

    /** @return the actions to tell now, in their order -- the sections gone forgotten first; one removed meanwhile: skipped */
    *actions() {
        sweep?.();
        for (const entry of [...this.#entries]) {
            if (!entry.removed) {
                yield entry.action;
            }
        }
    }

    /** Tells every action the values. */
    run(...values) {
        for (const action of this.actions()) {
            this.call(action, ...values);
        }
    }

    /** @return what the action gave -- undefined when it failed: said, YojaWeb's error actions told */
    call(action, ...values) {
        try {
            return action(...values);
        }
        catch (error) {
            console.error(this.#name + ': an action failed', error);
            if (this.#report) {
                report?.(error);
            }
            return undefined;
        }
    }

}
