/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fxml;

import afester.javafx.svg.SvgLoader;
import com.autotrol.events.ControllerRequests;
import com.autotrol.FXcomponents.EditMarkTableCell;
import com.autotrol.FXcomponents.EditTableCell;
import com.autotrol.FXcomponents.ZoomScrollAnchor;
import com.autotrol.config.EnviromentManager;
import com.autotrol.powsybl.extensions.VisualHandlerModes;
import com.autotrol.powsybl.extensions.VoltageLevelHandler;
import com.autotrol.util.ComponentTypeMap;
import com.autotrol.util.NominalVoltageSubstationDiagramStyleProvider2;
import com.autotrol.util.SelectionManager;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.config.YamlModuleConfigRepository;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.export.Exporter;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.network.NetworkFactoryConstants;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.substationdiagram.SubstationDiagram;
import com.powsybl.substationdiagram.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionFree;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.SubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.VerticalSubstationLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ComponentTypeName;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.svg.DefaultNodeLabelConfiguration;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.NodeLabelConfiguration;
import com.powsybl.substationdiagram.svg.SubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyleProvider;
import com.rte_france.powsybl.hades2.Hades2Config;
import com.rte_france.powsybl.hades2.Hades2Executor;
//import com.rte_france.powsybl.hades2.Hades2Factory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yaml.snakeyaml.Yaml;

/**
 * FXML Controller class
 *
 * @author cgalli
 */
public class MainFXMLController implements Initializable, EventHandler {

    @FXML
    public                  BorderPane                                                  mainPane;
    @FXML
    public                  Pane                                                        centralClip;
    @FXML
    public                  ChoiceBox                                                   substationList;
    @FXML
    public                  Button                                                      addSubstation;
    @FXML
    public                  Button                                                      substationInfo;
    @FXML
    public                  Button                                                      selectCell;
    @FXML
    public                  ChoiceBox                                                   ssLOChoice;
    @FXML
    public                  ToolBar                                                     palletteTB;
    @FXML
    public                  ToggleButton                                                showGrid;
    @FXML
    public                  ToggleButton                                                enableMag;
    @FXML
    public                  ToggleButton                                                enableOsnap;
    @FXML
    public                  TableView                                                   propGrid;
    
    private final           Label                                                       label               =new Label(); 
    private final           SelectionManager                                            selMan              =new SelectionManager();
    
    public static           String                                                      COMPONENTLIB_PROP   ="compLib";
    public static           String                                                      LOPARAMETERS_PROP   ="loPars";
    public static           String                                                      STYLEPROVIDER_PROP  ="styleProv";
    public static           String                                                      NETWORK_PROP        ="network";
    public static           String                                                      SUBSTATIONS_PROP    ="substations";
    
    public final            SimpleObjectProperty<ComponentLibrary>                      compLib             =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<LayoutParameters>                      loPars              =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<SubstationDiagramStyleProvider>        styleProv           =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<SubstationDiagramInitialValueProvider> intialValProv       =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<NodeLabelConfiguration>                nodeLabelConf       =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<SubstationLayoutFactory>               ssLOFactory         =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<PositionVoltageLevelLayoutFactory>     vlLOFactory         =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<Network>                               network             =new SimpleObjectProperty<>();
    public final            SimpleObjectProperty<ArrayList<Substation>>                 substations         =new SimpleObjectProperty<>();
    
