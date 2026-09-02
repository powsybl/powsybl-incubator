import { useState } from 'react';
import { validateValues, type EquipmentProperties, type PropertyDescriptor } from '../src';

interface PropertyFormProps {
    schema: readonly PropertyDescriptor[];
    initial: EquipmentProperties;
    submitLabel: string;
    onSubmit: (values: EquipmentProperties) => void;
    rename: boolean
}


export function PropertyForm({ schema, initial, submitLabel, onSubmit, rename }: PropertyFormProps) {
    const [values, setValues] = useState(initial);
    const [invalid, setInvalid] = useState<ReadonlySet<string>>(new Set());

    const set = (key: string, value: EquipmentProperties[string]) =>
        setValues((previous) => ({ ...previous, [key]: value }));

    return (
        <form
            onSubmit={(event) => {
                event.preventDefault();
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

                    {input(descriptor, values[descriptor.key], set, rename)}
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
    rename: boolean
) {
    if (descriptor.type === 'boolean') {
        return (
            <input
                type="checkbox"
                checked={value === true}
                onChange={(e) => set(descriptor.key, e.target.checked)}
            />
        );
    }
    if (descriptor.type === 'select') {
        return (
            <select value={String(value ?? '')} onChange={(e) => set(descriptor.key, e.target.value)}>
                {descriptor.options?.map((option) => (
                    <option key={option}>{option}</option>
                ))}
            </select>
        );
    }
    return (
        <input
            disabled={descriptor.key === 'equipmentId' && !rename}
            type={descriptor.type === 'number' ? 'number' : 'text'}
            value={String(value ?? '')}
            onChange={(e) =>
                set(
                    descriptor.key,
                    descriptor.type === 'number' ? Number(e.target.value) : e.target.value,
                )
            }
        />
    );
}
