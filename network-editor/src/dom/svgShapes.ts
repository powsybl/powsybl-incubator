import {
    BAY_SLOT_CLASS,
    PENDING_CREATE_CLASS,
    type BaySlotCandidate,
    type DiagramPoint,
} from '../core/types';
import { PENDING_BADGE_CLASS } from './editorStyle';

const SVG_NS = 'http://www.w3.org/2000/svg';

const LEAD = 16;
const TAIL = 16;
const PLAIN_LENGTH = 24;
const LABEL_HEIGHT = 10;
const LABEL_BASELINE = 7;
const MARKER_TOP = -14;
const NODE_CENTRE = 4;
const SLOT_RADIUS = 4;

export interface PendingBadgeView {
    id?: string;
    label: string;
}

export interface FeederShape {
    x: number;
    y: number;
    side: 'UP' | 'DOWN';
    withSwitch: boolean;
    switchSize: number;
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
    switchSize: number;
}

export type PendingPreview = FeederPreview | SwitchPreview;

interface PreviewLayout {
    id: string;
    from: DiagramPoint;
    to: DiagramPoint;
    box?: { at: number; size: number };
    label: { text: string; x: number; top: number };
}

export function svgElement<K extends keyof SVGElementTagNameMap>(name: K): SVGElementTagNameMap[K] {
    return document.createElementNS(SVG_NS, name);
}

export function createPendingPreview(view: PendingPreview): SVGGElement {
    return view.kind === 'SWITCH' ? switchPreview(view) : feederPreview(view);
}

function feederPreview(view: FeederPreview): SVGGElement {
    const { x, y, side, withSwitch, switchSize } = view;
    const length = withSwitch ? LEAD + switchSize + TAIL : PLAIN_LENGTH;
    const to = { x, y: y + (side === 'UP' ? -length : length) };

    return drawPreview({
        id: view.id,
        from: { x, y },
        to,
        box: withSwitch ? { at: LEAD + switchSize / 2, size: switchSize } : undefined,
        label: { text: view.label, x, top: side === 'UP' ? to.y - LABEL_HEIGHT : to.y },
    });
}

function switchPreview(view: SwitchPreview): SVGGElement {
    const { from, to } = view;
    const length = distance(from, to);
    const middle = { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 };
    const size = Math.min(view.switchSize, length);

    return drawPreview({
        id: view.id,
        from,
        to,
        box: { at: length / 2, size },
        label: { text: view.label, x: middle.x, top: middle.y - size - LABEL_HEIGHT },
    });
}

function drawPreview({ id, from, to, box, label }: PreviewLayout): SVGGElement {
    const length = distance(from, to);
    const angle = (Math.atan2(to.y - from.y, to.x - from.x) * 180) / Math.PI;

    const segment = svgElement('g');
    segment.setAttribute('transform', `translate(${from.x},${from.y}) rotate(${angle})`);
    segment.append(
        ...(box
            ? [
                  dashLine(0, box.at - box.size / 2),
                  dashBox(box.at, box.size),
                  dashLine(box.at + box.size / 2, length),
              ]
            : [dashLine(0, length)]),
    );

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

    marker.append(...labelBox(NODE_CENTRE, MARKER_TOP - index * (LABEL_HEIGHT + 2), view.label));
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

function dashBox(centre: number, size: number): SVGRectElement {
    const box = svgElement('rect');
    box.setAttribute('x', String(centre - size / 2));
    box.setAttribute('y', String(-size / 2));
    box.setAttribute('width', String(size));
    box.setAttribute('height', String(size));
    return box;
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
    text.setAttribute('y', String(top + LABEL_BASELINE));
    text.textContent = label;

    return [box, text];
}

function distance(from: DiagramPoint, to: DiagramPoint): number {
    return Math.hypot(to.x - from.x, to.y - from.y);
}
