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

let logToSave = ['LOG', 'TRACE', 'DEBUG', 'INFO', 'WRAN', 'ERROR']

if (yojaWeb?.config?.logToSave) {
    logToSave = yojaWeb?.config?.logToSave
}

function stringify(value) {
    let result = value
    if (value instanceof Object
        && !(value instanceof String
              || value instanceof Boolean
              || value instanceof Number)) {
        result = JSON.stringify(value)
    }
    return result
}

function toLog(level, args) {
    let message = ''
    if (Array.isArray(args)) {
        if (args.length == 1) {
            message = stringify(args[0])
        }
        else if (args.length > 1) {
            message = stringify(args)
        }
    }
    else if (args) {
        message = stringify(args)
    }
    return {date: Date.now(),
            level: level,
            message: message}
}

if (!window.yojaWebLogs) {
    window.yojaWebLogs = []

    if (logToSave.includes('LOG')) {
        const origConsoleLog = console.log;
        console.log = (...args) => {
            origConsoleLog.apply(console, args);
            window.yojaWebLogs.push(toLog('LOG', args));
        }
    }
    if (logToSave.includes('TRACE')) {
        const origConsoleTrace = console.trace;
        console.trace = (...args) => {
            origConsoleTrace.apply(console, args);
            window.yojaWebLogs.push(toLog('TRACE', args));
        }
    }
    if (logToSave.includes('DEBUG')) {
        const origConsoleDebug = console.debug;
        console.debug = (...args) => {
            origConsoleDebug.apply(console, args);
            window.yojaWebLogs.push(toLog('DEBUG', args));
        }
    }
    if (logToSave.includes('INFO')) {
        const origConsoleInfo = console.info;
        console.info = (...args) => {
            origConsoleInfo.apply(console, args);
            window.yojaWebLogs.push(toLog('INFO', args));
        }
    }
    if (logToSave.includes('WARN')) {
        const origConsoleWarn = console.warn;
        console.warn = (...args) => {
            origConsoleWarn.apply(console, args);
            window.yojaWebLogs.push(toLog('WARN', args));
        }
    }
    if (logToSave.includes('ERROR')) {
        const origConsoleError = console.error;
        console.error = (...args) => {
            origConsoleError.apply(console, args);
            window.yojaWebLogs.push(toLog('ERROR', args));
        }
    }
    console.warn('yojaWeb browser logs: ' + logToSave)
}
