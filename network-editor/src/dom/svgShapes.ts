import {
    BAY_SLOT_CLASS,
    PENDING_CREATE_CLASS,
    type BaySlotCandidate,
    type DiagramPoint,
} from '../core/types';
import { PENDING_BADGE_CLASS, PENDING_SYMBOL_CLASS } from './editorStyle';

const SVG_NS = 'http://www.w3.org/2000/svg';

// Default wire
const WIRE_BEFORE_SWITCH = 16;
const WIRE_AFTER_SWITCH = 16;
const WIRE_WITHOUT_SWITCH = 24;

// Label box next to a pending element.
const LABEL_HEIGHT = 10;
const LABEL_TEXT_BASELINE = 7;
const LABEL_GAP = 5;

// Badges stacked above a node that has pending changes.
const BADGE_X = 4;
const BADGE_FIRST_TOP = -14;
const BADGE_STEP = LABEL_HEIGHT + 2;

const SLOT_RADIUS = 4;

export interface PendingBadgeView {
    id?: string;
    label: string;
}

export interface SymbolView {
    element: SVGGElement;
    width: number;
    height: number;
}

export interface PlacedSymbol {
    at: number;
    symbol: SymbolView;
}

export interface FeederShape {
    x: number;
    y: number;
    side: 'UP' | 'DOWN';
    length: number;
    switch?: PlacedSymbol;
    terminal?: SymbolView;
    busbars?: PlacedSymbol[];
}

export interface FeederPreview extends FeederShape {
    kind: 'FEEDER';
    id: string;
    label: string;
}

export interface SwitchPreview {
    kind: 'SWITCH';
    id: string;
    label: string;
    from: DiagramPoint;
    to: DiagramPoint;
    symbol?: SymbolView;
}

export type PendingPreview = FeederPreview | SwitchPreview;

interface PreviewLayout {
    id: string;
    from: DiagramPoint;
    to: DiagramPoint;
    back?: number;
    gap?: PlacedSymbol;
    label: { text: string; x: number; top: number };
}

export function svgElement<K extends keyof SVGElementTagNameMap>(name: K): SVGElementTagNameMap[K] {
    return document.createElementNS(SVG_NS, name);
}

export function createPendingPreview(view: PendingPreview): SVGGElement {
    return view.kind === 'SWITCH' ? switchPreview(view) : feederPreview(view);
}

export function defaultFeederLength(switchSize?: number): number {
    return switchSize === undefined ? WIRE_WITHOUT_SWITCH : WIRE_BEFORE_SWITCH + switchSize + WIRE_AFTER_SWITCH;
}

export function defaultSwitchAt(switchSize: number): number {
    return WIRE_BEFORE_SWITCH + switchSize / 2;
}

function feederPreview(view: FeederPreview): SVGGElement {
    const { x, y, side, length, terminal, busbars = [] } = view;
    const sign = side === 'UP' ? -1 : 1;
    const end = y + sign * length;
    const terminalHeight = terminal?.height ?? 0;
    const labelTop =
        side === 'UP' ? end - terminalHeight - LABEL_GAP - LABEL_HEIGHT : end + terminalHeight + LABEL_GAP;

    const stub = drawPreview({
        id: view.id,
        from: { x, y },
        to: { x, y: end },
        back: Math.max(0, ...busbars.map((busbar) => -busbar.at)),
        gap: view.switch,
        label: { text: view.label, x, top: labelTop },
    });

    for (const { at, symbol } of busbars) {
        stub.prepend(placeSymbol(symbol, x, y + sign * at));
    }
    if (terminal) stub.append(placeSymbol(terminal, x, end + (sign * terminalHeight) / 2));
    return stub;
}

function switchPreview(view: SwitchPreview): SVGGElement {
    const { from, to, symbol } = view;
    const length = distance(from, to);
    const middle = { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 };
    const height = symbol?.height ?? 0;

    return drawPreview({
        id: view.id,
        from,
        to,
        gap: symbol && { at: length / 2, symbol },
        label: { text: view.label, x: middle.x, top: middle.y - height - LABEL_HEIGHT },
    });
}

function drawPreview({ id, from, to, back = 0, gap, label }: PreviewLayout): SVGGElement {
    const length = distance(from, to);
    const angle = (Math.atan2(to.y - from.y, to.x - from.x) * 180) / Math.PI;

    const segment = svgElement('g');
    segment.setAttribute('transform', `translate(${from.x},${from.y}) rotate(${angle})`);
    if (gap) {
        const half = gap.symbol.width / 2;
        segment.append(
            dashLine(-back, gap.at - half),
            placeSymbol(gap.symbol, gap.at, 0),
            dashLine(gap.at + half, length),
        );
    } else {
        segment.append(dashLine(-back, length));
    }

    const stub = svgElement('g');
    stub.setAttribute('class', PENDING_CREATE_CLASS);
    stub.id = id;
    stub.append(segment, ...labelBox(label.x, label.top, label.text));
    return stub;
}

export function createPendingBadge(view: PendingBadgeView, index: number): SVGGElement {
    const marker = svgElement('g');
    marker.setAttribute('class', PENDING_BADGE_CLASS);
    if (view.id) marker.id = view.id;

    marker.append(...labelBox(BADGE_X, BADGE_FIRST_TOP - index * BADGE_STEP, view.label));
    return marker;
}

export function createBaySlot(slot: BaySlotCandidate): SVGGElement {
    const point = svgElement('g');
    point.setAttribute('class', BAY_SLOT_CLASS);
    point.id = slot.id;
    point.setAttribute('transform', `translate(${slot.x},${slot.y})`);

    const dot = svgElement('circle');
    dot.setAttribute('r', String(SLOT_RADIUS));

    const hint = svgElement('title');
    hint.textContent = `Order ${slot.order}`;

    point.append(dot, hint);
    return point;
}

function dashLine(from: number, to: number): SVGLineElement {
    const line = svgElement('line');
    line.setAttribute('x1', String(from));
    line.setAttribute('x2', String(to));
    line.setAttribute('y1', '0');
    line.setAttribute('y2', '0');
    return line;
}

function placeSymbol(symbol: SymbolView, cx: number, cy: number): SVGGElement {
    const holder = svgElement('g');
    holder.setAttribute('class', PENDING_SYMBOL_CLASS);
    holder.setAttribute('transform', `translate(${cx - symbol.width / 2},${cy - symbol.height / 2})`);
    holder.append(symbol.element);
    return holder;
}

function labelBox(cx: number, top: number, label: string): [SVGRectElement, SVGTextElement] {
    const width = label.length * 3.4 + 8;

    const box = svgElement('rect');
    box.setAttribute('x', String(cx - width / 2));
    box.setAttribute('y', String(top));
    box.setAttribute('width', String(width));
    box.setAttribute('height', String(LABEL_HEIGHT));

    const text = svgElement('text');
    text.setAttribute('x', String(cx));
    text.setAttribute('y', String(top + LABEL_TEXT_BASELINE));
    text.textContent = label;

    return [box, text];
}

function distance(from: DiagramPoint, to: DiagramPoint): number {
    return Math.hypot(to.x - from.x, to.y - from.y);
}
