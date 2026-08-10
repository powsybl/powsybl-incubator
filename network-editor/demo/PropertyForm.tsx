import { useState } from 'react';
import { validateValues, type EquipmentProperties, type PropertyDescriptor } from '../src';

interface PropertyFormProps {
    schema: readonly PropertyDescriptor[];
    initial: EquipmentProperties;
    submitLabel: string;
    onSubmit: (values: EquipmentProperties) => void;
}

/**
 * The component publishes the fields to ask and the rules to check them: this
 * form only renders them. It knows no equipment type and no operation.
 */
export function PropertyForm({ schema, initial, submitLabel, onSubmit }: PropertyFormProps) {
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
