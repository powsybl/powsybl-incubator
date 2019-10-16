/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.powsybl.extensions;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

/**
 *
 * @author cgalli
 */
public class PredefinedCursors{
    
    public static final Cursor  normalCursor    =Cursor.CROSSHAIR;
    public static final Cursor  connPermision   =new ImageCursor(new Image(PredefinedCursors.class.getResourceAsStream("/img/icon_Permission.png")),32.5,32.5);
    public static final Cursor  connBlocking    =new ImageCursor(new Image(PredefinedCursors.class.getResourceAsStream("/img/icon_Bloxking.png")),32.5,32.5);
}
