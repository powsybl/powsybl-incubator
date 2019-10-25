/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol;

import fxml.MainFXMLController;
import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 *
 * @author cgalli
 */
public class EditorFXMain extends Application /*implements Initializable */{
    
//    private final   ObjectProperty<Network> networkProperty =new SimpleObjectProperty<>();
    private         MainFXMLController      controller;
    
//    @FXML
//    private BorderPane  viewPane;
    
    @Override
    public void start(Stage primaryStage) throws IOException {
//        Button btn = new Button();
//        btn.setText("Say 'Hello World'");
//        btn.setOnAction(new EventHandler<ActionEvent>() {
//            
//            @Override
//            public void handle(ActionEvent event) {
//                System.out.println("Hello World!");
//            }
//        });
//        
//        StackPane root = new StackPane();
//        root.getChildren().add(btn);
//        
//        Scene scene = new Scene(root, 300, 250);
//        
//        primaryStage.setTitle("Hello World!");
//        primaryStage.setScene(scene);
//        primaryStage.show();

        URL                                             url         =getClass().getResource("/fxml/mainFXML.fxml");
        FXMLLoader                                      loader      =new FXMLLoader(url);
        Parent                                          root        =loader.load();
        Scene                                           scene       =new Scene(root);
//        Path                                            nwkPath     =new File("/mnt/DATOS/Autotrol/Desarrollo/powsybl/powsybl-tutorials/cgmes/target/classes/MicroGridTestConfiguration_T4_NL_BB_Complete_v2.zip").toPath();
//        
//        loadNetwork(nwkPath);
//        
//        try
//        {
//            controller.set(MainFXMLController.NETWORK_PROP      , networkProperty.get());
//        }
//        catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex)
//        {
//            Logger.getLogger(EditorFXMain.class.getName()).log(Level.SEVERE, null, ex);
//        }
        controller=loader.getController();

        primaryStage.setScene(scene);
        primaryStage.setTitle("Network editor");
        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/tower.png")));
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
//    private void loadNetwork(Path file) {
////        Service<Network> networkService = new Service<Network>() {
////            @Override
////            protected Task<Network> createTask() {
////                return new Task<Network>() {
////                    @Override
////                    protected Network call() {
////                        Properties properties = new Properties();
////                        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");
////                        return Importers.loadNetwork(file, LocalComputationManager.getDefault(), new ImportConfig(), properties);
////                    }
////                };
////            }
////        };
////        networkService.setOnRunning(event -> {
//////            caseLoadingStatus.setStyle("-fx-background-color: yellow");
//////            casePathTextField.setText(file.toAbsolutePath().toString());
////        });
////        networkService.setOnSucceeded(event -> {
////            networkProperty.setValue((Network) event.getSource().getValue());
//////            initSubstationsTree();
//////            caseLoadingStatus.setStyle("-fx-background-color: green");
//////            preferences.put(CASE_PATH_PROPERTY, file.toAbsolutePath().toString());
////        });
////        networkService.setOnFailed(event -> {
////            Throwable exception = event.getSource().getException();
//////            LOGGER.error(exception.toString(), exception);
//////            casePathTextField.setText("");
//////            caseLoadingStatus.setStyle("-fx-background-color: red");
////        });
////        networkService.start();
//    
//        Properties properties = new Properties();
//        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");
//        networkProperty.set(Importers.loadNetwork(file, LocalComputationManager.getDefault(), new ImportConfig(), properties));
//    }

//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
////        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

}
