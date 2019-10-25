/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.util;

import afester.javafx.svg.SvgLoader;
import com.autotrol.events.ConnectionEvent;
import com.autotrol.events.ControllerRequests;
import com.autotrol.events.DraggedNodeEvent;
import com.autotrol.powsybl.extensions.ConnectionValidators;
import com.autotrol.powsybl.extensions.NodeHandler;
import com.autotrol.powsybl.extensions.NodeHandlerValidators;
import com.autotrol.powsybl.extensions.VisualHandlerModes;
import com.autotrol.powsybl.extensions.VoltageLevelHandler;
import com.autotrol.powsybl.extensions.WireHandler;
import com.google.common.collect.Lists;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusAdder;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.BusbarSectionAdder;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.DanglingLineAdder;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.GeneratorAdder;
import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.HvdcConverterStation.HvdcType;
import com.powsybl.iidm.network.HvdcConverterStationAdder;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.LccConverterStation;
import com.powsybl.iidm.network.LccConverterStationAdder;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.LineAdder;
import com.powsybl.iidm.network.LineCharacteristics;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.LoadAdder;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChanger.RegulationMode;
import com.powsybl.iidm.network.PhaseTapChangerAdder;
import com.powsybl.iidm.network.PhaseTapChangerHolder;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.RatioTapChangerAdder;
import com.powsybl.iidm.network.RatioTapChangerHolder;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.ShuntCompensatorAdder;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.StaticVarCompensatorAdder;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.SwitchKind;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.Terminal.NodeBreakerView;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.ThreeWindingsTransformer.Leg1;
import com.powsybl.iidm.network.ThreeWindingsTransformer.Leg2or3;
import com.powsybl.iidm.network.ThreeWindingsTransformerAdder;
import com.powsybl.iidm.network.ThreeWindingsTransformerAdder.Leg1Adder;
import com.powsybl.iidm.network.ThreeWindingsTransformerAdder.Leg2or3Adder;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.TieLine.HalfLine;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformerAdder;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.VoltageLevel.NodeBreakerView.SwitchAdder;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.iidm.network.VscConverterStationAdder;
import com.powsybl.substationdiagram.library.AnchorPoint;
import com.powsybl.substationdiagram.library.ComponentMetadata;
import com.powsybl.substationdiagram.library.ComponentTypeName;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.GraphMetadata.NodeMetadata;
import com.sun.javafx.scene.control.skin.TableHeaderRow;
import fxml.MainFXMLController;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import com.powsybl.iidm.network.impl.*;
import com.powsybl.substationdiagram.layout.CellBlockDecomposer;
import com.powsybl.substationdiagram.layout.ImplicitCellDetector;
import com.powsybl.substationdiagram.model.Block;
import com.powsybl.substationdiagram.model.BusCell;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.Cell;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyles;
import com.powsybl.substationdiagram.view.DisplayVoltageLevel;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.prism.NGNode;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;
//import javax.xml.stream.EventFilter;
//import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author cgalli
 */
public class SelectionManager implements EventHandler<Event>, DisplayVoltageLevel
{
    public static final     int                                                 OPERATION_STORE_MODES   =0;
    public static final     int                                                 OPERATION_RESTORE_MODES =1;
    
    public static final     DataFormat                                          graphCellDataFormat     =new DataFormat("powsybl/graphCell");
    
    private                 Group                                               contentPane;
    private                 Network                                             network;
    private                 TableView                                           propGrid;
    private                 GraphMetadata                                       metadata;
    private final           SimpleObjectProperty<Connectable>                   updatingConnectable =new SimpleObjectProperty();
//    private final           SimpleObjectProperty<Switch>                        updatingSwitch      =new SimpleObjectProperty();
    private final           SimpleObjectProperty<SwitchKind>                    insertingSwKind     =new SimpleObjectProperty<>(null);
    private final           SimpleObjectProperty<Class<? extends Connectable>>  insertingConnectable=new SimpleObjectProperty<>(null);
    private final           ObservableList<Node>                                lastSelectedNodes   =FXCollections.observableArrayList();
//    private final           SimpleObjectProperty<BusCell>                       selectedBusCell     =new SimpleObjectProperty<>();
    private final           SimpleObjectProperty<Cell>                          selectedCell     =new SimpleObjectProperty<>();
    private                 double                                              multSelX;
    private                 double                                              multSelY;
    private final           Rectangle                                           multSelRect         =new Rectangle();
//    private final           List<WireHandler>                                   wireHandlers        =new ArrayList<>();
    private final           Map<String, WireHandler>                            wireHandlers        =new HashMap<>();
    private final           Map<String, NodeHandler>                            nodeHandlers        =new HashMap<>();
    private final           Map<String, VoltageLevelHandler>                    vlHandlers          =new HashMap<>();
    private final           SimpleIntegerProperty                               changingTerm        =new SimpleIntegerProperty();
    private final           SimpleBooleanProperty                               managerIsValid      =new SimpleBooleanProperty();
    public static final     ArrayList<Class>                                    connectableInterf   =new ArrayList<>(Arrays.asList(new Class[]{
//                                                                                                                            Battery                 .class,
                                                                                                                            BusbarSection           .class,
                                                                                                                            DanglingLine            .class,
                                                                                                                            Generator               .class,
                                                                                                                            HvdcConverterStation    .class,
                                                                                                                            LccConverterStation     .class,
                                                                                                                            Line                    .class,
                                                                                                                            Load                    .class,
                                                                                                                            ShuntCompensator        .class,
                                                                                                                            StaticVarCompensator    .class,
                                                                                                                            TwoWindingsTransformer  .class,
                                                                                                                            ThreeWindingsTransformer.class,
                                                                                                                            TieLine                 .class,
                                                                                                                            VscConverterStation     .class
                                                                                                                        })
                                                                                                                    );
    
    private static final    SimpleObjectProperty<VisualHandlerModes>            storedMode          =new SimpleObjectProperty<VisualHandlerModes>();
    private static final    SimpleObjectProperty<Predicate<String>>             storedConnValidator =new SimpleObjectProperty<Predicate<String>>();
    private static final    SimpleObjectProperty<Predicate<NodeHandler>>        storedNHValidator   =new SimpleObjectProperty<Predicate<NodeHandler>>();
    private static final    SimpleObjectProperty                                storedFinalTarget   =new SimpleObjectProperty();
    private static final    SimpleObjectProperty                                storedOriginalSource=new SimpleObjectProperty();
    
    public        final     Node                                                SelectionManagerTgt =new Node() {
        @Override
        protected NGNode impl_createPeer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected boolean impl_computeContains(double localX, double localY) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };//Used as EventTarget for general messages sent from this SelectionManager
      //Use its userData memeber as data carrier
    
    @SuppressWarnings("Convert2Lambda")
    public SelectionManager()
    {
        multSelRect.setFill(Paint.valueOf("#03AA0011"));
        multSelRect.setStroke(Paint.valueOf("#03AA00ff"));
        multSelRect.setStrokeWidth(1.5);
        multSelRect.setStrokeLineCap(StrokeLineCap.ROUND);
        multSelRect.setVisible(false);
    }
    
