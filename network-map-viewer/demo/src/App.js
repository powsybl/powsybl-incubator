/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import React, {useCallback} from 'react';
import {NetworkMap, GeoData} from '@powsybl/network-map-viewer';
import {
    createTheme,
    ThemeProvider,
    StyledEngineProvider,
} from '@mui/material/styles';

import MapEquipments from './map-equipments';

import sposdata from './data/spos.json';
import lposdata from './data/lpos.json';
import smapdata from './data/smap.json';
import lmapdata from './data/lmap.json';

function App() {

    const INITIAL_POSITION = [9.322, 45.255];
    const INITIAL_ZOOM = 9;
    const LABELS_ZOOM_THRESHOLD = 9;
    const ARROWS_ZOOM_THRESHOLD = 7;
    const centerOnSubstation = {to: 'SUB1'};
    const useName = true;

    //called after a click (left mouse click) on a substation (represented as a stack of colored discs)
    const chooseVoltageLevelForSubstation = useCallback(
        (idSubstation, x, y) => {
            console.log('# Choose Voltage Level for substation: ' + idSubstation);
        },
        []
    );

    //called after a click (left mouse click) on a VL
    const openVoltageLevel = useCallback(
        (vlId) => {
            console.log('# OpenVoltageLevel: ' + vlId);
        },
        []
    );

    //called after a click (right mouse click) on a VL
    const voltageLevelMenuClick = (equipment, x, y) => {
        console.log("# VoltageLevel menu click: " + JSON.stringify(equipment));
    };

    //called after a click (right mouse click) on an equipment (line or substation)
    function showEquipmentMenu(equipment, x, y, type) {
        console.log('# Show equipment menu: ' + JSON.stringify(equipment) + ', type: ' + type);
    }

    const darkTheme = createTheme({
        palette: {
            mode: 'dark',
        },
        link: {
            color: 'green',
        },
        node: {
            background: '#1976d2',
            hover: '#90caf9',
            border: '#cce3f9',
        },
        selectedRow: {
            background: '#545C5B',
        },
        mapboxStyle: 'mapbox://styles/mapbox/dark-v9',
        aggrid: 'ag-theme-alpine-dark',
    });

    //declare data to be displayed: coordinates and network data
    const geoData = new GeoData(
        new Map(),
        new Map()
    );
    geoData.setSubstationPositions(sposdata);
    geoData.setLinePositions(lposdata);

    const mapEquipments = new MapEquipments(
        smapdata,
        lmapdata
    );

    const renderMap = () => {
        return (
            <NetworkMap
                mapEquipments={mapEquipments}
                geoData={geoData}
                labelsZoomThreshold={LABELS_ZOOM_THRESHOLD}
                arrowsZoomThreshold={ARROWS_ZOOM_THRESHOLD}
                initialPosition={INITIAL_POSITION}
                initialZoom={INITIAL_ZOOM}
                centerOnSubstation={centerOnSubstation}
                useName={useName}
                onSubstationClick={openVoltageLevel}
                onSubstationClickChooseVoltageLevel={
                    chooseVoltageLevelForSubstation
                }
                onSubstationMenuClick={(equipment, x, y) =>
                    showEquipmentMenu(equipment, x, y, 'substation')
                }
                onLineMenuClick={(equipment, x, y) =>
                    showEquipmentMenu(equipment, x, y, 'line')
                }
                onVoltageLevelMenuClick={voltageLevelMenuClick}
            />
        );
    };


    return (
        <div className="App">
            <header className="App-header">
            </header>
            <StyledEngineProvider injectFirst>
                <ThemeProvider theme={darkTheme}>
                    {renderMap()}
                </ThemeProvider>
            </StyledEngineProvider>

        </div>
    );
}

export default App;