    private final           ExtensionFilter                                             netExtFilter        =new ExtensionFilter("Networks","*.zip","*.xml","*.xiidm");
    private final           FileChooser                                                 fileChooser         =new FileChooser();
    private                 ZoomScrollAnchor                                            centralScroll/*       =new ZoomScrollAnchor(showGrid.isSelected(), enableMag.isSelected(), enableOsnap.isSelected())*/;
    private static final    String[]                                                    skipInTB            =new String[]{
                                                                                                                                ComponentTypeName.ARROW,
                                                                                                                                ComponentTypeName.NODE
                                                                                                                            };
    private static final    SimpleLongProperty                                          messageTime         =new SimpleLongProperty(5000000000l);
//    private static final    ComponentType[]                                             skipInTB            =new ComponentType[]{
//                                                                                                                                ComponentType.ARROW,
//                                                                                                                                ComponentType.NODE
//                                                                                                                            };
    private static final    EnviromentManager                                           envMan              =new EnviromentManager();
//    private        final    Alert                                                       alert   =new Alert(Alert.AlertType.ERROR);
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void initialize(URL url, ResourceBundle rb)
    {
        ImageView   showGridGrp     =new ImageView(new Image(this.getClass().getResourceAsStream("/img/grid.png")));
        ImageView   enableMagGrp    =new ImageView(new Image(this.getClass().getResourceAsStream("/img/magnetic.png")));
        ImageView   enableOsnapGrp  =new ImageView(new Image(this.getClass().getResourceAsStream("/img/nudges_on.png")));
        ImageView   addSubstationGrp=new ImageView(new Image(this.getClass().getResourceAsStream("/img/icon_add_white.png")));
        Group       substInfoGrp    =new SvgLoader().loadSvg(this.getClass().getResourceAsStream("/img/icon_eye_white.svg"));
        Group       selCellGrp      =new SvgLoader().loadSvg(this.getClass().getResourceAsStream("/img/icon_cell_white.svg"));
        
        showGrid                    .setGraphic(showGridGrp);
        showGrid                    .setTooltip(new Tooltip("Grid"));
        enableMag                   .setGraphic(enableMagGrp);
        enableMag                   .setTooltip(new Tooltip("Snap"));
        enableOsnap                 .setGraphic(enableOsnapGrp);
        enableOsnap                 .setTooltip(new Tooltip("OSnap"));
        addSubstation               .setGraphic(addSubstationGrp);
        addSubstation               .setTooltip(new Tooltip("Add Substation"));
        substationInfo              .setGraphic(substInfoGrp);
        substationInfo              .setTooltip(new Tooltip("Substation Info"));
        selectCell                  .setGraphic(selCellGrp);
        selectCell                  .setTooltip(new Tooltip("Select Cell"));
        
        centralScroll               =new ZoomScrollAnchor(showGrid.isSelected(), enableMag.isSelected(), enableOsnap.isSelected());
        
        centralScroll.showGrid      .bind(showGrid.selectedProperty());
        centralScroll.enableMag     .bind(enableMag.selectedProperty());
        centralScroll.enableOsnap   .bind(enableOsnap.selectedProperty());
        centralScroll               .setId("centralScroll");
        
        addSubstation               .setOnAction(event->{newSubstationAction();});
        substationInfo              .setOnAction(event->{renderSubstationInfo();});
        
        compLib                     .set(new ResourcesComponentLibrary("/ConvergenceLibrary"));

        compLib                     .addListener((observable,  oldValue, newValue) ->   {
                                                                                            loadTB();
                                                                                            nodeLabelConf.set(new DefaultNodeLabelConfiguration(compLib.get()));
                                                                                        });
        loPars                      .addListener((observable,  oldValue, newValue) -> {try{redrawGraph();}catch(NullPointerException ex){}});
        
        network.addListener((observable,  oldValue, newValue) -> {
             try
             {
                 reloadSubstations();
             }
             catch(NullPointerException ex)
             {}
             catch(Exception ex1)//Any other exception should return to previous state
             {
                 network.set(oldValue);
             }
        });
        
        substationList.setTooltip(new Tooltip("Substations"));
        substationList.valueProperty().addListener((observable, oldValue, newValue) -> {
            try{
//                VisualHandlerModes  mode    =VoltageLevelHandler.getMode();
                selMan.recoverModes(SelectionManager.OPERATION_STORE_MODES);
                redrawGraph();
                selMan.recoverModes(SelectionManager.OPERATION_RESTORE_MODES);
            }
            catch(Exception ex)
            {
                Logger.getLogger(MainFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
//        substationList.valueProperty().addListener((observable, oldValue, newValue) -> {try{redrawGraph();}catch(NullPointerException ex){}});
        
        loPars.set(new LayoutParameters());
        loPars.get().setArrowDistance(24)
                    .setShowGrid(false)
//                    .setDiagramName()
//                    .setExternCellHeight(64)
//                    .setInternCellHeight(64)
//                    .setStackHeight(32)
//                    .setVerticalSubstationPadding(64)
//                    .setInitialYBus(64)
                    .setShowInternalNodes(true);
        
        styleProv.set(new NominalVoltageSubstationDiagramStyleProvider2());
        if(styleProv.get() instanceof NominalVoltageSubstationDiagramStyleProvider2)
            ((NominalVoltageSubstationDiagramStyleProvider2)styleProv.get()).setDark(true);
        
        substations.set(new ArrayList<>());
        
        fileChooser.getExtensionFilters().add(netExtFilter);
        
        centralClip.getChildren().add(centralScroll);
        
        loadTB();
        
        ((Button)palletteTB.getItems().filtered(node->{return "palletteTB_VOLTAGE_LEVEL".equals(node.getId());}).get(0)).setOnAction(event->{newVoltLevelAction();});
        
        propGrid.getSelectionModel().cellSelectionEnabledProperty().set(true);
        propGrid.setEditable(true);

        
        ((TableColumn<Entry<String,Object>,Object>)propGrid.getColumns().get(1)).setCellFactory((TableColumn<Entry<String, Object>, Object> column) -> new EditTableCell());
        ((TableColumn<Entry<String,Object>,Object>)propGrid.getColumns().get(2)).setCellFactory((TableColumn<Entry<String, Object>, Object> column) -> new EditMarkTableCell());
        ((TableColumn<Entry<String,Object>,String>)propGrid.getColumns().get(0)).setCellValueFactory(data->new SimpleStringProperty(data.getValue().getKey()));
        ((TableColumn<Entry<String,Object>,Object>)propGrid.getColumns().get(1)).setCellValueFactory(data->new SimpleObjectProperty(data.getValue().getValue()));
        ((TableColumn<Entry<String,Object>,Object>)propGrid.getColumns().get(2)).setCellValueFactory(data->new SimpleObjectProperty(data.getValue().getValue()));
        ((TableColumn<Entry<String,Object>,Object>)propGrid.getColumns().get(1)).setEditable(true);
        ((TableColumn<Entry<String,Object>,Object>)propGrid.getColumns().get(0)).setSortable(false);
        ((TableColumn<Entry<String,Object>,Object>)propGrid.getColumns().get(1)).setSortable(false);
        propGrid.setOnMouseClicked(event3-> {   if(event3.getClickCount()==2)
                                                 {
                                                    TablePosition focusedCell = propGrid.getFocusModel().getFocusedCell();
                                                    
                                                    propGrid.edit(focusedCell.getRow(), focusedCell.getTableColumn());
                                                }
                                            });
        
        ssLOChoice.getItems().addAll("Horizontal", "Vertical");
        ssLOChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)->{
            switch(newValue.toString())
            {
                case "Horizontal":
                    ssLOFactory.set(new HorizontalSubstationLayoutFactory());
                    redrawGraph();
                    break;
                case "Vertical":
                    ssLOFactory.set(new VerticalSubstationLayoutFactory());
                    redrawGraph();
                    break;
            }
        });
        ssLOChoice.getSelectionModel().select(0);
        ssLOChoice.setTooltip(new Tooltip("Layout"));
        
        vlLOFactory.set(new PositionVoltageLevelLayoutFactory(new PositionFree()));
        vlLOFactory.get().setRemoveUnnecessaryFictitiousNodes(true);
        vlLOFactory.get().setFeederStacked(true);
        vlLOFactory.get().setSubstituteSingularFictitiousByFeederNode(true);
        
        nodeLabelConf.set(new DefaultNodeLabelConfiguration(compLib.get()));
        
//        Stage   window  =(Stage)alert.getDialogPane().getScene().getWindow();
//        alert.getButtonTypes().add(ButtonType.CANCEL);
//        alert.getDialogPane().getStylesheets().add(centralScroll.getScene().getRoot().getStylesheets().get(0));
//        window.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));
//        alert.setTitle("Error");
//        alert.setHeaderText(null);

/*
    NORMAL,
    CONNECTION,
    COMPONENT_INSERTION,
    REGULATING_TERM_SEARCH,
    SWITCH_INSERTION,
    BUSBAR_SECTION_INSERTION
*/      
        BackgroundFill  fill=new BackgroundFill(Paint.valueOf("#FFFF8888"),CornerRadii.EMPTY, Insets.EMPTY);
        Background      bg=new Background(fill);
        label.setPadding(new Insets(2.0,4.0,2.0,4.0));
        label   .setMouseTransparent(true);
        label   .setBackground(bg);
        label   .setFont(Font.font(label.getFont().getFamily(), FontWeight.BOLD, label.getFont().getSize()));
        label   .setVisible(false);
        centralClip.getChildren().add(label);
        VoltageLevelHandler.getModeProperty().addListener((ObservableValue<? extends VisualHandlerModes> observable, VisualHandlerModes oldValue, VisualHandlerModes newValue) -> {
            switch(newValue)
            {
                case NORMAL:
                    label.setVisible(false);
                    break;
                default:
                    label.setText(newValue.name().replaceAll("_", " "));
                    label   .setTextFill(Color.BLACK);
                    label.setVisible(true);
                        ;
            }
        });
    }  
    
    private void loadTB()
    {
//        String[]        compTypes   =ComponentTypeMap.networkTypes.keySet();
        Set<String>     compTypes   =ComponentTypeMap.networkTypes.keySet();
//        ComponentType[] compTypes   =ComponentType.values();
        double          maxW        =48.0;
        double          maxH        =48.0;
        Group           vlBimg;
        
        palletteTB.getItems().clear();
        Button  vlB =new Button();
        vlB.setId("palletteTB_VOLTAGE_LEVEL");
        vlB.setText("V LV");
        vlB.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        vlBimg=(new SvgLoader().loadSvg(this.getClass().getResourceAsStream("/img/VoltLevels.svg")));
        vlB.setGraphic(vlBimg);
        vlB.setTooltip(new Tooltip("VOLT LEVEL"));
        palletteTB.getItems().add(vlB);
        
//        for(ComponentType next:compTypes)
        for(String next:compTypes)
        {
            if(Arrays.asList(skipInTB).contains(next))//Skip
                continue;
            try
            {
                Group   nextImg =loadComp(next);
                Button  nextB   =new Button();
                
                nextB.setId("palletteTB_"+next);
//                nextB.setId("palletteTB_"+next.name());
                
                if(nextImg!=null)
                {
                    nextB.setGraphic(nextImg);
                    nextB.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
                else
                {
                    nextB.setText(next.substring(0, 4));
//                    nextB.setText(next.name().substring(0, 4));
                    nextB.setContentDisplay(ContentDisplay.TEXT_ONLY);
                }
                
                nextB.setTooltip(new Tooltip(next.replaceAll("_"," ")));
//                nextB.setTooltip(new Tooltip(next.name().replaceAll("_"," ")));
                
                palletteTB.getItems().add(nextB);
                
                maxW=Math.max(nextB.getWidth(),maxW);                
            }
            catch (TransformerException | IOException ex)
            {
                Logger.getLogger(MainFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        for(Node nextN:palletteTB.getItems())
        {
            Button  nextB   =(Button) nextN;
            
            nextB.setPrefWidth(maxW);
            nextB.setMinWidth(maxW);
            nextB.setMaxWidth(maxW);
            nextB.setPrefHeight(maxH);
            nextB.setMinHeight(maxH);
            nextB.setMaxHeight(maxH);
            
            nextB.setOnAction(event->{triggerPalletteTBActions(event);});
        }
    }
    
    private void reloadSubstations() throws NullPointerException
    {
        Iterable<Substation>    substationsIt   =network.get().getSubstations();
        ArrayList<Substation>   substationsAr   =new ArrayList<>();
        ArrayList<String>       substationsNm   =new ArrayList<>();
        
        this.substations.get().clear();
        this.substationList.getItems().clear();
        
        for(Substation ss:substationsIt)
        {
            substationsAr.add(ss);
            substationsNm.add(ss.getName()+" ["+ss.getId()+"]");
        }
        
        this.substations.get().addAll(substationsAr);
        this.substationList.getItems().addAll(substationsNm);
        
        ((SingleSelectionModel)this.substationList.selectionModelProperty().get()).select(0);
        
    }
    
    private void redrawGraph() throws NullPointerException
    {
        StringWriter            sw          =new StringWriter();
        StringWriter            mw          =new StringWriter();
        Group                   view;
        GraphMetadata           metaData    =null;
        int                     index       =substationList.getSelectionModel().getSelectedIndex();
        Substation              ss          =substations.get().isEmpty()?null:(index>=0?substations.get().get(index):null);        
        SubstationDiagram       diagram     =ss!=null?SubstationDiagram.build(ss, ssLOFactory.get(), vlLOFactory.get(), true):null;
        
        System.gc();
        if(diagram!=null)
        {
            intialValProv.set(new DefaultSubstationDiagramInitialValueProvider(network.get()));
            if(centralScroll.getChildrenUnmodifiable()!=null)
                centralScroll.getChildrenUnmodifiable().forEach(child->{child.removeEventHandler(ControllerRequests.REDRAW_GRAPH, this);});
            
//            diagram.writeSvg(compLib.get(), loPars.get(), intialValProv.get(), styleProv.get(), sw, mw);
            diagram.writeSvg(compLib.get(), loPars.get(), intialValProv.get(), styleProv.get(), nodeLabelConf.get(), sw, mw);
            view    =(new SvgLoader()).loadSvg(new StringBufferInputStream(sw.getBuffer().toString()));
            metaData=GraphMetadata.parseJson(new StringBufferInputStream(mw.getBuffer().toString()));
            view.setStyle("-fx-background-color: rgba(0,0,0,0);");
            view.addEventHandler(ControllerRequests.REDRAW_GRAPH, this);
            if(ss!=null)
                this.loPars.get()   .setDiagramName(ss.getName()+"@"+network.getName());
        }
        else
        {
            view=new Group();
        }
        
        centralScroll.setContent(view);
        mainPane.setOnKeyReleased(this);
        
        selMan.SelectionManagerTgt.removeEventHandler(ControllerRequests.REDRAW_GRAPH, this);
        selMan.SelectionManagerTgt.removeEventHandler(ControllerRequests.SELECT_SUBSTATION, this);
        selMan.SelectionManagerTgt.removeEventHandler(ControllerRequests.SHOW_MESSAGE, this);
        
        if(diagram!=null && metaData!=null)
        {
            selMan.manage(view, metaData, network.get(), propGrid);
            selMan.SelectionManagerTgt.addEventHandler(ControllerRequests.REDRAW_GRAPH, this);
            selMan.SelectionManagerTgt.addEventHandler(ControllerRequests.SELECT_SUBSTATION, this);
            selMan.SelectionManagerTgt.addEventHandler(ControllerRequests.SHOW_MESSAGE, this);
        }
        else
        {
            selMan.manage(null, null, null, null);
        }
        
        System.gc();
        centralScroll.setMouseTransparent(false);
    }
    
    public void newNetAction(ActionEvent event)
    {
        if(selMan!=null)
            selMan.clearSelected();
        propGrid.getItems().clear();
        System.gc();
        createNet();
    }

    private void newSubstationAction()
    {
        if(network.isNull().get())
            return;
        if(selMan!=null)
            selMan.clearSelected();
        propGrid.getItems().clear();
        System.gc();
        createSubstation();
    }
    
    public void saveXmlAction()
    {
        File    select  =fileChooser.showSaveDialog(null);
        Path    path;
        
        if(select==null)
            return;
        
        path=select.toPath();
        saveXml(path);
    }
    
    @SuppressWarnings({"UseSpecificCatch", "Convert2Lambda"})
    public void runLoadFlowAction()
    {
        Service lfService   =new Service() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Network call() throws IOException, Exception {
//                        try
//                        {
                            PlatformConfig              pc                  =checkConfig();
                            ComputationManager          computationManager  =new LocalComputationManager(pc);
                            LoadFlowParameters          loadflowParameters  =LoadFlowParameters.load(pc).setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES)
                                                                                                        .setNoGeneratorReactiveLimits(true)
                                                                                                        .setPhaseShifterRegulationOn(false)
                                                                                                        .setTransformerVoltageControlOn(true)
                                                                                                        .setSpecificCompatibility(false)
                                                                                                        ;
                            Hades2Executor              hadesEx =new Hades2Executor(network.get(), computationManager, 0, Hades2Config.fromPlatformConfig(pc));
                            LoadFlowResult              result  =hadesEx.run(network.get().getVariantManager().getWorkingVariantId(), loadflowParameters).get();

                            if(!result.isOk())
                            {
                                throw(new Exception("Loadflow calculation failed!:\n"+result.getLogs()));
                            }
//                        }
//                        catch(Exception ex)
//                        {
//                            showAlert(ex);
//                        }
                        
                        return null;
                    }
                };
            }
        };
        lfService.setOnScheduled(event->{
            propGrid.getItems().clear();
            propGrid.getItems().add(new SimpleEntry<>(new Date(System.currentTimeMillis()).toString(), new ReadOnlyObjectWrapper("Load Flow cumputation started.")));
            centralScroll.setMouseTransparent(true);
        });
        
        lfService.setOnRunning(event->{
            propGrid.getItems().add(new SimpleEntry<>("Load Flow running...",""));
        });
        
        lfService.setOnSucceeded(event->{
            centralScroll.setMouseTransparent(false);
            redrawGraph();
            propGrid.getItems().setAll(new SimpleEntry<>(new Date(System.currentTimeMillis()).toString(), new ReadOnlyObjectWrapper("Load Flow computed successfuly.")));
        });
        
        lfService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event2) {
                //                propGrid.getItems().clear();
                propGrid.getItems().set(1, new SimpleEntry<>(new Date(System.currentTimeMillis()).toString(), new ReadOnlyObjectWrapper("Load Flow aborted with errors.")));
                centralScroll.setMouseTransparent(false);
                
                if(event2.getSource().exceptionProperty().isNotNull().get())
                    Platform.runLater(()->{showAlert(event2.getSource().getException());});
            }
        });
        
