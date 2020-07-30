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
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

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

    public interface MapSupplier {
        Map<Long, Long> createLongMap();
        Map<String, Long> createStringMap();
        String name();
    }

    protected enum MapImplementation implements MapSupplier {
        JDK_HASH_MAP() {

            public Map<Long, Long> createLongMap() {
                return new HashMap<>();
            }

            public Map<String, Long> createStringMap() {
                return new HashMap<>();
            }

        },
        JDK_LINKED_HASH_MAP() {

            public Map<Long, Long> createLongMap() {
                return new LinkedHashMap<>();
            }

            public Map<String, Long> createStringMap() {
                return new LinkedHashMap<>();
            }
            
        },
        JDK_TREE_MAP() {
            public Map<Long, Long> createLongMap() {
                return new TreeMap<>();
            }

            public Map<String, Long> createStringMap() {
                return new TreeMap<>();
            }
        };

        protected static Stream<MapSupplier> stream() {
            return Arrays.stream(values());
        }

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
                .filter(this::isInvalidCodePoint)
                .limit(stringLength)
                .forEach(builder::appendCodePoint);
            return builder.toString();
        }).collect(Collectors.toList());
    }

    protected boolean isInvalidCodePoint(final int codePoint) {
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
        return random.longs(size, 0, Long.MAX_VALUE)
                .collect(() -> new ArrayList<Long>(size),
                        (list, key) -> list.add(key),
                        (x, y) -> x.addAll(y));
    }

    protected DynamicContainer createIntegerTests(final Collection<Long> nonNegativeKeys, Collection<Long> fullKeys) {
        return dynamicContainer(nonNegativeKeys.size() + " keys",
                MapImplementation.stream()
                .map(mapImplementation -> createIntegerTests(nonNegativeKeys, fullKeys, mapImplementation)));
    }

    protected DynamicContainer createIntegerTests(final Collection<Long> nonNegativeKeys, Collection<Long> fullKeys, final MapSupplier mapImplementation) {
        final int size = nonNegativeKeys.size();
        final String keyLabel = "int64";
        final Supplier<Map<Long, Long>> mapSupplier = mapImplementation::createLongMap;
        final List<Long> differentKeys = generateIntegerKeys(Long.MIN_VALUE, size);

        return dynamicContainer(mapImplementation.name(),
                Stream.of(createTest(mapImplementation, keyLabel, "randomShuffleInserts", size, () -> randomShuffleInserts(nonNegativeKeys, mapSupplier)),
                    createTest(mapImplementation, keyLabel, "randomShuffleFullInserts", size, () -> randomShuffleInserts(fullKeys, mapSupplier)),
                    createTest(mapImplementation, keyLabel, "randomShuffleFullDeletes", size, () -> randomShuffleFullDeletes(fullKeys, mapSupplier)),
                    createTest(mapImplementation, keyLabel, "randomShuffleReads", size, () -> randomShuffleReads(nonNegativeKeys, mapSupplier)),
                    createTest(mapImplementation, keyLabel, "randomShuffleFullReads", size, () -> randomShuffleReads(fullKeys, mapSupplier)),
                    createTest(mapImplementation, keyLabel, "randomShuffleFullReadMisses", size, () -> randomShuffleFullReadMisses(fullKeys, mapSupplier, differentKeys)),
                    createTest(mapImplementation, keyLabel, "randomShuffleFullReadsAfterDeletingHalf", size, () -> randomShuffleFullReadsAfterDeletingHalf(fullKeys, mapSupplier)),
                    createTest(mapImplementation, keyLabel, "randomFullIteration", size, () -> randomFullIteration(fullKeys, mapSupplier)))
                );
    }

    protected DynamicContainer createStringTests(final Collection<String> keys, final String keyLabel, final int stringKeyLength) {
        return dynamicContainer(keys.size() + " keys",
                MapImplementation.stream().map(implementation -> createStringTests(keyLabel, keys, implementation, stringKeyLength)));
    }

    protected DynamicContainer createStringTests(final String keyLabel, final Collection<String> keys, final MapSupplier mapImplementation, final int stringKeyLength) {
        final int size = keys.size();
        final Supplier<Map<String, Long>> supplier = mapImplementation::createStringMap;
        final List<String> differentKeys = generateStringKeys(stringKeyLength, size);

        return dynamicContainer(mapImplementation.name(),
                Stream.of(createTest(mapImplementation, keyLabel, "inserts", size, () -> randomShuffleInserts(keys, supplier)),
                        createTest(mapImplementation, keyLabel, "deletes", size, () -> randomShuffleFullDeletes(keys, supplier)),
                        createTest(mapImplementation, keyLabel, "reads", size, () -> randomShuffleReads(keys, supplier)),
                        createTest(mapImplementation, keyLabel, "readMisses", size, () -> randomShuffleFullReadMisses(keys, supplier, differentKeys)),
                        createTest(mapImplementation, keyLabel, "readsAfterDeletingHalf", size, () -> randomShuffleFullReadsAfterDeletingHalf(keys, supplier)))
                );
    }

    protected DynamicTest createTest(final MapSupplier mapSupplier, final String keyLabel, final String testLabel,
            final int size, final Callable<Long> invocation) {
        return dynamicTest(testLabel, emitTiming(mapSupplier, keyLabel, testLabel, size, invocation));
    }

    protected Executable emitTiming(final MapSupplier mapSupplier, final String keyLabel, final String testLabel,
            final int size, final Callable<Long> invocation) {
        return () -> {
            System.gc();
            final long elapsedNanoSeconds = invocation.call();
            // FIXME output timing to a file so that memory usage can be output to a separate file
            System.out.println(
                    keyLabel + '\t' + testLabel + '\t' + mapSupplier.name() + '\t' + size + '\t' + elapsedNanoSeconds);
        };
    }

    public final <K> long randomShuffleInserts(final Collection<K> keys, final Supplier<Map<K, Long>> mapSupplier) {
        final var map = mapSupplier.get();

        final var startNanos = System.nanoTime();
        keys.forEach(key -> map.put(key, 1l));
        final var endNanos = System.nanoTime();

        return endNanos - startNanos;
    }

    public final <K> long randomShuffleFullDeletes(final Collection<K> keys, final Supplier<Map<K, Long>> mapSupplier) {
        final var map = mapSupplier.get();
        final var deletionKeys = new ArrayList<>(keys);
        Collections.shuffle(deletionKeys);
        keys.forEach(key -> map.put(key, 1l));

        final var startNanos = System.nanoTime();
        deletionKeys.forEach(map::remove);
        final var endNanos = System.nanoTime();

        // clean up
        map.clear();
        deletionKeys.clear();

        return endNanos - startNanos;
    }

    public final <K> long randomShuffleReads(final Collection<K> keys, final Supplier<Map<K, Long>> mapSupplier) {
        final var map = mapSupplier.get();
        final var readKeys = new ArrayList<>(keys);
        Collections.shuffle(readKeys);
        keys.forEach(key -> map.put(key, 1l));

        final var startNanos = System.nanoTime();
        readKeys.forEach(map::get);
        final var endNanos = System.nanoTime();

        // clean up
        map.clear();
        readKeys.clear();

        return endNanos - startNanos;
    }

    public final <K> long randomShuffleFullReadMisses(final Collection<K> keys,
            final Supplier<Map<K, Long>> mapSupplier, final Collection<K> differentKeys) {
        final var map = mapSupplier.get();
        keys.forEach(key -> map.put(key, 1l));

        final var startNanos = System.nanoTime();
        differentKeys.forEach(map::get);
        final var endNanos = System.nanoTime();

        // clean up
        map.clear();

        return endNanos - startNanos;
    }

    public final <K> long randomShuffleFullReadsAfterDeletingHalf(final Collection<K> keys,
            final Supplier<Map<K, Long>> mapSupplier) {
        final var map = mapSupplier.get();
        keys.forEach(key -> map.put(key, 1l));

        final var readKeys = new ArrayList<>(keys);
        Collections.shuffle(readKeys);        
        readKeys.subList(0, readKeys.size() / 2).forEach(map::remove);
        Collections.shuffle(readKeys);

        final var startNanos = System.nanoTime();
        readKeys.forEach(map::get);
        final var endNanos = System.nanoTime();

        // clean up
        map.clear();
        readKeys.clear();

        return endNanos - startNanos;
    }

    public final <K> long randomFullIteration(final Collection<K> keys, final Supplier<Map<K, Long>> mapSupplier) {
        final var map = mapSupplier.get();
        keys.forEach(key -> map.put(key, 1l));

        final var startNanos = System.nanoTime();
        map.forEach((key, value) -> {
        });
        final var endNanos = System.nanoTime();

        // clean up
        map.clear();

        return endNanos - startNanos;
    }

    // FIXME measure memory usage for int64, small string, and large string
}