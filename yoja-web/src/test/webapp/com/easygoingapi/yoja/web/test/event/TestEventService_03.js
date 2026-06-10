'use strict'

const eventService = yojaWebApi.eventService

// test_02
eventService.on('test_02', {
    label: 'test_02_action_01',
    action: p => window.eventTest_03('test_02_action_01', p)
})
eventService.on('test_02', {
    label: 'test_02_action_02',
    action: p => window.eventTest_03('test_02_action_02', p)
})
// test_01
eventService.on('test_01', {
    label: 'test_01_action_01',
    action: p => window.eventTest_03('test_01_action_01', p)
})
eventService.on('test_01', {
    label: 'test_01_action_03',
    action: p => window.eventTest_03('test_01_action_03', p)
})
eventService.on('test_01', {
    label: 'test_01_action_02',
    position: 2,
    action: p => window.eventTest_03('test_01_action_02', p)
})
// AAA
eventService.on('AAA', {
    label: 'AAA_action_01',
    action: p => window.eventTest_03('AAA_action_01', p)
})