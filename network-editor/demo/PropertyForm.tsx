import { useState } from 'react';
import { validateValues, type EquipmentProperties, type PropertyDescriptor } from '../src';

interface PropertyFormProps {
    schema: readonly PropertyDescriptor[];
    initial: EquipmentProperties;
    submitLabel: string;
    onSubmit: (values: EquipmentProperties) => void;
}

export function PropertyForm({ schema, initial, submitLabel, onSubmit }: PropertyFormProps) {
    const [values, setValues] = useState(initial);
    const [invalid, setInvalid] = useState<ReadonlySet<string>>(new Set());

    const set = (key: string, value: EquipmentProperties[string]) =>
        setValues((current) => ({ ...current, [key]: value }));

    return (
        <form
            onSubmit={(e) => {
                e.preventDefault();
                const offending = validateValues(schema, values);
                setInvalid(new Set(offending));
                if (offending.length === 0) onSubmit(values);
            }}
        >
            {schema.map((descriptor) => (
                <label
                    key={descriptor.key}
                    className={`field${invalid.has(descriptor.key) ? ' invalid' : ''}`}
                >
                    <span>
                        {descriptor.key}
                        {descriptor.unit && ` (${descriptor.unit})`}
                        {descriptor.required && ' *'}
                    </span>
                    {input(descriptor, values[descriptor.key], set)}
                </label>
            ))}
            <button type="submit">{submitLabel}</button>
        </form>
    );
}

function input(
    descriptor: PropertyDescriptor,
    value: EquipmentProperties[string] | undefined,
    set: (key: string, value: EquipmentProperties[string]) => void,
) {
    if (descriptor.type === 'boolean') {
        return (
            <input
                type="checkbox"
                checked={value === true}
                disabled={descriptor.readOnly}
                onChange={(e) => set(descriptor.key, e.target.checked)}
            />
        );
    }
    if (descriptor.type === 'select') {
        return (
            <select
                value={String(value ?? '')}
                disabled={descriptor.readOnly}
                onChange={(e) => set(descriptor.key, e.target.value)}
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
            value={String(value ?? '')}
            disabled={descriptor.readOnly}
            min={descriptor.min}
            max={descriptor.max}
            onChange={(e) => set(descriptor.key, coerce(descriptor, e.target.value))}
        />
    );
}

/** An emptied number field must stay empty, not become 0. */
function coerce(descriptor: PropertyDescriptor, raw: string): EquipmentProperties[string] {
    if (descriptor.type !== 'number') return raw;
    return raw === '' ? '' : Number(raw);
}
