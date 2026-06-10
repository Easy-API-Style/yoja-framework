'use strict'

const eventService = yojaWebApi.eventService

export function test_01() {
    // test_02
    eventService.on('test_02', {
        label: 'test_02_action_01',
        action: p => console.info('test_01_action_01 -> ' + p)
    })
    eventService.on('test_02', {
        label: 'test_02_action_02',
        action: p => console.info('test_01_action_02 -> ' + p)
    })
    // test_01
    eventService.on('test_01', {
        label: 'test_01_action_01',
        action: p => console.info('test_01_action_01 -> ' + p)
    })
    eventService.on('test_01', {
        label: 'test_01_action_03',
        action: p => console.info('test_01_action_03 -> ' + p)
    })
    eventService.on('test_01', {
        label: 'test_01_action_02',
        position: 2,
        action: p => console.info('test_01_action_02 -> ' + p)
    })
    // AAA
    eventService.on('AAA', {
        label: 'AAA_action_01',
        action: p => console.info('AAA_action_01 -> ' + p)
    })
    
    ywAssert.assertEquals(1, eventService.count({endsWith: '02'}))
    ywAssert.assertEquals(2, eventService.count({startsWith: 'test'}))
    ywAssert.assertEquals(3, eventService.count())
    
    ywAssert.assertEquals(2, eventService.countAction({endsWith: '02'}))
    ywAssert.assertEquals(5, eventService.countAction({startsWith: 'test'}))
    ywAssert.assertEquals(6, eventService.countAction())
}

export function test_02() {
    const yojaEvents = eventService.events()
    ywAssert.assertEquals('AAA', yojaEvents[0].id)
    ywAssert.assertEquals('test_01', yojaEvents[1].id)
    ywAssert.assertEquals('test_02', yojaEvents[2].id)
}

export function test_03() {
    const yojaEvent_01 = eventService.events('test_01')[0]
    const yojaEvent_02 = eventService.event('test_02')
    eventService.pause('test_01')
    ywAssert.assertEquals(false, yojaEvent_01.isActive())
    ywAssert.assertEquals(true, yojaEvent_02.isActive())
    eventService.activate('test_01')
    ywAssert.assertEquals(true, yojaEvent_01.isActive())
    ywAssert.assertEquals(true, yojaEvent_02.isActive())
}

export function test_04() {
    ywAssert.assertEquals(true, eventService.has('test_01'))
    ywAssert.assertEquals(true, eventService.has({startsWith: 'test_'}))
    ywAssert.assertEquals(true, eventService.has({endsWith: '01'}))
    ywAssert.assertEquals(true, eventService.has({endsWith: ['01', '02']}))
    ywAssert.assertEquals(false, eventService.has({endsWith: '03'}))
}

export function test_05() {
    eventService.remove('test_01')
    ywAssert.assertEquals(2, eventService.count())
    ywAssert.assertEquals(3, eventService.countAction())
    ywAssert.assertEquals([], eventService.events('test_01'))
    ywAssert.assertEquals(null, eventService.event('test_01'))
}
