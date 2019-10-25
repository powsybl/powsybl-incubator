/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.FXcomponents;

import com.sun.javafx.scene.control.skin.TableHeaderRow;
import java.util.Map;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 *
 * @author cgalli
 */
public class EditMarkTableCell extends TableCell<Map.Entry<String, Object>, Object>
{
    @Override
    protected void updateItem(Object item, boolean empty)
    {
        int colIndex    =this.getTableView().getColumns().indexOf(this.getTableColumn());
        
        if(colIndex<1)
            return;
        
        this.setMouseTransparent(true);
        
        this.setContentDisplay(ContentDisplay.CENTER);
        this.setText(null);
        
//        TableHeaderRow   x;
        
        for(Node next:this.getTableView().getChildrenUnmodifiable().filtered((Node t) -> (t instanceof TableHeaderRow)))
        {
            ((TableHeaderRow)next).getColumnHeaderFor(this.getTableColumn()).setMouseTransparent(true);
        }
        
//        this.getTableColumn().get

//        if(((TableColumn)this.getTableView().getColumns().get(colIndex-1)).isEditable())
        if(item!=null?!(item instanceof ReadOnlyObjectWrapper) && (item instanceof Node?!((Node)item).isMouseTransparent():true):false)
        {
            Image       editIcon    =new Image(this.getClass().getResourceAsStream("/img/edit.png"));
            ImageView   imgNode     =new ImageView(editIcon);
//            AnchorPane  container   =new AnchorPane();
//            
//            container.getChildren().add(imgNode);
//            
//            AnchorPane.setBottomAnchor  (imgNode, 0.0);
//            AnchorPane.setTopAnchor     (imgNode, 0.0);
//            AnchorPane.setLeftAnchor    (imgNode, 0.0);
//            AnchorPane.setRightAnchor   (imgNode, 0.0);
            
//            this.setGraphic(container);
            this.setGraphic(imgNode);
        }
        else
            this.setGraphic(null);
        
        
    }
}
