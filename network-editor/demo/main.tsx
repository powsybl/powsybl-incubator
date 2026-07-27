import React from 'react';
import ReactDOM from 'react-dom/client';
import { SELECTED_CLASS, type SLDMetadata } from '../src';
import metadata1 from './data/metadata.json';
import metadata2 from './data/metadata2.json';
import {DiagramEditor} from "./DiagramEditor.tsx";

const m1 =  metadata1 as unknown as SLDMetadata;
const m2 = metadata2 as unknown as SLDMetadata;


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
                svgUrl="/demo/data/reseau.svg"
                metadata={m1}
            />
        </div>
        <div>
            <DiagramEditor
                title="Network Editor Demo — sld-example"
                svgUrl="/demo/data/reseau2.svg"
                metadata={m2}
            />
        </div>

    </>
);

ReactDOM.createRoot(document.getElementById('app')!).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);
