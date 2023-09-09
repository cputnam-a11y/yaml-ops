package com.thiakil.yamlops;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

public class YamlHelper {
    public static void dump(Writer output, Node rootNode, DumperOptions dumperOptions) throws IOException {
        Serializer serializer = new Serializer(new Emitter(output, dumperOptions), new Resolver(),
                dumperOptions, rootNode.getTag());
        serializer.open();
        serializer.serialize(rootNode);
        serializer.close();
    }

    public static String dumpString(Node rootNode) {
        return dumpString(rootNode, SnakeYamlOps.DEFAULT_OPTIONS);
    }

    public static String dumpString(Node rootNode, DumperOptions dumperOptions) {
        StringWriter stringWriter = new StringWriter();
        try {
            dump(stringWriter, rootNode, dumperOptions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stringWriter.toString();
    }

    public static Node load(Reader yaml) {
        Composer composer = new Composer(new ParserImpl(new StreamReader(yaml)), new Resolver());
        return composer.getSingleNode();
    }

    public static Node load(String yaml) {
        return load(new StringReader(yaml));
    }

    public static Node sortMappingKeys(Node rootNode, Comparator<String> keyComparator) {
        if (rootNode instanceof SequenceNode sequenceNode) {
            return new SequenceNode(
                    sequenceNode.getTag(),
                    sequenceNode.getValue().stream().map(node -> sortMappingKeys(node, keyComparator)).toList(),
                    sequenceNode.getFlowStyle()
            );
        }
        if (rootNode instanceof MappingNode mappingNode) {
            ArrayList<NodeTuple> sortedList = new ArrayList<>(mappingNode.getValue());
            sortedList.sort(Comparator.comparing(t->((ScalarNode)t.getKeyNode()).getValue(), keyComparator));
            return new MappingNode(mappingNode.getTag(), sortedList, mappingNode.getFlowStyle());
        }
        if (rootNode instanceof AnchorNode anchorNode) {
            return sortMappingKeys(anchorNode.getRealNode(), keyComparator);
        }
        return rootNode;
    }
}
