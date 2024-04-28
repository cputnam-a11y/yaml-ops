package com.thiakil.yamlops;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CodecTest {
    private enum Day {
        TUESDAY("tuesday", TuesdayData.CODEC),
        WEDNESDAY("wednesday", WednesdayData.CODEC),
        SUNDAY("sunday", SundayData.CODEC),
        ;

        private static final Map<String, Day> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(v -> v.name, Function.identity()));
        public static final Codec<Day> CODEC = Codec.STRING.comapFlatMap(DataResult.partialGet(BY_NAME::get, () -> "unknown day"), d -> d.name);

        private final String name;
        private final MapCodec<? extends DayData> codec;

        Day(final String name, final MapCodec<? extends DayData> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends DayData> codec() {
            return codec;
        }
    }

    interface DayData {
        Codec<DayData> CODEC = Day.CODEC.dispatch(DayData::type, Day::codec);
        Day type();
    }

    private static final class TuesdayData implements DayData {
        public static final MapCodec<TuesdayData> CODEC = Codec.INT.xmap(TuesdayData::new, d -> d.x).fieldOf("x");

        private final int x;

        private TuesdayData(final int x) {
            this.x = x;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TuesdayData that = (TuesdayData) o;
            return x == that.x;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x);
        }

        @Override
        public Day type() {
            return Day.TUESDAY;
        }
    }

    private static final class WednesdayData implements DayData {
        public static final MapCodec<WednesdayData> CODEC = Codec.STRING.xmap(WednesdayData::new, d -> d.y).fieldOf("y");

        private final String y;

        private WednesdayData(final String y) {
            this.y = y;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final WednesdayData that = (WednesdayData) o;
            return Objects.equals(y, that.y);
        }

        @Override
        public int hashCode() {
            return Objects.hash(y);
        }

        @Override
        public Day type() {
            return Day.WEDNESDAY;
        }
    }

    private static final class SundayData implements DayData {
        public static final MapCodec<SundayData> CODEC = Codec.FLOAT.xmap(SundayData::new, d -> d.z).fieldOf("z");

        private final float z;

        private SundayData(final float z) {
            this.z = z;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final SundayData that = (SundayData) o;
            return Float.compare(that.z, z) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(z);
        }

        @Override
        public Day type() {
            return Day.SUNDAY;
        }
    }

    private record SubData(String subField){
        static final MapCodec<SubData> CODEC = RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.fieldOf("subField").forGetter(SubData::subField)).apply(i, SubData::new));
    }

    private record  TestData(float a, double b, byte c, short d, int e, long f, boolean g, String h, List<String> i, Map<String, String> j, List<Pair<String, String>> k, DayData dayData, SubData subData) {
        public static final Codec<TestData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("a").forGetter(d -> d.a),
                Codec.DOUBLE.fieldOf("b").forGetter(d -> d.b),
                Codec.BYTE.fieldOf("c").forGetter(d -> d.c),
                Codec.SHORT.fieldOf("d").forGetter(d -> d.d),
                Codec.INT.fieldOf("e").forGetter(d -> d.e),
                Codec.LONG.fieldOf("f").forGetter(d -> d.f),
                Codec.BOOL.fieldOf("g").forGetter(d -> d.g),
                Codec.STRING.fieldOf("h").forGetter(d -> d.h),
                Codec.STRING.listOf().fieldOf("i").forGetter(d -> d.i),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("j").forGetter(d -> d.j),
                Codec.compoundList(Codec.STRING, Codec.STRING).fieldOf("k").forGetter(d -> d.k),
                DayData.CODEC.fieldOf("day_data").forGetter(d -> d.dayData),
                SubData.CODEC.forGetter(TestData::subData)
        ).apply(i, TestData::new));

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TestData testData = (TestData) o;
            return Float.compare(testData.a, a) == 0 &&
                    Double.compare(testData.b, b) == 0 &&
                    c == testData.c &&
                    d == testData.d &&
                    e == testData.e &&
                    f == testData.f &&
                    g == testData.g &&
                    h.equals(testData.h) &&
                    i.equals(testData.i) &&
                    j.size() == testData.j.size() &&
                    testData.j.keySet().containsAll(j.keySet()) &&
                    testData.j.entrySet().stream().allMatch(e -> Objects.equals(testData.j.get(e.getKey()), e.getValue())) &&
                    k.equals(testData.k) &&
                    dayData.equals(testData.dayData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c, d, e, f, g, h, i, j, k, dayData);
        }
    }

    private static TestData makeRandomTestData() {
        final Random random = new Random(4);
        return new TestData(
                random.nextFloat(),
                random.nextDouble(),
                (byte) random.nextInt(),
                (short) random.nextInt(),
                random.nextInt(),
                random.nextLong(),
                random.nextBoolean(),
                Float.toString(random.nextFloat()),
                IntStream.range(0, random.nextInt(100))
                        .mapToObj(i -> Float.toString(random.nextFloat()))
                        .collect(Collectors.toList()),
                IntStream.range(0, random.nextInt(100))
                        .boxed()
                        .collect(Collectors.toMap(
                                i -> Float.toString(random.nextFloat()),
                                i -> Float.toString(random.nextFloat()))
                        ),
                IntStream.range(0, random.nextInt(100))
                        .mapToObj(i -> Pair.of(Float.toString(random.nextFloat()), Float.toString(random.nextFloat())))
                        .collect(Collectors.toList()
                        ),
                new WednesdayData("meetings lol"),
                new SubData("mySubFieldData"));
    }

    private <T> void testWriteRead(final DynamicOps<T> ops) {
        final TestData data = makeRandomTestData();

        final DataResult<T> encoded = TestData.CODEC.encodeStart(ops, data);
        final DataResult<TestData> decoded = encoded.flatMap(r -> TestData.CODEC.parse(ops, r));

        Assertions.assertEquals(DataResult.success(data), decoded, "read(write(x)) == x");
    }

    private <T, U> void testReadWrite(final DynamicOps<T> ops, Function<DataResult<T>, U> equalityExtractor) {
        final TestData data = makeRandomTestData();

        final DataResult<T> encoded = TestData.CODEC.encodeStart(ops, data);
        final DataResult<TestData> decoded = encoded.flatMap(r -> TestData.CODEC.parse(ops, r));
        Assertions.assertEquals(DataResult.success(data), decoded, "read(write(x)) == x");

        final DataResult<T> reEncoded = decoded.flatMap(r -> TestData.CODEC.encodeStart(ops, r));

        Assertions.assertEquals(equalityExtractor.apply(encoded), equalityExtractor.apply(reEncoded), "write(read(x)) == x");
    }

    private <OTHER, OURS> void testConversionRead(final DynamicOps<OURS> testOps, final DynamicOps<OTHER> otherOps) {
        final TestData data = makeRandomTestData();

        final DataResult<OURS> encoded = TestData.CODEC.encodeStart(testOps, data);
        final DataResult<OTHER> converted = encoded.map(e -> testOps.convertTo(otherOps, e));
        final DataResult<TestData> decoded = converted.flatMap(r -> TestData.CODEC.parse(otherOps, r));

        Assertions.assertEquals(DataResult.success(data), decoded, "read(convert(write(x))) == x");
    }

    private <T> void testDumpParse(final DynamicOps<T> ops, Function<T, String> stringifier, Function<String, T> parser) {
        final TestData data = makeRandomTestData();

        final T encoded = TestData.CODEC.encodeStart(ops, data).getOrThrow();
        String dumped = stringifier.apply(encoded);
        //System.out.println(dumped);
        T rootNode = parser.apply(dumped);
        DataResult<TestData> parsed = TestData.CODEC.parse(ops, rootNode);

        Assertions.assertEquals(DataResult.success(data), parsed, "parse(dump(x)) == x");
    }

    @Test
    public void testWriteConvertReadSnake() {
        testConversionRead(new SnakeYamlOps(), JsonOps.INSTANCE);
    }

    @Test
    public void testWriteReadNormalSnake() {
        testWriteRead(new SnakeYamlOps());
    }

    @Test
    public void testReadWriteNormalSnake() {
        testReadWrite(new SnakeYamlOps(), dr-> dr.map(CodecTest::toGeneric));
    }

    @Test
    public void testDumpParseSnakeYaml() {
        SnakeYamlOps ops = new SnakeYamlOps();
        testDumpParse(ops, YamlHelper::dumpString, YamlHelper::load);
    }

    //convert nodes to generic objects, as they natively compare by instance (not contents)
    private static Object toGeneric(Node node) {
        if (node instanceof ScalarNode scalarNode) {
            return scalarNode.getValue();
        }
        if (node instanceof MappingNode mappingNode) {
            return mappingNode.getValue().stream().map(t->Map.entry(toGeneric(t.getKeyNode()), toGeneric(t.getValueNode()))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (node instanceof SequenceNode sequenceNode) {
            return sequenceNode.getValue().stream().map(CodecTest::toGeneric).toList();
        }
        return node;
    }
}