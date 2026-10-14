import type { SldComponentOptions } from '@powsybl/network-viewer-core';

import type { EditorModel } from '../core/EditorModel';
import { buildBayFlip, invertFlip, type BayFlip, type FeederLevels } from '../core/bayFlip';
import type { BayTurn } from '../core/commands/UpdateBayPositionCommand';
import {
    isCreatedNodeId,
    type DiagramPoint,
    type FeederDirection,
    type NodeMetadata,
    type SymbolProvider,
} from '../core/types';
import type { CellFlip, SvgDomService } from './SvgDomService';
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

    bayTurn(
        feeder: NodeMetadata,
        cellFeeders: readonly NodeMetadata[],
        from: FeederDirection,
        to: FeederDirection,
    ): BayTurn | undefined {
        const vlId = feeder.vid ?? '';
        const busbarYs = this.model.busbarNodes(vlId).flatMap((busbar) => {
            const span = this.dom.getDiagramSpan(busbar.id);
            return span ? [span.y] : [];
        });
        const source = this.levelsOf(feeder);
        if (busbarYs.length === 0 || !source) return undefined;

        const flip = buildBayFlip(busbarYs, source, this.feederLevels(vlId, to) ?? source, from, to);

        const heights = new Map<string, number>();
        for (const id of this.dom.bayCellElementIds(feeder.id)) {
            const componentType =
                this.model.getNodeById(id)?.componentType ??
                this.model.getFeederInfoById(id)?.componentType;
            if (componentType) heights.set(id, this.componentSize(componentType).height);
        }

        return {
            forth: this.cellFlip(feeder, cellFeeders, to, flip, heights),
            back: this.cellFlip(feeder, cellFeeders, from, invertFlip(flip), heights),
            feederNodeIds: cellFeeders.map((cellFeeder) => cellFeeder.id),
            from,
        };
    }

    private cellFlip(
        feeder: NodeMetadata,
        cellFeeders: readonly NodeMetadata[],
        direction: FeederDirection,
        y: BayFlip,
        heights: ReadonlyMap<string, number>,
    ): CellFlip {
        const orientation = direction === 'TOP' ? 'UP' : 'DOWN';
        const feeders = new Map<string, string | null>();
        const arrows = new Map<string, string | null>();

        for (const cellFeeder of cellFeeders) {
            const symbol = this.symbols.createSymbol(cellFeeder.componentType, { orientation });
            feeders.set(cellFeeder.id, symbol.firstElementChild?.getAttribute('transform') ?? null);

            for (const info of this.model.getFeederInfosForEquipment(cellFeeder.equipmentId ?? '')) {
                const { width, height } = this.componentSize(info.componentType);
                arrows.set(info.id, direction === 'BOTTOM' ? `rotate(180.0,${width / 2},${height / 2})` : null);
            }
        }
        return { feederNodeId: feeder.id, direction, y, heights, feeders, arrows };
    }
}
