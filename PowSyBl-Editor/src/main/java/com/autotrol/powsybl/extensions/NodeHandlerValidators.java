/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.powsybl.extensions;

import java.util.function.Predicate;

/**
 *
 * @author cgalli
 */
public class NodeHandlerValidators {
    public static final Predicate<NodeHandler>      allowAllNH              =(NodeHandler   nh) ->  true;   //Deny all
    public static final Predicate<NodeHandler>      denyAllNH               =(NodeHandler   nh) ->  false;  //Allow all

    public static Predicate<NodeHandler>allowOnlyVL(String vlID)
    {
        return (NodeHandler nh) -> {return nh.getVId().equals(vlID);};
    }
    
    public static Predicate<NodeHandler>denyOnlyVL(String vlID)
    {
        return (NodeHandler nh) -> {return !(nh.getVId().equals(vlID));};
    }
    
    
}
