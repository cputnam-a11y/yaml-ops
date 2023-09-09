package com.thiakil.yamlops.eoyaml.scalars;

import com.mojang.serialization.DynamicOps;

public class NumericScalar extends OpsScalar {
    private final Number value;
    private final String strValue;

    public NumericScalar(Number value) {
        this.value = value;
        this.strValue = value.toString();
    }

    @Override
    public <T> T convertTo(DynamicOps<T> ops) {
        return ops.createNumeric(value);
    }

    @Override
    public String value() {
        return strValue;
    }

    public Number rawValue() {
        return value;
    }
}
