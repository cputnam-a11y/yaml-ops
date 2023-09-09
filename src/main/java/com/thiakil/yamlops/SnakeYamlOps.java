package com.thiakil.yamlops;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
public class SnakeYamlOps implements DynamicOps<Node> {
    private static final DumperOptions DEFAULT_OPTIONS = new DumperOptions();
    static {
        DEFAULT_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    }
    private final Representer representer;
    private final Node EMPTY;
    private final MyConstructor constructor = new MyConstructor();
    private final DumperOptions dumperOptions;

    public SnakeYamlOps(DumperOptions dumperOptions) {
        this.dumperOptions = dumperOptions;
        representer = new Representer();
        representer.setDefaultFlowStyle(dumperOptions.getDefaultFlowStyle());
        representer.setDefaultScalarStyle(dumperOptions.getDefaultScalarStyle());
        EMPTY = representer.represent(null);
    }

    public SnakeYamlOps() {
        this(DEFAULT_OPTIONS);
    }

    @Override
    public Node empty() {
        return EMPTY;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, Node input) {
        if (input instanceof MappingNode mappingNode) {
            return convertMap(outOps, input);
        }
        if (input instanceof SequenceNode sequenceNode) {
            return convertList(outOps, input);
        }
        if (input instanceof ScalarNode scalarNode) {
            if (scalarNode.getTag() == Tag.BOOL) {
                return outOps.createBoolean(getBooleanValue(input).get().orThrow());
            }
            if (scalarNode.getTag() == Tag.INT || scalarNode.getTag() == Tag.FLOAT) {
                return outOps.createNumeric(getNumberValue(input).get().orThrow());
            }
            return outOps.createString(scalarNode.getValue());
        }
        throw new IllegalStateException("Unconvertable Node: "+input);
    }

    private DataResult<ScalarNode> getScalar(Node input) {
        if (input instanceof ScalarNode scalarNode) {
            return DataResult.success(scalarNode);
        }
        return DataResult.error(()->"Not a scalar: "+input);
    }

    private DataResult<SequenceNode> getSequence(Node input) {
        if (input instanceof SequenceNode sequenceNode) {
            return DataResult.success(sequenceNode);
        }
        if (YAML_NODE_STRATEGY.equals(EMPTY, input)) {
            return DataResult.success(new SequenceNode(Tag.MAP, Collections.emptyList(), dumperOptions.getDefaultFlowStyle()));
        }
        return DataResult.error(()->"Not a sequence: "+input);
    }

    private DataResult<MappingNode> getYMap(Node input) {
        if (input instanceof MappingNode mappingNode) {
            return DataResult.success(mappingNode);
        }
        if (YAML_NODE_STRATEGY.equals(EMPTY, input)) {
            return DataResult.success(new MappingNode(Tag.MAP, Collections.emptyList(), dumperOptions.getDefaultFlowStyle()));
        }
        return DataResult.error(()->"Not a MappingNode: "+input);
    }

    @Override
    public DataResult<Number> getNumberValue(Node input) {
        return getScalar(input).map(scalarNode -> (Number) constructor.constructObject(scalarNode));
    }

    @Override
    public Node createNumeric(Number i) {
        return representer.represent(i);
    }

    @Override
    public DataResult<String> getStringValue(Node input) {
        return getScalar(input).map(ScalarNode::getValue);
    }

    @Override
    public Node createString(String value) {
        return representer.represent(value);
    }

    @Override
    public DataResult<Node> mergeToList(Node list, Node value) {
        return getSequence(list)
                .map(SequenceNode::getValue)
                .map(existing -> {
                    List<Node> newValues = new ArrayList<>(existing);
                    newValues.add(value);
                    return new SequenceNode(Tag.SEQ, newValues, dumperOptions.getDefaultFlowStyle());
                });
    }

    @Override
    public DataResult<Node> mergeToMap(Node map, Node key, Node value) {
        DataResult<Map<Node, Node>> existingValues;
        DumperOptions.FlowStyle flowStyle;
        if (YAML_NODE_STRATEGY.equals(EMPTY, map)) {
            existingValues = DataResult.success(new HashMap<>());
            flowStyle = dumperOptions.getDefaultFlowStyle();
        } else {
            DataResult<MappingNode> yMap = getYMap(map);
            existingValues = yMap.map(mappingNode ->
                    mappingNode.getValue().stream()
                            .collect(Collectors.toMap(NodeTuple::getKeyNode, NodeTuple::getValueNode, (m1,m2)->m1, LinkedHashMap::new))
            );
            flowStyle = yMap.result().map(MappingNode::getFlowStyle).orElse(dumperOptions.getDefaultFlowStyle());
        }
        return existingValues.map(m->{
            m.put(key, value);
            return new MappingNode(Tag.MAP, m.entrySet().stream().map(e->new NodeTuple(e.getKey(), e.getValue())).toList(), flowStyle);
        });
    }

