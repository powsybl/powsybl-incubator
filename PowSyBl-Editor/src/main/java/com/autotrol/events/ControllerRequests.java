/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * Events intended to be passed from visual handlers or SelectionMnager to main FXML controller
 * @author cgalli
 */
public class ControllerRequests extends Event
{
    //EventType's names
    public static final String                          SHOW_MESSSAGE_NAME      ="SHOW_MESSAGE";
    public static final String                          REDRAW_GRAPH_NAME       ="REDRAW_GRAPH";
    public static final String                          SELECT_SUBSTATION_NAME  ="SELECT_SUBSTATION";
    
    //Event types
    public static final EventType<ControllerRequests>   REDRAW_GRAPH        =new EventType<>(Event.ANY, REDRAW_GRAPH_NAME);
    public static final EventType<ControllerRequests>   SHOW_MESSAGE        =new EventType<>(Event.ANY, SHOW_MESSSAGE_NAME);
    public static final EventType<ControllerRequests>   SELECT_SUBSTATION   =new EventType<>(Event.ANY, SELECT_SUBSTATION_NAME);
    //
    
    //Predefined Events
    public static final ControllerRequests              REDRAW_REQUEST      =new ControllerRequests(null, REDRAW_GRAPH);
    //
    
    public ControllerRequests(Object source, EventType<ControllerRequests> type)
    {
        super(source, null, type);
    }
}
