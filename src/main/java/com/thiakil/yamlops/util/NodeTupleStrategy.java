package com.thiakil.yamlops.util;

import it.unimi.dsi.fastutil.Hash;
import org.yaml.snakeyaml.nodes.NodeTuple;

import java.util.Objects;

public class NodeTupleStrategy implements Hash.Strategy<NodeTuple> {
    public static final Hash.Strategy<NodeTuple> INSTANCE = new NodeTupleStrategy();

    @Override
    public int hashCode(NodeTuple o) {
        return Objects.hash(NodeStrategy.INSTANCE.hashCode(o.getKeyNode()), NodeStrategy.INSTANCE.hashCode(o.getValueNode()));
    }

    @Override
    public boolean equals(NodeTuple a, NodeTuple b) {
        return NodeStrategy.INSTANCE.equals(a.getKeyNode(), b.getKeyNode()) && NodeStrategy.INSTANCE.equals(a.getValueNode(), b.getValueNode());
    }
}
