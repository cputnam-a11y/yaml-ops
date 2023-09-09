package com.thiakil.yamlops;

import com.amihaiemil.eoyaml.*;
import com.amihaiemil.eoyaml.extensions.MergedYamlMapping;
import com.amihaiemil.eoyaml.extensions.MergedYamlSequence;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.thiakil.yamlops.eoyaml.scalars.BooleanScalar;
import com.thiakil.yamlops.eoyaml.scalars.NumericScalar;
import com.thiakil.yamlops.eoyaml.scalars.OpsScalar;
import com.thiakil.yamlops.eoyaml.scalars.StringScalar;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@MethodsReturnNonnullByDefault
public class EOYamlOps implements DynamicOps<YamlNode> {
    static final YamlNode EMPTY = Yaml.createYamlScalarBuilder().buildPlainScalar();
    public static final EOYamlOps INSTANCE = new EOYamlOps();

    private EOYamlOps() {
    }

    @Override
    public YamlNode empty() {
        return EMPTY;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, YamlNode input) {
        return switch (input.type()) {
            case SCALAR -> {
                if (input instanceof OpsScalar os) {
                    yield os.convertTo(outOps);
                }
                //guess the type, types won't match exactly
                Optional<Number> asNum = getNumberValue(input).result();
                if (asNum.isPresent()) {
                    yield outOps.createNumeric(asNum.get());
                }
                Optional<Boolean> asBool = getBooleanValue(input).result();
                if (asBool.isPresent()) {
                    yield outOps.createBoolean(asBool.get());
                }
                yield outOps.createString(input.asScalar().value());
            }
            case MAPPING -> convertMap(outOps, input);
            case STREAM -> throw new IllegalArgumentException("Stream not a valid input");
            case SEQUENCE -> convertList(outOps, input);
        };
    }

    @Override
    public DataResult<Number> getNumberValue(YamlNode input) {
        return asScalar(input).flatMap(i -> {
            if (i instanceof NumericScalar ns) {
                return DataResult.success(ns.rawValue());
            }
            String scalar = i.value();
            try {
                return DataResult.success(new BigDecimal(scalar));
            } catch (NumberFormatException ignored) {
                return DataResult.error(() -> "Not a number: " + input);
            }
        });
    }

    @Override
    public YamlNode createNumeric(Number i) {
        return new NumericScalar(Objects.requireNonNull(i));
    }

    @Override
    public DataResult<Boolean> getBooleanValue(YamlNode input) {
        return asScalar(input).flatMap(scalar -> {
            if (scalar instanceof BooleanScalar bs) {
                return DataResult.success(bs.rawValue());
            }
            String value = scalar.value();
            if (value.equalsIgnoreCase("true")||value.equalsIgnoreCase("false")) {
                return DataResult.success(Boolean.valueOf(value));
            }
            return DataResult.error(()->"Not a boolean: "+input);
        });
    }

    @Override
    public YamlNode createBoolean(boolean value) {
        return new BooleanScalar(value);
    }

    @Override
    public DataResult<String> getStringValue(YamlNode input) {
        return asScalar(input).map(Scalar::value);
    }

    @Override
    public YamlNode createString(String value) {
        return new StringScalar(value);
    }

    @Override
    public DataResult<YamlNode> mergeToList(YamlNode list, YamlNode value) {
        YamlSequence newList = Yaml.createMutableYamlSequenceBuilder().add(value).build();
        if (list.type() == Node.SEQUENCE) {
            return DataResult.success(new MergedYamlSequence(list.asSequence(), newList));
        }
        if (list.isEmpty()) {
            return DataResult.success(newList);
        }
        return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
    }

    @Override
    public DataResult<YamlNode> mergeToMap(YamlNode map, YamlNode key, YamlNode value) {
        YamlMapping newMapping = Yaml.createMutableYamlMappingBuilder().add(key, value).build();
        if (map.isEmpty()) {
            return DataResult.success(newMapping);
        }
        if (map.type() != Node.MAPPING) {
            return DataResult.error(()->"Not a map: "+map, map);
        }
        return DataResult.success(new MergedYamlMapping(map.asMapping(), newMapping, true));
    }

    @Override
    public DataResult<Stream<Pair<YamlNode, YamlNode>>> getMapValues(YamlNode input) {
        return asMapping(input)
                .map(mapping->
                        mapping.keys().stream()
                                .map(key->new Pair<>(key, mapping.value(key)))
                );
    }

    @Override
    public YamlNode createMap(Stream<Pair<YamlNode, YamlNode>> map) {
        YamlMappingBuilder builder = Yaml.createMutableYamlMappingBuilder();
        map.forEach(p->builder.add(p.getFirst(), p.getSecond()));
        return builder.build();
    }

    @Override
    public YamlNode createMap(Map<YamlNode, YamlNode> map) {
        YamlMappingBuilder builder = Yaml.createMutableYamlMappingBuilder();
        map.forEach(builder::add);
        return builder.build();
    }

    @Override
    public DataResult<Stream<YamlNode>> getStream(YamlNode input) {
        if (input.type() == Node.SEQUENCE) {
            return DataResult.success(StreamSupport.stream(input.asSequence().spliterator(), false));
        }
        return DataResult.error(()->"Not a sequence: "+input);
    }

    @Override
    public YamlNode createList(Stream<YamlNode> input) {
        YamlSequenceBuilder builder = Yaml.createYamlSequenceBuilder();
        input.forEach(builder::add);
        return builder.build();
    }

    @Override
    public YamlNode remove(YamlNode input, String key) {
        if (input.type() == Node.MAPPING) {
            return new MergedYamlMapping(input.asMapping(), Yaml.createMutableYamlMappingBuilder().add(key, (YamlNode)null).build());
        }
        return input;
    }

    @Override
    public DataResult<YamlNode> getGeneric(YamlNode input, YamlNode key) {
        return asMapping(input).map(m->m.value(key));
    }

    private DataResult<Scalar> asScalar(YamlNode input) {
        if (input.type() == Node.SCALAR) {
            return DataResult.success(input.asScalar());
        }
        return DataResult.error(()->"Not a scalar: "+input);
    }

    private DataResult<YamlMapping> asMapping(YamlNode input) {
        if (input.type() == Node.MAPPING) {
            return DataResult.success(input.asMapping());
        }
        return DataResult.error(()->"Not a mapping: "+input);
    }
}
