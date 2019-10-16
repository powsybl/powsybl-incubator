/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.powsybl.iidm.network.impl;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;

/**
 *
 * @author cgalli
 */
public class Builders
{
    @SuppressWarnings("NonPublicExported")
    public static TerminalExt BuildTerminalExtBBT(Network network, VoltageLevel voltageLevel, Validable validable, String busId, String connectableBusId)
    {
        NetworkImpl nwi     =(NetworkImpl)network;
        TerminalExt terminal=(new TerminalBuilder(nwi.getRef(),validable)).setBus(busId).setConnectableBus(connectableBusId).build();
        terminal.setVoltageLevel((VoltageLevelExt) voltageLevel);
        terminal.setConnectable((AbstractConnectable) validable);
        ((VoltageLevelExt) voltageLevel).attach(terminal, false);
        return terminal;
        
        
    }
    
    @SuppressWarnings("NonPublicExported")
    public static TerminalExt BuildTerminalExtNBT(Network network, VoltageLevel voltageLevel, Validable validable, int node)
    {
        NetworkImpl nwi     =(NetworkImpl)network;
        TerminalExt terminal=(new TerminalBuilder(nwi.getRef(),validable)).setNode(node).build();
        terminal.setVoltageLevel((VoltageLevelExt) voltageLevel);
        terminal.setConnectable((AbstractConnectable) validable);
        ((VoltageLevelExt) voltageLevel).attach(terminal, false);
        return terminal;
        
        
    }
    
    public static void detachTerminal(Terminal term)
    {
        ((VoltageLevelExt)term.getVoltageLevel()).detach((TerminalExt) term);
    }
        
    public static void restoreTerminalBBT(Bus oldBus, Terminal term)
    {
        VoltageLevelExt vlExt=(VoltageLevelExt) oldBus.getVoltageLevel();
        
        ((TerminalExt)term).setVoltageLevel(vlExt);
        ((TerminalExt)term).getBusBreakerView().setConnectableBus(oldBus.getId());
        vlExt.attach((TerminalExt)term, false);
        
    }
            
    public static void attachTerminal(VoltageLevel vl, Terminal term)
    {
        VoltageLevelExt vlExt   =(VoltageLevelExt) vl;
        vlExt.attach((TerminalExt) term, false);
    }
    
}
