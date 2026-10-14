import type { SldComponentOptions } from '@powsybl/network-viewer-core';

import type { EditorModel } from '../core/EditorModel';
import type { FeederLevels } from '../core/bayFlip';
import {
    isCreatedNodeId,
    type DiagramPoint,
    type FeederDirection,
    type NodeMetadata,
    type SymbolProvider,
} from '../core/types';
import type { SvgDomService } from './SvgDomService';
import type { SymbolView } from './svgShapes';

export class DiagramGeometry {
    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly symbols: SymbolProvider,
    ) {}

    componentSize(componentType: string): { width: number; height: number } {
        return this.symbols.componentSize(componentType);
    }

    nodeCentre(node: NodeMetadata): DiagramPoint | undefined {
        const point = this.dom.getDiagramPoint(node.id);
        if (!point) return undefined;

        const { width, height } = this.componentSize(node.componentType);
        return { x: point.x + width / 2, y: point.y + height / 2 };
    }

    busbarY(vlId: string): number | undefined {
        for (const node of this.model.busbarNodes(vlId)) {
            const span = this.dom.getDiagramSpan(node.id);
            if (span) return span.y;
        }
        return undefined;
    }

    /** Heights of a feeder and of its breaker. */
    levelsOf(feeder: NodeMetadata): FeederLevels | undefined {
        const feederY = this.nodeCentre(feeder)?.y;
        if (feederY === undefined) return undefined;

        const breaker = this.model.bayBreaker(feeder);
        return { feederY, switchY: breaker && this.nodeCentre(breaker)?.y };
    }

    /** Heights of the first drawn feeder on that side, to align new bays with it. */
    feederLevels(vlId: string, direction: FeederDirection): FeederLevels | undefined {
        const feeder = this.model.feederNodes(vlId, direction).find((node) => !isCreatedNodeId(node.id));
        return feeder && this.levelsOf(feeder);
    }

    symbolFor(
        componentType: string,
        options: SldComponentOptions,
        anchorNodeId: string,
    ): SymbolView | undefined {
        const { width, height } = this.componentSize(componentType);
        if (width <= 0 || height <= 0) return undefined;
        const element = this.symbols.createSymbol(componentType, options);
        element.classList.add(...this.dom.getVoltageClasses(anchorNodeId));
        return { element, width, height };
    }
}