        VoltageLevelHandler.setMode(VisualHandlerModes.NORMAL);
        centralScroll.setCursor(Cursor.DEFAULT);
        
        lfService.start();
        
    }
    
    private PlatformConfig checkConfig() throws IOException
    {
        PlatformConfig              result;
        FileSystem                  fs          =FileSystems.getDefault();
        Path                        confDirPath =fs.getPath(System.getProperty("user.home"), ".itools", "hades2", "config");
        Path                        confYmlPath =fs.getPath(System.getProperty("user.home"), ".itools", "hades2", "config","config.yml");
        File                        confDirFile =confDirPath.toFile();
        File                        confYmlFile =confYmlPath.toFile();
        YamlModuleConfigRepository  repo;
        if(!confDirFile.exists())
            confDirFile.mkdirs();
        if(!confYmlFile.exists())
        {
            confYmlFile.createNewFile();
            InputStream         ymlStream       =this.getClass().getResourceAsStream("/config.yml");
            byte[]              buffer          =new byte[ymlStream.available()];

            try (FileOutputStream ymlFileStream = new FileOutputStream(confYmlFile))
            {
                ymlStream.read(buffer);
                ymlFileStream.write(buffer);
                ymlFileStream.flush();
                ymlFileStream.close();
            }

        }
        if(System.getProperty("powsybl.config.dirs")==null)
            System.setProperty("powsybl.config.dirs",confDirPath.toString());
        repo    =new YamlModuleConfigRepository(confYmlPath);
//        result  =new PlatformConfig(repo);
        result  =new PlatformConfig(repo, confDirPath);
//        PlatformConfig.setDefaultConfig(result);
        
//            fs.close();
        
        return result;
    }
    
    public void editHadesConfAction()
    {
        if(selMan!=null)
            selMan.clearSelected();
        propGrid.getItems().clear();
        System.gc();
        try
        {
            editConfig("hades2");
        }
        catch(Exception ex)
        {
            if(showAlert(ex))
                editHadesConfAction();
            
        }
    }
    
    private void editConfig(String modlName) throws FileNotFoundException, IOException, Exception
    {
        PlatformConfig                  cfg         =checkConfig();
        FileSystem                      fs          =FileSystems.getDefault();
        Path                            cfgFilePath =fs.getPath(cfg.getConfigDir().toString(), "config.yml");
        FileInputStream                 cfgStream   =new FileInputStream(cfgFilePath.toFile());
        Yaml                            yml         =new Yaml();
        Object                          _parsed     =yml.load(cfgStream);
        Map<String, Map<String,Object>> parsed;
        Button                          OKB         =new Button("Submit");
        Button                          CANCEL      =new Button("Cancel");
        
        if(!(_parsed instanceof Map))
            throw new Exception("Corrupted configuration file!");
        
        parsed=(Map<String, Map<String,Object>>)_parsed;
        
        parsed.get(modlName).forEach((String key, Object val) -> {propGrid.getItems().add(new SimpleEntry<>(key, new SimpleObjectProperty(val)));});
        
        propGrid.getItems().add(new SimpleEntry<>("Once finished..."    ,OKB));
        propGrid.getItems().add(new SimpleEntry<>("Cancel..."           ,CANCEL));
        
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                });
        OKB.setOnAction((ActionEvent event2)->{
                                                Map<String, Object> modlConfigs=parsed.get(modlName);
                                                ((ObservableList<SimpleEntry<String, SimpleObjectProperty>>)propGrid.getItems()).forEach((SimpleEntry<String, SimpleObjectProperty> item) -> {
                                                        if(item.getValue() instanceof SimpleObjectProperty)
                                                            modlConfigs.put(item.getKey(), item.getValue().get());
                                                });
                                                
                                                FileOutputStream    os  =null;
                                                String              newData;
                                                try
                                                {
                                                    os      =new FileOutputStream(cfgFilePath.toFile());
                                                    newData =yml.dumpAsMap(parsed);
                                                    os.write(newData.getBytes());
                                                    os.flush();
                                                    propGrid.getItems().clear();
                                                }
                                                catch (FileNotFoundException ex)
                                                {
                                                    showAlert(ex);
                                                } catch (IOException ex) {
                                                    showAlert(ex);
                                                }
                                                finally
                                                {
                                                    try
                                                    {
                                                        if(os!=null)
                                                            os.close();
                                                    }
                                                    catch (IOException | NullPointerException ex)
                                                    {
                                                        showAlert(ex);
                                                    }
                                                }

                                            });
        
    }
    
    public void selectCellAction()
    {
        if(selMan!=null)
            selMan.clearSelected();
        propGrid.getItems().clear();
        System.gc();
        
        selMan.triggerCellSelection();
//        selMan.triggerCellDuplication();
    }
    
    public void newVoltLevelAction()
    {
        if(selMan!=null)
            selMan.clearSelected();
        propGrid.getItems().clear();
        System.gc();
        createVoltageLevel();
    }
    
    public void renderSubstationInfo()
    {
        if(selMan==null)
            return;
        
        selMan.clearSelected();
        propGrid.getItems().clear();
        System.gc();
        
        selMan.renderSubstation(this.substationList.getSelectionModel().getSelectedIndex());
    }
    
    public void triggerPalletteTBActions(ActionEvent event)
    {
        if(!(event.getSource() instanceof Button))
            return;
        if(selMan!=null)
            selMan.clearSelected();
        propGrid.getItems().clear();
        System.gc();
        
        SnapshotParameters  pars        =new SnapshotParameters();
        Button              srcButton   =(Button)event.getSource();
        Node                graphic     =srcButton.getGraphic();
        String              srcButtonId =srcButton.getId();
        Image               srcImg;

        pars.setFill(Paint.valueOf("#ffffff33"));
        srcImg  =graphic!=null?graphic.snapshot(pars, null):srcButton.snapshot(pars, null);
        
        switch(srcButtonId)
        {
            case "palletteTB_NODE":
                //Do nothing
                break;
            case "palletteTB_BUSBAR_SECTION":
                selMan.selectVLForNewBusbar(new ImageCursor());
                break;
            case "palletteTB_BREAKER":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.BREAKER);
                break;
            case "palletteTB_DISCONNECTOR":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.DISCONNECTOR);
                break;
            case "palletteTB_LOAD_BREAK_SWITCH":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.LOAD_BREAK_SWITCH);
                break;
            case "palletteTB_DANGLING_LINE":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.DANGLING_LINE);
                break;
            case "palletteTB_INDUCTOR":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.INDUCTOR);
                break;
            case "palletteTB_CAPACITOR":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.CAPACITOR);
                break;
            case "palletteTB_LOAD":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.LOAD);
                break;
            case "palletteTB_LINE":
                Group       lineSvgGrp      =new SvgLoader().loadSvg(this.getClass().getResourceAsStream("/img/icon_line.svg"));
                srcImg=lineSvgGrp.snapshot(pars, null);
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.LINE);
                break;
            case "palletteTB_TWO_WINDINGS_TRANSFORMER":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.TWO_WINDINGS_TRANSFORMER);
                break;
            case "palletteTB_THREE_WINDINGS_TRANSFORMER":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.THREE_WINDINGS_TRANSFORMER);
                break;
            case "palletteTB_PHASE_SHIFT_TRANSFORMER":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.PHASE_SHIFT_TRANSFORMER);
                break;
            case "palletteTB_STATIC_VAR_COMPENSATOR":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.STATIC_VAR_COMPENSATOR);
                break;
            case "palletteTB_VSC_CONVERTER_STATION":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.VSC_CONVERTER_STATION);
                break;
            case "palletteTB_GENERATOR":
                selMan.triggerComponentInsert(new ImageCursor(srcImg, srcImg.getWidth()/2, srcImg.getHeight()/2), ComponentTypeName.GENERATOR);
                break;
            default:
                ;
        }
    }
    
    public void loadNetAction(ActionEvent event)
    {
        String  prevStr =(String)envMan.get(EnviromentManager.PROP_LAST_PATH);
        Path    prev    =prevStr!=null?Paths.get(prevStr):null;
        if(prev!=null)
            fileChooser.setInitialDirectory(prev.toFile());
        File    select  =fileChooser.showOpenDialog(null);
        Path    path;
        
        if(select==null)
            return;
        path=select.toPath();
        envMan.set(EnviromentManager.PROP_LAST_PATH, select.getParentFile().toPath().toString());
        try
        {
            envMan.writeEnv();
        }
        catch(IOException ex)
        {}
        
        loadNetwork(path);
    }
    
    public void set(String propName, Object value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        if(!getPropNames().contains(propName))
            return;
        if((this.getClass().getField(propName).getType()!=SimpleObjectProperty.class))
            return;
        
        ((SimpleObjectProperty)this.getClass().getField(propName).get(this)).setValue(value);
    }
    
    public Object get(String propName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        if(!getPropNames().contains(propName))
            return null;
        if(! (this.getClass().getField(propName).getType()!=SimpleObjectProperty.class))
            return null;
        
        return ((SimpleObjectProperty)this.getClass().getField(propName).get(this)).getValue();
    }
    
    private ArrayList<String> getPropNames()
    {
        Field[]             fields      =this.getClass().getFields();
        ArrayList<String>   fieldNames  =new ArrayList<>();
        
        for(Field next:fields)
            fieldNames.add(next.getName());
        
        return fieldNames;
    }
    
    private void loadNetwork(Path file)
    {
        Service<Network> networkService = new Service<Network>() {
            @Override
            protected Task<Network> createTask() {
                return new Task<Network>() {
                    @Override
                    protected Network call() {
                        Properties properties = new Properties();
                        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");
                        return Importers.loadNetwork(file, LocalComputationManager.getDefault(), new ImportConfig(), properties);
                    }
                };
            }
        };
        networkService.setOnRunning(event -> {
//            caseLoadingStatus.setStyle("-fx-background-color: yellow");
//            casePathTextField.setText(file.toAbsolutePath().toString());
//            this.centralScroll.setContent(new Label("Loading "+file.toString()));
            
            this.propGrid.getItems().clear();
            this.propGrid.getItems().add(new SimpleEntry<>("Loading...",new ReadOnlyObjectWrapper(file.toString())));
            centralScroll.setMouseTransparent(true);
        });
        networkService.setOnSucceeded(event -> {
            network.setValue((Network) event.getSource().getValue());
            centralScroll.setMouseTransparent(false);
//            initSubstationsTree();
//            caseLoadingStatus.setStyle("-fx-background-color: green");
//            preferences.put(CASE_PATH_PROPERTY, file.toAbsolutePath().toString());
        });
        networkService.setOnFailed(event -> {
//            Throwable exception = event.getSource().getException();
//            LOGGER.error(exception.toString(), exception);
//            casePathTextField.setText("");
//            caseLoadingStatus.setStyle("-fx-background-color: red");
            centralScroll.setMouseTransparent(false);
            this.propGrid.getItems().clear();
            Alert   alert=new Alert(AlertType.ERROR);
            Stage   window  =(Stage)alert.getDialogPane().getScene().getWindow();

            alert.getDialogPane().getStylesheets().add(this.mainPane.getParent().getStylesheets().get(0));
            window.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));
            
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No network found in "+file.toString());
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            
            alert.showAndWait();
        });
        networkService.start();
    }
    
    private void saveXml(Path file)
    {
        DataSource  dSrc    =Exporters.createDataSource(file);
        Exporter    exp     =Exporters.getExporter("XIIDM");
        Properties  params  =new Properties();
        
        //Properties fpor exporters (extracted from https://www.powsybl.org/docs/iidm/exporter/iidm.html)
        /*
        Configuration properties for XIIDM exporter

        These properties can be defined in the configuration file in the import-export-parameters-default-value module.
        iidm.export.xml.indent

        The iidm.export.xml.indent property is an optional property that defines if the XIIDM file generated by the XIIDM exporter will be indented or not. Its default value is true.
        iidm.export.xml.with-branch-state-variables

        The iidm.export.xml.with-branch-state-variables property is an optional property that defines if the network will be exported by the XIIDM exporter with branch states variables. Its default value is true.
        iidm.export.xml.only-main-cc

        The iidm.export.xml.only-main-cc property is an optional property that defines if the XIIDM exporter only exports the main CC of the network or not. Its default value is false.
        iidm.export.xml.anonymised

        The iidm.export.xml.anonymised property is an optional property that defines if the XIIDM exporter anonymises all equipments in the generated file or not. Its default value is false.
        iidm.export.xml.topology-level

        The iidm.export.xml.topology-level property is an optional property that defines if the most detailed topology in which the XIIDM exporter can export the network. Its default value is NODE_BREAKER.
        iidm.export.xml.throw-exception-if-extension-not-found

        The iidm.export.xml.throw-exception-if-extension-not-found property is an optional property that defines if the XIIDM exporter throws an exception if the network contains an unknown or unserializable extension or if it just ignores it. Its default value is false.
        iidm.export.xml.export-mode

        The iidm.export.xml.export-mode property is an optional property that defines the export mode of the XIIDM exporter. The export mode can be:

            IidmImportExportMode.UNIQUE_FILE: in case we want to export the network and its extensions in a unique file.

            IidmImportExportMode.EXTENSIONS_IN_ONE_SEPARATED_FILE: in case we want to export the network in a file and the extensions in a separate file. Example: if our network test has extensions then the network will be exported in the test.xiidm file when all its extensions will be exported in test-ext.xiidm file.

            IidmImportExportMode.ONE_SEPARATED_FILE_PER_EXTENSION_TYPE: in case we want to export network in a file and each extension type in a separate file. Example: if our network test has two extensions loadFoo and loadBar then the network will be exported in the test.xiidm file when loadFoo and loadBar will be exported respectively in test-loadFoo.xiidm and test-loadBar.xiidm.

        The default value for this parameter is IidmImportExportMode.NO_SEPARATED_FILE_FOR_EXTENSIONS.
        iidm.export.xml.extensions

        The iidm.export.xml.extensions property is an optional property that defines the list of extensions that we want to export by the XIIDM exporter. By default all extensions will be exported.
        
        */
        params.put("iidm.export.xml.export-mode", "UNIQUE_FILE");
        exp.export(network.get(), params, dSrc);
    }
    
