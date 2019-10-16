/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.FXcomponents;

import java.util.ArrayList;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

/**
 *
 * @author cgalli
 */
public class ZoomScrollAnchor extends AnchorPane
{
    public  final SimpleBooleanProperty         showGrid    =new SimpleBooleanProperty();
    public  final SimpleBooleanProperty         enableMag   =new SimpleBooleanProperty();
    public  final SimpleBooleanProperty         enableOsnap =new SimpleBooleanProperty();
    public  final SimpleBooleanProperty         isOsnapAct  =new SimpleBooleanProperty();
    private final SimpleObjectProperty<Node>    content     =new SimpleObjectProperty<>();
    public  final SimpleObjectProperty<Node>    snapCursor  =new SimpleObjectProperty<>();
    private final SimpleDoubleProperty          gridDist    =new SimpleDoubleProperty(16.0);
    private final SimpleDoubleProperty          gridMajor   =new SimpleDoubleProperty(4.00);
    private final Rectangle                     clippingRect=new Rectangle();
    private final Pane                          canvasPane  =new Pane();
    private final Pane                          gridPane    =new Pane();
    
    //Colors
    //OSnap #0065FF88
    //Snap  #CA6A3488

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ZoomScrollAnchor(boolean showGrid, boolean enableMag, boolean enableOsnap)
    {
        super();
        
        canvasPane.setId("canvasPane");
        
        ChangeListener<Node>    contentListener =(obsVal, oldContent, newContent) -> {
            canvasPane.getChildren().remove(oldContent);
            canvasPane.getChildren().add(newContent);
        };
        
        this.addEventHandler(ScrollEvent.SCROLL, event->{scrollAction(event);});
        this.addEventHandler(MouseEvent.MOUSE_MOVED, event->{mouseMoveAction(event);});
//        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, event->{mouseMoveAction(event);});
        
        this.content.   addListener(contentListener);

        this.parentProperty().addListener((obsVal,oldParent,newParent)->{
            newParent.layoutBoundsProperty().addListener((obsVal2, oldBounds, newBounds)-> {
                                                                                    clippingRect.setWidth(newBounds.getWidth());
                                                                                    clippingRect.setHeight(newBounds.getHeight());
                                                                                    this.setMinWidth(newBounds.getWidth());
                                                                                    this.setMinHeight(newBounds.getHeight());
                                                                                    
                                                                                    resizeCanvas();
                                                                                });
        });
        
        this.setClip(clippingRect);
        this.showGrid.set(showGrid);
        this.enableMag.set(enableMag);
        this.enableOsnap.set(enableOsnap);
        
        this.showGrid.addListener((observable, oldValue, newValue) -> {
            resizeCanvas();
        });
//        BackgroundFill  fill    =new BackgroundFill(Paint.valueOf("#00000000"), new CornerRadii(0.0), new Insets(0.0));
        
        this.getChildren().add(gridPane);
        this.getChildren().add(canvasPane);
        gridPane.setVisible(true);
//        gridPane.setBackground(new Background(fill));
//        gridPane.setOpacity(0.0);
        gridPane.setStyle("-fx-background-color: rgba(0,0,0,0);");
        canvasPane.setVisible(true);
        canvasPane.setStyle("-fx-background-color: rgba(0,0,0,0);");
//        canvasPane.setOpacity(0.0);
//        canvasPane.setBackground(new Background(fill));
        AnchorPane.setTopAnchor     (gridPane  , 0.0);
        AnchorPane.setRightAnchor   (gridPane  , 0.0);
        AnchorPane.setBottomAnchor  (gridPane  , 0.0);
        AnchorPane.setLeftAnchor    (gridPane  , 0.0);
        AnchorPane.setTopAnchor     (canvasPane, 0.0);
        AnchorPane.setRightAnchor   (canvasPane, 0.0);
        AnchorPane.setBottomAnchor  (canvasPane, 0.0);
        AnchorPane.setLeftAnchor    (canvasPane, 0.0);
        
        Rectangle   cursorRect  =new Rectangle(16, 16);
        
        cursorRect.setStroke(Paint.valueOf("#CA6A3488"));
        cursorRect.setStrokeWidth(3.0);
        cursorRect.setFill(Paint.valueOf("#00000000"));
        cursorRect.disableProperty().set(true);
        snapCursor.set(cursorRect);
        snapCursor.get().setVisible(false);
        
        this.getChildren().add(snapCursor.get());

        
    }
    
