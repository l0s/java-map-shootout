/**
 * Copyright © 2020 Carlos Macasaet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.macasaet.shootout;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * This class benchmarks and compares various Java {@link Map}
 * implementations. It is based on prior work by Tessil here:
 * https://tessil.github.io/2016/08/29/benchmark-hopscotch-map.html and
 * Nick Welch here: https://github.com/mackstann/hash-table-shootout . I
 * excluded any tests that assumed a hash table implementation as I also
 * plan to compare tree-based implementations.
 *
 * @see https://tessil.github.io/2016/08/29/benchmark-hopscotch-map.html
 * @see https://github.com/mackstann/hash-table-shootout
 */
public class MapShootout {

    private final Random random = new Random();
    private final List<MapSupplier> mapSuppliers;

    private PrintWriter outputSink;

    public MapShootout(final List<MapSupplier> mapSuppliers) {
        Objects.requireNonNull(mapSuppliers);
        this.mapSuppliers = mapSuppliers;
    }

    public MapShootout() {
        this(Arrays.asList(JdkHashTables.JDK_HASH_MAP,
                JdkHashTables.JDK_LINKED_HASH_MAP, JdkSearchTrees.JDK_TREE_MAP));
    }

    @TestFactory
    public Stream<DynamicNode> benchmarkMapImplementations() {
        final int smallStringLength = 16;
        final int largeStringLength = 64;

        final List<String> largeStringKeys = generateStringKeys(largeStringLength, 3_000_000);

        return Stream.of(
                dynamicContainer("Large String Tests",
                        IntStream.iterate(3_000_000, size -> size > 0, size -> size - 200_000)
                        .mapToObj(size -> largeStringKeys.subList(0, size))
                        .map(keys -> createStringTests(keys, "largeString", largeStringLength))),
                dynamicContainer("Small String Tests",
                    IntStream.iterate(3_000_000, size -> size > 0, size -> size - 200_000)
                        .mapToObj(size -> largeStringKeys
                                .subList(0, size)
                                .stream()
                                .map(largeString -> largeString.substring(0, smallStringLength))
                                .collect(Collectors.toList()))
                        .map(keys -> createStringTests(keys, "smallString", smallStringLength))),
                dynamicContainer("Integer Tests",
                    IntStream.iterate(3_000_000, size -> size > 0, size -> size - 200_000).mapToObj(size -> {
                        final var nonNegativeKeys = generateIntegerKeys(0, size);
                        final var fullKeys = generateIntegerKeys(Long.MIN_VALUE, size);
                        return createIntegerTests(nonNegativeKeys, fullKeys);
                    }))
        );
    }