//    private Group loadComp(ComponentType compType) throws TransformerConfigurationException, TransformerException, IOException
    private Group loadComp(String compType) throws TransformerConfigurationException, TransformerException, IOException
    {

        DOMImplementation       domImpl     =GenericDOMImplementation.getDOMImplementation();
        Document                document    =domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        StringBuilder           graphStyle  =new StringBuilder();
        SVGOMDocument           svgDocument =compLib.get().getSvgDocument(compType);
        
        if(svgDocument==null)
        {
            return null;
        }
        
        Element                 style/*       =svgDocument.createElement("style")*/;
        CDATASection            cd;
        DOMSource               source/*      =new DOMSource(svgDocument)*/;
        Transformer             transformer/* =TransformerFactory.newInstance().newTransformer(source)*/;
        StringWriter            writer      =new StringWriter();
        StringBufferInputStream is;
        StreamResult            result      =new StreamResult(writer);
        SvgLoader               loader      =new SvgLoader();
        
        //Style
        if(((NominalVoltageSubstationDiagramStyleProvider2)styleProv.get()).isDark())
            graphStyle.append(  "circle, rect, line, polyline, polygon, ellipse, path, tspan{fill:none; stroke:white; stroke-width:1;}\n"
                               +"text{fill:white; stroke:none;}");
        else
            graphStyle.append(  "circle, rect, line, polyline, polygon, ellipse, path, tspan{fill:none; stroke:black; stroke-width:1;}\n"
                               +"text{fill:black; stroke:none;}");
        
        style   =document.createElement("style");
        style.setAttribute("type", "text/css");
        cd      =document.createCDATASection(graphStyle.toString());
        style.appendChild(cd);

        document.adoptNode(style);
        document.getDocumentElement().appendChild(style);
        org.w3c.dom.Node    svgNode =document.importNode(svgDocument.getFirstChild(), true);
        
        svgNode.insertBefore(style, svgNode.getFirstChild());
        document.getDocumentElement().appendChild(svgNode);
        document.normalize();

        source      =new DOMSource(document);
        transformer =TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(source, result);
        writer.flush();

        is  =new StringBufferInputStream(writer.toString());
        return loader.loadSvg(is);
        
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void createNet()
    {
        ObservableList<Entry<String, Object>>   itemList    =propGrid.getItems().isEmpty()?FXCollections.observableArrayList():propGrid.getItems();
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        
        if(propGrid.getItems().isEmpty())
        {
            ChoiceBox   countryBox  =new ChoiceBox();
            
            countryBox.getItems().addAll((Object[])Country.values());
            itemList.add(new SimpleEntry<>("Network Id"                 ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("First Substation Country"   ,countryBox));countryBox.getSelectionModel().select(Country.AR);
            itemList.add(new SimpleEntry<>("First Substation TSO"       ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("First Substation Id"        ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("First Substation Name"      ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Once finished..."           ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."                  ,CANCEL));

            propGrid.setItems(itemList);
            propGrid.refresh();
        }
        
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                });
        OKB.setOnAction(event2->{
                                    try
                                    {
                                        Network     newNet  =NetworkFactory.findDefault().  createNetwork               (((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0).getValue().getValue(), NetworkFactoryConstants.DEFAULT);
                                        Substation  newSS   =newNet.newSubstation().        setCountry  (Country.valueOf(((ChoiceBox)((TableColumn<Entry<String,String>,Object>)propGrid.getColumns().get(1)).getCellObservableValue(1).getValue()).getSelectionModel().getSelectedItem().toString())).
                                                                                            setId                       ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(3).getValue().getValue())).
                                                                                            setTso                      ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(2).getValue().getValue())).
                                                                                            setName                     ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(4).getValue().getValue())).
                                                                                    add();
                                        
                                        network.    set(newNet);
                                        itemList.   clear();
                                        propGrid.   setItems(itemList);
                                        selMan.SelectionManagerTgt.removeEventHandler(ControllerRequests.SELECT_SUBSTATION, this);
                                        selMan.     manage(null, null, null, null);
                                        redrawGraph();
                                    }
                                    catch(Exception ex)
                                    {
                                        Alert   alert   =new Alert(AlertType.WARNING);
                                        Stage   window  =(Stage)alert.getDialogPane().getScene().getWindow();
                                        
                                        alert.getDialogPane().getStylesheets().add(this.mainPane.getParent().getStylesheets().get(0));
                                        alert.setHeaderText("");
                                        window.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));
                                        
                                        if(((ChoiceBox)((TableColumn<Entry<String,String>,Object>)propGrid.getColumns().get(1)).getCellObservableValue(1).getValue()).getSelectionModel().getSelectedItem()==null)
                                        {
                                            alert.setTitle("Warning");
                                            alert.setContentText("No Country Selected.");
                                        }
                                        else
                                        {
                                            alert.setContentText(ex.getLocalizedMessage());
                                        }
                                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                                        alert.showAndWait();
                                        createNet();
                                    }        
                                });
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void createSubstation()
    {
        ObservableList<Entry<String, Object>>   itemList    =propGrid.getItems().isEmpty()?FXCollections.observableArrayList():propGrid.getItems();
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        
        if(propGrid.getItems().isEmpty())
        {
            ChoiceBox   countryBox  =new ChoiceBox();
            
            countryBox.getItems().addAll((Object[])Country.values());
            itemList.add(new SimpleEntry<>("Substation Country" ,countryBox));countryBox.getSelectionModel().select(Country.AR);
            itemList.add(new SimpleEntry<>("Substation TSO"     ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Substation Id"      ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Substation Name"    ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Once finished..."   ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."          ,CANCEL));

            propGrid.setItems(itemList);
        }
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                });
        OKB.setOnAction(event2->{
                                    try
                                    {
                                        Network     net     =network.get();
                                        Substation  newSS   =net.   newSubstation().   setCountry  (Country.valueOf (((ChoiceBox)((TableColumn<Entry<String,String>,Object>)propGrid.getColumns().get(1)).getCellObservableValue(0).getValue()).getSelectionModel().getSelectedItem().toString())).
                                                                                       setId                        ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(2).getValue().getValue())).
                                                                                       setTso                       ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1).getValue().getValue())).
                                                                                       setName                      ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(3).getValue().getValue())).
                                                            add();
                                        itemList.   clear();
                                        propGrid.   setItems(itemList);
                                        selMan.SelectionManagerTgt.removeEventHandler(ControllerRequests.SELECT_SUBSTATION, this);
                                        selMan.manage(null, null, null, null);
                                        reloadSubstations();
                                        redrawGraph();
                                    }
                                    catch(Exception ex)
                                    {
                                        Alert   alert   =new Alert(AlertType.ERROR);
                                        Stage   window  =(Stage)alert.getDialogPane().getScene().getWindow();
                                        
                                        alert.setTitle("Error");
                                        alert.getDialogPane().getStylesheets().add(this.mainPane.getParent().getStylesheets().get(0));
                                        alert.setHeaderText("");
                                        window.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));
                                        
                                        if(((ChoiceBox)((TableColumn<Entry<String,String>,Object>)propGrid.getColumns().get(1)).getCellObservableValue(0).getValue()).getSelectionModel().getSelectedItem()==null)
                                        {
                                            alert.setContentText("No Country Selected.");
                                        }
                                        else
                                        {
                                            alert.setContentText(ex.getLocalizedMessage());
                                        }
                                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                                        alert.showAndWait();
                                        createSubstation();
                                    }        
                                });
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void createVoltageLevel()
    {
        ObservableList<Entry<String, Object>>   itemList    =propGrid.getItems().isEmpty()?FXCollections.observableArrayList():propGrid.getItems();
        Button                                  OKB         =new Button("Submit");
        Button                                  CANCEL      =new Button("Cancel");
        
        if(propGrid.getItems().isEmpty())
        {
            ChoiceBox   topoKindBox  =new ChoiceBox();
            
            topoKindBox.getItems().addAll((Object[])TopologyKind.values());
            
            itemList.add(new SimpleEntry<>("Voltage Level Id"           ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Voltage Level Name"         ,new SimpleStringProperty("")));
            itemList.add(new SimpleEntry<>("Voltage Level Topology Kind",topoKindBox));topoKindBox.getSelectionModel().select(TopologyKind.NODE_BREAKER);
            itemList.add(new SimpleEntry<>("Voltage Level Nominal V"    ,new SimpleDoubleProperty(0.0)));
            itemList.add(new SimpleEntry<>("Voltage Level Low Limit"    ,new SimpleDoubleProperty(0.0)));
            itemList.add(new SimpleEntry<>("Voltage Level High Limit"   ,new SimpleDoubleProperty(0.0)));
            itemList.add(new SimpleEntry<>("Once finished..."           ,OKB));
            itemList.add(new SimpleEntry<>("Cancel..."                  ,CANCEL));

            propGrid.setItems(itemList);
        }
        
        CANCEL.setOnAction(event3-> {
                                    propGrid.getItems().clear();
                                });
        OKB.setOnAction(event2->{
            try
            {
                Substation  ss=substations.get().get(this.substationList.getSelectionModel().getSelectedIndex());
                if(ss==null)
                    return;
                ss.newVoltageLevel().   setId               ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(0).getValue().getValue())).
                                        setName             ((((TableColumn<Entry<String,String>,SimpleStringProperty>)propGrid.getColumns().get(1)).getCellObservableValue(1).getValue().getValue())).
                                        setTopologyKind     (TopologyKind.valueOf (((ChoiceBox)((TableColumn<Entry<String,String>,Object>)propGrid.getColumns().get(1)).getCellObservableValue(2).getValue()).getSelectionModel().getSelectedItem().toString())).
                                        setNominalV         ((((TableColumn<Entry<String,Double>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(3).getValue().getValue())).
                                        setLowVoltageLimit  ((((TableColumn<Entry<String,Double>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(4).getValue().getValue())).
                                        setHighVoltageLimit ((((TableColumn<Entry<String,Double>,SimpleDoubleProperty>)propGrid.getColumns().get(1)).getCellObservableValue(5).getValue().getValue())).
                                    add();
                                    itemList.   clear();
                                    propGrid.   setItems(itemList);
                                    selMan.SelectionManagerTgt.removeEventHandler(ControllerRequests.SELECT_SUBSTATION, this);
                                    selMan.     manage(null, null, null, null);
                                    redrawGraph();
            }
            catch(Exception ex)
            {
                Alert   alert   =new Alert(AlertType.ERROR);
                Stage   window  =(Stage)alert.getDialogPane().getScene().getWindow();

                alert.setTitle("Error");
                alert.getDialogPane().getStylesheets().add(this.mainPane.getParent().getStylesheets().get(0));
                alert.setHeaderText("");
                window.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));

                if(ex.getCause()!=null)
                {
                    alert.setContentText(ex.getCause().getLocalizedMessage());
                }
                else
                {
                    alert.setContentText(ex.getLocalizedMessage());
                }
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.showAndWait();
                createVoltageLevel();
            }
        });
    }

    @Override
    public void handle(Event event)
    {
        if(event.getEventType().equals(ControllerRequests.REDRAW_GRAPH))
        {
            redrawGraph();
            event.consume();
        }
        if(event.getEventType().equals(ControllerRequests.SELECT_SUBSTATION))
        {
            Substation  nextSS      =event.getSource() instanceof Substation?(Substation)event.getSource():event.getSource() instanceof Node?((Substation)(((Node)event.getSource()).getUserData())):null;
            String      visibleName =nextSS!=null?nextSS.getName()+" ["+nextSS.getId()+"]":null;
            
            this.substationList.getSelectionModel().select(visibleName);
            
            event.consume();
        }
        if(event.getEventType().equals(ControllerRequests.SHOW_MESSAGE))
        {
            String  message =event.getSource() instanceof String?(String)event.getSource():event.getSource() instanceof Node?((String)(((Node)event.getSource()).getUserData())):null;
            
            if(message!=null)
                showMessage(message);
            
            event.consume();
        }
        else if(event.getEventType().equals(KeyEvent.KEY_RELEASED))
        {
            KeyEvent    kEv     =(KeyEvent) event;
            Node        focus   =mainPane.getScene().getFocusOwner();
            
            switch(kEv.getCode())
            {
                case D:
                    if(focus instanceof TextField || focus instanceof ComboBox)
                        return;
                    if(kEv.isControlDown())
                        if(selMan!=null && network.isNotNull().get())
                        {
                            selMan.triggerCellDuplication();
                        }
                    break;
                case BACK_SPACE:
                case DELETE:
                    if(kEv.isControlDown())
                        break;
                    if(focus instanceof TextField || focus instanceof ComboBox)
                        return;
                    
                    selMan.removeSelected();
                    event.consume();
                    
                    break;
            }
        }
    }
    
    private boolean showAlert(Throwable ex)
    {
        @SuppressWarnings("ThrowableResultIgnored")
        TextArea    text        =new TextArea(ex.getCause()!=null?ex.getCause().getLocalizedMessage():ex.getLocalizedMessage());
        Alert       alert       =new Alert(Alert.AlertType.ERROR);
        Stage       window      =(Stage)alert.getDialogPane().getScene().getWindow();
        
        text.setEditable(false);
        text.setMaxWidth(Double.MAX_VALUE);
        text.setMaxHeight(Double.MAX_VALUE);
        
        alert.setAlertType(AlertType.ERROR);
        alert.setResizable(true);
        
        if(text.getText().length()<=256)
            alert.setContentText(text.getText());
        else
            alert.getDialogPane().setContent(text);
        
        alert.getButtonTypes().add(ButtonType.CANCEL);
        window.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));
        alert.setTitle("Error");
        alert.setHeaderText(null);
        
        alert.getDialogPane().getStylesheets().add(propGrid.getParent().getScene().getRoot().getStylesheets().get(0));
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

//        alert.show();
        alert.showAndWait();
        
        return alert.getResult().equals(ButtonType.OK);
    }
    
    public void showMessage(String message)
    {
        long            startTime   =System.nanoTime();
        AnimationTimer  timer       =new AnimationTimer() {
            @Override
            public void handle(long now) {
                if(now-startTime>=messageTime.getValue())
                {
                    label.setVisible(false);
                    this.stop();
                }
            }
        };
        label.setText(message);
        label.setTextFill(Color.BLACK);
        label.setVisible(true);
     
        timer.start();
        
    }
}
