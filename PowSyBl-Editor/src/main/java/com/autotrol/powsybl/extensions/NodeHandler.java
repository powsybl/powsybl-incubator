/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.autotrol.powsybl.extensions;

import com.autotrol.events.DraggedNodeEvent;
import com.autotrol.FXcomponents.ZoomScrollAnchor;
import com.autotrol.events.ConnectionEvent;
import static com.autotrol.util.SelectionManager.getNearestAnchor;
import com.powsybl.substationdiagram.library.ComponentMetadata;
import com.powsybl.substationdiagram.library.ComponentSize;
import com.powsybl.substationdiagram.library.ComponentTypeName;
//import com.powsybl.substationdiagram.library.ComponentTypeName;
import com.powsybl.substationdiagram.model.BaseNode;
import com.powsybl.substationdiagram.model.BusCell;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.view.DisplayVoltageLevel;
//import com.powsybl.substationdiagram.view.WireHandler;
//import static fxml.MainFXMLController.getNearestAnchor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class NodeHandler implements BaseNode{

    private static final    SimpleObjectProperty<VisualHandlerModes>        mode            =new SimpleObjectProperty<>();
    private static final    SimpleObjectProperty<Object[]>                  params          =new SimpleObjectProperty<>();
    private static final    SimpleObjectProperty<Object>                    originalSource  =new SimpleObjectProperty<>();
    private static final    SimpleObjectProperty<EventTarget>               finalTarget     =new SimpleObjectProperty<>();
//    public static void setMode(VisualHandlerModes _mode)
//    {
//        mode.set(_mode);
//    }
    
    public static VisualHandlerModes getMode()
    {
        return mode.get();
    }
    
    private        final    SimpleObjectProperty<Predicate<String>>         connTypeValidator   =new SimpleObjectProperty<>();
    private        final    SimpleObjectProperty<Predicate<NodeHandler>>    nodeHandlerValidator=new SimpleObjectProperty<>();
    
    private final           Node                                            node;
//    private                 ComponentType                                   componentType;
    private                 String                                          componentType;
    private final           List<WireHandler>                               wireHandlers        =new ArrayList<>();
//    private final           boolean                                         rotated;
    private final           GraphMetadata                                   metadata;
    private final           String                                          vId;
    private                 Double                                          rotationAngle;
    private final           String                                          nextVId;
    private                 DisplayVoltageLevel                             displayVL;

//    private final           Cursor                                          normalCursor        =Cursor.CROSSHAIR;

    private                 double                                          mouseX;
    private                 double                                          mouseY;
    private                 double                                          prevTx;
    private                 double                                          prevTy;
//    public                  Pane                                            canvasPane;
    public                  ZoomScrollAnchor                                centralScroll;

    private        final    ChangeListener<VisualHandlerModes>              modeListener;

    private BusCell.Direction direction;
    
//    public NodeHandler(Node node, ComponentType componentType, Double rotationAngle,
    public NodeHandler(Node node, String componentType, Double rotationAngle,
                       GraphMetadata metadata,
                       String vId, String nextVId, BusCell.Direction direction)
    {

        this.node           =Objects.requireNonNull(node);
        this.componentType  =componentType;
        this.rotationAngle  =rotationAngle;
        this.metadata       =Objects.requireNonNull(metadata);
        this.vId            =Objects.requireNonNull(vId);
        this.nextVId        =nextVId;
        this.direction      =direction;
        
        this.modeListener   =(observble, oldValue, newValue)->{
            if(nodeHandlerValidator.isNull().get())
                nodeHandlerValidator.set(NodeHandlerValidators.allowAllNH);
            switch(newValue)
            {
                case NORMAL:
                    node.setCursor(Cursor.CROSSHAIR);
//                    node.setCursor(PredefinedCursors.normalCursor);
                    break;
                case CONNECTION:
                case COMPONENT_INSERTION:
                case REGULATING_TERM_SEARCH:
                case CELL_SELECTION:
                case SWITCH_INSERTION:
//                    if(connTypeValidator.get().test(componentType))
                    if(validate())
//                        node.setCursor(PredefinedCursors.connPermision);
                        node.setCursor(new ImageCursor(new Image(PredefinedCursors.class.getResourceAsStream("/img/icon_Permission.png")),16.0,16.0));
                    else
//                        node.setCursor(PredefinedCursors.connBlocking);
                        node.setCursor(new ImageCursor(new Image(PredefinedCursors.class.getResourceAsStream("/img/icon_Blocking.png")),16.0,16.0));
                    break;
                default:
                    node.setCursor(new ImageCursor(new Image(PredefinedCursors.class.getResourceAsStream("/img/icon_Blocking.png")),16.0,16.0));;
            }
        };
        
        connTypeValidator.addListener((observable, oldValue, newValue)->{
            this.modeListener.changed(mode, mode.get(), mode.get());
        });
        nodeHandlerValidator.addListener((observable, oldValue, newValue)->{
            this.modeListener.changed(mode, mode.get(), mode.get());
        });
        
        mode.addListener(modeListener);
//        if(mode.isNull().get())
//            mode.set(VisualHandlerModes.NORMAL);
        if(VoltageLevelHandler.getModeProperty().isNull().get())
            VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
        if(!mode.isBound())
            mode.bind(VoltageLevelHandler.getModeProperty());
        if(!params.isBound())
            params.bind(VoltageLevelHandler.getModeParamsProperty());
        if(!originalSource.isBound())
            originalSource.bind(VoltageLevelHandler.getOriginalSourceProperty());
        if(!finalTarget.isBound())
            finalTarget.bind(VoltageLevelHandler.getFinalTargetProperty());

        connTypeValidator.set(ConnectionValidators.defaultNHValidator);
        nodeHandlerValidator.set(NodeHandlerValidators.allowAllNH);
        
        try
        {
            Method  setTooltipM=node.getClass().getMethod("setTooltip", Tooltip.class);
            
            if(setTooltipM!=null)
                setTooltipM.invoke(node, new Tooltip(componentType.replaceAll("_"," ")));
//                setTooltipM.invoke(node, new Tooltip(componentType.name().replaceAll("_"," ")));
        }
        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
        }
        
        setDragAndDrop();
    }

    public Node getNode()
    {
        return node;
    }
    