    private void mouseMoveAction(MouseEvent event)
    {
        if(isOsnapAct.get())
        {
            event.consume();
            return;
        }
        
        if(!enableMag.get())
        {
            snapCursor.get().setVisible(false);
            event.consume();
            return;
        }
        
        showGridCursor(new Point2D(event.getX(), event.getY()));
        
        event.consume();
    }

    private void scrollAction(ScrollEvent event)
    {
        if(content.isNull().get())
            return;
        
        Node        _contentVal     =this.content.get();
        Scale       scale           =new Scale();
        Translate   translate       =new Translate(event.isShiftDown()?event.getDeltaY():0, event.isShiftDown()?0:event.getDeltaY());
        Point2D     pivot;
        Bounds      boundsContnent  =_contentVal.getBoundsInLocal();
        Bounds      boundsParent    =this.getBoundsInLocal();
        Point2D     parentMinXYCont =_contentVal.parentToLocal(boundsParent.getMinX(), boundsParent.getMinY());
        Point2D     parentMaxXYCont =_contentVal.parentToLocal(boundsParent.getMaxX(), boundsParent.getMaxY());
        Transform   lastT           =_contentVal.getTransforms().isEmpty()?new Scale(1.0,1.0,1.0):_contentVal.getTransforms().get(0);
        
        if(event.isControlDown())//zoom
        {
            pivot   =_contentVal.screenToLocal(new Point2D(event.getScreenX(), event.getScreenY()));

            if(event.getDeltaY()<0)
                scale   =new Scale(1/1.025,  1/1.025,   0,              0           );
            else if(event.getDeltaY()>0)
                scale   =new Scale(1.025,    1.025,     0,              0           );
            if(event.getDeltaY()<0)
                scale   =new Scale(1/1.025,  1/1.025,   pivot.getX(),   pivot.getY());
            else if(event.getDeltaY()>0)
                scale   =new Scale(1.025,    1.025  ,   pivot.getX(),   pivot.getY());

            lastT=lastT.createConcatenation(scale);
            _contentVal.getTransforms().clear();
//            if(_contentVal.getTransforms().isEmpty())
                _contentVal.getTransforms().add(lastT);
//            else
//                _contentVal.getTransforms().set(0, lastT);
////            _contentVal.getTransforms().add(scale);
        }
        else//pan
        {
            if(translate.getTx()<0.0)
                translate.setX(boundsContnent.getMaxX()-parentMaxXYCont.getX()+translate.getTx()>=0.0?translate.getTx():Math.min(parentMaxXYCont.getX()-boundsContnent.getMaxX(),0.0));
            else if(translate.getTx()>0.0)
                translate.setX(parentMinXYCont.getX()-boundsContnent.getMinX()-translate.getTx()>=0.0?translate.getTx():Math.max(parentMinXYCont.getX()-boundsContnent.getMinX(),0.0));

            if(translate.getTy()<0.0)
                translate.setY(boundsContnent.getMaxY()-parentMaxXYCont.getY()+translate.getTy()>=0.0?translate.getTy():Math.min(parentMaxXYCont.getY()-boundsContnent.getMaxY(),0.0));
            else if(translate.getTy()>0.0)
                translate.setY(parentMinXYCont.getY()-boundsContnent.getMinY()-translate.getTy()>=0.0?translate.getTy():Math.max(parentMinXYCont.getY()-boundsContnent.getMinY(),0.0));

            lastT=lastT.createConcatenation(translate);
//            if(_contentVal.getTransforms().isEmpty())
//                _contentVal.getTransforms().add(lastT);
//            else
//                _contentVal.getTransforms().set(0,lastT);
            _contentVal.getTransforms().clear();
//            _contentVal.getTransforms().add(translate);
            _contentVal.getTransforms().add(lastT);
        }
        resizeCanvas();
        event.consume();
    }
    
