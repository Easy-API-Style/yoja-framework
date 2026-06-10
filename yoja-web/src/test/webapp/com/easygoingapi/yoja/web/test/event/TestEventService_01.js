'use strict'

const eventService = yojaWebApi.eventService

export function test_01() {
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
    
    const events_test_01 = eventService.events('test_01')
    ywAssert.assertEquals(1, events_test_01.length)
    const yojaEvent = events_test_01[0]
    
    ywAssert.assertEquals('test_01', yojaEvent.id)
    
    ywAssert.assertEquals('test_01_action_01', yojaEvent.getAction(1).label)
    ywAssert.assertEquals('test_01_action_02', yojaEvent.getAction(2).label)
    ywAssert.assertEquals('test_01_action_03', yojaEvent.getAction(3).label)
    ywAssert.assertEquals(3, yojaEvent.size())
    
    yojaEvent.moveAction(2, 5)
    ywAssert.assertEquals('test_01_action_01', yojaEvent.getAction(1).label)
    ywAssert.assertEquals('test_01_action_03', yojaEvent.getAction(2).label)
    ywAssert.assertEquals('test_01_action_02', yojaEvent.getAction(3).label)
    ywAssert.assertEquals(3, yojaEvent.size())
    
    const removedAction = yojaEvent.removeAction(2)
    ywAssert.assertEquals('test_01_action_03', removedAction.label)
    ywAssert.assertEquals('test_01_action_01', yojaEvent.getAction(1).label)
    ywAssert.assertEquals('test_01_action_02', yojaEvent.getAction(2).label)
    ywAssert.assertEquals(2, yojaEvent.size())
        
    const newAction_98 = {
        label: 'test_01_action_98',
        action: p => console.info('test_01_action_98 -> ' + p)
    }
    newAction_98.position = 0
    yojaEvent.addAction(newAction_98)
    ywAssert.assertEquals('test_01_action_01', yojaEvent.getAction(1).label)
    ywAssert.assertEquals('test_01_action_02', yojaEvent.getAction(2).label)
    ywAssert.assertEquals(2, yojaEvent.size())
    
    newAction_98.position = 1
    yojaEvent.addAction(newAction_98)
    ywAssert.assertEquals('test_01_action_98', yojaEvent.getAction(1).label)
    ywAssert.assertEquals('test_01_action_01', yojaEvent.getAction(2).label)
    ywAssert.assertEquals('test_01_action_02', yojaEvent.getAction(3).label)
    ywAssert.assertEquals(3, yojaEvent.size())
    
    ywAssert.assertEquals(false, yojaEvent.hasAction(0))
    ywAssert.assertEquals(true, yojaEvent.hasAction(1))
    ywAssert.assertEquals(true, yojaEvent.hasAction(2))
    ywAssert.assertEquals(true, yojaEvent.hasAction(3))
    ywAssert.assertEquals(false, yojaEvent.hasAction(4))
    
    const newAction_99 = {
        label: 'test_01_action_99',
        action: p => console.info('test_01_action_99 -> ' + p)
    }
    yojaEvent.addAction(newAction_99)
    ywAssert.assertEquals('test_01_action_98', yojaEvent.getAction(1).label)
    ywAssert.assertEquals('test_01_action_01', yojaEvent.getAction(2).label)
    ywAssert.assertEquals('test_01_action_02', yojaEvent.getAction(3).label)
    ywAssert.assertEquals('test_01_action_99', yojaEvent.getAction(4).label)
    ywAssert.assertEquals(4, yojaEvent.size())
    
    yojaEvent.active(false)
    ywAssert.assertEquals(false, yojaEvent.isActive())
    
    yojaEvent.active(true)
    ywAssert.assertEquals(true, yojaEvent.isActive())
    
    const yojaEventAsJson = yojaEvent.toJson()
    ywAssert.assertEquals('test_01', yojaEventAsJson.id)
    ywAssert.assertEquals(true, yojaEventAsJson.active)
    ywAssert.assertEquals(4, yojaEventAsJson.actions.length)
    ywAssert.assertEquals('test_01_action_98',  yojaEventAsJson.actions[0].label)
    ywAssert.assertEquals(1,  yojaEventAsJson.actions[0].position)
}

export function test_02() {
    const actions = eventService.actions('test_01')
    ywAssert.assertEquals(4, actions.length)
    ywAssert.assertEquals('test_01_action_98', actions[0].label)
    ywAssert.assertEquals('test_01_action_01', actions[1].label)
    ywAssert.assertEquals('test_01_action_02', actions[2].label)
    ywAssert.assertEquals('test_01_action_99', actions[3].label)
    ywAssert.assertEquals(1, actions[0].position)
    ywAssert.assertEquals(2, actions[1].position)
    ywAssert.assertEquals(3, actions[2].position)
    ywAssert.assertEquals(4, actions[3].position)
    
    ywAssert.assertEquals('test_01', actions[0].eventId)
    ywAssert.assertEquals(true, actions[0].isActive())
}

export function test_03() {
    const yojaEvent = eventService.events('test_01')[0]
    eventService.pause('test_01')
    ywAssert.assertEquals(false, yojaEvent.isActive())
    eventService.activate('test_01')
    ywAssert.assertEquals(true, yojaEvent.isActive())
}

export function test_04() {
    ywAssert.assertEquals(true, eventService.has('test_01'))
    ywAssert.assertEquals(true, eventService.has({startsWith: 'test_'}))
    ywAssert.assertEquals(true, eventService.has({endsWith: '01'}))
    ywAssert.assertEquals(true, eventService.has({endsWith: ['01', '02']}))
    ywAssert.assertEquals(false, eventService.has({endsWith: '02'}))
    
    ywAssert.assertEquals(0, eventService.count({endsWith: '02'}))
    ywAssert.assertEquals(1, eventService.count({startsWith: 'test_'}))
    
    ywAssert.assertEquals(0, eventService.countAction({endsWith: '02'}))
    ywAssert.assertEquals(4, eventService.countAction({startsWith: 'test_'}))
}

export function test_05() {
    const yojaEvent = eventService.events('test_01')[0]
    yojaEvent.clearAction()
    ywAssert.assertEquals(0, yojaEvent.size())
    
}
