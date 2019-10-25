/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.substationdiagram.library.ComponentTypeName;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author cgalli
 */
public class ComponentTypeMap {
//    public static enum<String>  ComponentTypes;
    private static final    ArrayList<Field>    fields      =Lists.newArrayList(ComponentTypeName.class.getDeclaredFields());
    private static final    List<String>        fieldNames  =Lists.transform(fields.stream().filter((Field t) -> Modifier.isStatic(t.getModifiers())&&t.getType().equals(String.class)).collect(Collectors.toList()), Field::getName);
    
    
    public static final     HashMap<String, Class>   networkTypes    =new HashMap<>( 
//            Maps.asMap((Set<String>)Sets.newHashSet(ComponentType.values()), (ComponentType t) ->
            Maps.asMap((Set<String>)Sets.newHashSet(fieldNames), (String t) ->
            {
                switch(t)
                {
                    case ComponentTypeName.NODE:
                    case ComponentTypeName.BUSBAR_SECTION:
                        return BusbarSection.class;
                    case ComponentTypeName.BREAKER:
                    case ComponentTypeName.DISCONNECTOR:
                    case ComponentTypeName.LOAD_BREAK_SWITCH:
                        return Switch.class;
                    case ComponentTypeName.CAPACITOR:
                    case ComponentTypeName.INDUCTOR:
                        return ShuntCompensator.class;
                    case ComponentTypeName.GENERATOR:
                        return Generator.class;
                    case ComponentTypeName.DANGLING_LINE:
                        return DanglingLine.class;
                    case ComponentTypeName.LINE:
                        return Line.class;
                    case ComponentTypeName.LOAD:
                        return Load.class;
                    case ComponentTypeName.PHASE_SHIFT_TRANSFORMER:
                    case ComponentTypeName.TWO_WINDINGS_TRANSFORMER:
                        return TwoWindingsTransformer.class;
                    case ComponentTypeName.THREE_WINDINGS_TRANSFORMER:
                        return ThreeWindingsTransformer.class;
                    case ComponentTypeName.STATIC_VAR_COMPENSATOR:
                        return StaticVarCompensator.class;
                    case ComponentTypeName.VSC_CONVERTER_STATION:
                        return HvdcConverterStation.class;
//                        return VscConverterStation.class;
                    default:
                        return null;
                }
            })
    );
    
}
