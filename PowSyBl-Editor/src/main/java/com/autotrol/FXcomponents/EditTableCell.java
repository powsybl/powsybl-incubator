package com.autotrol.FXcomponents;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

/**
 *
 * @author cgalli
 */
public class EditTableCell extends TableCell<Entry<String, Object>, Object>
{
    @Override
    protected void updateItem(Object item, boolean empty) {
        
        try
        {
            super.updateItem(item, empty);
        }catch(Exception ex)
        {}

        if (item != null) {

            this.setPadding(Insets.EMPTY);
            
            if(updateTextField(item))
            {
                this.setTooltip(new Tooltip(this.getText()));
            }
            else if(updateCheckBox(item))
            {
                this.setTooltip(null);
            }
            else if (item instanceof Image) {
                setText(null);
                ImageView imageView = new ImageView((Image) item);
                imageView.setFitWidth(100);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                setGraphic(imageView);
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                this.setTooltip(null);
            }
            else if(item instanceof Node)
            {
                setGraphic((Node) item);
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                this.setTooltip(null);
            }   
            else {
                setText("N/A");
                setGraphic(null);
                this.setTooltip(null);
            }
        }
        else {
            setText(null);
            setGraphic(null);
            this.setTooltip(null);
        }
    }
    
    @Override
    public void startEdit()
    {
        super.startEdit();

        if(!this.isEditable())
            return;
        
        Node        g   =this.getGraphic();
        TextField   tf  =g instanceof TextField?(TextField)g:null;
        
        if(tf==null)
            return;
        
        updateTextField(getTableView().getItems().get(this.getIndex()).getKey()==this.getTableColumn().getCellData(this.getIndex())?getTableView().getItems().get(this.getIndex()).getKey():getTableView().getItems().get(this.getIndex()).getValue());
        this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
    }

    @Override
    public void commitEdit(Object item)
    {
        super.commitEdit(item);

        Node        g   =this.getGraphic();
        TextField   tf  =g instanceof TextField?(TextField)g:null;
        
        if(tf==null)
            return;
        
        this.setContentDisplay(ContentDisplay.TEXT_ONLY);
        
        Object  rowKey  =getTableView().getItems().get(this.getIndex()).getKey();
        Object  rowVal  =getTableView().getItems().get(this.getIndex()).getValue();
        Object  cellVal =this.getTableColumn().getCellData(this.getIndex());
        
        if(cellVal==rowKey)
            getTableView().getItems().set(this.getIndex(), new SimpleEntry<>(String.valueOf(item),getTableView().getItems().get(this.getIndex()).getValue()));
        if(rowVal==cellVal)
        {
            if(item instanceof Property)
                ((Property)item).setValue(tf.getTextFormatter().getValue());
            else
                item=tf.getTextFormatter().getValue();
            
            getTableView().getItems().get(this.getIndex()).setValue(item);
        }
        updateItem(item, false);
    }
    
    @Override
    public void cancelEdit()
    {
        super.cancelEdit();

        Node        g   =this.getGraphic();
        TextField   tf  =g instanceof TextField?(TextField)g:null;
        
        if(tf==null)
            return;
        
        this.setContentDisplay(ContentDisplay.TEXT_ONLY);
        this.setItem(getTableView().getItems().get(this.getIndex()));
    }
    
    @SuppressWarnings("null")
    private boolean updateTextField(Object item)
    {
        Property    _wrapper    =item instanceof Property?(Property)item:(item instanceof String || item instanceof Double || item instanceof Integer || item instanceof Float)?new SimpleStringProperty(String.valueOf(item)):null;
        TextField   tf          =null;
        
        if(_wrapper!=null?_wrapper.getValue() instanceof String || _wrapper.getValue() instanceof Double || _wrapper.getValue() instanceof Integer || _wrapper.getValue() instanceof Float:false)
        {
            this.setEditable(!(_wrapper instanceof ReadOnlyObjectWrapper));
            
            if(this.isEditable())
            {
                if(!(this.getGraphic()instanceof TextField))
                {
                    tf      =new TextField(_wrapper.getValue() instanceof String?(String)_wrapper.getValue():String.valueOf(_wrapper.getValue()));
                    setGraphic(tf);
                    tf.setOnKeyReleased(event->{
                                                switch(event.getCode())
                                                {
                                                    case ENTER:
                                                        commitEdit(this.getItem());                                                        
                                                        break;
                                                    case ESCAPE:
                                                        cancelEdit();
                                                        break;
                                                }
                                            }
                    );
                    tf.setTextFormatter(new TextFormatter(new StringConverter() {
                        @Override
                        public String toString(Object object) {
                            String result   =object==null?"":object.toString();

                            result=result.endsWith(".0")?object.toString().substring(0, result.length()-2):result;

                            return String.valueOf(result);
                        }

                        @Override
                        public Object fromString(String string) {
                            return  ((Property) item).getValue() instanceof Integer?Integer.valueOf (string):
                                    ((Property) item).getValue() instanceof Double? Double.valueOf  (string):
                                    ((Property) item).getValue() instanceof Float?  Float.valueOf   (string):
                                    string;
                        }
                    }));
                }
                else
                {
                    tf  =((TextField)this.getGraphic());
                }
            }
            
            if(isEditing())
            {
                try
                {
                    if(tf!=null)
                        tf.setText(String.valueOf(_wrapper.getValue()));
                }
                catch(Exception ex)
                {
                    System.out.print("Something wrong resetting text");
                }
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            else
            {
                setText(String.valueOf(item instanceof Property?((Property)item).getValue():item));
                this.setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
            return true;
        }
        return false;
    }
    
    @SuppressWarnings("null")
    private boolean updateCheckBox(Object item)
    {
        Property    _wrapper    =item instanceof Property?(Property)item:((item instanceof Boolean))?new SimpleBooleanProperty((boolean)item):null;
        CheckBox    checkBox;
        
        if(_wrapper!=null?_wrapper.getValue() instanceof Boolean:false)
        {
            this.setEditable(!(_wrapper instanceof ReadOnlyObjectWrapper));
            
            if(!(this.getGraphic() instanceof CheckBox))
            {
                checkBox=new CheckBox();
                checkBox.mouseTransparentProperty().set(!this.isEditable());
                this.setGraphic(checkBox);
                this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            else
            {
                checkBox=(CheckBox)this.getGraphic();
            }
            
            checkBox.setSelected((boolean) _wrapper.getValue());
            if(checkBox.selectedProperty().isBound())
            {
                checkBox.selectedProperty().unbindBidirectional(_wrapper);
            }
            checkBox.selectedProperty().bindBidirectional(_wrapper);
            
            return true;
        }
        
        return false;
    }

}