    protected List<String> generateStringKeys(final int stringLength, final int numKeys) {
        return IntStream.range(0, numKeys).mapToObj(ignore -> {
            final var builder = new StringBuilder(stringLength);
            random.ints(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
                .filter(this::isValidCodePoint)
                .limit(stringLength)
                .forEach(builder::appendCodePoint);
            return builder.toString();
        }).collect(Collectors.toList());
    }

    protected boolean isValidCodePoint(final int codePoint) {
        if (codePoint >= 0xe000 && codePoint <= 0xf8ff) {
            // basic multilingual plane (6,400 code points)
            return false;
        } else if (codePoint >= 0xf0000 && codePoint <= 0xfffff) {
            // Supplementary Private Use Area-A (65,534 code points)
            return false;
        } else if (codePoint >= 0x100000 && codePoint <= 0x10ffff) {
            // Supplementary Private Use Area-B (65,534 code points)
            return false;
        } else if (codePoint >= 0xd800 && codePoint <= 0xdfff) {
            // From jqwik DefaultCharacterArbitrary
            // https://en.wikipedia.org/wiki/UTF-16#U+D800_to_U+DFFF
            return false;
        } else if (codePoint >= 0xfdd0 && codePoint <= 0xfdef) {
            // From jqwik DefaultCharacterArbitrary
            return false;
        } else if (codePoint == 0xfffe || codePoint == 0xffff) {
            // From jqwik DefaultCharacterArbitrary
            return false;
        }
        return true;
    }

    protected List<Long> generateIntegerKeys(final long minValue, final int size) {
        return random.longs(size, minValue, Long.MAX_VALUE)
                .collect(() -> new ArrayList<Long>(size),
                        (list, key) -> list.add(key),
                        (x, y) -> x.addAll(y));
    }

    protected DynamicContainer createIntegerTests(final Collection<Long> nonNegativeKeys, final Collection<Long> fullKeys) {
        return dynamicContainer(nonNegativeKeys.size() + " keys",
                getMapSuppliers().stream()
                .map(mapImplementation -> createIntegerTests(nonNegativeKeys, fullKeys, mapImplementation))
        );
    }

    protected DynamicContainer createIntegerTests(final Collection<Long> nonNegativeKeys, Collection<Long> fullKeys, final MapSupplier mapImplementation) {
        final int size = nonNegativeKeys.size();
        final String keyLabel = "int64";
        final Supplier<Map<Long, Long>> mapSupplier = mapImplementation::createLongMap;
        final List<Long> differentKeys = generateIntegerKeys(Long.MIN_VALUE, size);

        return dynamicContainer(mapImplementation.name(),
                Stream.of(new Inserts<>(mapImplementation, mapSupplier, keyLabel, "randomShuffleInserts", nonNegativeKeys),
                        new Inserts<>(mapImplementation, mapSupplier, keyLabel, "randomShuffleFullInserts", fullKeys),
                        new Deletes<>(mapImplementation, mapSupplier, keyLabel, "randomShuffleFullDeletes", fullKeys),
                        new Reads<>(mapImplementation, mapSupplier, keyLabel, "randomShuffleReads", nonNegativeKeys),
                        new Reads<>(mapImplementation, mapSupplier, keyLabel, "randomShuffleFullReads", fullKeys),
                        new ReadMisses<>(mapImplementation, mapSupplier, keyLabel, "randomShuffleFullReadMisses", fullKeys, differentKeys),
                        new ReadsAfterDeletingHalf<>(mapImplementation, mapSupplier, keyLabel, "randomShuffleFullReadsAfterDeletingHalf", fullKeys),
                        new FullIteration<>(mapImplementation, mapSupplier, keyLabel, "randomFullIteration", fullKeys)
                )
                .map(MapBenchmark::asDynamicTest)
        );
    }

    protected DynamicContainer createStringTests(final Collection<String> keys, final String keyLabel, final int stringKeyLength) {
        return dynamicContainer(keys.size() + " keys",
                getMapSuppliers().stream()
                .map(implementation -> createStringTests(keyLabel, keys, implementation, stringKeyLength))
        );
    }

    protected DynamicContainer createStringTests(final String keyLabel, final Collection<String> keys, final MapSupplier mapImplementation, final int stringKeyLength) {
        final int size = keys.size();
        final Supplier<Map<String, Long>> supplier = mapImplementation::createStringMap;
        final List<String> differentKeys = generateStringKeys(stringKeyLength, size);

        return dynamicContainer(mapImplementation.name(),
                Stream.of(new Inserts<>(mapImplementation, supplier, keyLabel, "inserts", keys),
                    new Deletes<>(mapImplementation, supplier, keyLabel, "deletes", keys),
                    new Reads<>(mapImplementation, supplier, keyLabel, "reads", keys),
                    new ReadMisses<>(mapImplementation, supplier, keyLabel, "readMisses", keys, differentKeys),
                    new ReadsAfterDeletingHalf<>(mapImplementation, supplier, keyLabel, "readsAfterDeletingHalf", keys)
                )
                .map(MapBenchmark::asDynamicTest)
        );
    }

    protected class Inserts<K> extends MapBenchmark<K> {

        public Inserts(MapSupplier implementation, Supplier<Map<K, Long>> mapSupplier, String keyLabel,
                String testLabel, Collection<K> keys) {
            super(implementation, mapSupplier, keyLabel, testLabel, keys);
        }

        protected void benchmark(final Map<K, Long> map) {
            getKeys().forEach(key -> map.put(key, 1l));
        }

        protected PrintWriter getWriter() {
            return MapShootout.this.getWriter();
        }
        
    }

    protected class Deletes<K> extends MapBenchmark<K> {

        private List<K> deletionKeys;

        public Deletes(MapSupplier implementation, Supplier<Map<K, Long>> mapSupplier, String keyLabel,
                String testLabel, Collection<K> keys) {
            super(implementation, mapSupplier, keyLabel, testLabel, keys);
        }

        protected void benchmark(final Map<K, Long> map) {
            deletionKeys.forEach(map::remove);
        }

        protected void init() {
            super.init();

            deletionKeys = new ArrayList<>(getKeys());
            Collections.shuffle(deletionKeys);

            getKeys().forEach(key -> getMap().put(key, 1l));
        }

        protected void destroy() {
            deletionKeys.clear();
            deletionKeys = null;

            super.destroy();
        }

        protected PrintWriter getWriter() {
            return MapShootout.this.getWriter();
        }
        
    }

    protected class Reads<K> extends MapBenchmark<K> {

        private List<K> readKeys;

        public Reads(MapSupplier implementation, Supplier<Map<K, Long>> mapSupplier, String keyLabel,
                String testLabel, Collection<K> keys) {
            super(implementation, mapSupplier, keyLabel, testLabel, keys);
        }

        protected void init() {
            super.init();

            readKeys = new ArrayList<>(getKeys());
            Collections.shuffle(readKeys);

            getKeys().forEach(key -> getMap().put(key, 1l));
        }

        protected void destroy() {
            readKeys.clear();
            readKeys = null;

            super.destroy();
        }

        protected void benchmark(final Map<K, Long> map) {
            readKeys.forEach(map::get);
        }

        protected PrintWriter getWriter() {
            return MapShootout.this.getWriter();
        }
        
    }

    protected class ReadMisses<K> extends MapBenchmark<K> {
        
        private final Collection<K> differentKeys;

        public ReadMisses(MapSupplier implementation, Supplier<Map<K, Long>> mapSupplier, String keyLabel,
                String testLabel, Collection<K> keys, final Collection<K> differentKeys) {
            super(implementation, mapSupplier, keyLabel, testLabel, keys);
            Objects.requireNonNull(differentKeys);
            if (differentKeys.size() != keys.size()) {
                throw new IllegalArgumentException("key count mismatch");
            }
            this.differentKeys = differentKeys;
        }

        protected void init() {
            super.init();

            getKeys().forEach(key -> getMap().put(key, 1l));
        }

        protected void benchmark(final Map<K, Long> map) {
            differentKeys.forEach(map::get);
        }

        protected PrintWriter getWriter() {
            return MapShootout.this.getWriter();
        }
        
    }

    protected class ReadsAfterDeletingHalf<K> extends MapBenchmark<K> {

        private List<K> readKeys;

        public ReadsAfterDeletingHalf(MapSupplier implementation, Supplier<Map<K, Long>> mapSupplier,
                String keyLabel, String testLabel, Collection<K> keys) {
            super(implementation, mapSupplier, keyLabel, testLabel, keys);
        }

        protected void init() {
            super.init();
            
            readKeys = new ArrayList<>(getKeys());
            Collections.shuffle(readKeys);
            readKeys.subList(0, readKeys.size() / 2).forEach(getMap()::remove);
            Collections.shuffle(readKeys);
        }

        protected void destroy() {
            readKeys.clear();
            readKeys = null;

            super.destroy();
        }

        protected void benchmark(final Map<K, Long> map) {
            readKeys.forEach(map::get);
        }

        protected PrintWriter getWriter() {
            return MapShootout.this.getWriter();
        }

    }

    protected class FullIteration<K> extends MapBenchmark<K> {

        public FullIteration(MapSupplier implementation, Supplier<Map<K, Long>> mapSupplier, String keyLabel,
                String testLabel, Collection<K> keys) {
            super(implementation, mapSupplier, keyLabel, testLabel, keys);
        }

        protected void init() {
            super.init();
            
            getKeys().forEach(key -> getMap().put(key, 1l));
        }

        protected void benchmark(Map<K, Long> map) {
            getMap().forEach((key, value) -> {
            });
        }

        protected PrintWriter getWriter() {
            return MapShootout.this.getWriter();
        }
        
    }

    protected List<MapSupplier> getMapSuppliers() {
        return mapSuppliers;
    }

    protected PrintWriter getWriter() {
        if (outputSink == null) {
            setWriter(new PrintWriter(System.out));
        }
        return outputSink;
    }

    protected void setWriter(final PrintWriter outputSink) {
        Objects.requireNonNull(outputSink);
        this.outputSink = outputSink;
    }

}