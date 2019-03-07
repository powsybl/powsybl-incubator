/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.SubstationDiagram;
import com.powsybl.substationdiagram.layout.*;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
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
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

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

    private final CheckBox showNames = new CheckBox("Show names");

    private class SubstationDiagramPane extends BorderPane {

        private final FlowPane flowPane = new FlowPane();

        private final TextArea infoArea = new TextArea();

        private final TextArea svgArea = new TextArea();

        private final TextArea metadataArea = new TextArea();

        private final Tab tab1 = new Tab("Diagram", flowPane);

        private final Tab tab2 = new Tab("SVG", svgArea);

        private final Tab tab3 = new Tab("Metadata", metadataArea);

        private final TabPane tabPane = new TabPane(tab1, tab2, tab3);

        private final TitledPane titledPane = new TitledPane("Infos", infoArea);

        private final ChangeListener<LayoutParameters> listener;

        SubstationDiagramPane(VoltageLevel vl) {
            svgArea.setEditable(false);
            metadataArea.setEditable(false);
            infoArea.setEditable(false);
            infoArea.setText(String.join(System.lineSeparator(),
                                         "id: " + vl.getId(),
                                         "name: " + vl.getName()));
            tabPane.setSide(Side.BOTTOM);
            tab1.setClosable(false);
            tab2.setClosable(false);
            tab3.setClosable(false);
            setCenter(tabPane);
            setBottom(titledPane);
            listener = (observable, oldValue, newValue) -> loadDiagram(vl);
            layoutParameters.addListener(new WeakChangeListener<>(listener));
            loadDiagram(vl);
        }

        class SubstationDiagramResult {

            private final SubstationDiagramView view;

            private final String svgData;

            private final String metadataData;

            SubstationDiagramResult(SubstationDiagramView view, String svgData, String metadataData) {
                this.view = view;
                this.svgData = svgData;
                this.metadataData = metadataData;
            }

            SubstationDiagramView getView() {
                return view;
            }

            String getSvgData() {
                return svgData;
            }

            String getMetadataData() {
                return metadataData;
            }
        }

        private SubstationDiagramResult createSubstationDiagramView(VoltageLevel vl) {
            String svgData;
            String metadataData;
            try (StringWriter svgWriter = new StringWriter();
                 StringWriter metadataWriter = new StringWriter()) {
                SubstationDiagram diagram = SubstationDiagram.build(vl, getLayoutFactory(), showNames.isSelected());
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
                diagramView = SubstationDiagramView.load(svgInputStream, metadataInputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new SubstationDiagramResult(diagramView, svgData, metadataData);
        }

        private void loadDiagram(VoltageLevel vl) {
            Service<SubstationDiagramResult> loader = new Service<SubstationDiagramResult>() {
                @Override
                protected Task<SubstationDiagramResult> createTask() {
                    return new Task<SubstationDiagramResult>() {
                        @Override
                        protected SubstationDiagramResult call() {
                            return createSubstationDiagramView(vl);
                        }
                    };
                }
            };
            loader.setOnScheduled(event -> {
                Text loading = new Text("Loading...");
                loading.setFont(Font.font(30));
                flowPane.getChildren().setAll(loading);
                svgArea.setText("");
                metadataArea.setText("");
            });
            loader.setOnSucceeded(event -> {
                SubstationDiagramResult result = (SubstationDiagramResult) event.getSource().getValue();
                flowPane.getChildren().setAll(result.getView());
                svgArea.setText(result.getSvgData());
                metadataArea.setText(result.getMetadataData());
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

        private final String name;

        private final BooleanProperty checkedProperty = new SimpleBooleanProperty();

        SelectableVoltageLevel(String id, String name) {
            this.id = id;
            this.name = name;
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

        String getIdOrName() {
            return showNames.isSelected() ? name : id;
        }

        BooleanProperty checkedProperty() {
            return checkedProperty;
        }

        @Override
        public String toString() {
            return getIdOrName();
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
        showNames.selectedProperty().addListener((observable, oldValue, newValue) -> {
            voltageLevelList.refresh();
            refreshDiagram();
        });
        filterInput.textProperty().addListener(obs -> {
            String filter = filterInput.getText();
            if (filter == null || filter.length() == 0) {
                filteredSelectableVoltageLevels.setPredicate(s -> true);
            } else {
                filteredSelectableVoltageLevels.setPredicate(s -> s.getIdOrName().contains(filter));
            }
        });

        networkProperty.addListener((observable, oldNetwork, newNetwork) -> {
            if (newNetwork == null) {
                selectableVoltageLevels.clear();
            } else {
                selectableVoltageLevels.setAll(newNetwork.getVoltageLevelStream()
                        .map(vl -> new SelectableVoltageLevel(vl.getId(), vl.getName()))
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
        GridPane voltageLevelToolBar = new GridPane();
        voltageLevelToolBar.setHgap(5);
        voltageLevelToolBar.setVgap(5);
        voltageLevelToolBar.setPadding(new Insets(5, 5, 5, 5));
        voltageLevelToolBar.add(showNames, 0, 0, 2, 1);
        voltageLevelToolBar.add(filterLabel, 0, 1);
        voltageLevelToolBar.add(filterInput, 1, 1);
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        voltageLevelToolBar.getColumnConstraints().addAll(c0, c1);
        voltageLevelPane.setTop(voltageLevelToolBar);
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
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
            casePathTextField.setText("");
            caseLoadingStatus.setStyle("-fx-background-color: red");
        });
        networkService.start();
    }
}