    @Override
    public DataResult<Stream<Pair<Node, Node>>> getMapValues(Node input) {
        return getYMap(input).map(m-> m.getValue().stream()
                        .map(n->new Pair<>(n.getKeyNode(), n.getValueNode()))
                );
    }

    @Override
    public Node createMap(Stream<Pair<Node, Node>> map) {
        return new MappingNode(Tag.MAP, map.map(p->new NodeTuple(p.getFirst(), p.getSecond())).toList(), dumperOptions.getDefaultFlowStyle());
    }

    @Override
    public DataResult<Stream<Node>> getStream(Node input) {
        return getSequence(input).map(s->s.getValue().stream());
    }

    @Override
    public Node createList(Stream<Node> input) {
        return new SequenceNode(Tag.SEQ, input.toList(), dumperOptions.getDefaultFlowStyle());
    }

    @Override
    public Node remove(Node input, String key) {
        return getYMap(input).map(mN-> (Node)new MappingNode(
                Tag.MAP,
                mN.getValue().stream().filter(t->{
                    if (t.getKeyNode() instanceof ScalarNode scalarNode) {
                        return !key.equals(scalarNode.getValue());
                    }
                    return true;
                }).toList(),
                mN.getFlowStyle()
            )
        ).result().orElse(input);
    }

    @Override
    public DataResult<MapLike<Node>> getMap(Node input) {
        return getYMap(input).map(m-> MapLike.forMap(
                m.getValue().stream().collect(Collectors.toMap(
                        NodeTuple::getKeyNode,
                        NodeTuple::getValueNode,
                        (a,b)->b,
                        ()->new Object2ObjectOpenCustomHashMap<>(YAML_NODE_STRATEGY)
                    )
                ),
            this)
        );
    }

    public void dump(Writer output, Node rootNode) throws IOException {
        Serializer serializer = new Serializer(new Emitter(output, dumperOptions), new Resolver(),
                dumperOptions, rootNode.getTag());
        serializer.open();
        serializer.serialize(rootNode);
        serializer.close();
    }

    public String dumpString(Node rootNode) {
        StringWriter stringWriter = new StringWriter();
        try {
            dump(stringWriter, rootNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stringWriter.toString();
    }

    public Node load(Reader yaml) {
        Composer composer = new Composer(new ParserImpl(new StreamReader(yaml)), new Resolver());
        return composer.getSingleNode();
    }

    public Node load(String yaml) {
        return load(new StringReader(yaml));
    }

    private static class MyConstructor extends Constructor {
        @Override
        public Object constructObject(Node node) {
            return super.constructObject(node);
        }
    }

    public static final Hash.Strategy<Node> YAML_NODE_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(Node o) {
            if (o instanceof ScalarNode scalarNode) {
                return scalarNode.getValue().hashCode();
            }
            if (o instanceof MappingNode mappingNode) {
                return Objects.hash((Object[]) mappingNode.getValue().stream().map(NODE_TUPLE_STRATEGY::hashCode).toArray(Integer[]::new));
            }
            if (o instanceof SequenceNode sequenceNode) {
                return Objects.hash((Object[]) sequenceNode.getValue().stream().map(this::hashCode).toArray(Integer[]::new));
            }
            return o.hashCode();
        }

        @Override
        public boolean equals(Node a, Node b) {
            if ((a==null) != (b==null)) {
                return false;
            }
            if (a == b) {
                return true;
            }
            return hashCode(a) == hashCode(b);
        }
    };

    private static final Hash.Strategy<NodeTuple> NODE_TUPLE_STRATEGY = new Hash.Strategy<NodeTuple>() {
        @Override
        public int hashCode(NodeTuple o) {
            return Objects.hash(YAML_NODE_STRATEGY.hashCode(o.getKeyNode()), YAML_NODE_STRATEGY.hashCode(o.getValueNode()));
        }

        @Override
        public boolean equals(NodeTuple a, NodeTuple b) {
            return YAML_NODE_STRATEGY.equals(a.getKeyNode(), b.getKeyNode()) && YAML_NODE_STRATEGY.equals(a.getValueNode(), b.getValueNode());
        }
    };
}
