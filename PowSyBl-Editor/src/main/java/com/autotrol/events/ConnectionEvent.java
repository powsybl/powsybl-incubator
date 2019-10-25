/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.events;

import javafx.event.Event;
import javafx.event.EventTarget;
//import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.InputEvent;

/**
 *
 * @author cgalli
 */
public class ConnectionEvent extends InputEvent {
    //EventType's names
    public static final String                      CONNECT_TERMINAL_NAME           ="CONNECT_TERMINAL";
    public static final String                      INSERT_COMPONENT_NAME           ="INSERT_COMPONENT";
    public static final String                      INSERT_BUSBAR_SECTION_NAME      ="INSERT_BUSBAR_SECTION";
    public static final String                      INSERT_SWITCH_NAME              ="INSERT_SWITCH";
    public static final String                      SEARCH_REGULATING_TERMINAL_NAME ="SEARCH_REGULATING_TERMINAL";
    //Event types
    public static       EventType<ConnectionEvent>  CONNECT_TERMINAL                =new EventType<ConnectionEvent>(Event.ANY, CONNECT_TERMINAL_NAME);
    public static       EventType<ConnectionEvent>  INSERT_COMPONENT                =new EventType<ConnectionEvent>(Event.ANY, INSERT_COMPONENT_NAME);
    public static       EventType<ConnectionEvent>  INSERT_BUSBAR_SECTION           =new EventType<ConnectionEvent>(Event.ANY, INSERT_BUSBAR_SECTION_NAME);
    public static       EventType<ConnectionEvent>  INSERT_SWITCH                   =new EventType<ConnectionEvent>(Event.ANY, INSERT_SWITCH_NAME);
    public static       EventType<ConnectionEvent>  SEARCH_REGULATING_TERMINAL      =new EventType<ConnectionEvent>(Event.ANY, SEARCH_REGULATING_TERMINAL_NAME);
    //
    
    //Prameters
    private final       EventTarget                 finalTarget;
    private final       Object                      originalSource;
    private             int                         terminalNumber                  =-1;
    
    //
    
    public static ConnectionEvent buildConnectTermEvt(Object source, int terminalNumber)
    {
        ConnectionEvent evt =new ConnectionEvent(source, CONNECT_TERMINAL);
        
        evt.terminalNumber  =terminalNumber;
        return evt;
    }
    
    public ConnectionEvent(Object source, EventType<ConnectionEvent> type)
    {
        super(source, null, type);
        this.source     =source;
        originalSource  =source;
        finalTarget     =target;
    }
    
    public ConnectionEvent(Object source, EventTarget finalTarget, EventType<ConnectionEvent> type)
    {
        super(source, finalTarget, type);
        this.source         =source;
        this.originalSource =source;
        this.target         =finalTarget;
        this.finalTarget    =finalTarget;
    }
    
    public EventTarget getFinalTarget()
    {
        return finalTarget;
    }
    
    public Object getOriginalSource()
    {
        return originalSource;
    }
    
    public  int getTerminalNumber()
    {
        return terminalNumber;
    }
}
