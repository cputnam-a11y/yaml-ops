package com.thiakil.yamlops.util;

import it.unimi.dsi.fastutil.Hash;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.Objects;

public class NodeStrategy implements Hash.Strategy<Node> {
    public static final Hash.Strategy<Node> INSTANCE = new NodeStrategy();

    @Override
    public int hashCode(Node o) {
        if (o instanceof ScalarNode scalarNode) {
            return scalarNode.getValue().hashCode();
        }
        if (o instanceof MappingNode mappingNode) {
            return Objects.hash((Object[]) mappingNode.getValue().stream().map(NodeTupleStrategy.INSTANCE::hashCode).toArray(Integer[]::new));
        }
        if (o instanceof SequenceNode sequenceNode) {
            return Objects.hash((Object[]) sequenceNode.getValue().stream().map(this::hashCode).toArray(Integer[]::new));
        }
        return o.hashCode();
    }

    @Override
    public boolean equals(Node a, Node b) {
        if ((a == null) != (b == null)) {
            return false;
        }
        if (a == b) {
            return true;
        }
        return hashCode(a) == hashCode(b);
    }
}
