import { useState, type ReactNode } from 'react';
import type { ActionValues, EditorAction, EquipmentProperties, PropertyDescriptor } from '../src';
import { defaultsOf } from '../src/actions/properties.ts';
import { equipmentModification, modificationValues } from '../src/modifications/modifications.ts';

// The demo form: one input per descriptor of the action, keyed by the raw field names.
// A host builds its own forms; this one only shows how `initial` and `run` are used.

export interface PropertyFormProps {
    action: EditorAction;
    onDone?: () => void;
    submitLabel?: string;
    refusedLabel?: string;
    className?: string;
    children?: ReactNode;
}

export function PropertyForm({
    action,
    onDone,
    submitLabel = 'Apply',
    refusedLabel = 'Refused by the component',
    className,
    children,
}: PropertyFormProps) {
    const [refusedBy, setRefusedBy] = useState<string | null>(null);
    const shown = formValues(action);

    return (
        <>
            <form
                key={action.id}
                className={className}
                onSubmit={(e) => {
                    e.preventDefault();
                    const entered = readValues(action.form, new FormData(e.currentTarget));
                    const applied = action.run(submittedValues(action, entered));
                    setRefusedBy(applied ? null : action.id);
                    if (applied) onDone?.();
                }}
            >
                {action.form.map((descriptor) => (
                    <label key={descriptor.key} className="field">
                        <span>
                            {descriptor.key}
                            {descriptor.unit && ` (${descriptor.unit})`}
                            {descriptor.required && ' *'}
                        </span>
                        {input(descriptor, shown[descriptor.key])}
                    </label>
                ))}
                {children ?? <button type="submit">{submitLabel}</button>}
            </form>
            {refusedBy === action.id && <p className="refused">{refusedLabel}</p>}
        </>
    );
}

function input(descriptor: PropertyDescriptor, value: unknown) {
    if (descriptor.type === 'boolean') {
        return (
            <input
                type="checkbox"
                name={descriptor.key}
                defaultChecked={value === true}
                disabled={descriptor.readOnly}
            />
        );
    }
    if (descriptor.type === 'select') {
        return (
            <select
                name={descriptor.key}
                defaultValue={String(value ?? '')}
                required={descriptor.required}
                disabled={descriptor.readOnly}
            >
                {descriptor.options?.map((option) => (
                    <option key={option}>{option}</option>
                ))}
            </select>
        );
    }
    return (
        <input
            type={descriptor.type === 'number' ? 'number' : 'text'}
            name={descriptor.key}
            defaultValue={String(value ?? '')}
            required={descriptor.required}
            disabled={descriptor.readOnly}
            min={descriptor.min}
            max={descriptor.max}
            step="any"
        />
    );
}

function readValues(
    schema: readonly PropertyDescriptor[],
    data: FormData,
): EquipmentProperties {
    const values: EquipmentProperties = {};

    for (const descriptor of schema) {
        if (descriptor.readOnly) continue;

        if (descriptor.type === 'boolean') {
            values[descriptor.key] = data.has(descriptor.key);
            continue;
        }

        const raw = data.get(descriptor.key);
        if (raw === null || raw === '') continue;

        values[descriptor.key] = descriptor.type === 'number' ? Number(raw) : String(raw);
    }
    return values;
}

/** The plain values the form shows: a property edit holds a modification, a creation holds plain values already. */
function formValues(action: EditorAction): ActionValues {
    if (action.operation !== 'UPDATE_PROPERTIES' || !action.equipment) return action.initial;
    return {
        ...defaultsOf(action.form.filter((descriptor) => descriptor.editOnly)),
        ...modificationValues(action.initial),
        equipmentId: action.equipment.id,
    };
}

/** What `run` expects from the plain values the form entered. */
function submittedValues(action: EditorAction, entered: EquipmentProperties): ActionValues {
    if (action.operation !== 'UPDATE_PROPERTIES' || !action.equipment) return { ...action.initial, ...entered };
    // A plain form hands back every field: only the ones the user changed become a modification.
    const shown = formValues(action);
    const { equipmentId: _id, ...changes } = Object.fromEntries(
        Object.entries(entered).filter(([key, value]) => value !== shown[key]),
    );
    return equipmentModification(action.equipment.type, action.equipment.id, changes);
}