    /**
    @param contentPane The pane holding the svg representation of the network
     * @param metadata
     * @param network
    @param propGrid TreeView object to render iidm properties associated to the in-screen selected object 
    */
    @SuppressWarnings("Convert2Lambda")
    public void manage(Group contentPane, GraphMetadata metadata, Network network, TableView propGrid)
    {
        StringWriter    sw  =new StringWriter();
        StringWriter    mw  =new StringWriter();
        
        wireHandlers.   clear();
        nodeHandlers.   clear();
        vlHandlers.     clear();
        
        if(contentPane==null || network==null || propGrid==null || metadata==null)
        {
            this.contentPane    =null;
            this.network        =null;
            this.propGrid       =null;
            this.metadata       =null;
            this.managerIsValid.set(false);
            
            return;
        }
        
        this.contentPane    =contentPane;
        this.network        =network;
        this.propGrid       =propGrid;
        this.metadata       =metadata;
        
//        if(this.contentPane!=null)
        {
            this.contentPane.getScene().getRoot().setOnKeyPressed(event-> {
//                @Override
//                public void handle(KeyEvent event) {
                    
                    switch(event.getCode())
                    {
                        case ESCAPE:
                            if(NodeHandler.getMode().equals(VisualHandlerModes.CONNECTION) ||
                                NodeHandler.getMode().equals(VisualHandlerModes.COMPONENT_INSERTION) ||
                                NodeHandler.getMode().equals(VisualHandlerModes.SWITCH_INSERTION) ||
                                NodeHandler.getMode().equals(VisualHandlerModes.REGULATING_TERM_SEARCH) ||
                                NodeHandler.getMode().equals(VisualHandlerModes.CELL_SELECTION) ||
                                NodeHandler.getMode().equals(VisualHandlerModes.BUSBAR_SECTION_INSERTION))
                            {
//                                NodeHandler.setMode(VisualHandlerModes.NORMAL);
                                VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                                this.contentPane.getParent().setCursor(Cursor.DEFAULT);
                            }
                            break;
                        default:
                            ;
                    }
//                }
            });
            if(!((Pane)(this.contentPane.getParent())).getChildren().contains(multSelRect))
                ((Pane)(this.contentPane.getParent())).getChildren().add(multSelRect);
            installEditionHandlers(this.contentPane);
//            if(this.metadata!=null)
                installVisualHandlers(this.contentPane, this.metadata);
            installMultSelectionControl();
//            setNodeHandlerValidator("TERMINAL_FOR_CONNECTABLE");
        }
        
//        if(propGrid!=null)
        {
            for(Node next:this.propGrid.getChildrenUnmodifiable().filtered((Node t) -> (t instanceof TableHeaderRow)))
                for(TableColumn<Entry<String,Object>,Object> col:(ObservableList<TableColumn<Entry<String,Object>,Object>>)this.propGrid.getColumns())
                    ((TableHeaderRow)next).getColumnHeaderFor(col).setMouseTransparent(true);

            lastSelectedNodes.addListener(new ListChangeListener() {
                @Override
                public void onChanged(ListChangeListener.Change c) {
                    c.next();
                    for(Object next:c.getAddedSubList().toArray())
                    {
                        ((Node)next).setEffect(new DropShadow(BlurType.GAUSSIAN, Color.valueOf("#ffffff99"), 6.0, 0.2, 3.0, 3.0));

                    }
                    for(Object next:c.getRemoved().toArray())
                    {
                        ((Node)next).setEffect(null);
                    }

                    if(lastSelectedNodes.isEmpty())
                        propGrid.getItems().clear();
                }
            });
            if(VoltageLevelHandler.getMode()==VisualHandlerModes.NORMAL)
                this.propGrid.getItems().clear();
        }
        
        
        multSelRect.setVisible(false);
        managerIsValid.set(true);
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public void renderSubstation(int index)
    {
        if(network==null)
            return;
        if(network.getSubstationCount()<1)
            return;
        try
        {
            Substation                              substation  =(Substation) network.getSubstationStream().toArray()[index];
            ObservableList<Entry<String, Object>>   itemList;
            propGrid.getItems().clear();
            propGrid.refresh();
            System.gc();
            
            itemList    =propGrid.getItems();
            getParamsSubstation(substation, itemList);
            
            propGrid.setItems(itemList);
            propGrid.refresh();
        }
        catch(Exception ex)
        {
            showAlert(ex);
        }
    }
    
    @SuppressWarnings("null")
    private void render(Node selected) throws NoSuchMethodException, SecurityException, UnsupportedEncodingException
    {
        String                                  id              =new String(selected.getId().getBytes(Charset.forName("UTF-8")), "UTF8");
        
        if(id==null?true:"".equals(id))
            return;

        String                                  vid             =metadata.getNodeMetadata(id)!=null?metadata.getNodeMetadata(id).getVId():null;
        String                                  componentType   =metadata.getNodeMetadata(id)!=null?metadata.getNodeMetadata(id).getComponentType():null;
//        ComponentType                           componentType   =metadata.getNodeMetadata(id)!=null?metadata.getNodeMetadata(id).getComponentType():null;
        ObservableList<Entry<String,Object>>    itemList        =FXCollections.observableArrayList();
        Identifiable                            identifiable    =getIdentifiable(id);
        
        propGrid.getItems().clear();
        propGrid.refresh();
        System.gc();
        
        if(identifiable!=null)
            getParams(identifiable, itemList, selected);
        else /*if(componentType.equals(ComponentType.NODE))*/
        {
            if(network.getVoltageLevel(vid).getTopologyKind().equals(TopologyKind.NODE_BREAKER))
            {
                tryNode(id, vid, componentType, itemList);
            }
            else
                itemList.add(new SimpleEntry<>(componentType ,new ReadOnlyObjectWrapper(id)));
//                itemList.add(new SimpleEntry<>(componentType.name() ,new ReadOnlyObjectWrapper(id)));
        }
        
        propGrid.setItems(itemList);
        
        propGrid.refresh();
        
    }
    
    private Identifiable getIdentifiable(String id)
    {
        String                                  id_             =id.replaceAll("(_fictif)$", "").replaceAll("(_THREE)$", "").replaceAll("(_TWO)$", "").replaceAll("(_ONE)$", "").replaceAll("(_FOUR)$", "").replaceAll("^(FICT_)", "").replaceAll("(Fictif)$", "").replaceAll("(fict)$", "").replaceAll("^(LABEL_VL_)","");
        final String                            id2_;
        String                                  vid             =metadata.getNodeMetadata(id)!=null?metadata.getNodeMetadata(id).getVId():null;
        String                                  componentType   =metadata.getNodeMetadata(id)!=null?metadata.getNodeMetadata(id).getComponentType():null;
//        ComponentType                           componentType   =metadata.getNodeMetadata(id)!=null?metadata.getNodeMetadata(id).getComponentType():null;
        ObservableList<Entry<String,Object>>    itemList        =FXCollections.observableArrayList();
        Identifiable                            identifiable    =network.getIdentifiable(componentType!=null?id_:vid);
        Stream<Identifiable<?>>                 filterType      ;
        Stream<Identifiable<?>>                 filterTypeEscId ;
        Stream<Identifiable<?>>                 filterTypeName  ;
        
        if(componentType!=null && identifiable==null)
        {
            id2_=id_.replaceAll("^("+vid+"_)","");
            identifiable=network.getIdentifiable(id2_);//try once again
            if(identifiable==null)//Try with "escaped" id
            {
                filterType      =network.getIdentifiables().stream().filter((Identifiable t) -> {
                                                                                            Class   selectedType    =ComponentTypeMap.networkTypes.get(componentType);
                                                                                            return selectedType.isAssignableFrom(t.getClass());
                                                                                        });
                filterTypeName  =filterType.filter((Identifiable t) -> {
                                                                                            return SubstationDiagramStyles.escapeId(t.getId()).equals(id);
                                                                                        });
                Object[]  filteredArray   =filterTypeName.toArray();
                if(filteredArray!=null?filteredArray.length!=1:true)
                    ;
                else
                    identifiable=(Identifiable) filteredArray[0];
            }
            if(identifiable==null)//try with Name
            {
                filterType=network.getIdentifiables().stream().filter((Identifiable t) -> {
                                                                                            Class   selectedType    =ComponentTypeMap.networkTypes.get(componentType);
                                                                                            return selectedType.isAssignableFrom(t.getClass());
                                                                                        });
                filterTypeEscId  =filterType.filter((Identifiable t) -> {
                                                                                            return t.getName().equals(id2_);
                                                                                        });
                Object[]  filteredArray   =filterTypeEscId.toArray();
                if(filteredArray!=null?filteredArray.length!=1:true)
                    ;
                else
                    identifiable=(Identifiable) filteredArray[0];
            }
        }
        return identifiable;
    }
    
    private Pair<Identifiable, Class<? extends Identifiable>> findNetElem(VoltageLevel vl, String id_)
    {
        if(id_==null)
            Objects.requireNonNull(vl, "Voltage Level can't be null");
        
        Identifiable                    identifiable    =id_!=null?network.getIdentifiable(id_):vl;
        Class<? extends Identifiable>   clazz           =null;
        
        if(identifiable instanceof VoltageLevel)
            clazz=VoltageLevel.class;
        if(identifiable instanceof Bus)
            clazz=Bus.class;
        if(identifiable instanceof BusbarSection)
            clazz=BusbarSection.class;
        else if(identifiable instanceof Switch)
            clazz=Switch.class;
        else if(identifiable instanceof Line)
            clazz=Line.class;
        else if(identifiable instanceof DanglingLine)
            clazz=DanglingLine.class;
        else if(identifiable instanceof TwoWindingsTransformer)
            clazz=TwoWindingsTransformer.class;
        else if(identifiable instanceof ThreeWindingsTransformer)
            clazz=ThreeWindingsTransformer.class;
        else if(identifiable instanceof Generator)
            clazz=Generator.class;
        else if(identifiable instanceof Load)
            clazz=Load.class;
        else if(identifiable instanceof ShuntCompensator)
            clazz=ShuntCompensator.class;
        else if(identifiable instanceof StaticVarCompensator)
            clazz=StaticVarCompensator.class;
        else if(identifiable instanceof HvdcConverterStation)
        {
            if(((HvdcConverterStation)identifiable).getHvdcType()==HvdcType.VSC)
                clazz=VscConverterStation.class;
            else
                clazz=LccConverterStation.class;
        }
        
        return new Pair(identifiable, clazz);
    }
    
    private void getParams(Identifiable elem, ObservableList<Entry<String,Object>> itemList, Node... selected) throws NoSuchMethodException
    {
        Connectable     connElem    =elem instanceof Connectable    ?(Connectable)elem  :null;
        Bus             busElem     =elem instanceof Bus            ?(Bus)elem          :null;
        Switch          switElem    =elem instanceof Switch         ?(Switch)elem       :null;
        VoltageLevel    voltLevel   =elem instanceof VoltageLevel   ?(VoltageLevel)elem :null;//busElem!=null?busElem.getVoltageLevel():network.getVoltageLevel(vlId);
        String          typeName    =connElem!=null?connElem.getType().name().replaceAll("_", " "):busElem!=null?"BUS":switElem!=null?"SWITCH":voltLevel!=null?"VOLTAGE LEVEL":"?";
        List<Terminal>  terminals   =connElem!=null?(List<Terminal>)connElem.getTerminals():null;
        
        itemList.add(new SimpleEntry<>("Type"   ,new ReadOnlyObjectWrapper(typeName)));
        itemList.add(new SimpleEntry<>("Id"     ,new ReadOnlyObjectWrapper(elem.getId())));
        itemList.add(new SimpleEntry<>("Name"   ,new ReadOnlyObjectWrapper(elem.getName())));
        
        if(voltLevel!=null)
        {
            ChoiceBox   kindCh  =new ChoiceBox(); kindCh.getItems().addAll((Object[])TopologyKind.values()); kindCh.getSelectionModel().select(voltLevel.getTopologyKind());kindCh.setMouseTransparent(true);
            
            itemList.add(new SimpleEntry<>("Topology Kind"                      ,kindCh));
            itemList.add(new SimpleEntry<>("Nominal Voltage"                    ,new SimpleDoubleProperty(voltLevel.getNominalV())));          valueListener(itemList.get(4).getValue(), voltLevel.getClass().getMethod("setNominalV",        double.class),voltLevel    ,false);
            itemList.add(new SimpleEntry<>("Low High Limit"                     ,new SimpleDoubleProperty(voltLevel.getHighVoltageLimit())));  valueListener(itemList.get(5).getValue(), voltLevel.getClass().getMethod("setHighVoltageLimit",double.class),voltLevel    ,false);
            itemList.add(new SimpleEntry<>("Low Voltage Limit"                  ,new SimpleDoubleProperty(voltLevel.getLowVoltageLimit())));   valueListener(itemList.get(6).getValue(), voltLevel.getClass().getMethod("setLowVoltageLimit", double.class),voltLevel    ,false);
            return;
        }
        
        voltLevel=  busElem     !=null?busElem.getVoltageLevel():
                    switElem    !=null?switElem.getVoltageLevel():
                    terminals   !=null?terminals.get(0).getVoltageLevel():
                    null;
        
        if(voltLevel==null)
            return;
        
        try
        {
            if(connElem!=null)
                getParamsConnectable(connElem   ,itemList, selected);
            else if(busElem!=null)
                getBusParams(busElem            ,itemList, selected);
            else if(switElem!=null)
                getSwitchParams(switElem        ,itemList, selected);
        }
        catch (NoSuchMethodException ex)
        {
            Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
//    private void tryNode(String id, String vid, ComponentType componentType, ObservableList<Entry<String,Object>> itemList)
    private void tryNode(String id, String vid, String componentType, ObservableList<Entry<String,Object>> itemList)
    {
        VoltageLevel    voltLevel       =network.getVoltageLevel(vid);
        
        if(voltLevel.getTopologyKind().equals(TopologyKind.NODE_BREAKER))
        {
            int node =getNodeFromGraph(id, vid);
            
            if(node>=0)
            {
                itemList.add(new SimpleEntry<>("Type"           ,new ReadOnlyObjectWrapper("NODE")));
                itemList.add(new SimpleEntry<>("Voltage Level"  ,new ReadOnlyObjectWrapper(voltLevel.getName())));
                itemList.add(new SimpleEntry<>("Node number"    ,new ReadOnlyObjectWrapper(node)));
            }
        }
        else
        {
            itemList.add(new SimpleEntry<>(componentType, new ReadOnlyObjectWrapper(id)));
//            itemList.add(new SimpleEntry<>(componentType.name(),new ReadOnlyObjectWrapper(id)));
        }
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private int getNodeFromGraph(String id, String vid)
    {
        VoltageLevel    voltLevel       =network.getVoltageLevel(vid);
        
        try
        {
            Matcher         matcher     =Pattern.compile("^(FICT_"+vid+"_){1}(.){1,}(((F)|(f))((ictif))){1}$").matcher(id);
            String          filteredId  =id.replaceFirst("^(FICT_"+vid+"_){1}", "").replaceFirst("(((F)|(f))((ictif))){1}$", ""); matcher.find();
            final String    switchId    =matcher.group().equals(id)?filteredId:null;

            if(switchId!=null)
            {
                Optional<Switch>optional    =network.getSwitchStream().filter(switch__->{return ((Switch)switch__).getId().equals(switchId);}).findFirst();
                Switch          switch_     =optional.isPresent()?optional.get():null;
                
                if(switch_!=null)
                {
                    Terminal    t1=voltLevel.getNodeBreakerView().getTerminal(voltLevel.getNodeBreakerView().getNode1(switchId));
                    Terminal    t2=voltLevel.getNodeBreakerView().getTerminal(voltLevel.getNodeBreakerView().getNode2(switchId));
                    
                    if(t1!=null)
                        return voltLevel.getNodeBreakerView().getNode2(switchId);
                    if(t2!=null)
                        return voltLevel.getNodeBreakerView().getNode1(switchId);
                }
            }
        }
        catch(Exception ex)
        {
            try
            {
                Matcher         matcher     =Pattern.compile("^(FICT_"+vid+"_){1}(\\d){1,}$").matcher(id);
                String          filteredId  =id.replaceFirst("^(FICT_"+vid+"_){1}", ""); matcher.find();
                final String    nodeId      =matcher.group().equals(id)?filteredId:null;
                if(nodeId!=null)
                    return Integer.valueOf(filteredId);
            }
            catch(Exception ex2)
            {}
        }
        return -1;
    }
    
    private void getParamsSubstation(Substation substation, ObservableList<Entry<String,Object>> itemList) throws NoSuchMethodException
    {
        ChoiceBox<Country>  countryBox  =new ChoiceBox();

        countryBox.getItems().addAll((Country[])Country.values());
        countryBox.getSelectionModel().select(substation.getCountry().get());
        itemList.add(new SimpleEntry<>("Substation Id"      ,new ReadOnlyObjectWrapper(substation.getId())));
        itemList.add(new SimpleEntry<>("Substation Name"    ,new ReadOnlyObjectWrapper(substation.getName()))); //valueListener(itemList.get(1)   ,Substation.class.getMethod("setName", String.class)    ,substation ,true);
        itemList.add(new SimpleEntry<>("Substation Country" ,countryBox));                                      valueListener(countryBox.getSelectionModel().selectedItemProperty() ,Substation.class.getMethod("setCountry", Country.class)    ,substation ,false);
        itemList.add(new SimpleEntry<>("Substation TSO"     ,new SimpleStringProperty(substation.getTso())));   valueListener(itemList.get(3).getValue()                            ,Substation.class.getMethod("setTso"    , String.class )    ,substation ,false);
        
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void getParamsConnectable(Connectable elem, ObservableList<Entry<String,Object>> itemList, Node... selected) throws NoSuchMethodException
    {
        Objects.requireNonNull(elem);
        
        Class           clazz       =elem.getClass();
        Class           psbInterf;
        Stream<Class>   filtered    =(((Stream<Class>)Arrays.stream(clazz.getInterfaces())).filter(interf->{return SelectionManager.connectableInterf.contains(interf);}));
        Optional<Class> psbInterf_  =filtered.findFirst();
        int             offset;//      =itemList.size();
        List<Terminal>  terminals   =(List<Terminal>)elem.getTerminals();
        VoltageLevel    voltLevel   =terminals.get(0).getVoltageLevel();
        
        if(!psbInterf_.isPresent())
            return;
        if(voltLevel==null)
            return;
        
        for(Terminal next:terminals)
        {
            VoltageLevel    nextVL  =next.getVoltageLevel();
//            Bus             bus     =clazz!=BusbarSection.class?next.getBusBreakerView().getConnectableBus():next.getBusView().getConnectableBus();
            Bus             bus     =nextVL!=null?nextVL.getTopologyKind()==TopologyKind.BUS_BREAKER?next.getBusBreakerView().getConnectableBus():null:null;
            Button          recoB   =new Button(nextVL!=null?nextVL.getTopologyKind()==TopologyKind.NODE_BREAKER?nextVL.getName()+" / " +next.getNodeBreakerView().getNode():bus.getName():"<Disconnected>");
            
            recoB.setOnAction(event->{
                SnapshotParameters  pars        =new SnapshotParameters();
                WritableImage       cursorImg;

                VoltageLevelHandler.setMode(VisualHandlerModes.CONNECTION);
                setNodeHandlerValidator("TERMINAL_FOR_CONNECTABLE");
                changingTerm.set(terminals.indexOf(next));
                updatingConnectable.set(elem);

                pars.setFill(Paint.valueOf("#ffffff33"));
                cursorImg=selected[0].snapshot(pars, null);
                contentPane.getParent().setCursor(new ImageCursor(cursorImg, cursorImg.getWidth()/2, cursorImg.getHeight()/2));
            });
            itemList.add(new SimpleEntry<>("Terminal "+String.valueOf(terminals.indexOf(next)+1),recoB));
        }
        offset     =itemList.size();
        for(Terminal next:terminals)
        {
            try{
                next.setP(next.getP());//Try to set P.  If not possible, then just show it.
                next.setQ(next.getQ());
                itemList.add(new SimpleEntry<>("P"+String.valueOf(terminals.indexOf(next)+1),new SimpleDoubleProperty(next.getP())));                                   valueListener(itemList.get(offset   ).getValue()   ,Terminal.class.getMethod("setP"                                     ,double.class)  ,next  ,false);
                itemList.add(new SimpleEntry<>("Q"+String.valueOf(terminals.indexOf(next)+1),new SimpleDoubleProperty(next.getQ())));                                   valueListener(itemList.get(offset+1 ).getValue()   ,Terminal.class.getMethod("setQ"                                     ,double.class)  ,next  ,false);
                itemList.add(new SimpleEntry<>("I"+String.valueOf(terminals.indexOf(next)+1),new ReadOnlyObjectWrapper(next.getI())));
            }
            catch(Exception ex)
            {
                itemList.add(new SimpleEntry<>("P"+String.valueOf(terminals.indexOf(next)+1),new ReadOnlyObjectWrapper(next.getP())));
                itemList.add(new SimpleEntry<>("Q"+String.valueOf(terminals.indexOf(next)+1),new ReadOnlyObjectWrapper(next.getQ())));
                itemList.add(new SimpleEntry<>("I"+String.valueOf(terminals.indexOf(next)+1),new ReadOnlyObjectWrapper(next.getI())));
            }
        }

        offset     =itemList.size();
        psbInterf   =psbInterf_.get();
        
        switch(psbInterf.getName())
        {
            case "com.powsybl.iidm.network.BusbarSection":
                itemList.add(new SimpleEntry<>("Voltage"                    ,new ReadOnlyObjectWrapper(((BusbarSection)(elem)).getV())));
                itemList.add(new SimpleEntry<>("Angle"                      ,new ReadOnlyObjectWrapper(((BusbarSection)(elem)).getAngle())));
                break;
            case "com.powsybl.iidm.network.DanglingLine":
                itemList.add(new SimpleEntry<>("B"                          ,new SimpleDoubleProperty(((DanglingLine)(elem)).getB())));                             valueListener(itemList.get(offset   ).getValue()   ,DanglingLine.class.getMethod("setB"                                 ,double.class)  ,elem  ,false);
                itemList.add(new SimpleEntry<>("G"                          ,new SimpleDoubleProperty(((DanglingLine)(elem)).getG())));                             valueListener(itemList.get(offset+1 ).getValue()   ,DanglingLine.class.getMethod("setG"                                 ,double.class)  ,elem  ,false);
                itemList.add(new SimpleEntry<>("R"                          ,new SimpleDoubleProperty(((DanglingLine)(elem)).getR())));                             valueListener(itemList.get(offset+2 ).getValue()   ,DanglingLine.class.getMethod("setR"                                 ,double.class)  ,elem  ,false);
                itemList.add(new SimpleEntry<>("X"                          ,new SimpleDoubleProperty(((DanglingLine)(elem)).getX())));                             valueListener(itemList.get(offset+3 ).getValue()   ,DanglingLine.class.getMethod("setX"                                 ,double.class)  ,elem  ,false);
                itemList.add(new SimpleEntry<>("Injected P0"                ,new SimpleDoubleProperty(((DanglingLine)(elem)).getP0())));                            valueListener(itemList.get(offset+4 ).getValue()   ,DanglingLine.class.getMethod("setP0"                                ,double.class)  ,elem  ,false);
                itemList.add(new SimpleEntry<>("Injected Q0"                ,new SimpleDoubleProperty(((DanglingLine)(elem)).getQ0())));                            valueListener(itemList.get(offset+5 ).getValue()   ,DanglingLine.class.getMethod("setQ0"                                ,double.class)  ,elem  ,false);
                break;
            case "com.powsybl.iidm.network.Generator":
                ChoiceBox   srcCh   =new ChoiceBox(); srcCh.getItems().addAll((Object[])EnergySource.values()); srcCh.getSelectionModel().select(((Generator)(elem)).getEnergySource());
                Terminal    regT    =((Generator)(elem)).getRegulatingTerminal();
                itemList.add(new SimpleEntry<>("Source type"                ,srcCh));                                                                               valueListener(srcCh.getSelectionModel().selectedItemProperty(),   Generator.class.getMethod   ("setEnergySource",         EnergySource.class),  elem   ,false  , selected);
                itemList.add(new SimpleEntry<>("Intermittent"               ,new ReadOnlyObjectWrapper(((Generator)(elem)).getEnergySource().isIntermittent())));
                itemList.add(new SimpleEntry<>("Rated S"                    ,new SimpleDoubleProperty(((Generator)(elem)).getRatedS())));                           valueListener(itemList.get(offset+2 ).getValue()    ,Generator.class.getMethod  ("setRatedS"                            ,double.class),  elem  ,false);
                itemList.add(new SimpleEntry<>("Max P"                      ,new SimpleDoubleProperty(((Generator)(elem)).getMaxP())));                             valueListener(itemList.get(offset+3 ).getValue()    ,Generator.class.getMethod  ("setMaxP"                              ,double.class),  elem  ,false);
                itemList.add(new SimpleEntry<>("Min P"                      ,new SimpleDoubleProperty(((Generator)(elem)).getMinP())));                             valueListener(itemList.get(offset+4 ).getValue()    ,Generator.class.getMethod  ("setMinP"                              ,double.class),  elem  ,false);
                itemList.add(new SimpleEntry<>("Target P"                   ,new SimpleDoubleProperty(((Generator)(elem)).getTargetP())));                          valueListener(itemList.get(offset+5 ).getValue()    ,Generator.class.getMethod  ("setTargetP"                           ,double.class),  elem  ,false);
                itemList.add(new SimpleEntry<>("Target Q"                   ,new SimpleDoubleProperty(((Generator)(elem)).getTargetQ())));                          valueListener(itemList.get(offset+6 ).getValue()    ,Generator.class.getMethod  ("setTargetQ"                           ,double.class),  elem  ,false);
                itemList.add(new SimpleEntry<>("Target V"                   ,new SimpleDoubleProperty(((Generator)(elem)).getTargetV())));                          valueListener(itemList.get(offset+7 ).getValue()    ,Generator.class.getMethod  ("setTargetV"                           ,double.class),  elem  ,false);
                itemList.add(new SimpleEntry<>("Volt Regulated"             ,new SimpleBooleanProperty(((Generator)(elem)).isVoltageRegulatorOn())));               valueListener(itemList.get(offset+8 ).getValue()    ,Generator.class.getMethod  ("setVoltageRegulatorOn"                ,boolean.class), elem  ,false  ,selected);
                
                Button                          bRT1        =new Button(regT!=null?voltLevel.getTopologyKind()==TopologyKind.NODE_BREAKER?regT.getVoltageLevel().getName()+" / "+regT.getNodeBreakerView().getNode():regT.getBusBreakerView().getConnectableBus().getName():"Select Reg. Term.");
                final EventHandler<ActionEvent> rt1ConnEvH  =(ActionEvent actEvent) -> {
                                                                                            setNodeHandlerValidator("REGULATING_TERMINAL");
                                                                                            VoltageLevelHandler.setMode(VisualHandlerModes.REGULATING_TERM_SEARCH, selected!=null?selected[0]:null, bRT1);
                                                                                            Group   cursorImg   =(new SvgLoader()).loadSvg(this.getClass().getResourceAsStream("/img/PointToNode.svg"));
                                                                                            SnapshotParameters  snapParams  =new SnapshotParameters();
                                                                                            snapParams.setFill(Paint.valueOf("#ffffff00"));
                                                                                            contentPane.getParent().setCursor(new ImageCursor(cursorImg.snapshot(snapParams, null), 3, 3));
                                                                                        };

                bRT1.setOnAction(rt1ConnEvH);
                bRT1.addEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, eventRT1->
                //<editor-fold desc="Regulting Terminal Button terminal assingment">
                {
                    NodeHandler         nh2         =eventRT1.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)eventRT1.getOriginalSource()         :eventRT1.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)eventRT1.getOriginalSource()).getId()):null;
                    VoltageLevelHandler vlh2        =eventRT1.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)eventRT1.getOriginalSource() :eventRT1.getOriginalSource() instanceof Node?vlHandlers .get(((Node)eventRT1.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
                    Terminal            oldTerm     =((Generator)elem).getRegulatingTerminal();
                    Terminal            newRegT     =getTerminalForAt(elem, nh2, vlh2);

                    VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                    contentPane.getParent().setCursor(Cursor.DEFAULT);
                    
                    if(newRegT!=null)
                    {
                        try
                        {
                            bRT1.setText    (newRegT.getVoltageLevel().getName()+" / "+newRegT.getNodeBreakerView().getNode());
                        }
                        catch(Exception ex)
                        {
                            bRT1.setText    (newRegT.getBusBreakerView().getConnectableBus().getName());
                        }
                        ((Generator)(elem)).setRegulatingTerminal(newRegT);
                    }
                    
                    eventRT1.consume();
                });
                //</editor-fold>
                
                itemList.add(new SimpleEntry<>("Regulating Terminal"    ,bRT1));
                break;
            case "com.powsybl.iidm.network.VscConverterStation":
                ChoiceBox   typeCh2 =new ChoiceBox(); typeCh2.getItems().addAll((Object[])HvdcType.values()); typeCh2.getSelectionModel().select(((HvdcConverterStation)(elem)).getHvdcType());
                typeCh2.setMouseTransparent(true);//Special case: must be disabled to user input, 'cause StaticVarCompensator hasn't setType method.
                itemList.add(new SimpleEntry<>("HVDC Type"                  ,typeCh2));
                itemList.add(new SimpleEntry<>("Regulator On"               ,new SimpleBooleanProperty(((VscConverterStation)(elem)).isVoltageRegulatorOn())));     valueListener(itemList.get(offset+1).getValue()     ,VscConverterStation  .class.getMethod  ("setVoltageRegulatorOn"    ,boolean.class) ,elem  ,false  ,selected);
                itemList.add(new SimpleEntry<>("Loss Factor"                ,new SimpleFloatProperty(((VscConverterStation)(elem)).getLossFactor())));              valueListener(itemList.get(offset+2).getValue()     ,VscConverterStation  .class.getMethod  ("setLossFactor"            ,float .class)  ,elem  ,false);
                itemList.add(new SimpleEntry<>("Q Setpoint"                 ,new SimpleDoubleProperty(((VscConverterStation)(elem)).getReactivePowerSetpoint())));  valueListener(itemList.get(offset+3).getValue()     ,VscConverterStation  .class.getMethod  ("setReactivePowerSetpoint" ,double.class)  ,elem  ,false);
                itemList.add(new SimpleEntry<>("V Setpoint"                 ,new SimpleDoubleProperty(((VscConverterStation)(elem)).getVoltageSetpoint())));        valueListener(itemList.get(offset+4).getValue()     ,VscConverterStation  .class.getMethod  ("setVoltageSetpoint"       ,double.class)  ,elem  ,false);
                break;
            case "com.powsybl.iidm.network.LccConverterStation":
                ChoiceBox   typeCh3 =new ChoiceBox(); typeCh3.getItems().addAll((Object[])HvdcType.values()); typeCh3.getSelectionModel().select(((HvdcConverterStation)(elem)).getHvdcType());
                typeCh3.setMouseTransparent(true);//Special case: must be disabled to user input, 'cause StaticVarCompensator hasn't setType method.
                itemList.add(new SimpleEntry<>("HVDC Type"                  ,typeCh3));
                itemList.add(new SimpleEntry<>("Loss Factor"                ,new SimpleFloatProperty(((LccConverterStation)(elem)).getLossFactor())));              valueListener(itemList.get(offset+1).getValue()     ,LccConverterStation .class.getMethod ("setLossFactor"           ,float .class)   ,elem   ,false);
                itemList.add(new SimpleEntry<>("Power Factor"               ,new SimpleFloatProperty(((LccConverterStation)(elem)).getPowerFactor())));             valueListener(itemList.get(offset+2).getValue()     ,LccConverterStation .class.getMethod ("setPowerFactor"          ,float .class)   ,elem   ,false);
                break;
            case "com.powsybl.iidm.network.TieLine":
                HalfLine    hl1     =((TieLine)(elem)).getHalf1();
                HalfLine    hl2     =((TieLine)(elem)).getHalf2();
                Button      seeHl1  =new Button("Go to Half Line 1");
                Button      seeHl2  =new Button("Go to Half Line 2");
                Button      back2L  =new Button("Go back to "+elem.getName());
                
                back2L.setOnAction((ActionEvent event)->{
                    try
                    {
                        render(selected[0]);
                    }
                    catch (NoSuchMethodException | SecurityException | UnsupportedEncodingException ex)
                    {
                        Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                
                seeHl1.setOnAction((ActionEvent event) -> {
                    itemList.clear();
                    
                    try
                    {
                        int     offset2;
                        
                        itemList.add(new SimpleEntry<>("Half line 1"                ,back2L));
                        getLineParams(hl1, itemList, selected);
                        offset2=itemList.size();
                        itemList.add(new SimpleEntry<>("X node P"               ,new SimpleDoubleProperty(((HalfLine)(hl1)).getXnodeP())));    valueListener(itemList.get(offset2  ).getValue()    ,   hl1.getClass().getMethod ("setXnodeP"   ,double.class)  ,hl1   ,false);
                        itemList.add(new SimpleEntry<>("X node Q"               ,new SimpleDoubleProperty(((HalfLine)(hl1)).getXnodeQ())));    valueListener(itemList.get(offset2+1).getValue()    ,   hl1.getClass().getMethod ("setXnodeQ"   ,double.class)  ,hl1   ,false);
                    }
                    catch (NoSuchMethodException ex)
                    {
                        Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                
                seeHl2.setOnAction((ActionEvent event) -> {
                    itemList.clear();
                    
                    try
                    {
                        int     offset2;
                        
                        itemList.add(new SimpleEntry<>("Half line 2"                ,back2L));
                        getLineParams(hl2, itemList, selected);
                        offset2=itemList.size();
                        itemList.add(new SimpleEntry<>("X node P"               ,new SimpleDoubleProperty(((HalfLine)(hl2)).getXnodeP())));    valueListener(itemList.get(offset2  ).getValue()    ,   hl2.getClass().getMethod ("setXnodeP"   ,double.class)  ,hl2   ,false);
                        itemList.add(new SimpleEntry<>("X node Q"               ,new SimpleDoubleProperty(((HalfLine)(hl2)).getXnodeQ())));    valueListener(itemList.get(offset2+1).getValue()    ,   hl2.getClass().getMethod ("setXnodeQ"   ,double.class)  ,hl2   ,false);
                    }
                    catch (NoSuchMethodException ex)
                    {
                        Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                
                itemList.add(new SimpleEntry<>("Half line 1"                ,seeHl1));
                itemList.add(new SimpleEntry<>("Half line 2"                ,seeHl2));
                itemList.add(new SimpleEntry<>("UCTE Code"                  ,new ReadOnlyObjectWrapper(((TieLine)(elem)).getUcteXnodeCode())));
//                offset=itemList.size();
            case "com.powsybl.iidm.network.Line":
//                itemList.add(new SimpleEntry<>("B1"                         ,new SimpleDoubleProperty(((Line)(elem)).getB1())));                                    valueListener(itemList.get(offset   ).getValue()    ,Line                .class.getMethod ("setB1"                   ,double.class)   ,elem   ,false);
//                itemList.add(new SimpleEntry<>("G1"                         ,new SimpleDoubleProperty(((Line)(elem)).getG1())));                                    valueListener(itemList.get(offset+1 ).getValue()    ,Line                .class.getMethod ("setG1"                   ,double.class)   ,elem   ,false);
//                itemList.add(new SimpleEntry<>("R"                          ,new SimpleDoubleProperty(((Line)(elem)).getR())));                                     valueListener(itemList.get(offset+2 ).getValue()    ,Line                .class.getMethod ("setR"                    ,double.class)   ,elem   ,false);
//                itemList.add(new SimpleEntry<>("X"                          ,new SimpleDoubleProperty(((Line)(elem)).getX())));                                     valueListener(itemList.get(offset+3 ).getValue()    ,Line                .class.getMethod ("setX"                    ,double.class)   ,elem   ,false);
//                itemList.add(new SimpleEntry<>("B2"                         ,new SimpleDoubleProperty(((Line)(elem)).getB2())));                                    valueListener(itemList.get(offset+4 ).getValue()    ,Line                .class.getMethod ("setB2"                   ,double.class)   ,elem   ,false);
//                itemList.add(new SimpleEntry<>("G2"                         ,new SimpleDoubleProperty(((Line)(elem)).getG2())));                                    valueListener(itemList.get(offset+5 ).getValue()    ,Line                .class.getMethod ("setG2"                   ,double.class)   ,elem   ,false);
                itemList.add(new SimpleEntry<>("Is Tie Line"                ,new ReadOnlyObjectWrapper(((Line)(elem)).isTieLine())));
                getLineParams((LineCharacteristics)elem, itemList);
                break;
            case "com.powsybl.iidm.network.TieLine.HalfLine":
                break;
            case "com.powsybl.iidm.network.Load":
                ChoiceBox   typeCh  =new ChoiceBox(); typeCh.getItems().addAll((Object[]) LoadType.values()); typeCh.getSelectionModel().select(((Load)(elem)).getLoadType());
                itemList.add(new SimpleEntry<>("Load Type"                  ,typeCh));                                                                              valueListener(typeCh.getSelectionModel().selectedItemProperty(),    Load .class.getMethod ("setLoadType"             ,LoadType.class) ,elem   ,false  ,selected);
                itemList.add(new SimpleEntry<>("Demanded P0"                ,new SimpleDoubleProperty(((Load)(elem)).getP0())));                                    valueListener(itemList.get(offset+1).getValue()     ,Load                .class.getMethod ("setP0"                   ,double.class)   ,elem   ,false);
                itemList.add(new SimpleEntry<>("Demanded Q0"                ,new SimpleDoubleProperty(((Load)(elem)).getQ0())));                                    valueListener(itemList.get(offset+2).getValue()     ,Load                .class.getMethod ("setQ0"                   ,double.class)   ,elem   ,false);
                break;
            case "com.powsybl.iidm.network.ShuntCompensator":
                SimpleDoubleProperty    perSectB    =new SimpleDoubleProperty(((ShuntCompensator) (elem)).getbPerSection());
                SimpleIntegerProperty   totSect     =new SimpleIntegerProperty(((ShuntCompensator)(elem)).getMaximumSectionCount());
                SimpleIntegerProperty   curSect     =new SimpleIntegerProperty(((ShuntCompensator)(elem)).getCurrentSectionCount());
                itemList.add(new SimpleEntry<>("Total B"                    ,new ReadOnlyObjectWrapper(((ShuntCompensator)(elem)).getMaximumB())));
                itemList.add(new SimpleEntry<>("Per-section B"              ,perSectB));                                                                           valueListener(itemList.get(offset+1 ).getValue()     ,ShuntCompensator    .class.getMethod ("setbPerSection"          ,double.class)   ,elem   ,false ,selected);
                itemList.add(new SimpleEntry<>("Total Sections"             ,totSect));                                                                            valueListener(itemList.get(offset+2 ).getValue()     ,ShuntCompensator    .class.getMethod ("setMaximumSectionCount"  ,int.class)      ,elem   ,false ,selected);
                itemList.add(new SimpleEntry<>("Current Sections"           ,curSect));                                                                            valueListener(itemList.get(offset+3 ).getValue()     ,ShuntCompensator    .class.getMethod ("setCurrentSectionCount"  ,int.class)      ,elem   ,false ,selected);
                itemList.add(new SimpleEntry<>("Current B"                  ,new ReadOnlyObjectWrapper(((ShuntCompensator)(elem)).getCurrentB())));
                break;
            case "com.powsybl.iidm.network.StaticVarCompensator":
                ChoiceBox   regModCh    =new ChoiceBox(); regModCh.getItems().addAll((Object[])com.powsybl.iidm.network.StaticVarCompensator.RegulationMode.values()); regModCh.getSelectionModel().select(((StaticVarCompensator)(elem)).getRegulationMode());
                itemList.add(new SimpleEntry<>("Max B"                      ,new SimpleDoubleProperty(((StaticVarCompensator)(elem)).getBmax())));                 valueListener(itemList.get(offset+0 ).getValue()     ,StaticVarCompensator.class.getMethod ("setBmax"                 ,double.class)   ,elem  ,false);
                itemList.add(new SimpleEntry<>("Min B"                      ,new SimpleDoubleProperty(((StaticVarCompensator)(elem)).getBmax())));                 valueListener(itemList.get(offset+1 ).getValue()     ,StaticVarCompensator.class.getMethod ("setBmin"                 ,double.class)   ,elem  ,false);
                itemList.add(new SimpleEntry<>("Setpoint Q"                 ,new SimpleDoubleProperty(((StaticVarCompensator)(elem)).getReactivePowerSetPoint())));valueListener(itemList.get(offset+2 ).getValue()     ,StaticVarCompensator.class.getMethod ("setReactivePowerSetPoint",double.class)   ,elem  ,false);
                itemList.add(new SimpleEntry<>("Setpoint V"                 ,new SimpleDoubleProperty(((StaticVarCompensator)(elem)).getVoltageSetPoint())));      valueListener(itemList.get(offset+3 ).getValue()     ,StaticVarCompensator.class.getMethod ("setVoltageSetPoint"      ,double.class)   ,elem  ,false);
                itemList.add(new SimpleEntry<>("Regultion Mode"             ,regModCh));                                                                           valueListener(regModCh.getSelectionModel().selectedItemProperty(),    StaticVarCompensator.class.getMethod    ("setRegulationMode", StaticVarCompensator.RegulationMode.class),    elem    ,false  ,selected);
                break;
            case "com.powsybl.iidm.network.TwoWindingsTransformer":
                get2WTparams(elem, itemList, selected[0]);
                break;
            case "com.powsybl.iidm.network.ThreeWindingsTransformer":
                get3WTparams(elem, itemList, selected[0]);
                break;
        }
    }
    
    private void getSwitchParams(Switch elem, ObservableList<Entry<String,Object>> itemList, Node... selected) throws NoSuchMethodException
    {
        ChoiceBox   kindCh  =new ChoiceBox(); kindCh.getItems().addAll((Object[])SwitchKind.values()); kindCh.getSelectionModel().select(((Switch)elem).getKind().toString()); kindCh.getSelectionModel().select(((Switch)elem).getKind());kindCh.setMouseTransparent(true);

        int node1 = elem.getVoltageLevel().getNodeBreakerView().getNode1(elem.getId());
        int node2 = elem.getVoltageLevel().getNodeBreakerView().getNode2(elem.getId());
        Terminal    t1  =elem.getVoltageLevel().getNodeBreakerView().getTerminal1(elem.getId());
        Terminal    t2  =elem.getVoltageLevel().getNodeBreakerView().getTerminal2(elem.getId());
        if(t1!=null)
            itemList.add(new SimpleEntry<>("Side 1 (Terminal)" ,new ReadOnlyObjectWrapper(t1.getBusBreakerView().getConnectableBus()+" / "+t1.getNodeBreakerView().getNode())));
        else
            itemList.add(new SimpleEntry<>("Side 1 (Node)"      ,new ReadOnlyObjectWrapper("Node "+node1)));
        if(t2!=null)
            itemList.add(new SimpleEntry<>("Side 2 (Terminal)"  ,new ReadOnlyObjectWrapper(t2.getBusBreakerView().getConnectableBus()+" / "+t2.getNodeBreakerView().getNode())));
        else
            itemList.add(new SimpleEntry<>("Side 2 (Node)"      ,new ReadOnlyObjectWrapper("Node "+node2)));

        
        itemList.add(new SimpleEntry<>("Kind"       ,kindCh));
        itemList.add(new SimpleEntry<>("Fictitious" ,new SimpleBooleanProperty((elem).isFictitious())));    valueListener(itemList.get(6).getValue(),           elem.getClass().getMethod("setFictitious"       ,boolean.class) ,(elem)  ,false);
        itemList.add(new SimpleEntry<>("Retained"   ,new SimpleBooleanProperty((elem).isRetained  ())));    valueListener(itemList.get(7).getValue(),           elem.getClass().getMethod("setRetained"         ,boolean.class) ,(elem)  ,false);
        itemList.add(new SimpleEntry<>("Open"       ,new SimpleBooleanProperty((elem).isOpen      ())));    valueListener(itemList.get(8).getValue(),           elem.getClass().getMethod("setOpen"             ,boolean.class) ,(elem)  ,true   ,selected);
    }
    
    private void getBusParams(Bus elem, ObservableList<Entry<String,Object>> itemList, Node... selected) throws NoSuchMethodException
    {
        int offset  =itemList.size();
        
        itemList.add(new SimpleEntry<>("Voltage"    ,new SimpleDoubleProperty(elem.getV()    )));           valueListener(itemList.get(offset+0).getValue(),    elem.getClass().getMethod("setV"                ,double.class)  ,elem   ,false);
        itemList.add(new SimpleEntry<>("Angle"      ,new SimpleDoubleProperty(elem.getAngle())));           valueListener(itemList.get(offset+1).getValue(),    elem.getClass().getMethod("setAngle"            ,double.class)  ,elem   ,false);
        itemList.add(new SimpleEntry<>("P"          ,new ReadOnlyObjectWrapper(elem.getP()    )));
        itemList.add(new SimpleEntry<>("Q"          ,new ReadOnlyObjectWrapper(elem.getQ()    )));
    }
    
    private void getLineParams(LineCharacteristics elem, ObservableList<Entry<String,Object>> itemList, Node... selected) throws NoSuchMethodException
    {
        int offset  =itemList.size();
        
        itemList.add(new SimpleEntry<>("B1"        ,new SimpleDoubleProperty(((LineCharacteristics)(elem)).getB1())));     valueListener(itemList.get(offset  ).getValue(),    elem.getClass().getMethod("setB1"               ,double.class)  ,elem   ,false);
        itemList.add(new SimpleEntry<>("G1"        ,new SimpleDoubleProperty(((LineCharacteristics)(elem)).getG1())));     valueListener(itemList.get(offset+1).getValue(),    elem.getClass().getMethod("setG1"               ,double.class)  ,elem   ,false);
        itemList.add(new SimpleEntry<>("R"         ,new SimpleDoubleProperty(((LineCharacteristics)(elem)).getR())));      valueListener(itemList.get(offset+2).getValue(),    elem.getClass().getMethod("setR"                ,double.class)  ,elem   ,false);
        itemList.add(new SimpleEntry<>("X"         ,new SimpleDoubleProperty(((LineCharacteristics)(elem)).getX())));      valueListener(itemList.get(offset+3).getValue(),    elem.getClass().getMethod("setX"                ,double.class)  ,elem   ,false);
        itemList.add(new SimpleEntry<>("B2"        ,new SimpleDoubleProperty(((LineCharacteristics)(elem)).getB2())));     valueListener(itemList.get(offset+4).getValue(),    elem.getClass().getMethod("setB2"               ,double.class)  ,elem   ,false);
        itemList.add(new SimpleEntry<>("G2"        ,new SimpleDoubleProperty(((LineCharacteristics)(elem)).getG2())));     valueListener(itemList.get(offset+5).getValue(),    elem.getClass().getMethod("setG2"               ,double.class)  ,elem   ,false);
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void get2WTparams(Connectable elem, ObservableList<Entry<String,Object>>    itemList, Node... selected) throws NoSuchMethodException
    {
        Button  bAddRTC =new Button ("Modify RTC");
        Button  bAddPTC =new Button ("Modify PTC");
        int     offset  =itemList.size();

        itemList.add(new SimpleEntry<>("Primary Rated U"            ,new SimpleDoubleProperty(((TwoWindingsTransformer)(elem)).getRatedU1())));valueListener(itemList.get(offset   ).getValue(),   TwoWindingsTransformer.class.getMethod  ("setRatedU1", double.class),  elem   ,false);
        itemList.add(new SimpleEntry<>("Secondary Rated U"          ,new SimpleDoubleProperty(((TwoWindingsTransformer)(elem)).getRatedU2())));valueListener(itemList.get(offset+1 ).getValue(),   TwoWindingsTransformer.class.getMethod  ("setRatedU2", double.class),  elem   ,false);
        itemList.add(new SimpleEntry<>("Secondary B"                ,new SimpleDoubleProperty(((TwoWindingsTransformer)(elem)).getB())));      valueListener(itemList.get(offset+2 ).getValue(),   TwoWindingsTransformer.class.getMethod  ("setB"      , double.class),  elem   ,false);
        itemList.add(new SimpleEntry<>("Secondary G"                ,new SimpleDoubleProperty(((TwoWindingsTransformer)(elem)).getG())));      valueListener(itemList.get(offset+3 ).getValue(),   TwoWindingsTransformer.class.getMethod  ("setG"      , double.class),  elem   ,false);
        itemList.add(new SimpleEntry<>("Secondary R"                ,new SimpleDoubleProperty(((TwoWindingsTransformer)(elem)).getR())));      valueListener(itemList.get(offset+4 ).getValue(),   TwoWindingsTransformer.class.getMethod  ("setR"      , double.class),  elem   ,false);
        itemList.add(new SimpleEntry<>("Secondary X"                ,new SimpleDoubleProperty(((TwoWindingsTransformer)(elem)).getX())));      valueListener(itemList.get(offset+5 ).getValue(),   TwoWindingsTransformer.class.getMethod  ("setX"      , double.class),  elem   ,false);
        if(((TwoWindingsTransformer)(elem)).getRatioTapChanger()!=null)
        {
            RatioTapChanger tapCh   =((TwoWindingsTransformer)(elem)).getRatioTapChanger();
            Bus             regBus  =tapCh.getRegulationTerminal().getBusBreakerView().getConnectableBus();
            
            itemList.add(new SimpleEntry<>("Ratio Load Tap Changing Capable",new ReadOnlyObjectWrapper(tapCh.hasLoadTapChangingCapabilities())));
            itemList.add(new SimpleEntry<>("Ratio Target Voltage"           ,new SimpleDoubleProperty(tapCh.getTargetV())));                            valueListener(itemList.get(offset+7 ).getValue(),  RatioTapChanger.class.getMethod         ("setTargetV",          double.class),  tapCh   ,false);
            itemList.add(new SimpleEntry<>("Ratio Target Deadband"          ,new SimpleDoubleProperty(tapCh.getTargetDeadband())));                     valueListener(itemList.get(offset+8 ).getValue(),  RatioTapChanger.class.getMethod         ("setTargetDeadband",   double.class),  tapCh   ,false);
            switch(tapCh.getRegulationTerminal().getVoltageLevel().getTopologyKind())
            {
                case BUS_BREAKER:
                    itemList.add(new SimpleEntry<>("Ratio Regulation Bus"   ,new ReadOnlyObjectWrapper(regBus!=null?regBus.getName():"<Disconnected>")));
                    break;
                case NODE_BREAKER:
                    itemList.add(new SimpleEntry<>("Ratio Regulation Bus"   ,new ReadOnlyObjectWrapper(regBus!=null?regBus.getName()+" / "+tapCh.getRegulationTerminal().getNodeBreakerView().getNode():"<Disconnected>")));
                    break;
            }
            itemList.add(new SimpleEntry<>("Ratio Low Tap Position"          ,new ReadOnlyObjectWrapper(tapCh.getLowTapPosition())));                   
            itemList.add(new SimpleEntry<>("Ratio High Tap Position"         ,new ReadOnlyObjectWrapper(tapCh.getHighTapPosition())));                  
            itemList.add(new SimpleEntry<>("Ratio Regulator is Active"       ,new SimpleBooleanProperty(tapCh.isRegulating())));                        valueListener(itemList.get(offset+12).getValue(),  RatioTapChanger.class.getMethod         ("setRegulating",       boolean.class), tapCh   ,false  ,selected);
            itemList.add(new SimpleEntry<>("Ratio Tap Position"              ,new SimpleIntegerProperty(tapCh.getTapPosition())));                      valueListener(itemList.get(offset+13).getValue(),  RatioTapChanger.class.getMethod         ("setTapPosition",      int.class),     tapCh   ,false);
        }
        else
        {
            bAddRTC =new Button ("Add RTC");
        }
        
        offset=itemList.size();
        if(((PhaseTapChangerHolder)elem).getPhaseTapChanger()!=null)
        {
            PhaseTapChanger phTapCh =((PhaseTapChangerHolder)elem).getPhaseTapChanger();
            ChoiceBox       regModCh2=new ChoiceBox(); regModCh2.getItems().addAll((Object[])RegulationMode.values()); regModCh2.getSelectionModel().select(phTapCh.getRegulationMode());

            itemList.add(new SimpleEntry<>("Phase Regulation Mode"     ,regModCh2));                                                                        valueListener(regModCh2.getSelectionModel().selectedItemProperty(),    PhaseTapChanger.class.getMethod    ("setRegulationMode", RegulationMode.class),    phTapCh    ,false  ,selected);
            itemList.add(new SimpleEntry<>("Phase Regulation Target"   ,new SimpleDoubleProperty(phTapCh.getRegulationValue())));                           valueListener(itemList.get(offset+1).getValue()     ,PhaseTapChanger     .class.getMethod ("setRegulationValue"     ,double.class)    ,phTapCh,false);
            itemList.add(new SimpleEntry<>("Phase Target Deadband"     ,new SimpleDoubleProperty(phTapCh.getTargetDeadband())));                            valueListener(itemList.get(offset+2).getValue()     ,PhaseTapChanger     .class.getMethod ("setTargetDeadband"      ,double.class)    ,phTapCh,false);

            switch(phTapCh.getRegulationTerminal().getVoltageLevel().getTopologyKind())
            {
                case BUS_BREAKER:
                    itemList.add(new SimpleEntry<>("Phase Regulation Bus"   ,new ReadOnlyObjectWrapper(phTapCh.getRegulationTerminal().getBusBreakerView().getConnectableBus().getName())));
                    break;
                case NODE_BREAKER:
                    itemList.add(new SimpleEntry<>("Phase Regulation Bus"   ,new ReadOnlyObjectWrapper(phTapCh.getRegulationTerminal().getBusBreakerView().getConnectableBus().getName()+" / "+phTapCh.getRegulationTerminal().getNodeBreakerView().getNode())));
                    break;
            }
            itemList.add(new SimpleEntry<>("Phase Low Tap Position"    ,new ReadOnlyObjectWrapper(phTapCh.getLowTapPosition())));
            itemList.add(new SimpleEntry<>("Phase High Tap Position"   ,new ReadOnlyObjectWrapper(phTapCh.getHighTapPosition())));
            itemList.add(new SimpleEntry<>("Phase Regulator is Active" ,new SimpleBooleanProperty(phTapCh.isRegulating())));                                valueListener(itemList.get(offset+6).getValue(),    PhaseTapChanger.class.getMethod ("setRegulating",       boolean.class), phTapCh ,false  ,selected);
            itemList.add(new SimpleEntry<>("Phase Tap Position"        ,new SimpleIntegerProperty(phTapCh.getTapPosition())));                              valueListener(itemList.get(offset+7).getValue(),    PhaseTapChanger.class.getMethod ("setTapPosition",      int.class),     phTapCh ,false);
        }
        else
        {
            bAddPTC =new Button ("Add PTC");
        }
        
        if(((TwoWindingsTransformer)(elem)).getPhaseTapChanger()==null)
        {
            itemList.add(new SimpleEntry<>("Ratio Tap Changer"               ,bAddRTC));
            bAddRTC.setOnAction(event-> {
                                            propGrid.getItems().clear();
                                            this.initRTC((RatioTapChangerHolder) elem);
                                        });
        }
        if(((TwoWindingsTransformer)(elem)).getRatioTapChanger()==null)
        {
            itemList.add(new SimpleEntry<>("Phase Tap Changer"               ,bAddPTC));
            bAddPTC.setOnAction(event-> {
                                            propGrid.getItems().clear();
                                            this.initPTC((PhaseTapChangerHolder) elem);
                                        });
        }
    }
    
    private void get3WTparams(Connectable elem, ObservableList<Entry<String,Object>>    itemList, Node... selected) throws NoSuchMethodException
    {
        Button  bAdd2RTC =new Button ("Modify Secondary RTC");
        Button  bAdd3RTC =new Button ("Modify Tertiary RTC");

        int offset0 =itemList.size();
        int offset1;
        itemList.add(new SimpleEntry<>("Primary Rated U"                            ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg1().getRatedU()))); valueListener(itemList.get(offset0   ).getValue(),  Leg1.class.getMethod            ("setRatedU"        ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg1()  ,false);
        itemList.add(new SimpleEntry<>("Primary B"                                  ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg1().getB())));      valueListener(itemList.get(offset0+1 ).getValue(),  Leg1.class.getMethod            ("setB"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg1()  ,false);
        itemList.add(new SimpleEntry<>("Primary G"                                  ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg1().getG())));      valueListener(itemList.get(offset0+2 ).getValue(),  Leg1.class.getMethod            ("setG"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg1()  ,false);
        itemList.add(new SimpleEntry<>("Primary R"                                  ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg1().getR())));      valueListener(itemList.get(offset0+3 ).getValue(),  Leg1.class.getMethod            ("setR"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg1()  ,false);
        itemList.add(new SimpleEntry<>("Primary X"                                  ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg1().getX())));      valueListener(itemList.get(offset0+4 ).getValue(),  Leg1.class.getMethod            ("setX"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg1()  ,false);
        itemList.add(new SimpleEntry<>("Secondary Rated U"                          ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg2().getRatedU()))); valueListener(itemList.get(offset0+5 ).getValue(),  Leg2or3.class.getMethod         ("setRatedU"        ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg2()  ,false);
        itemList.add(new SimpleEntry<>("Secondary R"                                ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg2().getR())));      valueListener(itemList.get(offset0+6 ).getValue(),  Leg2or3.class.getMethod         ("setR"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg2()  ,false);
        itemList.add(new SimpleEntry<>("Secondary X"                                ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg2().getX())));      valueListener(itemList.get(offset0+7 ).getValue(),  Leg2or3.class.getMethod         ("setX"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg2()  ,false);
        if(((ThreeWindingsTransformer)(elem)).getLeg2().getRatioTapChanger()!=null)
        {
            RatioTapChanger tapCh2ry    =((ThreeWindingsTransformer)(elem)).getLeg2().getRatioTapChanger();
            Bus             regBus      =tapCh2ry.getRegulationTerminal().getBusBreakerView().getConnectableBus();


            itemList.add(new SimpleEntry<>("Sec. Ratio Load Tap Changing Capable"   ,new ReadOnlyObjectWrapper(tapCh2ry.hasLoadTapChangingCapabilities())));
            itemList.add(new SimpleEntry<>("Sec. Ratio Target Voltage"              ,new SimpleDoubleProperty(tapCh2ry.getTargetV())));                                    valueListener(itemList.get(offset0+9 ).getValue(),  RatioTapChanger.class.getMethod ("setTargetV"       ,double.class),  tapCh2ry   ,false);
            itemList.add(new SimpleEntry<>("Sec. Ratio Target Deadband"             ,new SimpleDoubleProperty(tapCh2ry.getTargetDeadband())));                             valueListener(itemList.get(offset0+10).getValue(),  RatioTapChanger.class.getMethod ("setTargetDeadband",double.class),  tapCh2ry   ,false);
            switch(tapCh2ry.getRegulationTerminal().getVoltageLevel().getTopologyKind())
            {
                case BUS_BREAKER:
                    itemList.add(new SimpleEntry<>("Sec. Ratio Regulation Bus"     ,new ReadOnlyObjectWrapper(regBus!=null?regBus.getName():"<Disconnected>")));
                    break;
                case NODE_BREAKER:
                    itemList.add(new SimpleEntry<>("Sec. Ratio Regulation Bus"     ,new ReadOnlyObjectWrapper(regBus!=null?regBus.getName()+" / "+tapCh2ry.getRegulationTerminal().getNodeBreakerView().getNode():"<Disconnected>")));
                    break;
            }
            itemList.add(new SimpleEntry<>("Sec. Ratio Low Tap Position"            ,new ReadOnlyObjectWrapper(tapCh2ry.getLowTapPosition())));
            itemList.add(new SimpleEntry<>("Sec. Ratio High Tap Position"           ,new ReadOnlyObjectWrapper(tapCh2ry.getHighTapPosition())));
            itemList.add(new SimpleEntry<>("Sec. Ratio Regulator is Active"         ,new SimpleBooleanProperty(tapCh2ry.isRegulating())));                                 valueListener(itemList.get(offset0+14).getValue(),  RatioTapChanger.class.getMethod ("setRegulating",       boolean.class), tapCh2ry    ,false  ,selected);
            itemList.add(new SimpleEntry<>("Sec. Ratio Tap Position"                ,new SimpleIntegerProperty(tapCh2ry.getTapPosition())));                               valueListener(itemList.get(offset0+15).getValue(),  RatioTapChanger.class.getMethod ("setTapPosition"   ,   int.class),     tapCh2ry    ,false);
        }
        else
        {
            bAdd2RTC =new Button ("Add Secondary RTC");
        }
        
        itemList.add(new SimpleEntry<>("Secondary Ratio Tap Changer"            ,bAdd2RTC));
        bAdd2RTC.setOnAction(event-> {
                                        propGrid.getItems().clear();
                                        this.initRTC((RatioTapChangerHolder)((ThreeWindingsTransformer)(elem)).getLeg2());
                                    });
        offset1=itemList.size();
        itemList.add(new SimpleEntry<>("Tertiary Rated U"                           ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg3().getRatedU()))); valueListener(itemList.get(offset1+0).getValue(),   Leg2or3.class.getMethod ("setRatedU"        ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg3()  ,false);
        itemList.add(new SimpleEntry<>("Tertiary R"                                 ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg3().getR())));      valueListener(itemList.get(offset1+1).getValue(),   Leg2or3.class.getMethod ("setR"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg3()  ,false);
        itemList.add(new SimpleEntry<>("Tertiary X"                                 ,new SimpleDoubleProperty(((ThreeWindingsTransformer)(elem)).getLeg3().getX())));      valueListener(itemList.get(offset1+2).getValue(),   Leg2or3.class.getMethod ("setX"             ,double.class),  ((ThreeWindingsTransformer)(elem)).getLeg3()  ,false);
        if(((ThreeWindingsTransformer)(elem)).getLeg3().getRatioTapChanger()!=null)
        {
            RatioTapChanger tapCh3ry    =((ThreeWindingsTransformer)(elem)).getLeg3().getRatioTapChanger();
            Bus             regBus      =tapCh3ry.getRegulationTerminal().getBusBreakerView().getConnectableBus();

            itemList.add(new SimpleEntry<>("Tert. Ratio Load Tap Changing Capable"  ,new ReadOnlyObjectWrapper(tapCh3ry.hasLoadTapChangingCapabilities())));
            itemList.add(new SimpleEntry<>("Tert. Ratio Target Voltage"             ,new SimpleDoubleProperty(tapCh3ry.getTargetV())));                                    valueListener(itemList.get(offset1+4).getValue(),   RatioTapChanger.class.getMethod ("setTargetV"       ,double.class),  tapCh3ry   ,false);
            itemList.add(new SimpleEntry<>("Tert. Ratio Target Deadband"            ,new SimpleDoubleProperty(tapCh3ry.getTargetDeadband())));                             valueListener(itemList.get(offset1+5).getValue(),   RatioTapChanger.class.getMethod ("setTargetDeadband",double.class),  tapCh3ry   ,false);
            switch(tapCh3ry.getRegulationTerminal().getVoltageLevel().getTopologyKind())
            {
                case BUS_BREAKER:
                    itemList.add(new SimpleEntry<>("Sec. Ratio Regulation Bus"     ,new ReadOnlyObjectWrapper(regBus!=null?regBus.getName():"<Disconnected>")));
                    break;
                case NODE_BREAKER:
                    itemList.add(new SimpleEntry<>("Sec. Ratio Regulation Bus"     ,new ReadOnlyObjectWrapper(regBus!=null?regBus.getName()+" / "+tapCh3ry.getRegulationTerminal().getNodeBreakerView().getNode():"<Disconnected>")));
                    break;
            }
            itemList.add(new SimpleEntry<>("Tert. Ratio Low Tap Position"           ,new ReadOnlyObjectWrapper(tapCh3ry.getLowTapPosition())));                                     
            itemList.add(new SimpleEntry<>("Tert. Ratio High Tap Position"          ,new ReadOnlyObjectWrapper(tapCh3ry.getHighTapPosition())));                                    
            itemList.add(new SimpleEntry<>("Tert. Ratio Regulator is Active"        ,new SimpleBooleanProperty(tapCh3ry.isRegulating())));                                 valueListener(itemList.get(offset1+9 ).getValue(),  RatioTapChanger.class.getMethod ("setRegulating",       boolean.class),  tapCh3ry   ,false  ,selected);
            itemList.add(new SimpleEntry<>("Tert. Ratio Tap Position"               ,new SimpleIntegerProperty(tapCh3ry.getTapPosition())));                               valueListener(itemList.get(offset1+10).getValue(),  RatioTapChanger.class.getMethod ("setTapPosition"   ,   int.class),      tapCh3ry   ,false);
        }
        else
        {
            bAdd3RTC =new Button ("Add Tertiary RTC");
        }
        
        itemList.add(new SimpleEntry<>("Tertiary Ratio Tap Changer"             ,bAdd3RTC));
        bAdd3RTC.setOnAction(event-> {
                                        propGrid.getItems().clear();
                                        this.initRTC((RatioTapChangerHolder)((ThreeWindingsTransformer)(elem)).getLeg3());
                                    });
    }
    
    private void setRTC(RatioTapChangerHolder elem, RatioStepParameters... setpsParams)
    {
        
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void initRTC(RatioTapChangerHolder elem)
    {
        ObservableList<Entry<String,Object>>    itemList    =FXCollections.observableArrayList();
        Connectable                             elemConn    =getConnectable(elem);
        BranchParameters                        nominals    =getBranchParams(elem);
        RatioTapChanger                         oldRTC      =elem.getRatioTapChanger();
        int                                     oldSteps    =oldRTC!=null?oldRTC.getStepCount()-1:1;
        int                                     oldLowTapPos=oldRTC!=null?oldRTC.getLowTapPosition():0;
        int                                     oldHighTapPos=oldRTC!=null?oldRTC.getHighTapPosition():0;
        boolean                                 oldTCUL     =oldRTC!=null?oldRTC.hasLoadTapChangingCapabilities():false;
        double                                  oldLowestR  =oldRTC!=null?Math.min(oldRTC.getStep(oldLowTapPos).getRho(), oldRTC.getStep(oldHighTapPos).getRho()):0.99;
        double                                  oldStepSize =oldRTC!=null?Math.abs(oldRTC.getStep(oldHighTapPos).getRho()-oldRTC.getStep(oldLowTapPos).getRho())/Double.valueOf(oldSteps):0.01;
        RTCStdParams                            stdParams   =new RTCStdParams();
        Terminal                                regTerm     =oldRTC!=null?oldRTC.getRegulationTerminal():null;
        VoltageLevel                            regTermVL   =regTerm!=null?regTerm.getVoltageLevel():null;
        Button                                  bRTerm      =new Button((regTerm!=null && regTermVL!=null)?regTermVL.getTopologyKind()==TopologyKind.NODE_BREAKER?regTermVL.getName()+" / " +regTerm.getNodeBreakerView().getNode():regTerm.getBusBreakerView().getConnectableBus().getName():"Select Reg. Terminal");
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        ThreeWindingsTransformer.Side           side        =null;
        
        if(this.propGrid.getItems().isEmpty())
        {
            
            if(elem instanceof Leg2or3)
            {
                Terminal legTerm=((Leg2or3) elem).getTerminal();
                side = ((ThreeWindingsTransformer)elemConn).getSide(legTerm);
            }
            bRTerm.setUserData(regTerm);
            itemList.add(new SimpleEntry<>((oldRTC!=null?"Modifying":"Adding")+
                                           " PTC for"               ,new ReadOnlyObjectWrapper(elemConn.getName()+(side!=null?" ("+side.toString()+")":""))));
            itemList.add(new SimpleEntry<>("Setps"                  ,new SimpleIntegerProperty(oldSteps)));
            itemList.add(new SimpleEntry<>("Step Size"              ,new SimpleDoubleProperty (oldStepSize)));
            itemList.add(new SimpleEntry<>("Lowest Ratio"           ,new SimpleDoubleProperty (oldLowestR)));
            itemList.add(new SimpleEntry<>("Low Tap Position"       ,new SimpleIntegerProperty(oldLowTapPos)));
            itemList.add(new SimpleEntry<>("Under Load Capable"     ,new SimpleBooleanProperty(oldTCUL)));
            itemList.add(new SimpleEntry<>("Regulating Terminal"    ,bRTerm));
            itemList.add(new SimpleEntry<>("Once finished..."       ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."              ,CANCEL));
        
            propGrid.setItems(itemList);
        }
        
        bRTerm.setOnAction(event->  {
                                        Group   cursorImg   =(new SvgLoader()).loadSvg(this.getClass().getResourceAsStream("/img/PointToNode.svg"));
                                        SnapshotParameters  snapParams  =new SnapshotParameters();
                                        snapParams.setFill(Paint.valueOf("#ffffff00"));
                                        
                                        setNodeHandlerValidator("REGULATING_TERMINAL");
                                        VoltageLevelHandler.setMode(VisualHandlerModes.REGULATING_TERM_SEARCH, nhFromIdentifiable(elemConn), bRTerm);
                                        contentPane.getParent().setCursor(new ImageCursor(cursorImg.snapshot(snapParams, null), 3, 3));
                                    });
        
        bRTerm.addEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, event2->{
            NodeHandler         nh2         =event2.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)event2.getOriginalSource()         :event2.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)event2.getOriginalSource()).getId()):null;
            VoltageLevelHandler vlh2        =event2.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)event2.getOriginalSource() :event2.getOriginalSource() instanceof Node?vlHandlers .get(((Node)event2.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
            String              targetVLId2 =vlh2       !=null?vlh2.getVId()                        :nh2!=null?nh2.getVId():null;
            VoltageLevel        targetVL2   =targetVLId2!=null?network.getVoltageLevel(targetVLId2) :null; if(targetVL2==null) return;
//            Terminal            oldTerm     =((RatioTapChangerHolder)elem).getRatioTapChanger().getRegulationTerminal();
            Terminal            regT        =this.getTerminalForAt(elemConn, nh2, vlh2);
            
            if(regT==null)
                regT=elemConn instanceof Branch?((Branch)elemConn).getTerminal(targetVLId2):elemConn instanceof Injection?((Injection)elemConn).getTerminal():null;
            if(regT==null)
            {
                event2.consume();
                return;
            }
            
            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
            contentPane.getParent().setCursor(Cursor.DEFAULT);
            
            bRTerm.setUserData  (regT);
            try
            {
                bRTerm.setText  (regT.getVoltageLevel().getName()+" / "+regT.getNodeBreakerView().getNode());
            }
            catch(Exception ex)
            {
                bRTerm.setText  (regT.getBusBreakerView().getConnectableBus().getName());
            }
            
            event2.consume();
        });
        
        CANCEL.setOnAction(event3-> {
                                        propGrid.getItems().clear();
                                        VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                                        contentPane.getParent().setCursor(Cursor.DEFAULT);
                                    });
        OKB.setOnAction(event2->{
            try
            {
                NodeHandler nh  =nhFromIdentifiable(elemConn);
                
                stdParams.steps         =(((TableColumn<Entry<String,String>,SimpleIntegerProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue());
                stdParams.stepSize      =(((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(2       ).getValue().getValue());
                stdParams.lowestRatio   =(((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(3       ).getValue().getValue());
                stdParams.lowTapPosition=(((TableColumn<Entry<String,String>,SimpleIntegerProperty>)propGrid.getColumns().get(1)).getCellObservableValue(4       ).getValue().getValue());
                stdParams.TCUL          =(((TableColumn<Entry<String,String>,SimpleBooleanProperty>)propGrid.getColumns().get(1)).getCellObservableValue(5       ).getValue().getValue());
                stdParams.regTerminal   =(Terminal) bRTerm.getUserData();
                setRTC(elem, stdParams);
                
                propGrid.getItems().clear();
                
                this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                
                if(nh!=null)
                    render(nh.getNode());
                
            }
            catch(Exception ex)
            {
                boolean retry   =showAlert(ex);
                
                if(retry)
                    initRTC(elem);
            }
        });
        
        propGrid.getItems().addListener((ListChangeListener.Change change)->{
            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);//Needed to reset unfinished regTerm or 2/3 term assignments, etc.
            contentPane.getParent().setCursor(Cursor.DEFAULT);
        });
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void initPTC(PhaseTapChangerHolder elem)
    {
        ObservableList<Entry<String,Object>>    itemList    =FXCollections.observableArrayList();
        Connectable                             elemConn    =getConnectable(elem);
        BranchParameters                        nominals    =getBranchParams(elem);
        PhaseTapChanger                         oldPTC      =elem.getPhaseTapChanger();
        int                                     oldSteps    =oldPTC!=null?oldPTC.getStepCount()-1:2;
        int                                     oldLowTapPos=oldPTC!=null?oldPTC.getLowTapPosition():0;
        int                                     oldHighTapPos=oldPTC!=null?oldPTC.getHighTapPosition():0;
        double                                  oldLowestR  =oldPTC!=null?Math.min(oldPTC.getStep(oldLowTapPos).getRho(), oldPTC.getStep(oldHighTapPos).getRho()):0.99;
        double                                  oldLowestP  =oldPTC!=null?Math.min(oldPTC.getStep(oldLowTapPos).getAlpha(), oldPTC.getStep(oldHighTapPos).getAlpha()):-0.1;
        double                                  oldRStepSize=oldPTC!=null?Math.abs(oldPTC.getStep(oldHighTapPos).getRho()-oldPTC.getStep(oldLowTapPos).getRho())/(oldSteps):0.01;
        double                                  oldPStepSize=oldPTC!=null?Math.abs(oldPTC.getStep(oldHighTapPos).getAlpha()-oldPTC.getStep(oldLowTapPos).getAlpha())/(oldSteps):0.1;
        PTCStdParams                            stdParams   =new PTCStdParams();
        Terminal                                regTerm     =oldPTC!=null?oldPTC.getRegulationTerminal():null;
        VoltageLevel                            regTermVL   =regTerm!=null?regTerm.getVoltageLevel():null;
        Button                                  bRTerm      =new Button((regTerm!=null && regTermVL!=null)?regTermVL.getTopologyKind()==TopologyKind.NODE_BREAKER?regTermVL.getName()+" / " +regTerm.getNodeBreakerView().getNode():regTerm.getBusBreakerView().getConnectableBus().getName():"Select Reg. Terminal");
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        
        if(this.propGrid.getItems().isEmpty())
        {
            bRTerm.setUserData(regTerm);
            itemList.add(new SimpleEntry<>((oldPTC!=null?"Modifying":"Adding")+
                                           " PTC for"               ,new ReadOnlyObjectWrapper(elemConn.getName())));
            itemList.add(new SimpleEntry<>("Setps"                  ,new SimpleIntegerProperty(oldSteps)));
            itemList.add(new SimpleEntry<>("Ratio Step Size"        ,new SimpleDoubleProperty (oldRStepSize)));
            itemList.add(new SimpleEntry<>("Phase Step Size"        ,new SimpleDoubleProperty (oldPStepSize)));
            itemList.add(new SimpleEntry<>("Lowest Ratio"           ,new SimpleDoubleProperty (oldLowestR)));
            itemList.add(new SimpleEntry<>("Lowest Angle"           ,new SimpleDoubleProperty (oldLowestP)));
            itemList.add(new SimpleEntry<>("Low Tap Position"       ,new SimpleIntegerProperty(oldLowTapPos)));
            itemList.add(new SimpleEntry<>("Regulating Terminal"    ,bRTerm));
            itemList.add(new SimpleEntry<>("Once finished..."       ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."              ,CANCEL));
        
            propGrid.setItems(itemList);
        }
        
        bRTerm.setOnAction(event->  {
                                        Group               cursorImg   =(new SvgLoader()).loadSvg(this.getClass().getResourceAsStream("/img/PointToNode.svg"));
                                        SnapshotParameters  snapParams  =new SnapshotParameters();
                                        snapParams.setFill(Paint.valueOf("#ffffff00"));
                                        
                                        setNodeHandlerValidator("REGULATING_TERMINAL");
                                        VoltageLevelHandler.setMode(VisualHandlerModes.REGULATING_TERM_SEARCH, nhFromIdentifiable(elemConn), bRTerm);
                                        contentPane.getParent().setCursor(new ImageCursor(cursorImg.snapshot(snapParams, null), 3, 3));
                                    });
        
        bRTerm.addEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, event2->{
            NodeHandler         nh2         =event2.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)event2.getOriginalSource()         :event2.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)event2.getOriginalSource()).getId()):null;
            VoltageLevelHandler vlh2        =event2.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)event2.getOriginalSource() :event2.getOriginalSource() instanceof Node?vlHandlers .get(((Node)event2.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
            String              targetVLId2 =vlh2!=null?vlh2.getVId():nh2!=null?nh2.getVId():null;
            VoltageLevel        targetVL2   =targetVLId2!=null?network.getVoltageLevel(targetVLId2) :null; if(targetVL2==null) return;
//            Terminal            oldTerm     =elem.getPhaseTapChanger().getRegulationTerminal();
            Terminal            regT        =this.getTerminalForAt(elemConn, nh2, vlh2);
            
            if(regT==null)
                regT=elemConn instanceof Branch?((Branch)elemConn).getTerminal(targetVLId2):((Injection)elemConn).getTerminal();
            if(regT==null)
                regT=(Terminal) elemConn.getTerminals().get(0);
            
            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
            contentPane.getParent().setCursor(Cursor.DEFAULT);
            
            bRTerm.setUserData  (regT);
            try
            {
                bRTerm.setText  (regT.getVoltageLevel().getName()+" / "+regT.getNodeBreakerView().getNode());
            }
            catch(Exception ex)
            {
                bRTerm.setText  (regT.getBusBreakerView().getConnectableBus().getName());
            }
            
            event2.consume();
        });
        
        CANCEL.setOnAction(event3-> {
                                        propGrid.getItems().clear();
                                        VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                                        contentPane.getParent().setCursor(Cursor.DEFAULT);
                                    });
        OKB.setOnAction(event2->{
            try
            {
                NodeHandler nh  =nhFromIdentifiable(elemConn);
                
                stdParams.steps         =(((TableColumn<Entry<String,String>,SimpleIntegerProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue());
                stdParams.rStepSize     =(((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(2       ).getValue().getValue());
                stdParams.pStepSize     =(((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(3       ).getValue().getValue());
                stdParams.lowestRatio   =(((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(4       ).getValue().getValue());
                stdParams.lowestAngle   =(((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(5       ).getValue().getValue());
                stdParams.lowTapPosition=(((TableColumn<Entry<String,String>,SimpleIntegerProperty>)propGrid.getColumns().get(1)).getCellObservableValue(6       ).getValue().getValue());
                stdParams.regTerminal   =(Terminal) bRTerm.getUserData();
                setPTC(elem, stdParams);
                
                this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                
                propGrid.getItems().clear();
                
                if(nh!=null)
                    render(nh.getNode());
            }
            catch(Exception ex)
            {
                boolean retry   =showAlert(ex);
                
                if(retry)
                    initPTC(elem);
            }
        });
        
        propGrid.getItems().addListener((ListChangeListener.Change change)->{
            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);//Needed to reset unfinished regTerm or 2/3 term assignments, etc.
            contentPane.getParent().setCursor(Cursor.DEFAULT);
        });
    }
    
    private NodeHandler nhFromIdentifiable(Identifiable elemConn)
    {
        if(nodeHandlers.containsKey(elemConn.getId()))
            return nodeHandlers.get(elemConn.getId());
        else if(nodeHandlers.containsKey(elemConn.getId()+"_ONE"))
            return nodeHandlers.get(elemConn.getId()+"_ONE");
        else if(nodeHandlers.containsKey(elemConn.getId()+"_TWO"))
            return nodeHandlers.get(elemConn.getId()+"_TWO");
        else if(nodeHandlers.containsKey(elemConn.getId()+"_THREE"))
            return nodeHandlers.get(elemConn.getId()+"_THREE");
        else if(nodeHandlers.containsKey("VL_LABEL_"+elemConn.getId()))
            return nodeHandlers.get("VL_LABEL_"+elemConn.getId());
        
        return null;
    }
    
    private void setRTC(RatioTapChangerHolder elem, RTCStdParams stdParams)
    {
        Connectable                     elemConn    =getConnectable(elem);
        BranchParameters                nominals    =getBranchParams(elem);
        double                          dR          =nominals.R*stdParams.stepSize;
        double                          dX          =nominals.X*stdParams.stepSize;
        double                          dG          =0.0;//nominals.G*stdParams.stepSize;
        double                          dB          =0.0;//nominals.B*stdParams.stepSize;
        RatioTapChangerAdder            RTCAdder    =elem.newRatioTapChanger();
        
        for(int step=0; step<=stdParams.steps;step++)
        {
            RatioTapChangerAdder.StepAdder  stepAdder   =RTCAdder.beginStep();
            double                          electricStep=(Double.valueOf(step)+/*Math.floor*/((stdParams.lowestRatio-1.00)/stdParams.stepSize));
            
            electricStep=Double.isNaN(electricStep)?0.0:electricStep;
            
            stepAdder   .setR(nominals.R + dR*electricStep)
                        .setX(nominals.X + dX*electricStep)
                        .setG(nominals.G + dG*electricStep)
                        .setB(nominals.B + dB*electricStep)
                        .setRho(1.00+stdParams.stepSize*electricStep)
                        .endStep();
        }
        RTCAdder.setLoadTapChangingCapabilities(stdParams.TCUL)
                .setLowTapPosition(stdParams.lowTapPosition)
                .setRegulating(false)
                .setTargetDeadband(1.0)
                .setTargetV(Double.NaN)
                .setRegulationTerminal((Terminal) stdParams.regTerminal)
                .setTapPosition(stdParams.lowTapPosition)
                .add();
        
        this.SelectionManagerTgt.setUserData("RTC adjusted.");
        this.SelectionManagerTgt.fireEvent(new ControllerRequests("RTC adjusted.", ControllerRequests.SHOW_MESSAGE));
    }
    
    private void setPTC(PhaseTapChangerHolder elem, PTCStdParams stdParams)
    {
        Connectable                     elemConn    =getConnectable(elem);
        BranchParameters                nominals    =getBranchParams(elem);
        double                          dR          =nominals.R*stdParams.rStepSize;
        double                          dX          =nominals.X*stdParams.rStepSize;
        double                          dG          =0.0;//nominals.G*stdParams.stepSize;
        double                          dB          =0.0;//nominals.B*stdParams.stepSize;
        PhaseTapChangerAdder            PTCAdder    =elem.newPhaseTapChanger();
        
        for(int step=0; step<=stdParams.steps;step++)
        {
            PhaseTapChangerAdder.StepAdder  stepAdder   =PTCAdder.beginStep();
            double                          rStep       =(Double.valueOf(step)+/*Math.floor*/((stdParams.lowestRatio-1.00)/stdParams.rStepSize));
            double                          pStep       =(Double.valueOf(step)+/*Math.floor*/((stdParams.lowestAngle     )/stdParams.pStepSize));
            
            rStep       =Double.isNaN(rStep)?0.0:rStep;
            pStep       =Double.isNaN(pStep)?0.0:pStep;
            
            stepAdder   .setR       (nominals.R + dR*rStep)
                        .setX       (nominals.X + dX*rStep)
                        .setG       (nominals.G + dG*rStep)
                        .setB       (nominals.B + dB*rStep)
                        .setRho     (1+stdParams.rStepSize*rStep)
                        .setAlpha   (  stdParams.pStepSize*pStep)
                        .endStep    ();
        }
        PTCAdder.setLowTapPosition(stdParams.lowTapPosition)
                .setRegulationValue(Double.NaN)
                .setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP)
                .setRegulating(false)
                .setTargetDeadband(1.0)
                .setRegulationTerminal((Terminal) stdParams.regTerminal)
                .setTapPosition(stdParams.lowTapPosition)
                .add();
        
        this.SelectionManagerTgt.setUserData("PTC adjusted.");
        this.SelectionManagerTgt.fireEvent(new ControllerRequests("PTC adjusted.", ControllerRequests.SHOW_MESSAGE));
    }
    
    private BranchParameters getBranchParams(Object elem)
    {
        BranchParameters    nominals    =new BranchParameters();
        
        try
        {
            Method  getR    =elem.getClass().getMethod("getR");
            
            getR.setAccessible(true);
            nominals.R=(double) getR.invoke(elem);
        }
        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
//            return null;
        }
        
        try
        {
            Method  getX    =elem.getClass().getMethod("getX");
            
            getX.setAccessible(true);
            nominals.X=(double) getX.invoke(elem);
        }
        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
//            return null;
        }
        
        try
        {
            Method  getG    =elem.getClass().getMethod("getG");
            getG.setAccessible(true);
            nominals.G=(double) getG.invoke(elem);
        }
        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
//            return null;
        }
        
        try
        {
            Method  getB    =elem.getClass().getMethod("getB");
            getB.setAccessible(true);
            nominals.B=(double) getB.invoke(elem);
        }
        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
//            return null;
        }
        
        return nominals;
    }
    
    @SuppressWarnings({"UseSpecificCatch", "null"})
    private Connectable getConnectable(Object elem)
    {
        Connectable connectable =null;
        Method      getTerminal =null;
        
        try {getTerminal =elem.getClass().getMethod("getTerminal");} catch (NoSuchMethodException | SecurityException ex) {}
        
        try
        {
            
            if(getTerminal!=null)
                getTerminal.setAccessible(true);
            connectable =elem instanceof Connectable? (Connectable)elem: ((Terminal)getTerminal.invoke(elem)).getConnectable();
        }
        catch (Exception ex)
        {
            showAlert(ex);
        }
        
        return connectable;
    }
    
    private Property binder(Object val1, Property prop2) throws IllegalArgumentException, NullPointerException
    {
        Property    prop1=  val1 instanceof Property?(Property)val1:
                            val1 instanceof Double  ?new SimpleDoubleProperty   ((double) val1):
                            val1 instanceof Float   ?new SimpleFloatProperty    ((float) val1):
                            val1 instanceof Integer ?new SimpleIntegerProperty  ((int) val1):
                            val1 instanceof String  ?new SimpleStringProperty   ((String) val1):null;
        
        if(prop1==null)
            throw new IllegalArgumentException("val1 parameter must be of type Property, String, Float, Double or Integer.");
        
        prop2.bindBidirectional(prop1);
        
        return prop1;
    }
    
    private void valueListener(Object value, Method callback, Object callbackInstance, boolean redrawOnChange, Node... refreshNodes)
    {
        ObservableValue    prop1=  value instanceof ObservableValue?(ObservableValue)value:
                            value instanceof Double  ?new SimpleDoubleProperty   ((double) value):
                            value instanceof Float   ?new SimpleFloatProperty    ((float) value):
                            value instanceof Integer ?new SimpleIntegerProperty  ((int) value):
                            value instanceof Boolean ?new SimpleBooleanProperty  ((boolean) value):
                            value instanceof String  ?new SimpleStringProperty   ((String) value):null;
        
        if(prop1==null)
            throw new IllegalArgumentException("val1 parameter must be of type Property, String, Float, Double or Integer.");
        
        @SuppressWarnings("Convert2Lambda")
        ChangeListener  newListener =new ChangeListener(){
            @Override                        
            public void changed(ObservableValue observable, Object oldValue, Object newValue){refreshNetValue(observable, oldValue, newValue, callback, callbackInstance, redrawOnChange, refreshNodes);}
        };
        
        prop1.addListener(newListener);
    }

    private void refreshNetValue(ObservableValue observable, Object oldValue, Object newValue, Method callback, Object callbackInstance, boolean redrawOnChange, Node... refreshNodes) throws java.lang.ClassCastException
    {
        try {
            if(!callback.isAccessible())
                callback.setAccessible(true);
            callback.invoke(callbackInstance, newValue);
            

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            
            boolean retry   =showAlert(ex);
            
            try
            {
                ((Property)observable).setValue(oldValue);
                if(!retry)
                    return;
            }
            catch(Exception ex2)
            {
                if(!retry)
                    return;
            }
        }
        if(refreshNodes!=null)
            for(Node node:refreshNodes)
                try 
                {
                    render(node);
                } catch (NoSuchMethodException | SecurityException | UnsupportedEncodingException ex) {
                    Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
        if(redrawOnChange && oldValue!=((Property)observable).getValue())
            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
    }
    
    public void clearSelected()
    {
        lastSelectedNodes.clear();
        this.selectedCell.setValue((BusCell)null);
    }

    private void installEditionHandlers(Parent parent)
    {
        for(Node next:parent.getChildrenUnmodifiable())
        {
            
            if(next instanceof Parent)
                installEditionHandlers((Parent)next);
            
            String              nextId                  =next!=null?next.getId():null;
            NodeMetadata        nextNodeMetadata        =nextId!=null?metadata.getNodeMetadata(nextId):null;
            ComponentMetadata   nextComponentMetadata   =nextId!=null&&nextNodeMetadata!=null?metadata.getComponentMetadata(nextNodeMetadata.getComponentType()):null;

            if(next==null)
                continue;
            if(nextNodeMetadata==null && nextComponentMetadata==null)
                continue;
            
            next.setOnMouseClicked(event->{//This handler can't be included into NodeHandler because network object isn't passed to them
                    Node                source              =event.getSource() instanceof Node?(Node)event.getSource():null;
                    String              id                  =source!=null?source.getId():null;
//                    String              id_                 =id!=null?id.replaceAll("(_fictif)$", "").replaceAll("(_THREE)$", "").replaceAll("(_TWO)$", "").replaceAll("(_ONE)$", "").replaceAll("(_FOUR)$", "").replaceAll("^(FICT_)", "").replaceAll("(Fictif)$", "").replaceAll("(fict)$", "").replaceAll("^(LABEL_VL_)",""):null;
                    NodeMetadata        nodeMetadata        =id!=null?metadata.getNodeMetadata(id):null;
                    ComponentMetadata   componentMetadata   =id!=null&&nodeMetadata!=null?metadata.getComponentMetadata(nodeMetadata.getComponentType()):null;
                    Identifiable        idElem              =id!=null?this.getIdentifiable(id):null;
//                    Identifiable        idElem              =id_!=null?network.getIdentifiable(id_):null;
                    
                    if(source==null)
                        return;
                    if(nodeMetadata==null && componentMetadata==null)
                        return;
                    if(!VisualHandlerModes.NORMAL.equals(VoltageLevelHandler.getMode()))
                    {
                        event.consume();
                        return;
                    }
                    //Primary clicks
                    if(event.getButton().equals(MouseButton.PRIMARY) && !event.isSecondaryButtonDown() &&!event.isMiddleButtonDown())
                    {
                        switch(event.getClickCount())
                        {
                            case 1://Single Click
                                event.consume();
                                break;
                            case 2://Double click
                                if(!event.isAltDown() && !event.isControlDown() &&! event.isMetaDown())//No modifiers active
                                {
                                    if(componentMetadata!=null && nodeMetadata!=null)
                                    {
                                        switch(componentMetadata.getType())
                                        {
                                            default:
//                                            case BREAKER:
//                                            case LOAD_BREAK_SWITCH:
//                                            case DISCONNECTOR:
//                                                Switch  sw      =network.getVoltageLevel(nodeMetadata.getVId()).getTopologyKind()==TopologyKind.BUS_BREAKER?
//                                                                    network.getVoltageLevel(nodeMetadata.getVId()).getBusBreakerView().getSwitch(next.getId()):
//                                                                    network.getVoltageLevel(nodeMetadata.getVId()).getNodeBreakerView().getSwitch(next.getId());

                                                if(network.getVoltageLevel(nodeMetadata.getVId()).getTopologyKind()==TopologyKind.BUS_BREAKER)
                                                {
                                                    Connectable     connElem    =null;
                                                    List<Terminal>  terms       =null;
                                                    if(idElem instanceof Connectable)
                                                        connElem=(Connectable) idElem;
                                                    
                                                    if(connElem!=null)
                                                        terms=connElem.getTerminals();
                                                    
                                                    if(terms!=null)
                                                    {
                                                        terms.forEach((Terminal term) -> {
                                                            if(term.isConnected())
                                                                term.disconnect();
                                                            else
                                                                term.connect();
                                                        });
//                                                        this.contentPane.fireEvent(new ControllerRequests(this.contentPane, ControllerRequests.REDRAW_GRAPH));
//                                                        this.contentPane.fireEvent(ControllerRequests.REDRAW_REQUEST);
                                                        this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                                                    }
                                                    event.consume();
                                                }
                                                else if(network.getVoltageLevel(nodeMetadata.getVId()).getTopologyKind()==TopologyKind.NODE_BREAKER)
                                                {
                                                    Switch  switchElem;//  =null;
                                                    if(idElem instanceof Switch)
                                                    {
                                                        switchElem  =(Switch)idElem;
                                                        switchElem.setOpen(!switchElem.isOpen());
//                                                        this.contentPane.fireEvent(ControllerRequests.REDRAW_REQUEST);
                                                        this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                                                    }
                                                }
                                                break;
//                                            default:
//                                                ;
                                        }
                                    }
                                }
                                break;
                            default:
                                ;
                        }
                    }
                    
                    //Secondary clicks
                    if(event.getButton().equals(MouseButton.SECONDARY) && !event.isAltDown() && !event.isControlDown() &&! event.isMetaDown() &!event.isPrimaryButtonDown() &&!event.isMiddleButtonDown())
                    {
                        switch(event.getClickCount())
                        {
                            case 1:
                                
                                break;
                            case 2:
                                break;
                            default:
                                ;
                        }
                    }

                });
            
            next.addEventHandler(MouseEvent.MOUSE_PRESSED, event->{//This handler can't be included into NodeHandler because network object isn't passed to them
                    Node                source              =event.getSource() instanceof Node?(Node)event.getSource():null;
                    String              id                  =source!=null?source.getId():null;
//                    String              id_                 =id!=null?id.replaceAll("(_fictif)$", "").replaceAll("(_THREE)$", "").replaceAll("(_TWO)$", "").replaceAll("(_ONE)$", "").replaceAll("(_FOUR)$", "").replaceAll("^(FICT_)", "").replaceAll("(Fictif)$", "").replaceAll("(fict)$", "").replaceAll("^(LABEL_VL_)",""):null;
                    NodeMetadata        nodeMetadata        =id!=null?metadata.getNodeMetadata(id):null;
                    ComponentMetadata   componentMetadata   =id!=null&&nodeMetadata!=null?metadata.getComponentMetadata(nodeMetadata.getComponentType()):null;
                    Identifiable        idElem              =id!=null?this.getIdentifiable(id):null;
//                    Identifiable        idElem              =id_!=null?network.getIdentifiable(id_):null;
                    
                    if(source==null)
                        return;
                    if(nodeMetadata==null && componentMetadata==null)
                        return;
                    if(!VisualHandlerModes.NORMAL.equals(VoltageLevelHandler.getMode()))
                    {
                        event.consume();
                        return;
                    }
                    //Primary clicks
                    if(event.getButton().equals(MouseButton.PRIMARY) && !event.isSecondaryButtonDown() &&!event.isMiddleButtonDown())
                    {
                        if(!event.isAltDown() && !event.isControlDown() &&! event.isMetaDown())//No modifiers active
                        {
                            try {

                                render(source);
                                lastSelectedNodes.setAll(source);
                                event.consume();
                            } catch (NoSuchMethodException | SecurityException | UnsupportedEncodingException ex) {
                                Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    
                    //Secondary clicks
                    if(event.getButton().equals(MouseButton.SECONDARY) && !event.isAltDown() && !event.isControlDown() &&! event.isMetaDown() &!event.isPrimaryButtonDown() &&!event.isMiddleButtonDown())
                    {
                    }

                });
        }

    }
    
    private /*static*/ void installVisualHandlers(Node node, GraphMetadata metadata,
                                            Map<String, WireHandler> wireHandlers,//List<WireHandler> wireHandlers,
                                            Map<String, NodeHandler> nodeHandlers,
                                            Map<String, VoltageLevelHandler> vlHandlers)
    {
        if (!StringUtils.isEmpty(node.getId())) {
            GraphMetadata.NodeMetadata nodeMetadata = metadata.getNodeMetadata(node.getId());
            if (nodeMetadata != null)
            {
                if (node instanceof Group &&
                        (nodeMetadata.getComponentType() != null) &&
                        (nodeMetadata.getComponentType().equals(ComponentTypeName.BREAKER) || nodeMetadata.getComponentType().equals(ComponentTypeName.DISCONNECTOR) || nodeMetadata.getComponentType().equals(ComponentTypeName.LOAD_BREAK_SWITCH))) {
//                        (nodeMetadata.getComponentType().equals(ComponentType.BREAKER) || nodeMetadata.getComponentType().equals(ComponentType.DISCONNECTOR) || nodeMetadata.getComponentType().equals(ComponentType.LOAD_BREAK_SWITCH))) {
                    setNodeVisibility((Group) node, nodeMetadata);
                }
                installVisualNodeHandlers(node, metadata, nodeMetadata, nodeHandlers, vlHandlers);
            }
            GraphMetadata.WireMetadata wireMetadata = metadata.getWireMetadata(node.getId());
            if (wireMetadata != null) {
                try
                {
                installVisualWireHandlers(node, metadata, wireMetadata, nodeHandlers, wireHandlers);
                }
                catch(Exception ex)
                {
                    Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            GraphMetadata.ArrowMetadata arrowMetadata = metadata.getArrowMetadata(node.getId());
            if (arrowMetadata != null) {
                WireHandler wireHandler = wireHandlers.get(arrowMetadata.getWireId());
                wireHandler.addArrow((Group) node);
            }
        }

        // propagate to children
        if (node instanceof Group) {
            Group group = (Group) node;
            for (Node child : group.getChildren()) {
                installVisualHandlers(child, metadata, wireHandlers, nodeHandlers, vlHandlers);
            }
        }
    }
    
    private /*static*/ void installVisualNodeHandlers(Node node, GraphMetadata metadata,
                                            GraphMetadata.NodeMetadata nodeMetadata,
                                            Map<String, NodeHandler> nodeHandlers,
                                            Map<String, VoltageLevelHandler> vlHandlers)
    {
        if (!nodeMetadata.isVLabel()) {
            NodeHandler nodeHandler = new NodeHandler(node, nodeMetadata.getComponentType(),
                                                      nodeMetadata.getRotationAngle(), metadata,
                                                      nodeMetadata.getVId(),
                                                      nodeMetadata.getNextVId(),
                                                      nodeMetadata.getDirection());
            nodeHandler.setDisplayVL(this);
            LoggerFactory.getLogger(MainFXMLController.class).trace("Add handler to node {} in voltageLevel {}", node.getId(), nodeMetadata.getVId());
            nodeHandlers.put(node.getId(), nodeHandler);
            //TODO remove this as node's listener
            nodeHandler.getNode().addEventHandler(ConnectionEvent.CONNECT_TERMINAL          , this);
            nodeHandler.getNode().addEventHandler(ConnectionEvent.INSERT_COMPONENT          , this);
            nodeHandler.getNode().addEventHandler(ConnectionEvent.INSERT_SWITCH             , this);
//                    nodeHandler.getNode().addEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, this);
//                    nodeHandler.getNode().addEventHandler(ConnectionEvent.INSERT_BUSBAR_SECTION     , this);
        } else {  // handler for voltageLevel label
            VoltageLevelHandler vlHandler = new VoltageLevelHandler(node, metadata, nodeMetadata.getVId());
            LoggerFactory.getLogger(MainFXMLController.class).trace("Add handler to voltageLvel label {}", node.getId());
            vlHandlers.put(nodeMetadata.getVId(), vlHandler);
            //TODO remove this as node's listener
            vlHandler.getNode().addEventHandler(ConnectionEvent.INSERT_BUSBAR_SECTION, this);//TODO: implement component insertion event firning, when insertin busbars
        }
    }
    
    private static void setNodeVisibility(Group node, GraphMetadata.NodeMetadata nodeMetadata) {
        node.getChildren().forEach(child -> child.setVisible((nodeMetadata.isOpen() && child.getId().equals("open")) || (!nodeMetadata.isOpen() && child.getId().equals("closed"))));
    }
    
    private static void installVisualWireHandlers(Node node, GraphMetadata metadata, GraphMetadata.WireMetadata wireMetadata, Map<String, NodeHandler> nodeHandlers, Map<String, WireHandler> wireHandlers) {
        NodeHandler nodeHandler1 = nodeHandlers.get(wireMetadata.getNodeId1());
        if (nodeHandler1 == null) {
            throw new PowsyblException("Node 1 " + wireMetadata.getNodeId1() + " not found");
        }
        NodeHandler nodeHandler2 = nodeHandlers.get(wireMetadata.getNodeId2());
        if (nodeHandler2 == null) {
            throw new PowsyblException("Node 2 " + wireMetadata.getNodeId2() + " not found");
        }
        WireHandler wireHandler = new WireHandler((Polyline) node, nodeHandler1, nodeHandler2, wireMetadata.isStraight(),
                wireMetadata.isSnakeLine(), metadata);
        LoggerFactory.getLogger(MainFXMLController.class).trace(" Added handler to wire between {} and {}", wireMetadata.getNodeId1(), wireMetadata.getNodeId2());
        wireHandlers.put(node.getId(), wireHandler);
    }
    
    private /*static*/ void installVisualHandlers(Node contentPane, GraphMetadata metadata)
    {
        wireHandlers.clear();
        nodeHandlers.clear();
        vlHandlers.clear();
        System.gc();
        installVisualHandlers(contentPane, metadata, wireHandlers, nodeHandlers, vlHandlers);
        
        // resolve links
        for (WireHandler wireHandler : wireHandlers.values()) {
            wireHandler.getNodeHandler1().addWire(wireHandler);
            wireHandler.getNodeHandler2().addWire(wireHandler);
        }

        // resolve voltageLevel handler
        vlHandlers.values().stream().forEach(v -> v.addNodeHandlers(nodeHandlers.values().stream().collect(Collectors.toList())));
    }
    
    private void installMultSelectionControl()
    {
        contentPane.getParent().setOnMousePressed(dragEvent->
        {
            if(VoltageLevelHandler.getMode()!=VisualHandlerModes.NORMAL)
            {
                dragEvent.consume();
                return;
            }
            multSelX    =dragEvent.getX();
            multSelY    =dragEvent.getY();
            
            multSelRect.setX(multSelX);
            multSelRect.setY(multSelY);
            multSelRect.setWidth(0.0);
            multSelRect.setHeight(0.0);
            
            if(lastSelectedNodes.isEmpty())
            {
                if(propGrid!=null?propGrid.getItems()!=null:false)
                    propGrid.getItems().clear();
            }
            else
                lastSelectedNodes.clear();
        });
        contentPane.getParent().setOnMouseDragged(dragEvent->
        {
            if(VoltageLevelHandler.getMode()!=VisualHandlerModes.NORMAL)
            {
                dragEvent.consume();
                return;
            }
            Point2D newCoords   =new Point2D(dragEvent.getX(), dragEvent.getY());
            Point2D oldCoords   =new Point2D(multSelX, multSelY);
            double  width       =newCoords.getX()-oldCoords.getX();
            double  height      =newCoords.getY()-oldCoords.getY();
            
            multSelRect.setX(oldCoords.getX()+(width>0?0:width));
            multSelRect.setY(oldCoords.getY()+(height>0?0:height));
            multSelRect.setWidth(width>0?width:-width);
            multSelRect.setHeight(height>0?height:-height);
            if(!multSelRect.isVisible())
                multSelRect.setVisible(true);
            selectMultiple(contentPane);
        });
        contentPane.getParent().setOnMouseReleased(dragEvent->
        {
            if(VoltageLevelHandler.getMode()!=VisualHandlerModes.NORMAL)
            {
                dragEvent.consume();
                return;
            }
            if(multSelRect.isVisible())
                multSelRect.setVisible(false);
            if(lastSelectedNodes.size()==1)
            {
                try
                {
                    render(lastSelectedNodes.get(0));
                } catch (NoSuchMethodException | SecurityException | UnsupportedEncodingException ex) {
                    Logger.getLogger(SelectionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
        
        vlHandlers.values().stream().forEach(element->{element.getNode().addEventHandler(DraggedNodeEvent.NODE_DRAGGED, this);});
        vlHandlers.values().stream().forEach(element->{element.getNode().addEventHandler(DraggedNodeEvent.NODE_DRAG_START, this);});
        nodeHandlers.values().stream().forEach(element->{element.getNode().addEventHandler(DraggedNodeEvent.NODE_DRAGGED, this);});
        nodeHandlers.values().stream().forEach(element->{element.getNode().addEventHandler(DraggedNodeEvent.NODE_DRAG_START, this);});
    }
    
    public static Point2D getNearestAnchor(Point2D refPoint, List<AnchorPoint> anchors)
    {
        ArrayList<Point2D>  points  =new ArrayList<>();
        
        for(AnchorPoint nextAnchor:anchors)
            points.add(new Point2D(nextAnchor.getX(), nextAnchor.getY()));
        
        return getNearest(refPoint, points);
    }
    
    public static Point2D getNearest(Point2D refPoint, List<Point2D> points)
    {
        if(points==null?true:points.isEmpty())
            return refPoint;
        
        Point2D     result  =points.get(0);
        double      minDist =result.distance(refPoint);
        
        for(Point2D nextAnchor:points)
        {
            double  dist=nextAnchor.distance(refPoint);
            
            if(dist<minDist)
            {
                result  =nextAnchor;
                minDist =result.distance(refPoint);
            }
        }
        
        return result;
        
    }

    public void selectMultiple(Parent parent)
    {
        Bounds  selectorBounds  =multSelRect.localToScreen(multSelRect.getBoundsInLocal());
        
        for(Node next:parent.getChildrenUnmodifiable())
        {
            if(next instanceof Parent)
                selectMultiple((Parent)next);
            
            String              nextId                  =next!=null?next.getId():null;
            NodeMetadata        nextNodeMetadata        =nextId!=null?metadata.getNodeMetadata(nextId):null;
            ComponentMetadata   nextComponentMetadata   =nextId!=null&&nextNodeMetadata!=null?metadata.getComponentMetadata(nextNodeMetadata.getComponentType()):null;

            if(next==null)
                continue;
            if(nextNodeMetadata==null && nextComponentMetadata==null)
                continue;
            Bounds  nodeBounds  =next.localToScreen(next.getBoundsInLocal());
            
            if(selectorBounds.intersects(nodeBounds))
            {
                if(!lastSelectedNodes.contains(next))
                    lastSelectedNodes.add(next);
            }
            else
                lastSelectedNodes.remove(next);
        }
    }

    public void handleDragNode(DraggedNodeEvent event)
    {
        Node                                    source      =event.getSource() instanceof Node? (Node)event.getSource():null;
        ObservableList<VoltageLevelHandler>     selectedVlh =FXCollections.observableArrayList();
        ObservableList<NodeHandler>             nestedNodes =FXCollections.observableArrayList();//NodeHandler's of nodes included in selected vl's
        ObservableList<NodeHandler>             selectedNode=FXCollections.observableArrayList();//NodeHandler's of all selected nodes not included in selected vl's
        
        if(source==null)
            return;
        
        vlHandlers.forEach((Object key, Object value) -> {
            VoltageLevelHandler vh  =(VoltageLevelHandler)value;
            if(lastSelectedNodes.contains(vh.getNode()))
            {
                if(source!=vh.getNode())
                    selectedVlh.add(vh);
                nestedNodes.addAll((vh).getNodeHandlers().filter((NodeHandler nestedNH) -> lastSelectedNodes.contains((nestedNH).getNode())).toArray(NodeHandler[]::new));
            }
        });
        
        selectedNode.addAll(nodeHandlers.values().stream().filter((element) -> element!=null?!nestedNodes.contains(element)&&(element.getNode()!=source)&&lastSelectedNodes.contains(element.getNode()):false).toArray(NodeHandler[]::new));
        
        selectedVlh.forEach(element->{
            if(event.getEventType().equals(DraggedNodeEvent.NODE_DRAG_START))
                element.presetTranslates();
            else if(event.getEventType().equals(DraggedNodeEvent.NODE_DRAGGED))
                element.translate(event.getDX(), event.getDY());
            
        });
//        
        selectedNode.forEach(element->{
            if(event.getEventType().equals(DraggedNodeEvent.NODE_DRAG_START))
                element.presetTranslates();
            else if(event.getEventType().equals(DraggedNodeEvent.NODE_DRAGGED))
                element.translate(event.getDX(), event.getDY());
        });
    }

    @Override
    public void handle(Event event)
    {
        if(event instanceof DraggedNodeEvent)
            handleDragNode((DraggedNodeEvent)event);
        else if(event instanceof ConnectionEvent)
        {
            handleConnection((ConnectionEvent)event);
        }
    }

    @SuppressWarnings({"UseSpecificCatch", "null"})
    private void handleConnection(ConnectionEvent event)
    {
        NodeHandler         nh          =event.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)event.getOriginalSource()         :event.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)event.getOriginalSource()).getId()):null;
        VoltageLevelHandler vlh         =event.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)event.getOriginalSource() :event.getOriginalSource() instanceof Node?vlHandlers .get(((Node)event.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
        String              targetVLId  =vlh        !=null?vlh.getVId()                         :nh!=null?nh.getVId():null;
        VoltageLevel        targetVL    =targetVLId !=null?network.getVoltageLevel(targetVLId)  :null;
        
        if(targetVL==null)
            return;
        
        VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
        contentPane.getParent().setCursor(Cursor.DEFAULT);
        switch(event.getEventType().getName())
        {
            case ConnectionEvent.CONNECT_TERMINAL_NAME:
                if(event.getTerminalNumber()>=0)
                    return;
                reconnectConnectable(event);
                event.consume();
//                this.contentPane.fireEvent(ControllerRequests.REDRAW_REQUEST);
                this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                break;
            case ConnectionEvent.INSERT_BUSBAR_SECTION_NAME:
                switch(targetVL.getTopologyKind())
                {
                    case BUS_BREAKER:
                        createBus(targetVL);
                        event.consume();
                        break;
                    case NODE_BREAKER:
                        createBusbarSection(targetVL);
                        event.consume();
                        break;
                }
                break;
            case ConnectionEvent.INSERT_COMPONENT_NAME:
                createConnectable(event);
                break;
            case ConnectionEvent.INSERT_SWITCH_NAME:
                switch(targetVL.getTopologyKind())
                {
                    case BUS_BREAKER:
                        event.consume();
                        return;
                    case NODE_BREAKER:
                        createSwitch(nh.getId(), targetVLId);
                        event.consume();
                        break;
                }
                break;
        }
    }
    
    @SuppressWarnings("null")
    private void reconnectConnectable(ConnectionEvent event)
    {
        Terminal        oldTerm =((Terminal)(updatingConnectable.get()).getTerminals().get(changingTerm.get()));
        VoltageLevel    oldVl   =oldTerm.getVoltageLevel();
        Bus             oldBus  =oldVl.getTopologyKind().equals(TopologyKind.BUS_BREAKER)   ?oldTerm.getBusBreakerView().getConnectableBus()   :null;
        int             oldNode =oldVl.getTopologyKind().equals(TopologyKind.NODE_BREAKER)  ?oldTerm.getNodeBreakerView().getNode() :-1;

        try
        {
            NodeHandler     nh      =event.getSource() instanceof Node?nodeHandlers.get(((Node)event.getSource()).getId()):(NodeHandler) event.getSource();
            String          id      =nh.getId();
            Identifiable    identif =getIdentifiable(id);
            String          id_     =identif!=null?identif.getId():null;//id.replaceAll("(_fictif)$", "").replaceAll("(_THREE)$", "").replaceAll("(_TWO)$", "").replaceAll("(_ONE)$", "").replaceAll("(_FOUR)$", "").replaceAll("^(FICT_)", "").replaceAll("(Fictif)$", "").replaceAll("(fict)$", "").replaceAll("^(LABEL_VL_)","");
            String          vlId    =nh.getVId();
            VoltageLevel    vl      =network.getVoltageLevel(vlId);
            Terminal        newTerm;
            
            if(identif==null&&vl.getTopologyKind().equals(TopologyKind.BUS_BREAKER))
                return;

            Builders.detachTerminal(oldTerm);

            newTerm=vl.getTopologyKind().equals(TopologyKind.BUS_BREAKER)?Builders.BuildTerminalExtBBT(network, vl, ((Validable)updatingConnectable.get()), id_, id_):
                    Builders.BuildTerminalExtNBT(network, vl, ((Validable)updatingConnectable.get()), 
                            getNodeFromGraph(id,vlId));

            updatingConnectable.get().getTerminals().set(changingTerm.get(), newTerm);
            if(updatingConnectable.get() instanceof Generator)
            {
                try
                {
                    ((Generator)updatingConnectable.get()).getRegulatingTerminal().getBusBreakerView().getConnectableBus().getName();
                }
                catch(Exception ex2)
                {
                    ((Generator)updatingConnectable.get()).setRegulatingTerminal(newTerm);
                }
            }
            else if(updatingConnectable.get() instanceof TwoWindingsTransformer)
            {
                try
                {
                    if(((TwoWindingsTransformer)updatingConnectable.get()).getRatioTapChanger()!=null)
                        ((TwoWindingsTransformer)updatingConnectable.get()).getRatioTapChanger().getRegulationTerminal().getBusBreakerView().getConnectableBus().getName();
                }
                catch(Exception ex2)
                {
                    ((TwoWindingsTransformer)updatingConnectable.get()).getRatioTapChanger().setRegulationTerminal(newTerm);
                }
            }
            else if(updatingConnectable.get() instanceof ThreeWindingsTransformer)
            {
                Leg2or3 tapHolder   =changingTerm.get()==1?((ThreeWindingsTransformer)updatingConnectable).getLeg2():changingTerm.get()==2?((ThreeWindingsTransformer)updatingConnectable).getLeg3():null;
                    
                try
                {
                    if(tapHolder!=null?tapHolder.getRatioTapChanger()!=null:false)
                        tapHolder.getRatioTapChanger().getRegulationTerminal().getBusBreakerView().getConnectableBus().getName();
                }
                catch(Exception ex2)
                {
                    tapHolder.getRatioTapChanger().setRegulationTerminal(newTerm);
                }
            }
        }
        catch(Exception ex)
        {
            oldTerm=oldVl.getTopologyKind().equals(TopologyKind.BUS_BREAKER)?Builders.BuildTerminalExtBBT(network, oldVl, ((Validable)updatingConnectable.get()), oldBus.getId(), oldBus.getId()):Builders.BuildTerminalExtNBT(network, oldVl, ((Validable)updatingConnectable.get()), oldNode);
            updatingConnectable.get().getTerminals().set(changingTerm.get(), oldTerm);

            showAlert(ex);
        }
    }

    private void setNodeHandlerValidator(String descriptor)
    {
        switch(descriptor)
        {
            case "TERMINAL_FOR_CONNECTABLE":
                nodeHandlers.forEach((String nId, NodeHandler nH) -> {
                    VoltageLevel    vl=network.getVoltageLevel(nH.getVId());
                    switch(vl.getTopologyKind())
                    {
                        case BUS_BREAKER:
                            nH.setConnTypeValidator(ConnectionValidators.BusBreakerTerminalNHValidator);
                            nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);
                            break;
                        case NODE_BREAKER:
                            nH.setConnTypeValidator(ConnectionValidators.NodeBreakerTerminalNHValidator);
                            nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);
                            break;
                        default:
                            nH.setConnTypeValidator(ConnectionValidators.defaultNHValidator);
                            nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);
                    }
                });
                break;
            case "REGULATING_TERMINAL":
                nodeHandlers.forEach((String nId, NodeHandler nH) -> {
//                    nH.setConnTypeValidator(ConnectionValidators.NodeBreakerSwitchTerminalNHValidator);
                    nH.setConnTypeValidator(ConnectionValidators.RegulationTerminalNHValidator);
                    nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);
                });
                break;
            case "TERMINAL_FOR_SWITCH":
                nodeHandlers.forEach((String nId, NodeHandler nH) -> {
                    VoltageLevel    vl=network.getVoltageLevel(nH.getVId());
                    
                    switch(vl.getTopologyKind())
                    {
                        case NODE_BREAKER:
                            nH.setConnTypeValidator(ConnectionValidators.NodeBreakerSwitchTerminalNHValidator);
                            nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);
                            break;
                        default:
                            nH.setConnTypeValidator(ConnectionValidators.defaultNHValidator);
                            nH.setNodeHandlerValidator(NodeHandlerValidators.denyAllNH);
                    }
                });
                break;
            case "VOLTAGE_LEVEL":
                nodeHandlers.forEach((String nId, NodeHandler nH) -> {
                    nH.setConnTypeValidator(ConnectionValidators.VoltageLevelNHValidator);
                    nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);
                });
                break;
            case "*":
                nodeHandlers.forEach((String nId, NodeHandler nH) -> {
                    nH.setConnTypeValidator(ConnectionValidators.defaultNHValidator);
                    nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);
                });
                break;
        }
    }
    
    public void recoverModes(int operation)
    {
        switch(operation)
        {
            case OPERATION_STORE_MODES:
                storedMode.set(VoltageLevelHandler.getMode());
                if(!nodeHandlers.isEmpty())
                {
                    if(VoltageLevelHandler.hasFinalTarget())
                    {
                        storedFinalTarget.set(VoltageLevelHandler.getFinalTarget());
                        storedOriginalSource.set(VoltageLevelHandler.getOriginalSource());
                    }
                    else
                    {
                        storedFinalTarget.set(null);
                        storedOriginalSource.set(null);
                    }
                    storedConnValidator.set(nodeHandlers.get((String)nodeHandlers.keySet().toArray()[0]).getConnTypeValidator());
                    storedNHValidator.set(nodeHandlers.get((String)nodeHandlers.keySet().toArray()[0]).getNodeHandlerValidator());
                }
                break;
            case OPERATION_RESTORE_MODES:
                if(storedMode.isNotNull().get())
                {
                    if(storedFinalTarget.isNotNull().get())
                    {
                        VoltageLevelHandler.setMode(storedMode.get(),storedOriginalSource.get(),(EventTarget)storedFinalTarget.get());
                    }
                    else
                    {
                        VoltageLevelHandler.setMode(storedMode.get());
                    }
                }
                nodeHandlers.forEach((String key, NodeHandler nh)->{if(storedConnValidator.isNotNull().get())   nh.setConnTypeValidator     (storedConnValidator.get());
                                                                    if(storedNHValidator.isNotNull().get())     nh.setNodeHandlerValidator  (storedNHValidator.get());});
                    
            default:;
        }
    }

    private boolean showAlert(Throwable ex)
    {
        Alert   alert=new Alert(Alert.AlertType.ERROR);
        Stage   window  =(Stage)alert.getDialogPane().getScene().getWindow();
        
        alert.getButtonTypes().add(ButtonType.CANCEL);

        alert.getDialogPane().getStylesheets().add(this.propGrid.getParent().getScene().getRoot().getStylesheets().get(0));
        window.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));

        alert.setTitle("Error");
        alert.setHeaderText(null);

        if(ex.getCause()!=null)
            alert.setContentText(ex.getCause().getLocalizedMessage());
        else
            alert.setContentText(ex.getLocalizedMessage());

        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        alert.showAndWait();
        
        return alert.getResult().equals(ButtonType.OK);
    }

    public void selectVLForNewBusbar(ImageCursor cursor)
    {
        if(!managerIsValid.get())
            return;
        VoltageLevelHandler.setMode(VisualHandlerModes.BUSBAR_SECTION_INSERTION);
        setNodeHandlerValidator("VOLTAGE_LEVEL");
        contentPane.getParent().setCursor(cursor);
        
    }
    
//    public void triggerComponentInsert(Cursor cursor, ComponentType type)
    public void triggerComponentInsert(Cursor cursor, String type)
    {
        if(!managerIsValid.get())
            return;
        switch(type)
        {
            case ComponentTypeName.BREAKER:
                insertingSwKind.set(SwitchKind.BREAKER);
                VoltageLevelHandler.setMode(VisualHandlerModes.SWITCH_INSERTION);
                setNodeHandlerValidator("TERMINAL_FOR_SWITCH");
                break;
            case ComponentTypeName.DISCONNECTOR:
                insertingSwKind.set(SwitchKind.DISCONNECTOR);
                VoltageLevelHandler.setMode(VisualHandlerModes.SWITCH_INSERTION);
                setNodeHandlerValidator("TERMINAL_FOR_SWITCH");
                break;
            case ComponentTypeName.LOAD_BREAK_SWITCH:
                insertingSwKind.set(SwitchKind.LOAD_BREAK_SWITCH);
                VoltageLevelHandler.setMode(VisualHandlerModes.SWITCH_INSERTION);
                setNodeHandlerValidator("TERMINAL_FOR_SWITCH");
                break;
            default:
                insertingConnectable.set(ComponentTypeMap.networkTypes.get(type));
                insertingSwKind.set(null);
                VoltageLevelHandler.setMode(VisualHandlerModes.COMPONENT_INSERTION);
                setNodeHandlerValidator("TERMINAL_FOR_CONNECTABLE");
        }
        contentPane.getParent().setCursor(cursor);
        
    }
    
    public void removeSelected()
    {
        StringBuilder                   message =new StringBuilder();
        ObservableList<Node>            copy    =FXCollections.observableArrayList(this.lastSelectedNodes);
        ObservableList<Node>            switches=copy.filtered((Node node)->{
            Identifiable    i   =this.getIdentifiable(node.getId());
            return i instanceof Switch;
        });
        ObservableList<Node>            vls=copy.filtered((Node node)->{
            Identifiable    i   =this.getIdentifiable(node.getId());
            return i instanceof VoltageLevel;
        });
        ObservableList<Node>            conns=copy.filtered((Node node)->{
            Identifiable    i   =this.getIdentifiable(node.getId());
            return i instanceof Connectable;
        });
        
        message.append(removeNodes(conns));
        message.append(removeNodes(switches));
        message.append(removeNodes(vls));
        this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
        
        if(message.length()>0)
        {
            message.insert(0, "Removed nodes:\n");
            this.SelectionManagerTgt.setUserData(message.toString());
            this.SelectionManagerTgt.fireEvent(new ControllerRequests(message.toString(), ControllerRequests.SHOW_MESSAGE));
        }
    }
    
    private StringBuilder removeNodes(List<Node> nodes)
    {
        StringBuilder                   message =new StringBuilder();

        for(Node next:nodes)
        {
            try
            {
                Identifiable    i   =this.getIdentifiable(next.getId());//network.getIdentifiable(id_);
                String          id_ =i!=null?i.getId():null;
                if(i!=null)
                {
                    if(i instanceof Connectable)
                    {
                        ((Connectable)i).remove();
                        lastSelectedNodes.remove(next);
                        message.append("\n").append(((Connectable)i).getId());
                    }
                    else if(i instanceof Switch)
                    {
                        ((Switch)i).getVoltageLevel().getNodeBreakerView().removeSwitch(id_);
                        lastSelectedNodes.remove(next);
                        message.append("\n").append(((Switch)i).getId());
                    }
                    else if(i instanceof VoltageLevel)
                    {
                        ((VoltageLevel)i).remove();
                        lastSelectedNodes.remove(next);
                        message.append("\n").append(((VoltageLevel)i).getId());
                    }
                }
            }
            catch(Exception | java.lang.AssertionError ex)
            {
                showAlert(ex);
            }
        }
        return message;
    }
    
    private void createBus(VoltageLevel targetVL)
    {
        ObservableList<Entry<String, Object>>   itemList    =propGrid.getItems().isEmpty()?FXCollections.observableArrayList():propGrid.getItems();
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        
        if(propGrid.getItems().isEmpty())
        {
           
            itemList.add(new SimpleEntry<>("Bus Id"                     ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Bus Name"                   ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Once finished..."           ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."                  ,CANCEL));

            propGrid.setItems(itemList);
        }
        
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                    this.SelectionManagerTgt.setUserData("Busbar Section insertion cancelled.");
                                    this.SelectionManagerTgt.fireEvent(new ControllerRequests("Busbar Section insertion cancelled.", ControllerRequests.SHOW_MESSAGE));
                                });
        OKB.setOnAction(event2->{
            try
            {
                BusAdder    newBus  =targetVL.getBusBreakerView().newBus();
                String      id      =((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0).getValue().getValue();
                newBus.     setId   (id).
                            setName ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1).getValue().getValue())).
                        add();
                                    itemList.   clear();
                                    propGrid.   setItems(itemList);
                                    this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                                    this.SelectionManagerTgt.setUserData("Busbar added.");
                                    this.SelectionManagerTgt.fireEvent(new ControllerRequests("Busbar added.", ControllerRequests.SHOW_MESSAGE));
            }
            catch(Exception ex)
            {
                if(showAlert(ex))
                    createBus(targetVL);
            }
        });
    }
    
    private void createBusbarSection(VoltageLevel targetVL)
    {
        ObservableList<Entry<String, Object>>   itemList    =propGrid.getItems().isEmpty()?FXCollections.observableArrayList():propGrid.getItems();
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        final List<Integer>                     emptyNodes  =this.emptyNodesFinder(targetVL.getNodeBreakerView());
        int                                     nodeNumber  =emptyNodes.isEmpty()?addNode(targetVL.getNodeBreakerView()):emptyNodes.get(0);//this.nextNodeNumber(targetVL.getNodeBreakerView());
//        int                                     nodeNumber  =emptyNodes.isEmpty()?targetVL.getNodeBreakerView().getNodeCount():emptyNodes.get(0);//this.nextNodeNumber(targetVL.getNodeBreakerView());
        
        
        if(propGrid.getItems().isEmpty())
        {
            SimpleStringProperty    nameProp    =new SimpleStringProperty("");
            itemList.add(new SimpleEntry<>("Section Id"                 ,nameProp));
            itemList.add(new SimpleEntry<>("Section Node Number"        ,new ReadOnlyObjectWrapper(nodeNumber)));
            itemList.add(new SimpleEntry<>("Once finished..."           ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."                  ,CANCEL));

            propGrid.setItems(itemList);
        }
        
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                    this.SelectionManagerTgt.setUserData("Busbar Section insertion cancelled.");
                                    this.SelectionManagerTgt.fireEvent(new ControllerRequests("Busbar Section insertion cancelled.", ControllerRequests.SHOW_MESSAGE));
        });
        OKB.setOnAction(event2->{
            try
            {
                BusbarSectionAdder  newSection  =targetVL.getNodeBreakerView().newBusbarSection();
//                VoltageLevel.NodeBreakerView newView=targetVL.   getNodeBreakerView();
//                if(emptyNodes.isEmpty())
//                    newView = targetVL.   getNodeBreakerView().setNodeCount(targetVL.getNodeBreakerView().getNodeCount()+1);
//                targetVL.   getNodeBreakerView().setNodeCount(nextNodeNumber(targetVL.getNodeBreakerView()));
                
//                if(targetVL.getNodeBreakerView().getNodeCount()<nodeNumber+1 && emptyNodes.isEmpty())
//                    targetVL.getNodeBreakerView().setNodeCount(nodeNumber+1);

                reorderNodes(targetVL.getNodeBreakerView());
                
                if(nodeNumber>targetVL.getNodeBreakerView().getNodeCount()-1)
                    targetVL.getNodeBreakerView().setNodeCount(nodeNumber+1);
                
                String              id          =((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0).getValue().get();
                
                newSection. setId   (id).
//                            setNode ((Integer)itemList.get(1).getValue()).
//                            setNode (emptyNodes.isEmpty()?targetVL.getNodeBreakerView().getNodeCount()-1:emptyNodes.get(0)).
//                            setNode (nextNodeNumber(newView)).
                            setNode (nodeNumber).
                        add();
                itemList.   clear();
                propGrid.   setItems(itemList);
                this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                this.SelectionManagerTgt.setUserData("Busbar Section added.");
                this.SelectionManagerTgt.fireEvent(new ControllerRequests("Busbar Section added.", ControllerRequests.SHOW_MESSAGE));
            }
            catch(Exception ex)
            {
                boolean retry   =showAlert(ex);
                
//                targetVL.getNodeBreakerView().setNodeCount(targetVL.getNodeBreakerView().getNodeCount()-1);
                reorderNodes(targetVL.getNodeBreakerView());
                if(retry)
                    createBusbarSection(targetVL);
            }
        });
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void createSwitch(String targetElemId, String targetVId)
    {
        ObservableList<Entry<String, Object>>   itemList    =propGrid.getItems().isEmpty()?FXCollections.observableArrayList():propGrid.getItems();
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        ChoiceBox<SwitchKind>                   swKindCh    =new ChoiceBox(FXCollections.observableArrayList(SwitchKind.values())); swKindCh.getSelectionModel().select(0);
        VoltageLevel                            targetVL    =network.getVoltageLevel(targetVId);
        Identifiable                            targetIdf   =getIdentifiable(targetElemId);/* if(targetIdf==null) return;*/
        BusbarSection                           targetBusS  =targetIdf!=null?network.getBusbarSection(targetIdf.getId()):null;
        int                                     targetNode  =targetBusS==null?getNodeFromGraph(targetElemId, targetVId):-1;
//        String                                  node1Name   =targetBusS!=null?targetBusS.getName()+" / "+targetBusS.getTerminal().getNodeBreakerView().getNode():"Node "+targetNode;
        String                                  node1Name   =targetVL.getName()+" / "+(targetBusS!=null?targetBusS.getTerminal().getNodeBreakerView().getNode():targetNode);
        Object[]                                nodes       =new Object[targetVL.getNodeBreakerView().getNodes().length + 1];
        final ComboBox<Object>                  cbT2        =new ComboBox<>();
        
        if(!insertingSwKind.isNull().get())
            swKindCh.getSelectionModel().select(insertingSwKind.get());
        
        for(int index=0;index<targetVL.getNodeBreakerView().getNodes().length;index++){nodes[index]=targetVL.getNodeBreakerView().getNodes()[index];}
        nodes[nodes.length-1]   ="Select from graph";
        cbT2.getItems().addAll(FXCollections.observableArrayList(nodes));
        cbT2.setEditable(true);
        cbT2.selectionModelProperty().get().selectedItemProperty().addListener((observable, oldValue, newValue)->{
            
            if(newValue.toString().equals("Select from graph"))
            {
                VoltageLevelHandler.setMode(VisualHandlerModes.SWITCH_INSERTION);
                OKB.disableProperty().setValue(Boolean.TRUE);
                nodeHandlers.forEach((String nId, NodeHandler nH) -> {nH.setNodeHandlerValidator(NodeHandlerValidators.allowOnlyVL(targetVId));});
            }
            else
            {
                VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                contentPane.getParent().setCursor(Cursor.DEFAULT);
                OKB.disableProperty().setValue(Boolean.FALSE);
                nodeHandlers.forEach((String nId, NodeHandler nH) -> {nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);});
            }
            
        });
        
        if(propGrid.getItems().isEmpty())//First node assigned, start parameters edition...
        {
            int node1   =targetBusS!=null?targetBusS.getTerminal().getNodeBreakerView().getNode():targetNode;
            if(node1<0)
            {
                showAlert(new Exception("Invalid node"));
                return;
            }
            
            itemList.add(new SimpleEntry<>("Switch Id"                  ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Switch Name"                ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Switch Kind"                ,swKindCh));
            itemList.add(new SimpleEntry<>("Node1"                      ,new ReadOnlyObjectWrapper(node1Name)));
            itemList.add(new SimpleEntry<>("Node2"                      ,cbT2));//new SimpleIntegerProperty(0)));
            itemList.add(new SimpleEntry<>("Fictitious"                 ,new SimpleBooleanProperty(false)));
            itemList.add(new SimpleEntry<>("Open"                       ,new SimpleBooleanProperty(false)));
            itemList.add(new SimpleEntry<>("Retained"                   ,new SimpleBooleanProperty(false)));
            itemList.add(new SimpleEntry<>("Once finished..."           ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."                  ,CANCEL));

            propGrid.setItems(itemList);
        }
        else if(itemList.get(0).getKey().equals("Switch Id"))//Seccond node assigned from graph.  Update node and return to continue editing parameters
        {
            int node2   =targetBusS!=null?targetBusS.getTerminal().getNodeBreakerView().getNode():targetNode;
            
            if(node2<0)
            {
                showAlert(new Exception("Invalid node"));
                ((ComboBox)itemList.get(4).getValue()).setValue("");
            }
            else
                ((ComboBox)itemList.get(4).getValue()).setValue(node2);
            ((Button)itemList.get(8).getValue()).disableProperty().setValue(Boolean.FALSE);
            return;
        }
        else //something went wrong.  Grid is initialized, but pararmeters does'nt match switch's ones.  Perhaps some other function or event did it between node1 and 2 assignments.
             //do nothing and return.
            return;
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                    VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                                    contentPane.getParent().setCursor(Cursor.DEFAULT);
                                    nodeHandlers.forEach((String nId, NodeHandler nH) -> {nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);});
                                    this.SelectionManagerTgt.setUserData("Switch insertion cancelled.");
                                    this.SelectionManagerTgt.fireEvent(new ControllerRequests("Switch insertion cancelled.", ControllerRequests.SHOW_MESSAGE));
                                });
        OKB.setOnAction(event2->{
            try
            {
                Integer     node2           =Integer.valueOf(cbT2.getValue().toString());
                
                reorderNodes(targetVL.getNodeBreakerView());
                if(node2>targetVL.getNodeBreakerView().getNodeCount()-1)
                    targetVL.getNodeBreakerView().setNodeCount(node2+1);
                
                SwitchAdder newSwitch       =targetVL.getNodeBreakerView().newSwitch();
                newSwitch.  setId           ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0).getValue().getValue())).
                            setName         ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1).getValue().getValue())).
                            setKind         (swKindCh.getSelectionModel().getSelectedItem()).
                            setNode1        (targetBusS!=null?targetBusS.getTerminal().getNodeBreakerView().getNode():targetNode).
                            setNode2        (node2).
                            setFictitious   ((((TableColumn<Entry<String,String>,SimpleBooleanProperty>)propGrid.getColumns().get(1)).getCellObservableValue(5).getValue().getValue())).
                            setOpen         ((((TableColumn<Entry<String,String>,SimpleBooleanProperty>)propGrid.getColumns().get(1)).getCellObservableValue(6).getValue().getValue())).
                            setRetained     ((((TableColumn<Entry<String,String>,SimpleBooleanProperty>)propGrid.getColumns().get(1)).getCellObservableValue(7).getValue().getValue())).
                        add();
                itemList    .clear();
                propGrid    .setItems(itemList);
                this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                this.SelectionManagerTgt.setUserData("Switch added.");
                this.SelectionManagerTgt.fireEvent(new ControllerRequests("Switch added.", ControllerRequests.SHOW_MESSAGE));
            }
            catch(Exception ex)
            {
                boolean retry   =showAlert(ex);
                
//                targetVL.getNodeBreakerView().setNodeCount(targetVL.getNodeBreakerView().getNodeCount()-1);
                reorderNodes(targetVL.getNodeBreakerView());
                if(retry)
                    createBusbarSection(targetVL);
            }
        });

    }
    
    private void createConnectable(ConnectionEvent event)
    {
        NodeHandler                             nh          =event.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)event.getOriginalSource()         :event.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)event.getOriginalSource()).getId()):null;
        VoltageLevelHandler                     vlh         =event.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)event.getOriginalSource() :event.getOriginalSource() instanceof Node?vlHandlers .get(((Node)event.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
        String                                  targetVLId  =vlh        !=null?vlh.getVId()                         :nh!=null?nh.getVId():null;
        VoltageLevel                            targetVL    =targetVLId !=null?network.getVoltageLevel(targetVLId)  :null;
        String                                  targetElemId=nh!=null?nh.getId():targetVLId;
        
        if(targetVL==null)
            return;
        
        Class<? extends Connectable>            clazz       =this.insertingConnectable.get();
        String                                  preffix     =this.insertingConnectable.get().getSimpleName();
        ObservableList<Entry<String, Object>>   itemList    =propGrid.getItems().isEmpty()?FXCollections.observableArrayList():propGrid.getItems();
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        Bus                                     targetBus   =targetVL.getTopologyKind()==TopologyKind.BUS_BREAKER   ?network.getBusBreakerView().getBus(targetElemId)   :null;
        BusbarSection                           targetBusS  =targetVL.getTopologyKind()==TopologyKind.NODE_BREAKER  ?network.getBusbarSection(targetElemId)             :null;
        int                                     targetNode  =targetBusS==null?getNodeFromGraph(targetElemId, targetVLId):-1;
        String                                  node1Name   =targetBus!=null?targetBus.getName():targetBusS!=null?targetBusS.getName():targetVL.getName()+" / "+targetNode;
        final Button                            bT2         =new Button("Select Term. 2");
        final Button                            bT3         =new Button("Select Term. 3");
        final EventHandler<ActionEvent>         term2ConnEvH;//=(ActionEvent actEvent) -> {VoltageLevelHandler.setMode(VisualHandlerModes.COMPONENT_INSERTION, nh, bT2);};
        final EventHandler<ActionEvent>         term3ConnEvH;//=(ActionEvent actEvent) -> {VoltageLevelHandler.setMode(VisualHandlerModes.COMPONENT_INSERTION, nh, bT3);};
        final Substation                        substation1 =targetVL.getSubstation();
        final int                               offset;//   =0;
//        String                                  id;
        
        if(clazz==null)
            return;
        term2ConnEvH=(ActionEvent actEvent) -> {
                                                    VoltageLevelHandler.setMode(VisualHandlerModes.COMPONENT_INSERTION, nh, bT2);
                                                    setNodeHandlerValidator("TERMINAL_FOR_CONNECTABLE");
                                                    Group               cursorImg   =(new SvgLoader()).loadSvg(this.getClass().getResourceAsStream("/img/PointToNode.svg"));
                                                    SnapshotParameters  snapParams  =new SnapshotParameters();
                                                    snapParams.setFill(Paint.valueOf("#ffffff00"));
                                                    contentPane.getParent().setCursor(new ImageCursor(cursorImg.snapshot(snapParams, null), 3, 3));
                                                };
        
        term3ConnEvH=(ActionEvent actEvent) -> {
                                                    VoltageLevelHandler.setMode(VisualHandlerModes.COMPONENT_INSERTION, nh, bT3);
                                                    setNodeHandlerValidator("TERMINAL_FOR_CONNECTABLE");
                                                    Group   cursorImg   =(new SvgLoader()).loadSvg(this.getClass().getResourceAsStream("/img/PointToNode.svg"));
                                                    SnapshotParameters  snapParams  =new SnapshotParameters();
                                                    snapParams.setFill(Paint.valueOf("#ffffff00"));
                                                    contentPane.getParent().setCursor(new ImageCursor(cursorImg.snapshot(snapParams, null), 3, 3));
                                                };
        
        
        if(propGrid.getItems().isEmpty())
        {
            bT2.setOnAction(term2ConnEvH);
            bT3.setOnAction(term3ConnEvH);


            bT2.addEventHandler(ConnectionEvent.INSERT_COMPONENT, event2->
            //<editor-fold desc="Terminal 2 Button terminal assingment">
            {
                NodeHandler         nh2         =event2.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)event2.getOriginalSource()         :event2.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)event2.getOriginalSource()).getId()):null;
                VoltageLevelHandler vlh2        =event2.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)event2.getOriginalSource() :event2.getOriginalSource() instanceof Node?vlHandlers .get(((Node)event2.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
                String              targetVLId2 =vlh2       !=null?vlh2.getVId()                        :nh2!=null?nh2.getVId():null;
                VoltageLevel        targetVL2   =targetVLId2!=null?network.getVoltageLevel(targetVLId2) :null; if(targetVL2==null) return;
                String              targetId2   =nh2!=null?nh2.getId():null; if(targetId2==null) return;
                Bus                 targetBus2  =targetVL2.getTopologyKind()==TopologyKind.BUS_BREAKER   ?network.getBusBreakerView().getBus(targetId2)   :null;
                Identifiable        identif2    =getIdentifiable(targetId2);
//                BusbarSection       targetBusS2 =targetVL2.getTopologyKind()==TopologyKind.NODE_BREAKER  ?network.getBusbarSection(targetId2)             :null;
                BusbarSection       targetBusS2 =identif2 instanceof BusbarSection   ?network.getBusbarSection(identif2.getId())    :null;
                int                 targetNode2 =targetBusS2==null                  ?getNodeFromGraph(/*targetElemId*/targetId2, /*targetVId*/targetVLId2):-1;
                String              node2Name   =targetBus2!=null                   ?targetBus2.getName():targetBusS2!=null?targetBusS2.getName():targetVL2.getName()+" / "+targetNode2;

                VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                contentPane.getParent().setCursor(Cursor.DEFAULT);
                bT2.setUserData(targetBus2  !=null?targetBus2           :targetBusS2!=null?targetBusS2          :new Pair<>(targetVL2, targetNode2));
                bT2.setText(targetBus2      !=null?targetBus2.getName() :targetBusS2!=null?targetBusS2.getName():targetVL2.getName()+" / "+targetNode2);
                event2.consume();
            });
            //</editor-fold>

            bT3.addEventHandler(ConnectionEvent.INSERT_COMPONENT, event3->
            //<editor-fold desc="Terminal 3 Button terminal assingment">
            {
                NodeHandler         nh3         =event3.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)event3.getOriginalSource()         :event3.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)event3.getOriginalSource()).getId()):null;
                VoltageLevelHandler vlh3        =event3.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)event3.getOriginalSource() :event3.getOriginalSource() instanceof Node?vlHandlers .get(((Node)event3.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
                String              targetVLId3 =vlh3       !=null?vlh3.getVId()                        :nh3!=null?nh3.getVId():null;
                VoltageLevel        targetVL3   =targetVLId3!=null?network.getVoltageLevel(targetVLId3) :null; if(targetVL3==null) return;
                String              targetId3   =nh3!=null?nh3.getId():null; if(targetId3==null) return;
                Bus                 targetBus3  =targetVL3.getTopologyKind()==TopologyKind.BUS_BREAKER   ?network.getBusBreakerView().getBus(targetId3)   :null;
                Identifiable        identif3    =getIdentifiable(targetId3);
//                BusbarSection       targetBusS3 =targetVL3.getTopologyKind()==TopologyKind.NODE_BREAKER  ?network.getBusbarSection(targetId3)             :null;
                BusbarSection       targetBusS3 =identif3 instanceof BusbarSection   ?network.getBusbarSection(identif3.getId())    :null;
                int                 targetNode3 =targetBusS3==null?getNodeFromGraph(/*targetElemId*/targetId3, /*targetVId*/targetVLId3):-1;
                String              node3Name   =targetBus3!=null?targetBus3.getName():targetBusS3!=null?targetBusS3.getName():targetVL3.getName()+" / "+targetNode3;

                VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                contentPane.getParent().setCursor(Cursor.DEFAULT);
                bT3.setUserData(targetBus3  !=null?targetBus3           :targetBusS3!=null?targetBusS3          :new Pair<>(targetVL3, targetNode3));
                bT3.setText(targetBus3      !=null?targetBus3.getName() :targetBusS3!=null?targetBusS3.getName():targetVL3.getName()+" / "+targetNode3);
                event3.consume();
            });
            //</editor-fold>

            itemList.add(new SimpleEntry<>(preffix+" Id"    ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>(preffix+" Name"  ,new SimpleStringProperty("")));
            if(Injection.class.isAssignableFrom(clazz))//1 terminal
            {
                itemList.add(new SimpleEntry<>("Terminal"   ,new ReadOnlyObjectWrapper(node1Name)));
            }
            else if(Branch.class.isAssignableFrom(clazz))//2 terminals
            {
                itemList.add(new SimpleEntry<>("Terminal 1" ,new ReadOnlyObjectWrapper(node1Name)));
                itemList.add(new SimpleEntry<>("Terminal 2" ,bT2));

            }
            else if(ThreeWindingsTransformer.class.isAssignableFrom(clazz))//3 terminals
            {
                itemList.add(new SimpleEntry<>("Terminal 1" ,new ReadOnlyObjectWrapper(node1Name)));
                itemList.add(new SimpleEntry<>("Terminal 2" ,bT2));
                itemList.add(new SimpleEntry<>("Terminal 3" ,bT3));
            }
            offset  =itemList.size();
            
            switch(clazz.getName())
            {
//                case "com.powsybl.iidm.network.BusbarSection":
//                    break;
                case "com.powsybl.iidm.network.DanglingLine":
                    itemList.add(new SimpleEntry<>("R"      ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("X"      ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("B"      ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("G"      ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("P0"     ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Q0"     ,new SimpleDoubleProperty(0.0)));

                    OKB.setOnAction(actEvt->{
                        DanglingLineAdder   adder=  targetVL.newDanglingLine()  .setId  ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                                                                .setName((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                                                                .setR   ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset  ).getValue().getValue()))
                                                                                .setX   ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                                                                .setB   ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()))
                                                                                .setG   ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+3).getValue().getValue()))
                                                                                .setP0  ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+4).getValue().getValue()))
                                                                                .setQ0  ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+5).getValue().getValue()));
                        if(targetBus!=null)
                            adder.setBus(targetBus.getId());
                        else if(targetBusS!=null)
                            adder.setNode(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            adder.setNode(targetNode);
                        
                        try{
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane.getParent().setCursor(Cursor.DEFAULT);
//                            contentPane .fireEvent(ControllerRequests.REDRAW_REQUEST);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            event.consume();
                            this.SelectionManagerTgt.setUserData("Dangling Line added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("Dangling Line added.", ControllerRequests.SHOW_MESSAGE));
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    break;
                case "com.powsybl.iidm.network.Generator":
                    ChoiceBox<EnergySource>         srcCh       =new ChoiceBox(); srcCh.getItems().addAll((EnergySource[])EnergySource.values()); srcCh.getSelectionModel().select(0);
                    Button                          bRT1        =new Button("Select Reg. Term.");
                    final EventHandler<ActionEvent> rt1ConnEvH  =(ActionEvent actEvent) -> {
                                                                                                setNodeHandlerValidator("REGULATING_TERMINAL");
                                                                                                VoltageLevelHandler.setMode(VisualHandlerModes.REGULATING_TERM_SEARCH, nh, bRT1);
                                                                                                Group   cursorImg   =(new SvgLoader()).loadSvg(this.getClass().getResourceAsStream("/img/PointToNode.svg"));
                                                                                                SnapshotParameters  snapParams  =new SnapshotParameters();
                                                                                                snapParams.setFill(Paint.valueOf("#ffffff00"));
                                                                                                contentPane.getParent().setCursor(new ImageCursor(cursorImg.snapshot(snapParams, null), 3, 3));
                                                                                            };
                    
                    bRT1.setOnAction(rt1ConnEvH);
                    bRT1.addEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, eventRT1->
                    //<editor-fold desc="Regulting Terminal Button terminal assingment">
                    {
                        NodeHandler         nh2         =eventRT1.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)eventRT1.getOriginalSource()         :eventRT1.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)eventRT1.getOriginalSource()).getId()):null;
                        VoltageLevelHandler vlh2        =eventRT1.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)eventRT1.getOriginalSource() :eventRT1.getOriginalSource() instanceof Node?vlHandlers .get(((Node)eventRT1.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
                        String              targetVLId2 =vlh2       !=null?vlh2.getVId()                        :nh2!=null?nh2.getVId():null;
                        VoltageLevel        targetVL2   =targetVLId2!=null?network.getVoltageLevel(targetVLId2) :null; if(targetVL2==null) return;
                        String              targetId2   =nh2!=null?nh2.getId():null; if(targetId2==null) return;
                        Bus                 targetBus2  =targetVL2.getTopologyKind()==TopologyKind.BUS_BREAKER   ?network.getBusBreakerView().getBus(targetId2)   :null;
                        BusbarSection       targetBusS2 =targetVL2.getTopologyKind()==TopologyKind.NODE_BREAKER  ?network.getBusbarSection(targetId2)             :null;
                        int                 targetNode2 =targetBusS2==null?getNodeFromGraph(/*targetElemId*/targetId2, /*targetVId*/targetVLId2):-1;
                        String              node2Name   =targetBus2!=null?targetBus2.getName():targetBusS2!=null?targetBusS2.getName():targetVL2.getName()+" / "+targetNode2;
                        Terminal            regT        =targetBusS2!=null?targetBusS2.getTerminal():targetNode2>=0?targetVL2.getNodeBreakerView().getTerminal(targetNode2):null;

                        VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                        contentPane.getParent().setCursor(Cursor.DEFAULT);
                        bRT1.setUserData(regT);
                        if(regT!=null)
                            bRT1.setText    (regT.getBusBreakerView().getConnectableBus().getName()+" / "+regT.getNodeBreakerView().getNode());
                        else
                            bRT1.setText    (targetBus2  !=null?targetBus2.getName() :targetBusS2!=null?targetBusS2.getName():targetVL2.getName()+" / "+targetNode2);
                        eventRT1.consume();
                    });
                    //</editor-fold>
                    
                    itemList.add(new SimpleEntry<>("EnergySource"       ,srcCh));
                    itemList.add(new SimpleEntry<>("Rated S"            ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Max P"              ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Min P"              ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Target P"           ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Target Q"           ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Target V"           ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Volt Regulated"     ,new SimpleBooleanProperty(false)));
                    itemList.add(new SimpleEntry<>("Regulating Terminal",bRT1));
                    
                    OKB.setOnAction(actEvt->{
                        GeneratorAdder  adder   =targetVL.newGenerator();
                        
                        adder   .setId                  ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName                ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                .setEnergySource        (srcCh. getValue())
                                .setRatedS              ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(4       ).getValue().getValue()))
                                .setMaxP                ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(5       ).getValue().getValue()))
                                .setMinP                ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(6       ).getValue().getValue()))
                                .setTargetP             ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(7       ).getValue().getValue()))
                                .setTargetQ             ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(8       ).getValue().getValue()))
                                .setTargetV             ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(9       ).getValue().getValue()))
                                .setVoltageRegulatorOn  ((((TableColumn<Entry<String,String>,SimpleBooleanProperty>)propGrid.getColumns().get(1)).getCellObservableValue(10      ).getValue().getValue()));
                        if(bRT1.getUserData()!=null)
                            adder.setRegulatingTerminal((Terminal) bRT1.getUserData());
                        
                        if(targetBus!=null)
                        {
                            adder.setBus(targetBus.getId());
                        }
                        else if(targetBusS!=null)
                        {
                            adder.setNode(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        }
                        else
                        {
                            adder.setNode(targetNode);
                        }
                        
                        try{
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane .setCursor(Cursor.DEFAULT);
//                            contentPane .fireEvent(ControllerRequests.REDRAW_REQUEST);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            event.consume();
                            this.SelectionManagerTgt.setUserData("Generator added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("Generator added.", ControllerRequests.SHOW_MESSAGE));
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    break;
                case "com.powsybl.iidm.network.VscConverterStation":
                case "com.powsybl.iidm.network.LccConverterStation":
                case "com.powsybl.iidm.network.HvdcConverterStation":
                    HvdcType                initial =insertingConnectable.get()==LccConverterStation.class?HvdcType.LCC:HvdcType.VSC;
                    ChoiceBox<HvdcType>     typeCh2 =new ChoiceBox(); typeCh2.getItems().addAll((HvdcType[])HvdcType.values()); typeCh2.getSelectionModel().select(initial);
                    
                    typeCh2.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                                                                                                                            insertingConnectable.set(newValue==HvdcType.VSC?VscConverterStation.class:LccConverterStation.class);
                                                                                                                            propGrid.getItems().clear();
                                                                                                                            createConnectable(event);
                                                                                                                        });
                    
                    itemList.add(new SimpleEntry<>("HVDC Type"                  ,typeCh2));
                    if(typeCh2.getValue()==HvdcType.VSC)
                    {
                        itemList.add(new SimpleEntry<>("Regulator On"       ,new SimpleBooleanProperty(false)));
                        itemList.add(new SimpleEntry<>("Loss Factor"        ,new SimpleFloatProperty(0)));
                        itemList.add(new SimpleEntry<>("Q Setpoint"         ,new SimpleDoubleProperty(0)));
                        itemList.add(new SimpleEntry<>("V Setpoint"         ,new SimpleDoubleProperty(0)));
                    }
                    else
                    {
                        itemList.add(new SimpleEntry<>("Loss Factor"        ,new SimpleFloatProperty(0)));
                        itemList.add(new SimpleEntry<>("Power Factor"       ,new SimpleFloatProperty(0)));
                    }
                    
                    OKB.setOnAction(actEvt->{
                        HvdcConverterStationAdder   adder   =typeCh2.getValue()==HvdcType.VSC?targetVL.newVscConverterStation():targetVL.newLccConverterStation();
                        
                        adder   .setId  ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                ;
                        if(typeCh2.getValue()==HvdcType.VSC)
                        {
                            ((VscConverterStationAdder)adder)   .setVoltageRegulatorOn      ((((TableColumn<Entry<String,String>,SimpleBooleanProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                                                .setLossFactor              ((((TableColumn<Entry<String,String>,SimpleFloatProperty>)  propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()))
                                                                .setReactivePowerSetpoint   ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(offset+3).getValue().getValue()))
                                                                .setVoltageSetpoint         ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>) propGrid.getColumns().get(1)).getCellObservableValue(offset+4).getValue().getValue()))
                                                                ;
                        }
                        else
                        {
                            ((LccConverterStationAdder)adder)   .setLossFactor              ((((TableColumn<Entry<String,String>,SimpleFloatProperty>)  propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                                                .setPowerFactor             ((((TableColumn<Entry<String,String>,SimpleFloatProperty>)  propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()));
                        }
                        
                        if(targetBus!=null)
                            adder.setBus(targetBus.getId());
                        else if(targetBusS!=null)
                            adder.setNode(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            adder.setNode(targetNode);
                        
                        try
                        {
                            if(typeCh2.getValue()==HvdcType.VSC)
                                ((VscConverterStationAdder)adder).add();
                            else
                                ((LccConverterStationAdder)adder).add();
                            
                            VoltageLevelHandler .setMode(VisualHandlerModes.NORMAL);
                            contentPane         .setCursor(Cursor.DEFAULT);
//                            contentPane         .fireEvent(ControllerRequests.REDRAW_REQUEST);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            event.consume();
                            this.SelectionManagerTgt.setUserData("HVDC added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("HVDC added.", ControllerRequests.SHOW_MESSAGE));
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    break;
                case "com.powsybl.iidm.network.TieLine":
                case "com.powsybl.iidm.network.Line":
                    SimpleBooleanProperty   tieLine =new SimpleBooleanProperty(insertingConnectable.get()==TieLine.class);
                    itemList.add(new SimpleEntry<>("B1"         ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("G1"         ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("R"          ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("X"          ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("B2"         ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("G2"         ,new SimpleDoubleProperty(0.0)));
//                    itemList.add(new SimpleEntry<>("Is TieLine" ,tieLine));
//                    
//                    tieLine.addListener((observable, oldValue, newValue) -> {
//                                                                                insertingConnectable.set(tieLine.get()?TieLine.class:Line.class);
//                                                                                propGrid.getItems().clear();
//                                                                                createConnectable(event);
//                                                                            });
                    
                    OKB.setOnAction(actEvt->{
                        Pair<VoltageLevel, Integer> pair2   =bT2.getUserData() instanceof Pair?(Pair<VoltageLevel, Integer>)bT2.getUserData():null;
                        BusbarSection               bbs2    =bT2.getUserData() instanceof BusbarSection?(BusbarSection) bT2.getUserData():null;
                        Bus                         bus2    =bT2.getUserData() instanceof Bus?(Bus) bT2.getUserData():null;
                        VoltageLevel                vl2     =pair2!=null?pair2.getKey():
                                                             bbs2!=null?bbs2.getTerminal().getVoltageLevel():
                                                             bus2!=null?bus2.getVoltageLevel():null;
                        LineAdder   adder   =network.newLine();
//                        TieLineAdder    adder2;
//                        
//                        adder2.
                        
                        adder   .setId      ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName    ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                .setB1      ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset  ).getValue().getValue()))
                                .setG1      ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                .setR       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()))
                                .setX       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+3).getValue().getValue()))
                                .setB2      ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+4).getValue().getValue()))
                                .setG2      ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+5).getValue().getValue()))
                                ;
                        
                        adder.setVoltageLevel1(targetVLId);
                        if(targetBus!=null)
                            adder.setBus1(targetBus.getId());
                        else if(targetBusS!=null)
                            adder.setNode1(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            adder.setNode1(targetNode);
                        if(vl2!=null)
                            adder.setVoltageLevel2(vl2.getId());
                        if(pair2!=null)
                            adder.setNode2(pair2.getValue());
                        else if(bbs2!=null)
                            adder.setNode2(bbs2.getTerminal().getNodeBreakerView().getNode());
                        else if(bus2!=null)
                            adder.setBus2(bus2.getId());
                        
                        try{
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane.getParent().setCursor(Cursor.DEFAULT);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            event.consume();
                            this.SelectionManagerTgt.setUserData("Line added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("Line added.", ControllerRequests.SHOW_MESSAGE));
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    break;
                case "com.powsybl.iidm.network.Load":
                    ChoiceBox<LoadType> typeCh  =new ChoiceBox(); typeCh.getItems().addAll((LoadType[]) LoadType.values()); typeCh.getSelectionModel().select(0);

                    itemList.add(new SimpleEntry<>("Rated S"            ,typeCh));
                    itemList.add(new SimpleEntry<>("P0"                 ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Q0"                 ,new SimpleDoubleProperty(0.0)));
                    
                    OKB.setOnAction(actEvt->{
                        LoadAdder   adder   =targetVL.newLoad();
                        
                        adder   .setId  ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                .setLoadType(typeCh.getValue())
                                .setP0  ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                .setQ0  ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()));
                        
                        if(targetBus!=null)
                            adder.setBus(targetBus.getId());
                        else if(targetBusS!=null)
                            adder.setNode(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            adder.setNode(targetNode);
                        
                        try{
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane.getParent().setCursor(Cursor.DEFAULT);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            event.consume();
                            this.SelectionManagerTgt.setUserData("Load added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("Load added.", ControllerRequests.SHOW_MESSAGE));
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    
                    break;
                case "com.powsybl.iidm.network.ShuntCompensator":
                    SimpleDoubleProperty    perSectB    =new SimpleDoubleProperty(0.0);
                    SimpleIntegerProperty   totSect     =new SimpleIntegerProperty(1);
                    SimpleIntegerProperty   curSect     =new SimpleIntegerProperty(0);
                    itemList.add(new SimpleEntry<>("Per-section B"              ,perSectB));
                    itemList.add(new SimpleEntry<>("Total Sections"             ,totSect));
                    itemList.add(new SimpleEntry<>("Current Sections"           ,curSect));
                    
                    OKB.setOnAction(actEvt->{
                        ShuntCompensatorAdder   adder   =targetVL.newShuntCompensator();
                        
                        adder   .setId                  ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName                ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                .setbPerSection         ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset  ).getValue().getValue()))
                                .setMaximumSectionCount ((((TableColumn<Entry<String,String>,SimpleIntegerProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                .setCurrentSectionCount ((((TableColumn<Entry<String,String>,SimpleIntegerProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()));
                        
                        if(targetBus!=null)
                            adder.setBus(targetBus.getId());
                        else if(targetBusS!=null)
                            adder.setNode(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            adder.setNode(targetNode);
                        
                        try{
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane .setCursor(Cursor.DEFAULT);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            event.consume();
                            this.SelectionManagerTgt.setUserData("Shunt Compensator added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("Shunt Compensator added.", ControllerRequests.SHOW_MESSAGE));
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    
                    break;
                case "com.powsybl.iidm.network.StaticVarCompensator":
                    ChoiceBox<StaticVarCompensator.RegulationMode>  regModCh    =new ChoiceBox(); regModCh.getItems().addAll((StaticVarCompensator.RegulationMode[])StaticVarCompensator.RegulationMode.values()); regModCh.getSelectionModel().select(0);
                    
                    itemList.add(new SimpleEntry<>("Max B"                      ,new SimpleDoubleProperty(0)));
                    itemList.add(new SimpleEntry<>("Min B"                      ,new SimpleDoubleProperty(0)));
                    itemList.add(new SimpleEntry<>("Setpoint Q"                 ,new SimpleDoubleProperty(0)));
                    itemList.add(new SimpleEntry<>("Setpoint V"                 ,new SimpleDoubleProperty(0)));
                    itemList.add(new SimpleEntry<>("Regultion Mode"             ,regModCh));
                    
                    OKB.setOnAction(actEvt->{
                        StaticVarCompensatorAdder   adder   =targetVL.newStaticVarCompensator();
                        
                        adder   .setId                      ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName                    ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                .setBmax                    ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset  ).getValue().getValue()))
                                .setBmin                    ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                .setReactivePowerSetPoint   ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()))
                                .setVoltageSetPoint         ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+3).getValue().getValue()))
                                .setRegulationMode          (regModCh.getValue());
                        
                        if(targetBus!=null)
                            adder.setBus(targetBus.getId());
                        else if(targetBusS!=null)
                            adder.setNode(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            adder.setNode(targetNode);
                        
                        try{
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane .setCursor(Cursor.DEFAULT);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            this.SelectionManagerTgt.setUserData("SVC added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("SVC added.", ControllerRequests.SHOW_MESSAGE));
                            event.consume();
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    
                    break;
                case "com.powsybl.iidm.network.TwoWindingsTransformer":
                    itemList.add(new SimpleEntry<>("B"          ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("G"          ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("R"          ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("X"          ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Rated U1"   ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Rated U2"   ,new SimpleDoubleProperty(0.0)));
                    
                    OKB.setOnAction(actEvt->{
                        Pair<VoltageLevel, Integer> pair2       =bT2.getUserData() instanceof Pair?(Pair<VoltageLevel, Integer>)bT2.getUserData():null;
                        BusbarSection               bbs2        =bT2.getUserData() instanceof BusbarSection?(BusbarSection) bT2.getUserData():null;
                        Bus                         bus2        =bT2.getUserData() instanceof Bus?(Bus) bT2.getUserData():null;
                        VoltageLevel                vl2         =pair2!=null?pair2.getKey():
                                                                 bbs2!=null?bbs2.getTerminal().getVoltageLevel():
                                                                 bus2!=null?bus2.getVoltageLevel():null;
                        Substation                  substation2 =vl2!=null?vl2.getSubstation():null;
                        
                        TwoWindingsTransformerAdder adder   =substation1.newTwoWindingsTransformer();
                        adder   .setId      ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName    ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()))
                                .setB       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset  ).getValue().getValue()))
                                .setG       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                .setR       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()))
                                .setX       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+3).getValue().getValue()))
                                .setRatedU1 ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+4).getValue().getValue()))
                                .setRatedU2 ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+5).getValue().getValue()))
                                ;
                        
//                        RatioTapChangerAdder RTAdder=adder.n;
                        adder.setVoltageLevel1(targetVLId);
                        if(targetBus!=null)
                            adder.setBus1(targetBus.getId());
                        else if(targetBusS!=null)
                            adder.setNode1(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            adder.setNode1(targetNode);
                        if(vl2!=null)
                            adder.setVoltageLevel2(vl2.getId());
                        if(pair2!=null)
                            adder.setNode2(pair2.getValue());
                        else if(bbs2!=null)
                            adder.setNode2(bbs2.getTerminal().getNodeBreakerView().getNode());
                        else if(bus2!=null)
                            adder.setBus2(bus2.getId());
                        
                        try{
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane .setCursor(Cursor.DEFAULT);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            this.SelectionManagerTgt.setUserData("2W-Transformer added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("2W-Transformer added.", ControllerRequests.SHOW_MESSAGE));
                            event.consume();
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    break;
                case "com.powsybl.iidm.network.ThreeWindingsTransformer":
                    itemList.add(new SimpleEntry<>("Primary B"  ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Primary G"  ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Primary R"  ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Primary X"  ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Rated U1"   ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Secondary R",new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Secondary X",new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Rated U2"   ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Tertiary R" ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Tertiary X" ,new SimpleDoubleProperty(0.0)));
                    itemList.add(new SimpleEntry<>("Rated U3"   ,new SimpleDoubleProperty(0.0)));
                    
                    OKB.setOnAction(actEvt->{
                        Pair<VoltageLevel, Integer> pair2       =bT2.getUserData() instanceof Pair?(Pair<VoltageLevel, Integer>)bT2.getUserData():null;
                        BusbarSection               bbs2        =bT2.getUserData() instanceof BusbarSection?(BusbarSection) bT2.getUserData():null;
                        Bus                         bus2        =bT2.getUserData() instanceof Bus?(Bus) bT2.getUserData():null;
                        VoltageLevel                vl2         =pair2!=null?pair2.getKey():
                                                                 bbs2!=null?bbs2.getTerminal().getVoltageLevel():
                                                                 bus2!=null?bus2.getVoltageLevel():null;
                        Substation                  substation2 =vl2!=null?vl2.getSubstation():null;
                        
                        Pair<VoltageLevel, Integer> pair3       =bT3.getUserData() instanceof Pair?(Pair<VoltageLevel, Integer>)bT3.getUserData():null;
                        BusbarSection               bbs3        =bT3.getUserData() instanceof BusbarSection?(BusbarSection) bT3.getUserData():null;
                        Bus                         bus3        =bT3.getUserData() instanceof Bus?(Bus) bT3.getUserData():null;
                        VoltageLevel                vl3         =pair3!=null?pair3.getKey():
                                                                 bbs3!=null?bbs3.getTerminal().getVoltageLevel():
                                                                 bus3!=null?bus3.getVoltageLevel():null;
                        Substation                  substation3 =vl3!=null?vl3.getSubstation():null;
                        
                        ThreeWindingsTransformerAdder   adder   =substation1.newThreeWindingsTransformer();
                        adder   .setId      ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0       ).getValue().getValue()))
                                .setName    ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1       ).getValue().getValue()));
                        Leg1Adder   l1Adder =adder.newLeg1();
                                l1Adder
                                .setB((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset  ).getValue().getValue()))
                                .setG       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+1).getValue().getValue()))
                                .setR       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+2).getValue().getValue()))
                                .setX       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+3).getValue().getValue()))
                                .setRatedU  ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+4).getValue().getValue()))
                                .setVoltageLevel(targetVLId);
                        Leg2or3Adder    l2Adder =adder.newLeg2();
                                l2Adder
                                .setR       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+5).getValue().getValue()))
                                .setX       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+6).getValue().getValue()))
                                .setRatedU  ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+7).getValue().getValue()))
                                .setVoltageLevel(vl2!=null?vl2.getId():targetVLId);
                        Leg2or3Adder    l3Adder =adder.newLeg3();
                                l3Adder
                                .setR       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+8).getValue().getValue()))
                                .setX       ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+9).getValue().getValue()))
                                .setRatedU  ((((TableColumn<Entry<String,String>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(offset+10).getValue().getValue()))
                                .setVoltageLevel(vl3!=null?vl3.getId():targetVLId);
                        
//                        RatioTapChangerAdder RTAdder=adder.n;
//                        adder.setVoltageLevel1(targetVLId);
                        if(targetBus!=null)
                            l1Adder.setBus(targetBus.getId());
                        else if(targetBusS!=null)
                            l1Adder.setNode(targetBusS.getTerminal().getNodeBreakerView().getNode());
                        else
                            l1Adder.setNode(targetNode);
//                        if(vl2!=null)
//                            l1Adder.setVoltageLevel(vl2.getId());
                        if(pair2!=null)
                            l2Adder.setNode(pair2.getValue());
                        else if(bbs2!=null)
                            l2Adder.setNode(bbs2.getTerminal().getNodeBreakerView().getNode());
                        else if(bus2!=null)
                            l2Adder.setBus(bus2.getId());
//                        if(vl3!=null)
//                            l3Adder.setVoltageLevel(vl2.getId());
                        if(pair3!=null)
                            l3Adder.setNode(pair3.getValue());
                        else if(bbs3!=null)
                            l3Adder.setNode(bbs3.getTerminal().getNodeBreakerView().getNode());
                        else if(bus3!=null)
                            l3Adder.setBus(bus3.getId());
                        
                        try{
                            l1Adder.add();
                            l2Adder.add();
                            l3Adder.add();
                            adder.add();
                            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                            contentPane .setCursor(Cursor.DEFAULT);
                            this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                            this.SelectionManagerTgt.setUserData("3W-Transformer added.");
                            this.SelectionManagerTgt.fireEvent(new ControllerRequests("3W-Transformer added.", ControllerRequests.SHOW_MESSAGE));
                            event.consume();
                        }
                        catch(Exception ex)
                        {
                            showAlert(ex);
                            
                            createConnectable(event);
                        }
                    });
                    break;
            }
            itemList.add(new SimpleEntry<>("Once finished..."           ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."                  ,CANCEL));
            
            CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                    VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                                    contentPane.getParent().setCursor(Cursor.DEFAULT);
                                    nodeHandlers.forEach((String nId, NodeHandler nH) -> {nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);});
                                    this.SelectionManagerTgt.setUserData("Insertion cancelled.");
                                    this.SelectionManagerTgt.fireEvent(new ControllerRequests("Insertion cancelled.", ControllerRequests.SHOW_MESSAGE));
                                });

            propGrid    .setItems(itemList);
            
            propGrid.getItems().addListener((ListChangeListener.Change change)->{
                        VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);//Needed to reset unfinished regTerm or 2/3 term assignments, etc.
                        contentPane.getParent().setCursor(Cursor.DEFAULT);
                    });
        }
        
    }

    @Override
    public void display(String voltageLevelId)
    {
        if(network==null)
            return;
        if(network.getVoltageLevel(voltageLevelId)==null)
            return;
        
        Substation  nextSS      =network.getVoltageLevel(voltageLevelId).getSubstation();
        this.SelectionManagerTgt.setUserData(nextSS);
        this.SelectionManagerTgt.fireEvent(new ControllerRequests(nextSS, ControllerRequests.SELECT_SUBSTATION));
    }
    
    public Substation getSubstation(String nodeId)
    {
        if(network==null)
            return null;
        
        String  voltageLevelId  =nodeHandlers.keySet().contains(nodeId)?
                nodeHandlers.get(nodeId).getVId():
                (vlHandlers.keySet().contains(nodeId)?
                vlHandlers.get(nodeId).getVId():
                null);
        
        if(network.getVoltageLevel(voltageLevelId)==null)
            return null;
        
        return network.getVoltageLevel(voltageLevelId).getSubstation();
    }
    
    public class BranchParameters
    {
        public  double  R;
        public  double  X;
        public  double  G;
        public  double  B;
    }
    
    public class RatioStepParameters extends BranchParameters
    {
        public  double  Rho;
    }
    
    public class PhaseStepParameters extends RatioStepParameters
    {
        public  double  Alpha;
    }
    
    public class RTCStdParams
    {
        public  int         steps;          //taps          =steps+1
        public  double      stepSize;       //
        public  double      lowestRatio;    //highestRatio  =lowestRatio + steps*stepSize
        public  int         lowTapPosition; //
        public  boolean     TCUL;
        public  Terminal    regTerminal;
    }
    
    public class PTCStdParams
    {
        public  int         steps;          //taps          =steps+1
        public  double      rStepSize;      //
        public  double      pStepSize;      //
        public  double      lowestRatio;    //highestRatio  =lowestRatio + steps*rStepSize
        public  double      lowestAngle;    //highestAngle  =lowestAngle + steps*pStepSize
        public  int         lowTapPosition; //
        public  Terminal    regTerminal;
    }
    
    private Terminal getTerminalForAt(Connectable For, NodeHandler nhAt, VoltageLevelHandler vlhAt)
    {
        String              vlAtId  =vlhAt  !=null?vlhAt.getVId()                   :nhAt!=null?nhAt.getVId():null;
        VoltageLevel        vlAt    =vlAtId !=null?network.getVoltageLevel(vlAtId)  :null;                          if(vlAt==null) return null;
        String              atId    =nhAt   !=null?nhAt.getId()                     :null;                          if(atId==null) return null;
        Identifiable        elemAt  =getIdentifiable(atId);
        Terminal            resultT =null;
        
//        if(elemAt instanceof BusbarSection)
//        {
//            BusbarSection   bbs =(BusbarSection)elemAt;
//            
//            resultT=bbs.getTerminal();
//        }
//        else
        if(elemAt instanceof Connectable)
        {
            Connectable connAt  =(Connectable)elemAt;
            
            if(connAt.getTerminals().size()==1)
                resultT=(Terminal) connAt.getTerminals().get(0);
            else if(atId.matches("(.){1,}((_ONE){1}(_fictif){0,1})$"))
                resultT=(Terminal) connAt.getTerminals().get(0);
            else if(atId.matches("(.){1,}((_TWO){1}(_fictif){0,1})$"))
                resultT=(Terminal) connAt.getTerminals().get(1);
            else if(atId.matches("(.){1,}((_THREE){1}(_fictif){0,1})$"))
                resultT=(Terminal) connAt.getTerminals().get(2);
        }
        else if(elemAt instanceof Bus)
        {
            Bus             bb  =(Bus)elemAt;
            
            for(Terminal next:((List<? extends Terminal>)For.getTerminals()))
            {
                if(next.getBusBreakerView().getConnectableBus()==bb)
                {
                    resultT=next;
                    break;
                }
            }
            
//            resultT=resultT!=null?resultT:Builders.BuildTerminalExtBBT(network, vlAt, (Validable)For, atId, atId);
                
        }
        else if(elemAt==null)
        {
            if(vlAt.getTopologyKind()!=TopologyKind.NODE_BREAKER)
                return null;
            
            int         nodeAt  =this.getNodeFromGraph(atId, vlAtId);
            
            resultT=vlAt.getNodeBreakerView().getTerminal(nodeAt);
            
//            resultT=resultT!=null?resultT:Builders.BuildTerminalExtNBT(network, vlAt, (Validable)For, nodeAt);
        }
        
        return resultT;
    }
    
    public void triggerCellDuplication()
    {
        SelectionManager    THIS    =this;
        
        ChangeListener  cellIselected   =new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue==null)
                {
                    SelectionManager.this.selectedCell.removeListener(this);
                    return;
                }
                initCellCopy();
                SelectionManager.this.clearSelected();
            }
        };
        this.selectedCell.addListener(cellIselected);
        if(this.selectedCell.isNull().get())
        {
//            this.selectedBusCell.addListener(cellIselected);
            triggerCellSelection();
        }
        else
        {
            initCellCopy();
//            SelectionManager.this.clearSelected();
        }
        
    }
    
    private void initCellCopy()
    {
//        BusCell                                                                                                 bc          =SelectionManager.this.selectedBusCell.get();
        Cell                                                                                                    bc          =SelectionManager.this.selectedCell.get();
//                ImmutableMap<String, BusNode>                                                                           busNodes    =Maps.uniqueIndex(bc.getBusNodes().iterator(), BusNode::getId);
//                ImmutableMap<String, com.powsybl.substationdiagram.model.Node>                                          nodes       =Maps.uniqueIndex(Sets.newConcurrentHashSet(bc.getNodes()),com.powsybl.substationdiagram.model.Node::getId);
        Map<com.powsybl.substationdiagram.model.Node.NodeType, List<com.powsybl.substationdiagram.model.Node>>  groupedNodes=bc.getNodes().stream().collect(Collectors.groupingBy(com.powsybl.substationdiagram.model.Node::getType));
        List<BusNode>                                                                                           busNodes    =bc instanceof BusCell?((BusCell)bc).getBusNodes():new ArrayList<>();
        List<com.powsybl.substationdiagram.model.Node>                                                          switches    =groupedNodes.get(com.powsybl.substationdiagram.model.Node.NodeType.SWITCH);
//                List<com.powsybl.substationdiagram.model.Node>                                                          feeders     =groupedNodes.get(com.powsybl.substationdiagram.model.Node.NodeType.FEEDER);
//                List<com.powsybl.substationdiagram.model.Node>                                                          ficts       =groupedNodes.get(com.powsybl.substationdiagram.model.Node.NodeType.FICTITIOUS);
        List<String>                                                                                            busSectIds  =Lists.transform(busNodes, BusNode::getId);
        List<String>                                                                                            switchIds   =Lists.transform(switches, com.powsybl.substationdiagram.model.Node::getId);
//                List<String>                                                                                            feederIds   =Lists.transform(feeders , com.powsybl.substationdiagram.model.Node::getId);
        Object[]                                                                                                nwBusSect   =network.getIdentifiables().stream().filter(idable  ->busSectIds.contains(idable .getId())).toArray();
        Object[]                                                                                                nwSwitches  =network.getSwitchStream()          .filter(nwSw    ->switchIds .contains(nwSw   .getId())).toArray();
//                Object[]                                                                                                nwConnect   =network.getIdentifiables().stream().filter(idable  ->feederIds .contains(idable .getId())).toArray();
        VoltageLevel                                                                                            vl          =bc.getGraph().getVoltageLevel();
//        network.getVariantManager().cloneVariant("InitialState", "Edition", true);

        if(vl.getTopologyKind()==TopologyKind.NODE_BREAKER)
            switchCopy(Arrays.copyOf(nwSwitches,nwSwitches.length,new Switch[]{}.getClass()), Arrays.copyOf(nwBusSect, nwBusSect.length, (new BusbarSection[]{}.getClass())), vl, null, vl.getNodeBreakerView().getNodeCount()-1, false);
        else
            showAlert(new Exception("Cell duplication supported only in Node-Breaker Toplogies."));
    }
    /**
     * @param swArr Array containing switches to copy in a new cell
     * @param bsArr Array containing bus sections in the original cell.  Used to connect border swithes to the busbrs in the same way as original switches are connected.
     */
    private void switchCopy(Switch[] swArr, BusbarSection[] bsArr, VoltageLevel vl, Map<Integer, Integer> availableNodes, int biggestNodeUsed, boolean needsRedraw)
    {
        ArrayList<SwitchAdder>                              swDupAdders =new ArrayList<>();
        ArrayList<Switch>                                   swInvalids  =new ArrayList<>();
        int                                                 nodeCount   =vl.getTopologyKind()==TopologyKind.NODE_BREAKER?vl.getNodeBreakerView().getNodeCount():-1;
        List<Terminal>                                      termInBus   =Lists.transform(Arrays.asList(bsArr), BusbarSection::getTerminal);
        List<NodeBreakerView>                               nbvInBus    =Lists.transform(termInBus, Terminal::getNodeBreakerView);
        List<Integer>                                       nodeInBus   =Lists.transform(nbvInBus, NodeBreakerView::getNode);
        Button                                              OKB         =new Button("Submit");
        Button                                              CANCEL      =new Button("Cancel");
        Map<Integer,Integer>                                neededNodes =new HashMap<>();
        Map<Integer,Integer>                                neededNodes_;
        propGrid.getItems().clear();
        
        //Compute needed nodes
        if(availableNodes==null?true:availableNodes.isEmpty())
        {
            List<Integer>   emptyNodes  =emptyNodesFinder(vl.getNodeBreakerView());
            for(Switch sw:swArr)
            {
                int oldNode1=vl.getNodeBreakerView().getNode1(sw.getId());
                int oldNode2=vl.getNodeBreakerView().getNode2(sw.getId());
                
                if(!nodeInBus.contains(oldNode1)&&!neededNodes.containsKey(oldNode1))
                {
                    neededNodes.put(oldNode1, emptyNodes.isEmpty()?nodeCount++:emptyNodes.remove(0));
//                    nodeCount++;
                }
                if(!nodeInBus.contains(oldNode2)&&!neededNodes.containsKey(oldNode2))
                {
                    neededNodes.put(oldNode2, emptyNodes.isEmpty()?nodeCount++:emptyNodes.remove(0));
//                    nodeCount++;
                }
            }
            reorderNodes(vl.getNodeBreakerView());
            if(nodeCount!=vl.getNodeBreakerView().getNodeCount())
                vl.getNodeBreakerView().setNodeCount(nodeCount);

        }
        else
            neededNodes=availableNodes;
        
        neededNodes_=neededNodes;

        //
        for(Switch sw:swArr)
        {
            int oldNode1=vl.getNodeBreakerView().getNode1(sw.getId());
            int oldNode2=vl.getNodeBreakerView().getNode2(sw.getId());
            int node1   =nodeInBus.contains(oldNode1)?oldNode1:neededNodes.get(oldNode1);
            int node2   =nodeInBus.contains(oldNode2)?oldNode2:neededNodes.get(oldNode2);
            
            propGrid.getItems().add(new SimpleEntry<>("Id "  +sw.getId()+" ->",new SimpleStringProperty("")));
            propGrid.getItems().add(new SimpleEntry<>("Name "+sw.getName()+" ->",new SimpleStringProperty("")));
            SwitchAdder swAdder =
            vl.getNodeBreakerView().newSwitch() .setKind(sw.getKind())
                                                .setFictitious(sw.isFictitious())
                                                .setNode1(node1)
                                                .setNode2(node2)
                                                .setRetained(sw.isRetained())
                                                .setOpen(sw.isOpen());
            swDupAdders.add(swAdder);
        }
        
        OKB.setOnAction(event2->{
                                    boolean needsRedraw2    =needsRedraw;
                                    int     biggestNodeUsed2=biggestNodeUsed;
                                    try
                                    {
                                        for(SwitchAdder swAdder:swDupAdders)
                                        {
                                            int index           =swDupAdders.indexOf(swAdder);
                                            try{
                                                Switch  added=
                                                swAdder .setId  (((SimpleEntry<String, SimpleStringProperty>)propGrid.getItems().get(2*index)).getValue().get())
                                                        .setName(((SimpleEntry<String, SimpleStringProperty>)propGrid.getItems().get(2*index+1)).getValue().get())
                                                        .add();
//                                                if(needsRedraw==null)
//                                                    needsRedraw=new boolean[]{true};
                                                biggestNodeUsed2=Math.max(biggestNodeUsed2, vl.getNodeBreakerView().getNode1(added.getId()));
                                                biggestNodeUsed2=Math.max(biggestNodeUsed2, vl.getNodeBreakerView().getNode2(added.getId()));
                                                needsRedraw2=true;
                                            }
                                            catch(Exception ex2)
                                            {
                                                swInvalids.add(swArr[index]);
                                            }
                                        }
                                        if(!swInvalids.isEmpty())
                                            throw(new Exception("Some switch ID's or names are invalid.  Please try again."));
                                    }
                                    catch(Exception ex)
                                    {
                                        showAlert(ex);
                                        switchCopy(swInvalids.toArray(new Switch[swInvalids.size()]), bsArr, vl, neededNodes_, biggestNodeUsed2,needsRedraw2);
                                    }
                                    if(swInvalids.isEmpty())
                                    {
                                        if(needsRedraw2)
                                            SelectionManager.this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                                        if(vl.getNodeBreakerView().getNodeCount()-1>biggestNodeUsed2)
                                            vl.getNodeBreakerView().setNodeCount(biggestNodeUsed2+1);
//                                        reorderNodes(vl.getNodeBreakerView());
                                        this.SelectionManagerTgt.setUserData("Cell copied.");
                                        this.SelectionManagerTgt.fireEvent(new ControllerRequests("Cell copied.", ControllerRequests.SHOW_MESSAGE));
                                    }
                                });
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                    VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
                                    contentPane.getParent().setCursor(Cursor.DEFAULT);
                                    nodeHandlers.forEach((String nId, NodeHandler nH) -> {nH.setNodeHandlerValidator(NodeHandlerValidators.allowAllNH);});
                                    propGrid.getItems().clear();
                                    if(vl.getNodeBreakerView().getNodeCount()-1>biggestNodeUsed)
                                        vl.getNodeBreakerView().setNodeCount(biggestNodeUsed+1);
                                    this.clearSelected();
                                    if(needsRedraw)
                                        SelectionManager.this.SelectionManagerTgt.fireEvent(ControllerRequests.REDRAW_REQUEST);
                                });
        
        propGrid.getItems().add(new SimpleEntry<>("Once finished..."           ,OKB));
        propGrid.getItems().add(new SimpleEntry<>("Cancel..."                  ,CANCEL));
    }
    
    public void triggerCellSelection()
    {
        Group                           cursorImg   =(new SvgLoader()).loadSvg(this.getClass().getResourceAsStream("/img/PointToNode.svg"));
        SnapshotParameters              snapParams  =new SnapshotParameters();
        EventHandler<ConnectionEvent>   eh;
        SelectionManager                THIS        =this;

        if(contentPane==null)
            return;
        if(contentPane.getParent()==null)
            return;
            
        snapParams.setFill(Paint.valueOf("#ffffff00"));
        contentPane.getParent().setCursor(new ImageCursor(cursorImg.snapshot(snapParams, null), 3, 3));
        
        VoltageLevelHandler.setMode(VisualHandlerModes.CELL_SELECTION);
        setNodeHandlerValidator("*");
//        setNodeHandlerValidator("REGULATING_TERMINAL");
        VoltageLevelHandler.setMode(VisualHandlerModes.CELL_SELECTION, this.SelectionManagerTgt, this.SelectionManagerTgt);
        
        eh=new EventHandler<ConnectionEvent>()  {
                                                    @Override public void handle(ConnectionEvent event)
                                                    {
                                                        NodeHandler                                 targetNH    =event.getOriginalSource() instanceof NodeHandler           ?(NodeHandler)event.getOriginalSource()         :event.getOriginalSource() instanceof Node?nodeHandlers.get(((Node)event.getOriginalSource()).getId()):null;
                                                        VoltageLevelHandler                         targetVLH   =event.getOriginalSource() instanceof VoltageLevelHandler   ?(VoltageLevelHandler)event.getOriginalSource() :event.getOriginalSource() instanceof Node?vlHandlers .get(((Node)event.getOriginalSource()).getId().replaceFirst("^(LABEL_VL_)", "")):null;
                                                        Graph                                       vlGraph;
                                                        com.powsybl.substationdiagram.model.Node    selectedGraphNode;
                                                        Cell                                        selectedGraphCell;
                                                        ImplicitCellDetector                        cellDetector=new ImplicitCellDetector();
                                                        CellBlockDecomposer                         cellDecomp  =new CellBlockDecomposer();
//                                                        com.powsybl.substationdiagram.model.Node    startingNode;
//                                                        BusCell                                     decomposedCell;
                                                        Cell                                        decomposedCell;
                                                        
                                                        VoltageLevelHandler .setMode(VisualHandlerModes.NORMAL);
                                                        SelectionManagerTgt .removeEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, this);
                                                        contentPane         .getParent().setCursor(Cursor.DEFAULT);
                                                        
                                                        event.consume();
                                                        
                                                        if(targetNH==null)
                                                            return;
                                                        if(targetVLH==null)
                                                            targetVLH=vlHandlers.get(targetNH.getVId());
                                                        
                                                        vlGraph             =Graph.create(network.getVoltageLevel(targetVLH.getVId()));
                                                        
                                                        cellDetector.detectCells(vlGraph);
                                                        
                                                        selectedGraphNode   =vlGraph.getNode(targetNH.getId());
                                                        if(selectedGraphNode==null)
                                                            return;
                                                        
                                                        selectedGraphCell   =selectedGraphNode.getCell();
                                                        if(selectedGraphCell==null)
                                                            return;
                                                        THIS.clearSelected();
                                                        cellDecomp.determineBlocks(selectedGraphCell);
                                                        com.powsybl.substationdiagram.model.Node    extremityNode   =selectedGraphCell.getRootBlock().getExtremityNode(Block.Extremity.START);
                                                        Identifiable                                extremityIdable =network.getIdentifiable(extremityNode.getId());
                                                        
//                                                        
//                                                        try
//                                                        {
//                                                        Toolkit.getToolkit().getSystemClipboard().putContent(new Pair<>(graphCellDataFormat, new CellSerializable(selectedGraphCell)));
//                                                        }
//                                                        catch(Exception ex)
//                                                        {
//                                                            System.out.print("");
//                                                        }
                                                        
                                                        if(!vlGraph.getBusCells().findFirst().equals(Optional.empty()))
                                                            decomposedCell=vlGraph.getBusCells().findFirst().get();
                                                        else
                                                            decomposedCell=vlGraph.getCells().stream().findFirst().get();
                                                        if(decomposedCell==null)
                                                            return;
                                                        THIS.selectedCell.set(decomposedCell);
                                                        
                                                        selectedGraphCell.getNodes().forEach((com.powsybl.substationdiagram.model.Node t) -> {
                                                            NodeHandler nextNH  =nodeHandlers.get(t.getId());
                                                            if(nextNH!=null)
                                                                lastSelectedNodes.add(nextNH.getNode());
                                                        });
                                                    };
                                                };
        
        //In case of cancelling, remove listeners
        VoltageLevelHandler.getModeProperty().addListener(new ChangeListener<VisualHandlerModes>() {
            @Override
            public void changed(ObservableValue<? extends VisualHandlerModes> observable, VisualHandlerModes oldValue, VisualHandlerModes newValue) {
                if (oldValue!=null?oldValue==VisualHandlerModes.CELL_SELECTION:false) {
                    SelectionManagerTgt.removeEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, SelectionManager.this);
                    VoltageLevelHandler.getModeProperty().removeListener(this);
                }
            }
        });
        
        this.SelectionManagerTgt.addEventHandler(ConnectionEvent.SEARCH_REGULATING_TERMINAL, eh);

    }
    
    private List<Integer> emptyNodesFinder(com.powsybl.iidm.network.VoltageLevel.NodeBreakerView view)
    {
        reorderNodes(view);
        int[]           nodes       =view.getNodes();
//        List<Integer>   expectedNodes=new ArrayList<>();for(Integer node:IntStream.rangeClosed(0, nodes.length-1).toArray()){expectedNodes.add(node);}
        List<Integer>   nodesList   =new ArrayList<>();for(Integer node:nodes){nodesList.add(node);}
        List<Switch>    switches    =Lists.newArrayList(view.getSwitches());
        List<String>    ids         =Lists.transform(switches, Switch::getId);
        List<Integer>   switchsNode1=Lists.transform(ids,view::getNode1);
        List<Integer>   switchsNode2=Lists.transform(ids,view::getNode2);
        List<Terminal>  terminals   =Lists.transform(nodesList, view::getTerminal);
        List<Terminal>  notNullTerms=terminals.stream().filter(terminal->terminal!=null).collect(Collectors.toList());
        List<Integer>   termNodes   =Lists.transform(Lists.transform(notNullTerms, Terminal::getNodeBreakerView),Terminal.NodeBreakerView::getNode);
        List<Integer>   joined      =switchsNode1.stream().distinct().collect(Collectors.toList());
        List<Integer>   distincts;
        
//        expectedNodes.removeAll(nodesList);
//        nodesList.addAll(expectedNodes);
        
        joined.addAll(switchsNode2.stream().distinct().collect(Collectors.toList()));
        joined.addAll(termNodes.stream().distinct().collect(Collectors.toList()));
        
        distincts=joined.stream().distinct().collect(Collectors.toList());
        
        nodesList.removeAll(distincts);
        

        return nodesList;
    }
    
    private int addNode(com.powsybl.iidm.network.VoltageLevel.NodeBreakerView view)
    {
        reorderNodes(view);
////        int[]           nodes       =view.getNodes();
////        List<Integer>   expectedNodes=new ArrayList<>();for(Integer node:IntStream.rangeClosed(0, nodes.length-1).toArray()){expectedNodes.add(node);}
//        List<Integer>   expectedNodes=new ArrayList<>(IntStream.rangeClosed(0, view.getNodeCount()-1).boxed().collect(Collectors.toList()));//for(Integer node:IntStream.rangeClosed(0, view.getNodeCount()-1).toArray()){expectedNodes.add(node);}
////        List<Integer>   nodesList   =new ArrayList<>();for(Integer node:nodes){nodesList.add(node);}
////        List<Integer>   nodesList   =new ArrayList<>();for(Integer node:nodes){nodesList.add(node);}
//        
////        expectedNodes.removeAll(nodesList);
////        expectedNodes.sort(Comparator.naturalOrder());
////        nodesList.sort(Comparator.naturalOrder());
//        
////        if(expectedNodes.isEmpty())
////            return nodesList.size();
//        
////        return expectedNodes.get(0);
////        return nodesList.get(nodesList.size()-1)+1;
//        for(int nextN:expectedNodes)
//        {
//            try
//            {
//                if(view.getTerminal(nextN)==null)
//                    return nextN;
//            }
//            catch(PowsyblException ex)
//            {
//                return nextN;
//            }
//            catch(Exception ex)
//            {
//                
//            }
//        }
        List<Integer>   nodesList   =new ArrayList<>();for(Integer node:view.getNodes()){nodesList.add(node);}
        nodesList.sort(Comparator.naturalOrder());
        if(!nodesList.isEmpty())
            view.setNodeCount(nodesList.get(nodesList.size()-1)+1);
//        else
//            view.setNodeCount(1);
        
        return view.getNodeCount();
    }
    
    private void reorderNodes(com.powsybl.iidm.network.VoltageLevel.NodeBreakerView view)
    {
//        network.getVariantManager().cloneVariant("InitialState", "dummy");
//        network.getVariantManager().setWorkingVariant("dummy");
//        network.getVariantManager().setWorkingVariant("InitialState");
//        network.getVariantManager().removeVariant("dummy");
//        
        List<Integer>   nodesList   =new ArrayList<>();for(Integer node:view.getNodes()){nodesList.add(node);}
        nodesList.sort(Comparator.naturalOrder());
        
        if(!nodesList.isEmpty())
            view.setNodeCount(nodesList.get(nodesList.size()-1)+1);
    }

}