    public void setContent(Node content)
    {
        this.content.setValue(content);
        resizeCanvas();
        
        this.content.get().layoutBoundsProperty().addListener((obsVal2, oldBounds, newBounds)-> {resizeCanvas();});
    }
    
    public static Transform computeTrans(ObservableList<Transform> transforms)
    {
        Transform   result  =Transform.scale(1.0, 1.0);
        
        for(Transform next:transforms)
            result=result.createConcatenation(next);
        
        return result;
    }
    
    private void resizeCanvas()
    {
        if(this.content.isNull().get())
            return;
        
        Point2D min =getUserMinMax(true);
        Point2D max =getUserMinMax(false);
        
        canvasPane.setLayoutX (this.content.get().localToParent(min).getX());
        canvasPane.setLayoutY (this.content.get().localToParent(min).getY());
        canvasPane.setPrefSize(this.content.get().localToParent(max).getX()-this.content.get().localToParent(min).getX(),
                               this.content.get().localToParent(max).getY()-this.content.get().localToParent(min).getY());
        
        //Draw grid
        gridPane.setLayoutX   (this.content.get().localToParent(min).getX());
        gridPane.setLayoutY   (this.content.get().localToParent(min).getY());
        gridPane.setPrefSize  (this.content.get().localToParent(max).getX()-this.content.get().localToParent(min).getX(),
                               this.content.get().localToParent(max).getY()-this.content.get().localToParent(min).getY());
//        gridPane.getChildren().clear();
        cleanParent(gridPane);
        
        if(showGrid.get())
            gridPane.getChildren().add(gridLines());
        
    }
    
    private Point2D getUserMinMax(boolean min)
    {
        Parent  _content;
        Point2D userBounds;
        double  x           =0.0;
        double  y           =0.0;
     
        if(content.isNull().get())
            return null;
        if(!(content.get() instanceof Parent))
            return null;
        
        _content=(Parent) content.get();
        
        for(Node nextChild:_content.getChildrenUnmodifiable())
        {
            if(min)
            {
                if(nextChild!=canvasPane)
                {
                    x=Math.min(x, Math.min(nextChild.getBoundsInParent().getMinX(), _content.parentToLocal(this.layoutBoundsProperty().get()).getMinX()));
                    y=Math.min(y, Math.min(nextChild.getBoundsInParent().getMinY(), _content.parentToLocal(this.layoutBoundsProperty().get()).getMinY()));
                }
            }
            else
            {
                if(nextChild!=canvasPane)
                {
                    x=Math.max(x, Math.max  (nextChild.getBoundsInParent().getMaxX(), _content.parentToLocal(this.layoutBoundsProperty().get()).getMaxX()));
                    y=Math.max(y, Math.max  (nextChild.getBoundsInParent().getMaxY(), _content.parentToLocal(this.layoutBoundsProperty().get()).getMaxY()));
                }
            }
        }
        
        userBounds=new Point2D(x,y);
        return userBounds;
    }
    
    public Point2D getContentCoords(Point2D screenXY)
    {
        return this.content.get().screenToLocal(screenXY);
    }
    