//    public void setCursor(Cursor cursor)
//    {
//        node.setCursor(cursor);
//    }
//    
//    public void resetCursor()
//    {
//        node.setCursor(PredefinednormalCursor);
//    }

    @Override
    public String getId()
    {
        return node.getId();
    }

    public String getVId()
    {
        return vId;
    }

    public BusCell.Direction getDirection()
    {
        return direction;
    }

    @Override
//    public ComponentType getComponentType()
    public String getComponentType()
    {
        return componentType;
    }

    public void addWire(WireHandler w)
    {
        wireHandlers.add(w);
        w.getNode().toBack();
    }

    public void removeWire(WireHandler w)
    {
        wireHandlers.remove(w);
    }

    public List<WireHandler> getWireHandlers()
    {
        return wireHandlers;
    }

    @Override
    public double getX()
    {
        ComponentSize size = componentType != null
                ? metadata.getComponentMetadata(componentType).getSize()
                : new ComponentSize(0, 0);
        return node.localToParent(node.getLayoutX() + size.getWidth() / 2,
                                  node.getLayoutY() + size.getHeight() / 2).getX();
    }

    @Override
    public double getY()
    {
        ComponentSize size = componentType != null
                ? metadata.getComponentMetadata(componentType).getSize()
                : new ComponentSize(0, 0);
        return node.localToParent(node.getLayoutX() + size.getWidth() / 2,
                                  node.getLayoutY() + size.getHeight() / 2).getY();
//        return node.localToScene(node.getLayoutX() + size.getWidth() / 2,
//                                  node.getLayoutY() + size.getHeight() / 2).getY();
    }

    public void setDragAndDrop()
    {
        
        if (!StringUtils.isEmpty(node.getId()))
        {
//            GraphMetadata.NodeMetadata nodeMetadata = metadata.getNodeMetadata(node.getId());
                node.setOnMouseEntered(event-> {
                    if(centralScroll==null)
                        try {
                            searchParent("centralScroll");
                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                        }
//                    if(mode==VisualHandlerModes.NORMAL)
//                    switch(mode)
//                    {
//                        case NORMAL:
                            node.setOpacity(0.55);
                            if(!centralScroll.enableOsnap.get() || event.isPrimaryButtonDown() || event.isMiddleButtonDown() || event.isSecondaryButtonDown())
                            {
                                event.consume();
                                return;
                            }
                            try{
                                String              id                  =node.getId();
                                ComponentMetadata   componentMetadata   =id!=null?metadata.getComponentMetadata(metadata.getNodeMetadata(id).getComponentType()):null;
                                Point2D             refPoint            =new Point2D(event.getX(), event.getY());
                                Point2D             screenAnchor        =componentMetadata!=null?(componentMetadata.getAnchorPoints().isEmpty()?node.localToScreen(node.parentToLocal(new Point2D(node.getTranslateX(), node.getTranslateY()))):node.localToScreen(getNearestAnchor(refPoint, componentMetadata.getAnchorPoints()))):node.localToScreen(node.parentToLocal(new Point2D(node.getTranslateX(), node.getTranslateY())));
                                Point2D             snapAnchor          =centralScroll.snapCursor.get().localToParent(centralScroll.snapCursor.get().screenToLocal(screenAnchor));

                                centralScroll.snapCursor.get().setTranslateX(snapAnchor.getX()-8);
                                centralScroll.snapCursor.get().setTranslateY(snapAnchor.getY()-8);
                                ((Rectangle)centralScroll.snapCursor.get()).setStroke(Paint.valueOf("#0065FF"));
                                centralScroll.snapCursor.get().setVisible(true);
                                centralScroll.isOsnapAct.set(true);
                                event.consume();
                            }catch(Exception ex)
                            {
                                centralScroll.snapCursor.get().setVisible(false);
                            }
//                            break;
//                    }
                });
                
                node.setOnMouseExited(event-> {
                    if(centralScroll==null)
                        try {
                            searchParent("centralScroll");
                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                        }
                    node.setOpacity(1.0);
//                    node.setEffect(null);
                    
                    if(!centralScroll.enableOsnap.get())
                    {
                        event.consume();
                        return;
                    }
                    
                    try{
                        centralScroll.isOsnapAct.set(false);
                        centralScroll.snapCursor.get().setVisible(false);
                    }catch(Exception ex)
                    {
                        centralScroll.snapCursor.get().setVisible(false);
                    }
                    event.consume();
                });
        }
        
        node.setOnMousePressed(event -> {
            switch(mode.get())
            {
                case NORMAL:
                    if(centralScroll==null)
                        try {
                            searchParent("centralScroll");
                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                        }

                    if(event.getButton().compareTo(MouseButton.PRIMARY)==0)
                    {
                        Point2D parentCoords    =node.localToParent(event.getX(), event.getY());

                        if(centralScroll!=null?centralScroll.isOsnapAct.get() || centralScroll.enableMag.get():false)
                        {
                            Point2D snapCoordsInRoot    =centralScroll.snapCursor.get().getParent().localToScene(centralScroll.snapCursor.get().getTranslateX(), centralScroll.snapCursor.get().getTranslateY());
                            Point2D snapCoordsInLocal   =node.sceneToLocal(snapCoordsInRoot);
                            Point2D snapCoordsInParent  =node.localToParent(snapCoordsInLocal);

                            mouseX  =snapCoordsInParent.getX()/* - node.getTranslateX()*/;
                            mouseY  =snapCoordsInParent.getY()/* - node.getTranslateY()*/;

                        }
                        else
                        {
                            mouseX = parentCoords.getX()/* - node.getTranslateX()*/;
                            mouseY = parentCoords.getY()/* - node.getTranslateY()*/;
                        }

                        presetTranslates();
                        node.fireEvent(new DraggedNodeEvent(node));//help to notify other selected nodes to trigger presetTranslates, same as this
                    }

                    if(event.getButton().compareTo(MouseButton.SECONDARY)==0)
                    {

                    }
                    break;
                    
                default:
                    break;
            }
            event.consume();
        });
        node.setOnMouseReleased(event -> {
            
            if(event.getButton().compareTo(MouseButton.PRIMARY)==0)
            {
                if(event.isControlDown())
                {
//                    if(componentType==ComponentType.LINE || componentType==ComponentType.TWO_WINDINGS_TRANSFORMER || componentType==ComponentType.PHASE_SHIFT_TRANSFORMER)
                    if(componentType.equals(ComponentTypeName.LINE) || componentType.equals(ComponentTypeName.TWO_WINDINGS_TRANSFORMER) || componentType.equals(ComponentTypeName.PHASE_SHIFT_TRANSFORMER))
                        displayNextVoltageLevel();
                }
                else
                {
                    switch(mode.get())
                    {
                        case CONNECTION:
                            if(validate())
                            {
                                NodeHandler     THIS    =this;
                                EventTarget target  =VoltageLevelHandler.hasFinalTarget()?VoltageLevelHandler.consumeFinalTarget():node;
                                Event.fireEvent(target, VoltageLevelHandler.getModeParams()==null?new ConnectionEvent(THIS, target, ConnectionEvent.CONNECT_TERMINAL):
                                                ConnectionEvent.buildConnectTermEvt(THIS, (int) VoltageLevelHandler.getModeParams()[0]));
                            }
                            break;

                        case REGULATING_TERM_SEARCH:
                        case CELL_SELECTION:
                            if(validate())
                            {
                                NodeHandler THIS    =this;
                                EventTarget target  =VoltageLevelHandler.hasFinalTarget()?VoltageLevelHandler.consumeFinalTarget():node;
                                Event.fireEvent(target, new ConnectionEvent(THIS, target, ConnectionEvent.SEARCH_REGULATING_TERMINAL));
                            }
                            break;

                        case COMPONENT_INSERTION:
                            if(validate())
                            {
                                NodeHandler THIS    =this;
                                EventTarget target  =VoltageLevelHandler.hasFinalTarget()?VoltageLevelHandler.consumeFinalTarget():node;
                                Event.fireEvent(target, new ConnectionEvent(THIS, target, ConnectionEvent.INSERT_COMPONENT));
                            }
                            break;

                        case SWITCH_INSERTION:
                            if(validate())
                            {
                                NodeHandler THIS    =this;
                                EventTarget target  =VoltageLevelHandler.hasFinalTarget()?VoltageLevelHandler.consumeFinalTarget():node;
                                Event.fireEvent(target, new ConnectionEvent(THIS, target, ConnectionEvent.INSERT_SWITCH));
                            }
                            break;

                        case BUSBAR_SECTION_INSERTION:
                        default:
                            break;
                    }
                }
            }
            event.consume();
        });

        node.setOnMouseDragged(event -> {
            
            switch(mode.get())
            {
                case NORMAL:
                    if(centralScroll==null)
                        try {
                            searchParent("centralScroll");
                        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                        }

                    if(event.getButton().compareTo(MouseButton.PRIMARY)==0)
                    {
                        Point2D parentCoords    =node.localToParent(event.getX(), event.getY());
                        double  dX;
                        double  dY;

                        if(centralScroll!=null?centralScroll.enableMag.get():false)
                        {
                            Point2D snapCoordsInScreen  =centralScroll.snapCursor.get().getParent().localToScene(centralScroll.snapCursor.get().getTranslateX(), centralScroll.snapCursor.get().getTranslateY());
                            Point2D snapCoordsInLocal   =node.sceneToLocal(snapCoordsInScreen);
                            Point2D snapCoordsInParent  =node.localToParent(snapCoordsInLocal);
                            Point2D screenCoords        =node.localToScreen(event.getX(), event.getY());
                            Point2D centralScrollCoords =centralScroll.screenToLocal(screenCoords);

                            dX  =snapCoordsInParent.getX() - mouseX;
                            dY  =snapCoordsInParent.getY() - mouseY;

                            centralScroll.showGridCursor(centralScrollCoords);
                        }
                        else
                        {
                            dX   =parentCoords.getX() - mouseX;
                            dY   =parentCoords.getY() - mouseY;                    
                        }

                        translate(dX, dY);
                        node.fireEvent(new DraggedNodeEvent(node, dX, dY));//help to notify other selected nodes to trigger translation, same as this

                    }
                    break;
                case CONNECTION:
                    //Do nothing.
                    break;
                default:
                    ;
            }
            
            event.consume();
        });
    }

    public void translate(double translateX, double translateY)
    {
//        node.setTranslateX(translateX);
//        node.setTranslateY(translateY);
        node.setTranslateX(translateX + prevTx);
        node.setTranslateY(translateY + prevTy);
        
//        System.out.print("["+this.node.getId()+"] PretTX="+prevTx+", dX="+translateX+"\n");
        
        for (WireHandler w : wireHandlers) {
            w.refresh();
        }
    }
    
    private void searchParent(String id) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        Parent  searchParent    =node.getParent();
        
        while(searchParent!=null)
        {
            if(searchParent.getId()!=null)
                if(id.equals(searchParent.getId()))
                    break;
            searchParent=searchParent.getParent();
        }
        
        if(searchParent!=null)
            if(searchParent.getId()!=null)
                if(id.equals(searchParent.getId()))
                {
                    this.getClass().getField(id).set(this, searchParent);
                }
    }
    
    private boolean validate()
    {
        return this.connTypeValidator.get().test(componentType) && this.nodeHandlerValidator.get().test(this);
    }
    
    public void presetTranslates()
    {
        prevTx  =node.getTranslateX();
        prevTy  =node.getTranslateY();        
    }
    
//    public void setConnTypeValidator(Predicate<ComponentType> validator)
    public void setConnTypeValidator(Predicate<String> validator)
    {
        connTypeValidator.set(validator);
    }

    public void setNodeHandlerValidator(Predicate<NodeHandler> validator)
    {
        nodeHandlerValidator.set(validator);
    }

    public Predicate<String> getConnTypeValidator()
    {
        return connTypeValidator.get();
    }

    public Predicate<NodeHandler> getNodeHandlerValidator()
    {
        return nodeHandlerValidator.get();
    }

    @Override
    public Double getRotationAngle()
    {
        return rotationAngle;
    }

    @Override
    public boolean isRotated()
    {
        return rotationAngle != null;
    }
    
    public void setDisplayVL(DisplayVoltageLevel displayVL)
    {
        this.displayVL = displayVL;
    }
    
    private void displayNextVoltageLevel()
    {
        if (nextVId != null) {
            displayVL.display(nextVId);
        }
    }
}
