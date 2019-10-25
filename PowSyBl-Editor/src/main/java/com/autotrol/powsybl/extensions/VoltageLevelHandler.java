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
import com.powsybl.substationdiagram.layout.HorizontalSubstationLayout;
import com.powsybl.substationdiagram.layout.InfoCalcPoints;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.ComponentSize;
import com.powsybl.substationdiagram.library.ComponentTypeName;
import com.powsybl.substationdiagram.model.BaseNode;
import com.powsybl.substationdiagram.model.BusCell;
import com.powsybl.substationdiagram.model.Coord;
import com.powsybl.substationdiagram.svg.GraphMetadata;
//import com.powsybl.substationdiagram.view.WireHandler;
import javafx.scene.Node;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventTarget;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public final class VoltageLevelHandler implements BaseNode {

    private static final    SimpleObjectProperty<VisualHandlerModes>        mode            =new SimpleObjectProperty<>();
    private static final    SimpleObjectProperty<Object[]>                  params          =new SimpleObjectProperty<>();
    private static final    SimpleObjectProperty<Object>                    originalSource  =new SimpleObjectProperty<>();
    private static final    SimpleObjectProperty<EventTarget>               finalTarget     =new SimpleObjectProperty<>();
    
    public static void setMode(VisualHandlerModes _mode, Object... _params)
    {
        mode            .set(_mode);
        params          .set(_params.length>0?_params:null);
        finalTarget     .set(null);
        originalSource  .set(null);
    }
    
    public static void setMode(VisualHandlerModes _mode, Object _originalSource, EventTarget _finalTarget, Object... _params)
    {
        mode            .set(_mode);
        params          .set(_params.length>0?_params:null);
        finalTarget     .set(_finalTarget);
        originalSource  .set(_originalSource);
    }
    
//    public static void setMode(VisualHandlerModes _mode, EventTarget _target)
//    {
//        mode.set(_mode);
//        finalTarget.set(_target);
//    }
    
    public static boolean hasFinalTarget()
    {
        return finalTarget.isNotNull().get();
    }
    
    public static EventTarget consumeFinalTarget()
    {
        EventTarget oldTarget   =finalTarget.get();
        
        finalTarget.set(null);
        
        return oldTarget;
    }
    
    public static VisualHandlerModes getMode()
    {
        return mode.get();
    }    
    
    public static SimpleObjectProperty<VisualHandlerModes> getModeProperty()
    {
        return mode;
    }    
    
    public static Object getOriginalSource()
    {
        return originalSource.get();
    }    
    
    public static SimpleObjectProperty<Object> getOriginalSourceProperty()
    {
        return originalSource;
    }    
    
    public static Object getFinalTarget()
    {
        return finalTarget.get();
    }    
    
    public static SimpleObjectProperty<EventTarget> getFinalTargetProperty()
    {
        return finalTarget;
    }    
    
    public static Object[] getModeParams()
    {
        return params.get();
    }    
    
    public static SimpleObjectProperty<Object[]> getModeParamsProperty()
    {
        return params;
    }    
    
    private final           Node                                            node;   // node for voltageLevel label
    private final           List<NodeHandler>                               nodeHandlers    =new ArrayList<>();
    private final           String                                          vId;
    private                 double                                          mouseX;
    private                 double                                          mouseY;
    private                 double                                          prevTx;
    private                 double                                          prevTy;
    private final           GraphMetadata                                   metadata;
    private        final    ChangeListener<VisualHandlerModes>              modeListener;

    public                  ZoomScrollAnchor                                centralScroll;
    
    public VoltageLevelHandler(Node node, GraphMetadata metadata, String vId) {
        this.node = Objects.requireNonNull(node);
        this.metadata = Objects.requireNonNull(metadata);
        this.vId = Objects.requireNonNull(vId);

//        node.setCursor(Cursor.CROSSHAIR);
        
        this.modeListener   =(observble, oldValue, newValue)->{
            switch(newValue)
            {
                case NORMAL:
                    this.node.setCursor(Cursor.CROSSHAIR);
                    break;
                case BUSBAR_SECTION_INSERTION:
                    this.node.setCursor(new ImageCursor(new Image(PredefinedCursors.class.getResourceAsStream("/img/icon_Permission.png")),16.0,16.0));
                    break;
                default:
                    this.node.setCursor(new ImageCursor(new Image(PredefinedCursors.class.getResourceAsStream("/img/icon_Blocking.png")),16.0,16.0));
                    break;
            }
        };
        
        mode.addListener(modeListener);
        
        if(mode.isNull().get())
            mode.set(VisualHandlerModes.NORMAL);
        else
            this.modeListener.changed(mode, mode.get(), mode.get());
        
        setDragAndDrop();
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String getId() {
        return node.getId();
    }

    public String getVId() {
        return vId;
    }

    @Override
//    public ComponentType getComponentType() {
    public String getComponentType() {
        return null;
    }

    @Override
    public boolean isRotated() {
        return false;
    }

    @Override
    public double getX() {
        ComponentSize size = new ComponentSize(0, 0);
        return node.localToParent(node.getLayoutX() + size.getWidth() / 2,
                                  node.getLayoutY() + size.getHeight() / 2).getX();
    }

    @Override
    public double getY() {
        ComponentSize size = new ComponentSize(0, 0);
        return node.localToParent(node.getLayoutX() + size.getWidth() / 2,
                                  node.getLayoutY() + size.getHeight() / 2).getY();
    }

    public void addNodeHandlers(List<NodeHandler> nodeHandlers) {
        this.nodeHandlers.addAll(nodeHandlers);
    }

    public void setDragAndDrop() {
        VoltageLevelHandler THIS    =this;
        node.setOnMousePressed(event -> {
            if(centralScroll==null)
                try {
                    searchParent("centralScroll");
                } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                }
            switch(mode.get())
            {
                case NORMAL:
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
                        node.fireEvent(new DraggedNodeEvent(node));
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
            if(centralScroll==null)
                try {
                    searchParent("centralScroll");
                } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
                }
            if(event.getButton().compareTo(MouseButton.PRIMARY)==0)
            {
                switch(mode.get())
                {
                    case BUSBAR_SECTION_INSERTION:
//                        Event.fireEvent(hasFinalTarget()?consumeFinalTarget():node,new ConnectionEvent(THIS, ConnectionEvent.INSERT_BUSBAR_SECTION));//Fire event to be captured by SelectionManager to insert new component (only BUSBAR_SECTION's must be accepted
                        node.fireEvent(new ConnectionEvent(THIS, ConnectionEvent.INSERT_BUSBAR_SECTION));//Fire event to be captured by SelectionManager to insert new component (only BUSBAR_SECTION's must be accepted
//                        node.fireEvent(new ConnectionEvent(THIS, ConnectionEvent.INSERT_BUSBAR_SECTION, VoltageLevelHandler.consumeFinalTarget()));//Fire event to be captured by SelectionManager to insert new component (only BUSBAR_SECTION's must be accepted
                        break;
                    default:
                                break;
                }
            }
            event.consume();
        });

        node.setOnMouseDragged(event -> {
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
//                node.setTranslateX(dX+prevTx);
//                node.setTranslateY(dY+prevTy);
//                
//                // apply transformation to all nodes of the voltageLevel in nodeHandlers list
//                nodeHandlers.stream().filter(n -> n.getVId().equals(vId)).forEach(v -> v.translate( dX, dY));
//
//                // redraw the snakeLines between the voltage levels
//                redrawSnakeLines();
                node.fireEvent(new DraggedNodeEvent(node, dX, dY));
            }

            event.consume();
        });
    }

    private void redrawSnakeLines() {
        // redraw the snakeLines between the voltage levels
        //
        Map<BusCell.Direction, Integer> nbSnakeLinesTopBottom = EnumSet.allOf(BusCell.Direction.class).stream().collect(Collectors.toMap(Function.identity(), v -> 0));
        Map<String, Integer> nbSnakeLinesBetween = new HashMap<>();

        Map<String, Coord> posVL = new HashMap<>();

        List<WireHandler> whSnakeLines = new ArrayList<>();
        for (NodeHandler nh : nodeHandlers) {
            if (nh.getComponentType().equals(ComponentTypeName.BUSBAR_SECTION)) {
                if (!posVL.containsKey(nh.getVId())) {
                    posVL.put(nh.getVId(), new Coord(Double.MAX_VALUE, 0));
                }
                double x = Math.min(posVL.get(nh.getVId()).getX(), nh.getX());
                posVL.put(nh.getVId(), new Coord(x, 0));
            }

            for (WireHandler wh : nh.getWireHandlers()) {
                if (wh.isSnakeLine()) {
                    whSnakeLines.add(wh);
                }
            }
            nbSnakeLinesBetween.put(nh.getVId(), 0);
        }

        for (WireHandler wh : whSnakeLines) {
            List<Double> pol = calculatePolylineSnakeLine(metadata.getLayoutParameters(),
                    wh,
                    posVL,
                    nbSnakeLinesTopBottom,
                    nbSnakeLinesBetween);
            ((Polyline) wh.getNode()).getPoints().setAll(pol);
        }
    }

    private List<Double> calculatePolylineSnakeLine(LayoutParameters layoutParam,
                                                   WireHandler wh,
                                                   Map<String, Coord> posVL,
                                                   Map<BusCell.Direction, Integer> nbSnakeLinesTopBottom,
                                                   Map<String, Integer> nbSnakeLinesBetween) {
        NodeHandler nh1 = wh.getNodeHandler1();
        NodeHandler nh2 = wh.getNodeHandler2();

        BusCell.Direction dNode1 = nh1.getDirection();
        BusCell.Direction dNode2 = nh2.getDirection();

        double xMaxGraph;
        String idMaxGraph;

        if (posVL.get(nh1.getVId()).getX() > posVL.get(nh2.getVId()).getX()) {
            xMaxGraph = posVL.get(nh1.getVId()).getX();
            idMaxGraph = nh1.getVId();
        } else {
            xMaxGraph = posVL.get(nh2.getVId()).getX();
            idMaxGraph = nh2.getVId();
        }

        double x1 = wh.getNodeHandler1().getX();
        double y1 = wh.getNodeHandler1().getY();
        double x2 = wh.getNodeHandler2().getX();
        double y2 = wh.getNodeHandler2().getY();

        InfoCalcPoints info = new InfoCalcPoints();
        info.setLayoutParam(layoutParam);
        info.setdNode1(dNode1);
        info.setdNode2(dNode2);
        info.setNbSnakeLinesTopBottom(nbSnakeLinesTopBottom);
        info.setNbSnakeLinesBetween(nbSnakeLinesBetween);
        info.setX1(x1);
        info.setX2(x2);
        info.setY1(y1);
        info.setY2(y2);
        info.setxMaxGraph(xMaxGraph);
        info.setIdMaxGraph(idMaxGraph);

        return HorizontalSubstationLayout.calculatePolylinePoints(info);
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
    
    public void presetTranslates()
    {
        prevTx=node.getTranslateX();
        prevTy=node.getTranslateY();

        nodeHandlers.stream().filter(n -> n.getVId().equals(vId)).forEach(v -> v.presetTranslates());

    }

    public void translate(double dX, double dY)
    {
        node.setTranslateX(dX+prevTx);
        node.setTranslateY(dY+prevTy);

        // apply transformation to all nodes of the voltageLevel in nodeHandlers list
        nodeHandlers.stream().filter(n -> n.getVId().equals(vId)).forEach(v -> v.translate( dX, dY));

        // redraw the snakeLines between the voltage levels
        redrawSnakeLines();
    }
    
    public Stream<NodeHandler> getNodeHandlers()
    {
        return nodeHandlers.stream();
    }

    @Override
    public Double getRotationAngle() {
        return null;
    }
}
