/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.powsybl.extensions;

import com.powsybl.substationdiagram.library.ComponentTypeName;
import java.util.function.Predicate;

/**
 *
 * @author cgalli
 */
public class ConnectionValidators
{
    //NodeHandler validators
    public static final Predicate<String>    defaultNHValidator                  =(String componentType)  ->  true;
    public static final Predicate<String>    VoltageLevelNHValidator             =(String componentType)  ->  false;//When searching VL's, all components must be invalid
    public static final Predicate<String>    BusBreakerTerminalNHValidator       =(String componentType)  ->  componentType.equals(ComponentTypeName.BUSBAR_SECTION);
    public static final Predicate<String>    NodeBreakerTerminalNHValidator      =(String componentType)  ->  /*componentType==ComponentTypeName.BUSBAR_SECTION||*/ componentType.equals(ComponentTypeName.NODE);
    public static final Predicate<String>    NodeBreakerSwitchTerminalNHValidator=(String componentType)  ->  componentType.equals(ComponentTypeName.BUSBAR_SECTION)||componentType.equals(ComponentTypeName.NODE);//Used for insertion of switches
    public static final Predicate<String>    RegulationTerminalNHValidator       =(String componentType)  ->  componentType.equals(ComponentTypeName.BUSBAR_SECTION) ||
                                                                                                              componentType.equals(ComponentTypeName.NODE) ||
                                                                                                              componentType.equals(ComponentTypeName.CAPACITOR) ||
                                                                                                              componentType.equals(ComponentTypeName.LINE) ||
                                                                                                              componentType.equals(ComponentTypeName.DANGLING_LINE) ||
                                                                                                              componentType.equals(ComponentTypeName.PHASE_SHIFT_TRANSFORMER) ||
                                                                                                              componentType.equals(ComponentTypeName.TWO_WINDINGS_TRANSFORMER) ||
                                                                                                              componentType.equals(ComponentTypeName.THREE_WINDINGS_TRANSFORMER) ||
                                                                                                              componentType.equals(ComponentTypeName.INDUCTOR) ||
                                                                                                              componentType.equals(ComponentTypeName.GENERATOR) ||
                                                                                                              componentType.equals(ComponentTypeName.STATIC_VAR_COMPENSATOR) ||
                                                                                                              componentType.equals(ComponentTypeName.VSC_CONVERTER_STATION) ||
                                                                                                              componentType.equals(ComponentTypeName.LOAD);
}
