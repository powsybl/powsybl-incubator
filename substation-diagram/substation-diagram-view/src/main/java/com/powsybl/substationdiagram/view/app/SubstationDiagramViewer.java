package com.powsybl.substationdiagram.view.app;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.SubstationDiagram;
import com.powsybl.substationdiagram.layout.ImplicitCellDetector;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionFree;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.RandomVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.view.NavigationListener;
import com.powsybl.substationdiagram.view.NetworkStyleHandler;
import com.powsybl.substationdiagram.view.SubstationDiagramView;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SubstationDiagramViewer extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationDiagramViewer.class);

    private  static final String SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY = "selectedVoltageLevelIds";
    private static final String CASE_PATH_PROPERTY = "casePath";

    private final Map<String, VoltageLevelLayoutFactory> layouts
            = ImmutableMap.of("Auto extensions", new PositionVoltageLevelLayoutFactory(),
                              "Auto without extensions", new PositionVoltageLevelLayoutFactory(new ImplicitCellDetector(), new PositionFree()),
                              "Random", new RandomVoltageLevelLayoutFactory(500, 500));

    private final ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");

    private final ObservableList<SelectableVoltageLevel> selectableVoltageLevels = FXCollections.observableArrayList();
    private final FilteredList<SelectableVoltageLevel> filteredSelectableVoltageLevels = new FilteredList<>(selectableVoltageLevels, s -> true);
    private final ListView<SelectableVoltageLevel> voltageLevelList = new ListView<>(filteredSelectableVoltageLevels);
    private final TextField filterInput = new TextField();

    private final Button caseLoadingStatus = new Button("  ");
    private final TextField casePathTextField = new TextField();
    private final BorderPane selectedDiagramPane = new BorderPane();
    private final TabPane checkedDiagramsPane = new TabPane();
    private GridPane parametersPane;

    private final ObjectProperty<Network> networkProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<LayoutParameters> layoutParameters = new SimpleObjectProperty<>(new LayoutParameters()
            .setShowGrid(true));

    private final Preferences preferences = Preferences.userNodeForPackage(SubstationDiagramView.class);

    private final ObjectMapper objectMapper = JsonUtil.createObjectMapper();

    private final ComboBox<String> layoutComboBox = new ComboBox<>();

    private class SubstationDiagramPane extends FlowPane {

        private final ChangeListener<LayoutParameters> listener;

        SubstationDiagramPane(VoltageLevel vl) {
            listener = (observable, oldValue, newValue) -> loadDiagram(vl);
            layoutParameters.addListener(new WeakChangeListener<>(listener));
            loadDiagram(vl);
        }

        private SubstationDiagramView createSubstationDiagramView(VoltageLevel vl) {
            String svgData;
            String metadataData;
            try (StringWriter svgWriter = new StringWriter();
                 StringWriter metadataWriter = new StringWriter()) {
                SubstationDiagram diagram = SubstationDiagram.build(vl, getLayoutFactory());
                diagram.writeSvg(componentLibrary, layoutParameters.get(), svgWriter, metadataWriter, null);
                svgWriter.flush();
                metadataWriter.flush();
                svgData = svgWriter.toString();
                metadataData = metadataWriter.toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            SubstationDiagramView diagramView;
            try (InputStream svgInputStream = new ByteArrayInputStream(svgData.getBytes(StandardCharsets.UTF_8));
                 InputStream metadataInputStream = new ByteArrayInputStream(metadataData.getBytes(StandardCharsets.UTF_8))) {
                diagramView = SubstationDiagramView.load(svgInputStream, metadataInputStream, new NavigationListener() {
                    @Override
                    public void onNavigationEvent(String id, ComponentType type) {
                        Objects.requireNonNull(id);
                        Objects.requireNonNull(type);
                        String strippedId = id.substring(0, id.length() - 4);
                        Side side = id.endsWith(Side.ONE.toString()) ? Side.ONE : Side.TWO;
                        VoltageLevel vl = getNextVoltageLevel(strippedId, type, side);
                        filterInput.clear();
                        loadDiagram(vl);
                        for (int i = 0; i < voltageLevelList.getItems().size(); i++) {
                            SelectableVoltageLevel svl = voltageLevelList.getItems().get(i);
                            if (svl.getId().equals(vl.getId())) {
                                voltageLevelList.getSelectionModel().select(i);
                                voltageLevelList.getFocusModel().focus(i);
                                voltageLevelList.scrollTo(i);
                            }
                        }
                        voltageLevelList.refresh();
                    }

                    @Override
                    public String getDestination(String id, ComponentType type) {
                        Objects.requireNonNull(id);
                        Objects.requireNonNull(type);
                        String strippedId = id.substring(0, id.length() - 4);
                        Side side = id.endsWith(Side.ONE.toString()) ? Side.ONE : Side.TWO;
                        VoltageLevel vl = getNextVoltageLevel(strippedId, type, side);
                        return vl != null ? vl.getId() : null;
                    }

                    private VoltageLevel getNextVoltageLevel(String id, ComponentType type, Side side) {
                        VoltageLevel nextvl = null;
                        switch (type) {
                            case LINE: {
                                Branch line = networkProperty.get().getLine(id);
                                if (side.equals(Side.ONE)) {
                                    nextvl = line.getTerminal2().getVoltageLevel();
                                } else {
                                    nextvl = line.getTerminal1().getVoltageLevel();
                                }
                                break;
                            }
                            case TWO_WINDINGS_TRANSFORMER: {
                                TwoWindingsTransformer twt = networkProperty.get().getTwoWindingsTransformer(id);

                                if (side.equals(Side.ONE)) {
                                    nextvl = twt.getTerminal2().getVoltageLevel();
                                } else {
                                    nextvl = twt.getTerminal1().getVoltageLevel();
                                }
                                break;
                            }
                        }
                        return nextvl;
                    }
                }, new NetworkStyleHandler(networkProperty.get()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return diagramView;
        }

        private void loadDiagram(VoltageLevel vl) {
            Service<SubstationDiagramView> loader = new Service<SubstationDiagramView>() {
                @Override
                protected Task<SubstationDiagramView> createTask() {
                    return new Task<SubstationDiagramView>() {
                        @Override
                        protected SubstationDiagramView call() {
                            return createSubstationDiagramView(vl);
                        }
                    };
                }
            };
            loader.setOnScheduled(event -> {
                Text loading = new Text("Loading...");
                loading.setFont(Font.font(30));
                getChildren().setAll(loading);
            });
            loader.setOnSucceeded(event -> {
                SubstationDiagramView view = (SubstationDiagramView) event.getSource().getValue();
                getChildren().setAll(view);
            });
            loader.setOnFailed(event -> {
                Throwable e = event.getSource().getException();
                LOGGER.error(e.toString(), e);
            });
            loader.start();
        }
    }

    private class SelectableVoltageLevel {

        private final String id;

        private final BooleanProperty checkedProperty = new SimpleBooleanProperty();

        SelectableVoltageLevel(String id) {
            this.id = id;
            checkedProperty.addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    addDiagramTab();
                } else {
                    removeDiagramTab();
                }
                saveSelectedDiagrams();
            });
        }

        private void removeDiagramTab() {
            checkedDiagramsPane.getTabs().removeIf(tab -> tab.getText().equals(id));
        }

        private void addDiagramTab() {
            VoltageLevel vl = networkProperty.get().getVoltageLevel(id);
            if (vl != null) {
                Tab tab = new Tab(id, new SubstationDiagramPane(vl));
                tab.setOnCloseRequest(event -> checkedProperty.set(false));
                checkedDiagramsPane.getTabs().add(tab);
                checkedDiagramsPane.getSelectionModel().select(tab);
            } else {
                LOGGER.warn("Voltage level {} not found", id);
            }
        }

        String getId() {
            return id;
        }

        BooleanProperty checkedProperty() {
            return checkedProperty;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private VoltageLevelLayoutFactory getLayoutFactory() {
        String selectedItem = layoutComboBox.getSelectionModel().getSelectedItem();
        return layouts.get(selectedItem);
    }

    private void setParameters(LayoutParameters layoutParameters) {
        this.layoutParameters.set(new LayoutParameters(layoutParameters));
    }

    private void addSpinner(String label, double min, double max, double amountToStepBy, int row,
                            Function<LayoutParameters, Double> initializer,
                            BiFunction<LayoutParameters, Double, LayoutParameters> updater) {
        Spinner<Double> spinner = new Spinner<>(min, max, initializer.apply(layoutParameters.get()), amountToStepBy);
        spinner.setEditable(true);
        spinner.valueProperty().addListener((observable, oldValue, newValue) -> setParameters(updater.apply(layoutParameters.get(), newValue)));
        parametersPane.add(new Label(label), 0, row);
        parametersPane.add(spinner, 0, row + 1);
    }

    private void addCheckBox(String label, int row,
                             Function<LayoutParameters, Boolean> initializer,
                             BiFunction<LayoutParameters, Boolean, LayoutParameters> updater) {
        CheckBox cb = new CheckBox(label);
        cb.setSelected(initializer.apply(layoutParameters.get()));
        cb.selectedProperty().addListener((observable, oldValue, newValue) -> setParameters(updater.apply(layoutParameters.get(), newValue)));
        parametersPane.add(cb, 0, row);
    }

    private void createParametersPane() {
        parametersPane = new GridPane();
        parametersPane.setHgap(5);
        parametersPane.setVgap(5);
        parametersPane.setPadding(new Insets(5, 5, 5, 5));

        // layout list
        layoutComboBox.getItems().addAll(layouts.keySet());
        layoutComboBox.getSelectionModel().selectFirst();
        layoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Layout:"), 0, 0);
        parametersPane.add(layoutComboBox, 0, 1);

        addSpinner("Initial busbar X:", 0, 100, 5, 2, LayoutParameters::getInitialYBus, LayoutParameters::setInitialXBus);
        addSpinner("Initial busbar Y:", 0, 500, 5, 4, LayoutParameters::getInitialYBus, LayoutParameters::setInitialYBus);
        addSpinner("Busbar vertical space:", 10, 100, 5, 6, LayoutParameters::getVerticalSpaceBus, LayoutParameters::setVerticalSpaceBus);
        addSpinner("Horizontal busbar padding:", 10, 100, 5, 8, LayoutParameters::getHorizontalBusPadding, LayoutParameters::setHorizontalBusPadding);
        addSpinner("Cell width:", 10, 100, 5, 10, LayoutParameters::getCellWidth, LayoutParameters::setCellWidth);
        addSpinner("Extern cell height:", 100, 500, 10, 12, LayoutParameters::getExternCellHeight, LayoutParameters::setExternCellHeight);
        addSpinner("Intern cell height:", 10, 100, 5, 14, LayoutParameters::getInternCellHeight, LayoutParameters::setInternCellHeight);
        addSpinner("Stack height:", 10, 100, 5, 16, LayoutParameters::getStackHeight, LayoutParameters::setStackHeight);
        addCheckBox("Show grid", 18, LayoutParameters::isShowGrid, LayoutParameters::setShowGrid);
        addCheckBox("Show internal nodes", 19, LayoutParameters::isShowInternalNodes, LayoutParameters::setShowInternalNodes);

        CheckBox stackCb = new CheckBox("Stack feeders");
        VoltageLevelLayoutFactory layoutFactory = getLayoutFactory();
        stackCb.setSelected(layoutFactory instanceof PositionVoltageLevelLayoutFactory && ((PositionVoltageLevelLayoutFactory) layoutFactory).isStack());
        stackCb.setDisable(!(layoutFactory instanceof PositionVoltageLevelLayoutFactory));
        stackCb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            ((PositionVoltageLevelLayoutFactory) layoutFactory).setStack(newValue);
            // just to trigger diagram update
            refreshDiagram();
        });
        parametersPane.add(stackCb, 0, 20);
    }

    private void refreshDiagram() {
        layoutParameters.set(new LayoutParameters(layoutParameters.get()));
    }

    private void saveSelectedDiagrams() {
        try {
            String selectedVoltageLevelIdsPropertyValue = objectMapper.writeValueAsString(selectableVoltageLevels.stream()
                    .filter(selectableVoltageLevel -> selectableVoltageLevel.checkedProperty().get())
                    .map(SelectableVoltageLevel::getId)
                    .collect(Collectors.toList()));
            preferences.put(SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY, selectedVoltageLevelIdsPropertyValue);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadSelectedDiagrams() {
        String selectedVoltageLevelIdsPropertyValue = preferences.get(SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY, null);
        if (selectedVoltageLevelIdsPropertyValue != null) {
            try {
                Set<String> selectedVoltageLevelIds = new HashSet<>(objectMapper.readValue(selectedVoltageLevelIdsPropertyValue, new TypeReference<List<String>>() {
                }));
                selectableVoltageLevels.stream()
                        .filter(selectableVoltageLevel -> selectedVoltageLevelIds.contains(selectableVoltageLevel.getId()))
                        .forEach(selectableVoltageLevel -> selectableVoltageLevel.checkedProperty().set(true));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        voltageLevelList.setCellFactory(CheckBoxListCell.forListView(SelectableVoltageLevel::checkedProperty));
        filterInput.textProperty().addListener(obs -> {
            String filter = filterInput.getText();
            if (filter == null || filter.length() == 0) {
                filteredSelectableVoltageLevels.setPredicate(s -> true);
            } else {
                filteredSelectableVoltageLevels.setPredicate(s -> s.getId().contains(filter));
            }
        });

        networkProperty.addListener((observable, oldNetwork, newNetwork) -> {
            if (newNetwork == null) {
                selectableVoltageLevels.clear();
            } else {
                selectableVoltageLevels.setAll(newNetwork.getVoltageLevelStream()
                        .map(vl -> new SelectableVoltageLevel(vl.getId()))
                        .collect(Collectors.toList()));
            }
        });
        TabPane diagramsPane = new TabPane();
        diagramsPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        diagramsPane.getTabs().setAll(new Tab("Selected", selectedDiagramPane),
                                      new Tab("Checked", checkedDiagramsPane));

        createParametersPane();

        BorderPane voltageLevelPane = new BorderPane();
        Label filterLabel = new Label("Filter:");
        filterLabel.setMinWidth(40);
        HBox filterPane = new HBox(5, filterLabel, filterInput);
        HBox.setHgrow(filterInput, Priority.ALWAYS);
        filterPane.setPadding(new Insets(5, 5, 5, 5));
        voltageLevelPane.setTop(filterPane);
        voltageLevelPane.setCenter(voltageLevelList);

        SplitPane splitPane = new SplitPane(voltageLevelPane, diagramsPane, parametersPane);
        splitPane.setDividerPositions(0.2, 0.7, 0.1);

        caseLoadingStatus.setStyle("-fx-background-color: red");
        casePathTextField.setEditable(false);
        Button caseButton = new Button("...");
        caseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open case File");
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                loadNetwork(file.toPath());
            }
        });
        HBox.setHgrow(casePathTextField, Priority.ALWAYS);
        HBox casePane = new HBox(3, caseLoadingStatus, casePathTextField, caseButton);
        BorderPane.setMargin(casePane, new Insets(3, 3, 3, 3));
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(splitPane);
        mainPane.setTop(casePane);

        voltageLevelList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<SelectableVoltageLevel>) c -> {
            while (c.next()) {
                for (SelectableVoltageLevel s : c.getAddedSubList()) {
                    VoltageLevel vl = networkProperty.get().getVoltageLevel(s.getId());
                    selectedDiagramPane.setCenter(new SubstationDiagramPane(vl));
                }
            }
        });

        // selected diagrams reloading
        selectableVoltageLevels.addListener(new ListChangeListener<SelectableVoltageLevel>() {
            @Override
            public void onChanged(Change<? extends SelectableVoltageLevel> c) {
                loadSelectedDiagrams();
                selectableVoltageLevels.remove(this);
            }
        });

        // case reloading
        String casePathPropertyValue = preferences.get(CASE_PATH_PROPERTY, null);
        if (casePathPropertyValue != null) {
            loadNetwork(Paths.get(casePathPropertyValue));
        }

        Scene scene = new Scene(mainPane, 1000, 800);
        primaryStage.setTitle("Substation diagram viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadNetwork(Path file) {
        Service<Network> networkService = new Service<Network>() {
            @Override
            protected Task<Network> createTask() {
                return new Task<Network>() {
                    @Override
                    protected Network call() {
                        return Importers.loadNetwork(file);
                    }
                };
            }
        };
        networkService.setOnRunning(event -> {
            caseLoadingStatus.setStyle("-fx-background-color: yellow");
            casePathTextField.setText(file.toAbsolutePath().toString());
        });
        networkService.setOnSucceeded(event -> {
            networkProperty.setValue((Network) event.getSource().getValue());
            caseLoadingStatus.setStyle("-fx-background-color: green");
            preferences.put(CASE_PATH_PROPERTY, file.toAbsolutePath().toString());
        });
        networkService.setOnFailed(event -> {
            casePathTextField.setText("");
            caseLoadingStatus.setStyle("-fx-background-color: red");
        });
        networkService.start();
    }
}
