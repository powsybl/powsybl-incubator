import { useState, type ReactNode } from 'react';
import type { EditorAction } from '../core/actions';
import type { EquipmentProperties } from '../core/types';
import type { PropertyDescriptor } from '../properties';

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

    return (
        <>
            <form
                key={action.id}
                className={className}
                onSubmit={(e) => {
                    e.preventDefault();
                    const values = {
                        ...action.initial,
                        ...readValues(action.form, new FormData(e.currentTarget)),
                    };
                    const applied = action.run(values);
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
                        {input(descriptor, action.initial[descriptor.key])}
                    </label>
                ))}
                {children ?? <button type="submit">{submitLabel}</button>}
            </form>
            {refusedBy === action.id && <p className="refused">{refusedLabel}</p>}
        </>
    );
}

function input(descriptor: PropertyDescriptor, value: EquipmentProperties[string] | undefined) {
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