    private Group gridLines()
    {
        Group       gridLines   =new Group();
        double      minX        =content.get().parentToLocal(canvasPane.layoutBoundsProperty().get()).getMinX();
        double      maxX        =content.get().parentToLocal(canvasPane.layoutBoundsProperty().get()).getMaxX();
        double      minY        =content.get().parentToLocal(canvasPane.layoutBoundsProperty().get()).getMinY();
        double      maxY        =content.get().parentToLocal(canvasPane.layoutBoundsProperty().get()).getMaxY();
        double      screenDist/*  =content.get().localToScreen(new Point2D(this.gridDist.doubleValue(), this.gridDist.doubleValue())).getX()*/;
        Transform   t           =new Scale(1.0, 1.0);
        
        for(Transform nextT:this.content.get().getTransforms())
            t=t.createConcatenation(nextT);
        
        screenDist=this.gridDist.doubleValue()*t.getMxx();
//        if((this.gridDist.doubleValue()*Math.round(maxX/this.gridDist.doubleValue()))/this.gridDist.doubleValue()<=192)
        if(screenDist>=6.0)
        {
            for(double x=this.gridDist.doubleValue()*Math.round(minX/this.gridDist.doubleValue()); x<=this.gridDist.doubleValue()*Math.round(maxX/this.gridDist.doubleValue()); x+=this.gridDist.doubleValue())
            {
                Line    newLine =new Line(x,minY,x,maxY);

                if(Math.floorMod((long) x,this.gridDist.longValue()*this.gridMajor.longValue())!=0)
                    newLine.setStrokeWidth(0.5/t.getMxx());
                else
                    newLine.setStrokeWidth(1.0/t.getMxx());
                newLine.setStroke(Paint.valueOf("#88888888"));
                newLine.setVisible(true);
                gridLines.getChildren().add(newLine);
            }
            for(double y=this.gridDist.doubleValue()*Math.round(minY/this.gridDist.doubleValue()); y<=this.gridDist.doubleValue()*Math.round(maxY/this.gridDist.doubleValue()); y+=this.gridDist.doubleValue())
            {
                Line    newLine =new Line(minX, y, maxX, y);

                if(Math.floorMod((long) y,this.gridDist.longValue()*this.gridMajor.longValue())!=0)
                    newLine.setStrokeWidth(0.5/t.getMxx());
                else
                    newLine.setStrokeWidth(1.0/t.getMxx());
                newLine.setStroke(Paint.valueOf("#88888888"));
                newLine.setVisible(true);
                gridLines.getChildren().add(newLine);
            }

            for(Transform nextT:this.content.get().getTransforms())
            {

                gridLines.getTransforms().add(nextT);
            }
        }
        
        return gridLines;
    }
    
    private static void cleanParent(Parent p)
    {
        if(!(   p instanceof Group ||
                p instanceof Pane ))
            return;
        
        for(Node next:
                        p instanceof Group  ?((Group)p).getChildren():
                        p instanceof Pane   ?((Pane )p).getChildren():
                        new ArrayList<Node>()
            )
        {
            if(next instanceof Parent)
                cleanParent((Parent)next);
        }
        
        (   p instanceof Group  ?((Group)p).getChildren():
            p instanceof Pane   ?((Pane )p).getChildren():
                    new ArrayList<>()).clear();
    }
    
    public void showGridCursor(Point2D coords)
    {
        if(content.isNull().get())
            return;
//        Point2D canvasPos       =canvasPane.parentToLocal(event.getX(),event.getY());
        Point2D canvasPos       =canvasPane.parentToLocal(coords);
        Point2D contentPos      =content.get().parentToLocal(canvasPos);
        Point2D contentPosGrid  =new Point2D(   Math.floor(contentPos.getX()/gridDist.doubleValue())*gridDist.doubleValue(),
                                                Math.floor(contentPos.getY()/gridDist.doubleValue())*gridDist.doubleValue());
        Point2D canvasPosGrid   =content.get().localToParent(contentPosGrid);
        Point2D snapPos         =canvasPane.localToParent(canvasPosGrid);
        
        snapCursor.get().setTranslateX(snapPos.getX()-8);
        snapCursor.get().setTranslateY(snapPos.getY()-8);
        ((Rectangle)snapCursor.get()).setStroke(Paint.valueOf("#CA6A3488"));
        snapCursor.get().setVisible(true);
        
    }
}
