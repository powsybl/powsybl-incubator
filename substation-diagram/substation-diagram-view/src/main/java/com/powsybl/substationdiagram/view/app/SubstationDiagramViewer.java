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
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.ContainerType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.SubstationDiagram;
import com.powsybl.substationdiagram.VoltageLevelDiagram;
import com.powsybl.substationdiagram.cgmes.CgmesVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionFree;
import com.powsybl.substationdiagram.layout.PositionFromExtension;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.RandomVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.SubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.VerticalSubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.util.NominalVoltageSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.util.SmartVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.view.AbstractContainerDiagramView;
import com.powsybl.substationdiagram.view.SubstationDiagramView;
import com.powsybl.substationdiagram.view.VoltageLevelDiagramView;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SubstationDiagramViewer extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationDiagramViewer.class);

    private  static final String SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY = "selectedVoltageLevelIds";
    private  static final String SELECTED_SUBSTATION_IDS_PROPERTY = "selectedSubstationIds";

    private static final String CASE_PATH_PROPERTY = "casePath";

    private final Map<String, VoltageLevelLayoutFactory> voltageLevelsLayouts
            = ImmutableMap.of("Smart", new SmartVoltageLevelLayoutFactory(),
                              "Auto extensions", new PositionVoltageLevelLayoutFactory(new PositionFromExtension()),
                              "Auto without extensions", new PositionVoltageLevelLayoutFactory(new PositionFree()),
                              "Random", new RandomVoltageLevelLayoutFactory(500, 500),
                              "Cgmes", new CgmesVoltageLevelLayoutFactory());

    private final Map<String, SubstationDiagramStyleProvider> styles
            = ImmutableMap.of("Default", new DefaultSubstationDiagramStyleProvider(),
                              "Nominal voltage", new NominalVoltageSubstationDiagramStyleProvider());

    private final Map<String, SubstationLayoutFactory> substationsLayouts
            = ImmutableMap.of("Horizontal", new HorizontalSubstationLayoutFactory(),
                              "Vertical", new VerticalSubstationLayoutFactory());

    private final ComponentLibrary convergenceComponentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
    private final ComponentLibrary flatDesignComponentLibrary = new ResourcesComponentLibrary("/FlatDesignLibrary");

    private final Map<String, ComponentLibrary> svgLibraries
            = ImmutableMap.of("CVG Design", convergenceComponentLibrary,
                              "Flat Design", flatDesignComponentLibrary);

    private final ObservableList<SelectableSubstation> selectableSubstations = FXCollections.observableArrayList();

    private final ObservableList<SelectableVoltageLevel> selectableVoltageLevels = FXCollections.observableArrayList();

    private final TextField filterInput = new TextField();

    private final TreeView<Container> substationsTree = new TreeView<>();

    private final Button caseLoadingStatus = new Button("  ");
    private final TextField casePathTextField = new TextField();
    private final BorderPane selectedDiagramPane = new BorderPane();
    private final TabPane checkedDiagramsPane = new TabPane();
    private GridPane parametersPane;

    private final ObjectProperty<Network> networkProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<LayoutParameters> layoutParameters = new SimpleObjectProperty<>(new LayoutParameters()
            .setShowGrid(true));

    private final Preferences preferences = Preferences.userNodeForPackage(VoltageLevelDiagramView.class);

    private final ObjectMapper objectMapper = JsonUtil.createObjectMapper();

    private final ComboBox<String> voltageLevelLayoutComboBox = new ComboBox<>();

    private final ComboBox<String> substationLayoutComboBox = new ComboBox<>();

    private final ComboBox<String> styleComboBox = new ComboBox<>();

    private final ComboBox<String> svgLibraryComboBox = new ComboBox<>();

    private final CheckBox showNames = new CheckBox("Show names");

    private class ContainerDiagramPane extends BorderPane {
        private final ScrollPane flowPane = new ScrollPane();

        private final TextArea infoArea = new TextArea();

        private final VBox svgArea = new VBox();
        private final TextField svgSearchField = new TextField();
        private final Button svgSearchButton = new Button("Search");
        private final TextArea svgTextArea = new TextArea();
        private AtomicReference<Integer> svgSearchStart = new AtomicReference<>(0);

        private final VBox metadataArea = new VBox();
        private final TextField metadataSearchField = new TextField();
        private final Button metadataSearchButton = new Button("Search");
        private final TextArea metadataTextArea = new TextArea();
        private final AtomicReference<Integer> metadataSearchStart = new AtomicReference<>(0);

        private final Tab tab1 = new Tab("Diagram", flowPane);

        private final Tab tab2 = new Tab("SVG", svgArea);

        private final Tab tab3 = new Tab("Metadata", metadataArea);

        private final TabPane tabPane = new TabPane(tab1, tab2, tab3);

        private final TitledPane titledPane = new TitledPane("Infos", infoArea);

        private final ChangeListener<LayoutParameters> listener;

        ContainerDiagramPane(Container c) {
            createSearchArea(svgSearchField, svgSearchButton, svgTextArea, svgArea, svgSearchStart);
            createSearchArea(metadataSearchField, metadataSearchButton, metadataTextArea, metadataArea, metadataSearchStart);

            infoArea.setEditable(false);
            infoArea.setText(String.join(System.lineSeparator(),
                                         "id: " + c.getId(),
                                         "name: " + c.getName()));
            tabPane.setSide(Side.BOTTOM);
            tab1.setClosable(false);
            tab2.setClosable(false);
            tab3.setClosable(false);
            setCenter(tabPane);
            setBottom(titledPane);
            listener = (observable, oldValue, newValue) -> loadDiagram(c);
            layoutParameters.addListener(new WeakChangeListener<>(listener));
            loadDiagram(c);
        }

        class ContainerDiagramResult {

            private final AbstractContainerDiagramView view;

            private final String svgData;

            private final String metadataData;

            ContainerDiagramResult(AbstractContainerDiagramView view, String svgData, String metadataData) {
                this.view = view;
                this.svgData = svgData;
                this.metadataData = metadataData;
            }

            AbstractContainerDiagramView getView() {
                return view;
            }

            String getSvgData() {
                return svgData;
            }

            String getMetadataData() {
                return metadataData;
            }
        }

        private ContainerDiagramResult createContainerDiagramView(Container c) {
            String svgData;
            String metadataData;
            try (StringWriter svgWriter = new StringWriter();
                 StringWriter metadataWriter = new StringWriter()) {
                SubstationDiagramStyleProvider styleProvider = styles.get(styleComboBox.getSelectionModel().getSelectedItem());

                if (c.getContainerType() == ContainerType.VOLTAGE_LEVEL) {
                    VoltageLevelDiagram diagram = VoltageLevelDiagram.build((VoltageLevel) c, getVoltageLevelLayoutFactory(), showNames.isSelected(),
                                                                            layoutParameters.get().isShowSelfFor3WT());
                    diagram.writeSvg(getComponentLibrary(), layoutParameters.get(), styleProvider, svgWriter, metadataWriter);
                } else if (c.getContainerType() == ContainerType.SUBSTATION) {
                    SubstationDiagram diagram = SubstationDiagram.build((Substation) c, getSubstationLayoutFactory(), getVoltageLevelLayoutFactory(), showNames.isSelected());
                    diagram.writeSvg(getComponentLibrary(), layoutParameters.get(), styleProvider, svgWriter, metadataWriter);
                }

                svgWriter.flush();
                metadataWriter.flush();
                svgData = svgWriter.toString();
                metadataData = metadataWriter.toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            AbstractContainerDiagramView diagramView;
            try (InputStream svgInputStream = new ByteArrayInputStream(svgData.getBytes(StandardCharsets.UTF_8));
                 InputStream metadataInputStream = new ByteArrayInputStream(metadataData.getBytes(StandardCharsets.UTF_8))) {
                if (c.getContainerType() == ContainerType.VOLTAGE_LEVEL) {
                    diagramView = VoltageLevelDiagramView.load(svgInputStream, metadataInputStream);
                } else if (c.getContainerType() == ContainerType.SUBSTATION) {
                    diagramView = SubstationDiagramView.load(svgInputStream, metadataInputStream);
                } else {
                    throw new AssertionError();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new ContainerDiagramResult(diagramView, svgData, metadataData);
        }

        private void loadDiagram(Container c) {
            Service<ContainerDiagramResult> loader = new Service<ContainerDiagramResult>() {
                @Override
                protected Task<ContainerDiagramResult> createTask() {
                    return new Task<ContainerDiagramResult>() {
                        @Override
                        protected ContainerDiagramResult call() {
                            return createContainerDiagramView(c);
                        }
                    };
                }
            };
            loader.setOnScheduled(event -> {
                Text loading = new Text("Loading...");
                loading.setFont(Font.font(30));
                flowPane.setContent(loading);
                svgTextArea.setText("");
                metadataTextArea.setText("");
            });
            loader.setOnSucceeded(event -> {
                ContainerDiagramResult result = (ContainerDiagramResult) event.getSource().getValue();
                flowPane.setContent(result.getView());
                svgTextArea.setText(result.getSvgData());
                metadataTextArea.setText(result.getMetadataData());
            });
            loader.setOnFailed(event -> {
                Throwable e = event.getSource().getException();
                LOGGER.error(e.toString(), e);
            });
            loader.start();
        }

        private ComponentLibrary getComponentLibrary() {
            String selectedItem = svgLibraryComboBox.getSelectionModel().getSelectedItem();
            return svgLibraries.get(selectedItem);
        }

        private SubstationLayoutFactory getSubstationLayoutFactory() {
            String selectedItem = substationLayoutComboBox.getSelectionModel().getSelectedItem();
            return substationsLayouts.get(selectedItem);
        }

        private void createSearchArea(TextField searchField, Button searchButton,
                                      TextArea textArea, VBox area,
                                      AtomicReference<Integer> searchStart) {
            HBox searchBox = new HBox();
            searchBox.setSpacing(20);
            searchBox.setPadding(new Insets(10));
            searchField.setPrefColumnCount(35);
            searchBox.getChildren().add(searchField);
            searchBox.getChildren().add(searchButton);

            searchStart.set(0);
            searchButton.setOnAction(evh -> {
                String txtPattern = searchField.getText();
                Pattern pattern = Pattern.compile(txtPattern);
                Matcher matcher = pattern.matcher(textArea.getText());
                boolean found = matcher.find(searchStart.get());
                if (found) {
                    textArea.selectRange(matcher.start(), matcher.end());
                    searchStart.set(matcher.end());
                } else {
                    textArea.deselect();
                    searchStart.set(0);
                    found = matcher.find(searchStart.get());
                    if (found) {
                        textArea.selectRange(matcher.start(), matcher.end());
                        searchStart.set(matcher.end());
                    }
                }
            });
            searchField.textProperty().addListener((observable, oldValue, newValue) ->
                searchStart.set(0)
            );

            area.setSpacing(8);
            area.getChildren().add(searchBox);
            area.getChildren().add(textArea);
            VBox.setVgrow(searchBox, Priority.NEVER);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            textArea.setEditable(false);
        }
    }

    abstract class AbstractSelectableContainer {

        protected final String id;

        protected final String name;

        protected final BooleanProperty checkedProperty = new SimpleBooleanProperty();

        AbstractSelectableContainer(String id, String name) {
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

        abstract void addDiagramTab();

        protected String getId() {
            return id;
        }

        protected String getIdOrName() {
            return showNames.isSelected() ? name : id;
        }

        public BooleanProperty checkedProperty() {
            return checkedProperty;
        }

        public void setCheckedProperty(Boolean b) {
            checkedProperty.setValue(b);
        }

        @Override
        public String toString() {
            return getIdOrName();
        }

        private void saveSelectedDiagrams() {
            try {
                String selectedVoltageLevelIdsPropertyValue = objectMapper.writeValueAsString(selectableVoltageLevels.stream()
                        .filter(selectableVoltageLevel -> selectableVoltageLevel.checkedProperty().get())
                        .map(SelectableVoltageLevel::getId)
                        .collect(Collectors.toList()));
                preferences.put(SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY, selectedVoltageLevelIdsPropertyValue);

                String selectedSubstationIdsPropertyValue = objectMapper.writeValueAsString(selectableSubstations.stream()
                        .filter(selectableSubstation -> selectableSubstation.checkedProperty().get())
                        .map(SelectableSubstation::getId)
                        .collect(Collectors.toList()));
                preferences.put(SELECTED_SUBSTATION_IDS_PROPERTY, selectedSubstationIdsPropertyValue);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private class SelectableVoltageLevel extends AbstractSelectableContainer {

        SelectableVoltageLevel(String id, String name) {
            super(id, name);
        }

        @Override
        protected void addDiagramTab() {
            VoltageLevel vl = networkProperty.get().getVoltageLevel(id);
            if (vl != null) {
                Tab tab = new Tab(id, new ContainerDiagramPane(vl));
                tab.setOnCloseRequest(event -> {
                    checkedProperty.set(false);
                    uncheckvItemTree(id);
                });
                checkedDiagramsPane.getTabs().add(tab);
                checkedDiagramsPane.getSelectionModel().select(tab);
            } else {
                LOGGER.warn("Voltage level {} not found", id);
            }
        }

        private void uncheckvItemTree(String id) {
            substationsTree.getRoot().getChildren().stream().forEach(childS ->
                    childS.getChildren().stream().forEach(childV -> {
                        if (childV.getValue().getId().equals(id)) {
                            ((CheckBoxTreeItem) childV).setSelected(false);
                        }
                    })
            );
        }
    }

    private class SelectableSubstation extends AbstractSelectableContainer {
        SelectableSubstation(String id, String name) {
            super(id, name);
        }

        @Override
        protected void addDiagramTab() {
            Substation s = networkProperty.get().getSubstation(id);
            if (s != null) {
                Tab tab = new Tab(id, new ContainerDiagramPane(s));
                tab.setOnCloseRequest(event -> {
                    checkedProperty.set(false);
                    unchecksItemTree(id);
                });
                checkedDiagramsPane.getTabs().add(tab);
                checkedDiagramsPane.getSelectionModel().select(tab);
            } else {
                LOGGER.warn("Substation {} not found", id);
            }
        }

        private void unchecksItemTree(String id) {
            substationsTree.getRoot().getChildren().stream().forEach(child -> {
                if (child.getValue().getId().equals(id)) {
                    ((CheckBoxTreeItem) child).setSelected(false);
                }
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private VoltageLevelLayoutFactory getVoltageLevelLayoutFactory() {
        String selectedItem = voltageLevelLayoutComboBox.getSelectionModel().getSelectedItem();
        return voltageLevelsLayouts.get(selectedItem);
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

    private void initPositionLayoutCheckBox(Function<PositionVoltageLevelLayoutFactory, Boolean> initializer, CheckBox stackCb) {
        VoltageLevelLayoutFactory layoutFactory = getVoltageLevelLayoutFactory();
        stackCb.setSelected(layoutFactory instanceof PositionVoltageLevelLayoutFactory && initializer.apply((PositionVoltageLevelLayoutFactory) layoutFactory));
        stackCb.setDisable(!(layoutFactory instanceof PositionVoltageLevelLayoutFactory));
    }

    private void addPositionLayoutCheckBox(String label, int rowIndex, Function<PositionVoltageLevelLayoutFactory, Boolean> initializer,
                                           BiFunction<PositionVoltageLevelLayoutFactory, Boolean, PositionVoltageLevelLayoutFactory> updater) {
        CheckBox stackCb = new CheckBox(label);
        initPositionLayoutCheckBox(initializer, stackCb);
        voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> initPositionLayoutCheckBox(initializer, stackCb));
        stackCb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            VoltageLevelLayoutFactory layoutFactory = getVoltageLevelLayoutFactory();
            if (layoutFactory instanceof PositionVoltageLevelLayoutFactory) {
                updater.apply((PositionVoltageLevelLayoutFactory) layoutFactory, newValue);
                // just to trigger diagram update
                refreshDiagram();
            }
        });

        parametersPane.add(stackCb, 0, rowIndex);
    }

    private void createParametersPane() {
        parametersPane = new GridPane();
        parametersPane.setHgap(5);
        parametersPane.setVgap(5);
        parametersPane.setPadding(new Insets(5, 5, 5, 5));

        int rowIndex = 0;

        // svg library list
        svgLibraryComboBox.getItems().addAll(svgLibraries.keySet());
        svgLibraryComboBox.getSelectionModel().selectFirst();
        svgLibraryComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Design:"), 0, rowIndex++);
        parametersPane.add(svgLibraryComboBox, 0, rowIndex++);

        styleComboBox.getItems().addAll(styles.keySet());
        styleComboBox.getSelectionModel().selectFirst();
        styleComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Style:"), 0, rowIndex++);
        parametersPane.add(styleComboBox, 0, rowIndex++);

        // substation layout list
        substationLayoutComboBox.getItems().addAll(substationsLayouts.keySet());
        substationLayoutComboBox.getSelectionModel().selectFirst();
        substationLayoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Substation Layout:"), 0, rowIndex++);
        parametersPane.add(substationLayoutComboBox, 0, rowIndex++);

        // voltageLevel layout list
        voltageLevelLayoutComboBox.getItems().addAll(voltageLevelsLayouts.keySet());
        voltageLevelLayoutComboBox.getSelectionModel().selectFirst();
        voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("VoltageLevel Layout:"), 0, rowIndex++);
        parametersPane.add(voltageLevelLayoutComboBox, 0, rowIndex++);

        addSpinner("Horizontal substation padding:", 50, 300, 5, rowIndex, LayoutParameters::getHorizontalSubstationPadding, LayoutParameters::setHorizontalSubstationPadding);
        rowIndex += 2;
        addSpinner("Vertical substation padding:", 50, 300, 5, rowIndex, LayoutParameters::getVerticalSubstationPadding, LayoutParameters::setVerticalSubstationPadding);
        rowIndex += 2;
        addSpinner("Initial busbar X:", 0, 100, 5, rowIndex, LayoutParameters::getInitialXBus, LayoutParameters::setInitialXBus);
        rowIndex += 2;
        addSpinner("Initial busbar Y:", 0, 500, 5, rowIndex, LayoutParameters::getInitialYBus, LayoutParameters::setInitialYBus);
        rowIndex += 2;
        addSpinner("Busbar vertical space:", 10, 100, 5, rowIndex, LayoutParameters::getVerticalSpaceBus, LayoutParameters::setVerticalSpaceBus);
        rowIndex += 2;
        addSpinner("Horizontal busbar padding:", 10, 100, 5, rowIndex, LayoutParameters::getHorizontalBusPadding, LayoutParameters::setHorizontalBusPadding);
        rowIndex += 2;
        addSpinner("Cell width:", 10, 100, 5, rowIndex, LayoutParameters::getCellWidth, LayoutParameters::setCellWidth);
        rowIndex += 2;
        addSpinner("Extern cell height:", 100, 500, 10, rowIndex, LayoutParameters::getExternCellHeight, LayoutParameters::setExternCellHeight);
        rowIndex += 2;
        addSpinner("Intern cell height:", 10, 100, 5, rowIndex, LayoutParameters::getInternCellHeight, LayoutParameters::setInternCellHeight);
        rowIndex += 2;
        addSpinner("Stack height:", 10, 100, 5, rowIndex, LayoutParameters::getStackHeight, LayoutParameters::setStackHeight);
        rowIndex += 2;
        addCheckBox("Show grid", rowIndex, LayoutParameters::isShowGrid, LayoutParameters::setShowGrid);
        rowIndex += 1;
        addCheckBox("Show internal nodes", rowIndex, LayoutParameters::isShowInternalNodes, LayoutParameters::setShowInternalNodes);
        rowIndex += 1;
        addCheckBox("Draw straight wires", rowIndex, LayoutParameters::isDrawStraightWires, LayoutParameters::setDrawStraightWires);
        rowIndex += 1;
        addPositionLayoutCheckBox("Stack feeders", rowIndex, PositionVoltageLevelLayoutFactory::isFeederStacked, PositionVoltageLevelLayoutFactory::setFeederStacked);
        rowIndex += 1;
        addPositionLayoutCheckBox("Remove fictitious nodes", rowIndex, PositionVoltageLevelLayoutFactory::isRemoveUnnecessaryFictitiousNodes, PositionVoltageLevelLayoutFactory::setRemoveUnnecessaryFictitiousNodes);
        rowIndex += 1;
        addPositionLayoutCheckBox("Substitute singular fictitious nodes", rowIndex, PositionVoltageLevelLayoutFactory::isSubstituteSingularFictitiousByFeederNode, PositionVoltageLevelLayoutFactory::setSubstituteSingularFictitiousByFeederNode);
        rowIndex += 1;
        addCheckBox("Show self for three windings transformers", rowIndex, LayoutParameters::isShowSelfFor3WT, LayoutParameters::setShowSelfFor3WT);
        rowIndex += 1;
        addSpinner("Scale factor:", 1, 20, 1, rowIndex, LayoutParameters::getScaleFactor, LayoutParameters::setScaleFactor);
    }

    private void refreshDiagram() {
        layoutParameters.set(new LayoutParameters(layoutParameters.get()));
    }

    private void loadSelectedVoltageLevelsDiagrams() {
        String selectedIdsPropertyValue = preferences.get(SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY, null);
        if (selectedIdsPropertyValue != null) {
            try {
                Set<String> selectedIds = new HashSet<>(objectMapper.readValue(selectedIdsPropertyValue, new TypeReference<List<String>>() {
                }));
                selectableVoltageLevels.stream()
                        .filter(selectableObject -> selectedIds.contains(selectableObject.getId()))
                        .forEach(selectableVoltageLevel -> selectableVoltageLevel.checkedProperty().set(true));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void loadSelectedSubstationsDiagrams() {
        String selectedPropertyValue = preferences.get(SELECTED_SUBSTATION_IDS_PROPERTY, null);

        if (selectedPropertyValue != null) {
            try {
                Set<String> selectedSubstationIds = new HashSet<>(objectMapper.readValue(selectedPropertyValue, new TypeReference<List<String>>() {
                }));
                selectableSubstations.stream()
                        .filter(selectableSubstation -> selectedSubstationIds.contains(selectableSubstation.getId()))
                        .forEach(selectableSubstation -> selectableSubstation.checkedProperty().set(true));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /*
        Handling the display of names/id in the substations tree
     */
    private void initTreeCellFactory() {
        substationsTree.setCellFactory(param -> {
            CheckBoxTreeCell<Container> treeCell = new CheckBoxTreeCell<>();
            StringConverter<TreeItem<Container>> strConvert = new StringConverter<TreeItem<Container>>() {
                @Override
                public String toString(TreeItem<Container> c) {
                    if (c.getValue() != null) {
                        return showNames.isSelected() ? c.getValue().getName() : c.getValue().getId();
                    } else {
                        return "";
                    }
                }

                @Override
                public TreeItem<Container> fromString(String string) {
                    return null;
                }
            };
            treeCell.setConverter(strConvert);
            return treeCell;
        });
    }

    @Override
    public void start(Stage primaryStage) {
        initTreeCellFactory();

        showNames.selectedProperty().addListener((observable, oldValue, newValue) -> {
            substationsTree.refresh();
            refreshDiagram();
        });
        filterInput.textProperty().addListener(obs ->
            initSubstationsTree()
        );

        // handling the change of the network
        networkProperty.addListener((observable, oldNetwork, newNetwork) -> {
            if (newNetwork == null) {
                selectableVoltageLevels.clear();
                selectableSubstations.clear();
            } else {
                selectableVoltageLevels.setAll(newNetwork.getVoltageLevelStream()
                        .map(vl -> new SelectableVoltageLevel(vl.getId(), vl.getName()))
                        .collect(Collectors.toList()));
                selectableSubstations.setAll(newNetwork.getSubstationStream()
                        .map(s -> new SelectableSubstation(s.getId(), s.getName()))
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
        voltageLevelPane.setCenter(substationsTree);

        SplitPane splitPane = new SplitPane(voltageLevelPane, diagramsPane, new ScrollPane(parametersPane));
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

        // selected voltegeLevels diagrams reloading
        selectableVoltageLevels.addListener(new ListChangeListener<SelectableVoltageLevel>() {
            @Override
            public void onChanged(Change<? extends SelectableVoltageLevel> c) {
                loadSelectedVoltageLevelsDiagrams();
                selectableVoltageLevels.remove(this);
            }
        });

        // selected substation diagrams reloading
        selectableSubstations.addListener(new ListChangeListener<SelectableSubstation>() {
            @Override
            public void onChanged(Change<? extends SelectableSubstation> c) {
                loadSelectedSubstationsDiagrams();
                selectableSubstations.remove(this);
            }
        });

        // Handling selection of a substation or a voltageLevel in the substations tree
        substationsTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Container>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<Container>> observable, TreeItem<Container> oldValue, TreeItem<Container> newValue) {
                if (newValue == null) {
                    return;
                }
                Container c = newValue.getValue();
                selectedDiagramPane.setCenter(new ContainerDiagramPane(c));
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
                        Properties properties = new Properties();
                        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");
                        return Importers.loadNetwork(file, LocalComputationManager.getDefault(), new ImportConfig(), properties);
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
            initSubstationsTree();
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

    /*
        check/uncheck a voltageLevel in the substations tree
     */
    private void checkVoltageLevel(VoltageLevel v, Boolean checked) {
        selectableVoltageLevels.stream()
                .filter(selectableVoltageLevel -> selectableVoltageLevel.getIdOrName().equals(showNames.isSelected() ? v.getName() : v.getId()))
                .forEach(selectableVoltageLevel -> selectableVoltageLevel.setCheckedProperty(checked));
    }

    /*
        check/uncheck a substation in the substations tree
     */
    private void checkSubstation(Substation s, Boolean checked) {
        selectableSubstations.stream()
                .filter(selectableSubstation -> selectableSubstation.getIdOrName().equals(showNames.isSelected() ? s.getName() : s.getId()))
                .forEach(selectableSubstation -> selectableSubstation.setCheckedProperty(checked));
    }

    private void initVoltageLevelsTree(TreeItem<Container> rootItem,
                                       Substation s, String filter, boolean emptyFilter,
                                       Map<String, SelectableSubstation> mapSubstations,
                                       Map<String, SelectableVoltageLevel> mapVoltageLevels) {
        boolean firstVL = true;
        CheckBoxTreeItem<Container> sItem = null;

        for (VoltageLevel v : s.getVoltageLevels()) {
            boolean vlOk = showNames.isSelected() ? v.getName().contains(filter) : v.getId().contains(filter);

            if (!emptyFilter && !vlOk) {
                continue;
            }

            CheckBoxTreeItem<Container> vItem = new CheckBoxTreeItem<>(v);
            vItem.setIndependent(true);
            if (mapVoltageLevels.containsKey(v.getId()) && mapVoltageLevels.get(v.getId()).checkedProperty().get()) {
                vItem.setSelected(true);
            }

            if (firstVL) {
                sItem = new CheckBoxTreeItem<>(s);
                sItem.setIndependent(true);
                sItem.setExpanded(true);
                if (mapSubstations.containsKey(s.getId()) && mapSubstations.get(s.getId()).checkedProperty().get()) {
                    sItem.setSelected(true);
                }
                rootItem.getChildren().add(sItem);
                sItem.selectedProperty().addListener((obs, oldVal, newVal) ->
                    checkSubstation(s, newVal)
                );
            }

            firstVL = false;
            sItem.getChildren().add(vItem);
            vItem.selectedProperty().addListener((obs, oldVal, newVal) ->
                    checkVoltageLevel(v, newVal));
        }
    }

    private void initSubstationsTree() {
        String filter = filterInput.getText();
        boolean emptyFilter = StringUtils.isEmpty(filter);

        Network n = networkProperty.get();
        TreeItem<Container> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        Map<String, SelectableSubstation> mapSubstations = selectableSubstations.stream()
                .collect(Collectors.toMap(SelectableSubstation::getId, Function.identity()));
        Map<String, SelectableVoltageLevel> mapVoltageLevels = selectableVoltageLevels.stream()
                .collect(Collectors.toMap(SelectableVoltageLevel::getId, Function.identity()));

        for (Substation s : n.getSubstations()) {
            initVoltageLevelsTree(rootItem, s, filter, emptyFilter, mapSubstations, mapVoltageLevels);
        }

        if (substationsTree.getRoot() != null) {
            substationsTree.getRoot().getChildren().clear();
        }

        substationsTree.setRoot(rootItem);
        substationsTree.setShowRoot(false);
    }
}
