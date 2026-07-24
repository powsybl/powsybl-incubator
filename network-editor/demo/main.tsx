import React from 'react';
import ReactDOM from 'react-dom/client';
import { type SLDMetadata } from '../src';
import metadata1 from './data/metadata.json';
import metadata2 from './data/metadata2.json';
import {DiagramEditor} from "./DiagramEditor.tsx";

const m1 =  metadata1 as unknown as SLDMetadata;
const m2 = metadata2 as unknown as SLDMetadata;


const App = () => (
    <>
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
