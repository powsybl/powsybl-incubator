import { describe, expect, it } from 'vitest';
import { DEFAULT_LAYOUT_PARAMS } from '../types.ts';
import { defaultFeederLevels } from './bayFlip.ts';

// Busbars of the demo diagram (demo/data/reseau.svg).
const BUS_TOP = 280;
const BUS_BOTTOM = 305;

describe('defaultFeederLevels', () => {
    it('places a bay below the last busbar like powsybl-diagram', () => {
        // Matches the drawn bottom bays of the demo: breaker centred at 375, line at 477.
        expect(defaultFeederLevels(DEFAULT_LAYOUT_PARAMS, BUS_TOP, BUS_BOTTOM, 'BOTTOM')).toEqual({
            feederY: 477,
            switchY: 375,
        });
    });

    it('places a bay above the first busbar', () => {
        expect(defaultFeederLevels(DEFAULT_LAYOUT_PARAMS, BUS_TOP, BUS_BOTTOM, 'TOP')).toEqual({
            feederY: 108,
            switchY: 210,
        });
    });

    it('uses externCellHeight when cell heights are not adapted to content', () => {
        const params = { ...DEFAULT_LAYOUT_PARAMS, adaptCellHeightToContent: false };
        expect(defaultFeederLevels(params, BUS_TOP, BUS_BOTTOM, 'BOTTOM')).toEqual({
            feederY: BUS_BOTTOM + 280,
            switchY: BUS_BOTTOM + 124,
        });
    });
});
