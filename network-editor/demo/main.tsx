import React from 'react';
import ReactDOM from 'react-dom/client';
import { SELECTED_CLASS, type EquipmentProperties, type SLDMetadata } from '../src';
import metadata1 from './data/vl1_metadata.json';
import properties1 from './data/vl1_properties.json';
import {DiagramEditor} from "./DiagramEditor.tsx";

const m1 =  metadata1 as unknown as SLDMetadata;
// Real IIDM values, as a backend would serve them.
const p1 = properties1 as Record<string, EquipmentProperties>;

const selectionStyle = `
.${SELECTED_CLASS} { outline: 2px dashed #1976d2; outline-offset: 2px; }
.${SELECTED_CLASS} .sld-label { fill: #1976d2; }
`;

const App = () => (
    <>
        <style>{selectionStyle}</style>
        <div>
            <DiagramEditor
                title="Network Editor Demo — sld-example"
                svgUrl="/demo/data/v1.svg"
                metadata={m1}
                initialProperties={p1}
            />
        </div>

    </>
);

ReactDOM.createRoot(document.getElementById('app')!).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);
