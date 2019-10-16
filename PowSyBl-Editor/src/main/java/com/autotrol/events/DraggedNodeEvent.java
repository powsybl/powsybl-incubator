/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.events;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.InputEvent;

/**
 *
 * @author cgalli
 */
public class DraggedNodeEvent extends InputEvent{
    public static           EventType<DraggedNodeEvent>   NODE_DRAG_START =new EventType<DraggedNodeEvent>(Event.ANY, "NODE_DRAG_START");
    public static           EventType<DraggedNodeEvent>   NODE_DRAGGED    =new EventType<DraggedNodeEvent>(Event.ANY, "NODE_DRAGGED");
    private final double    dX;
    private final double    dY;
    
//    public DraggedNodeEvent(EventType<? extends MouseEvent> eventType, double x, double y, double screenX, double screenY, MouseButton button, int clickCount, boolean shiftDown, boolean controlDown, boolean altDown, boolean metaDown, boolean primaryButtonDown, boolean middleButtonDown, boolean secondaryButtonDown, boolean synthesized, boolean popupTrigger, boolean stillSincePress, PickResult pickResult) {
//        super(eventType, x, y, screenX, screenY, button, clickCount, shiftDown, controlDown, altDown, metaDown, primaryButtonDown, middleButtonDown, secondaryButtonDown, synthesized, popupTrigger, stillSincePress, pickResult);
//    }
//
//    public DraggedNodeEvent(Object source, EventTarget target, EventType<? extends MouseEvent> eventType, double x, double y, double screenX, double screenY, MouseButton button, int clickCount, boolean shiftDown, boolean controlDown, boolean altDown, boolean metaDown, boolean primaryButtonDown, boolean middleButtonDown, boolean secondaryButtonDown, boolean synthesized, boolean popupTrigger, boolean stillSincePress, PickResult pickResult) {
//        super(source, target, eventType, x, y, screenX, screenY, button, clickCount, shiftDown, controlDown, altDown, metaDown, primaryButtonDown, middleButtonDown, secondaryButtonDown, synthesized, popupTrigger, stillSincePress, pickResult);
//    }
    public DraggedNodeEvent(Object source, double dx, double dy)
    {
        super(source, null, NODE_DRAGGED);
        this.dX= dx;
        this.dY= dy;
    }
    
    public DraggedNodeEvent(Object source)
    {
        super(source, null, NODE_DRAG_START);
        this.dX= 0;
        this.dY= 0;
    }
    
    public double getDX()
    {
        return dX;
    }
    
    public double getDY()
    {
        return dY;
    }
}
