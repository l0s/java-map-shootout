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

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.junit.jupiter.api.DynamicTest;

/**
 * A Map operation performance test that can be converted to a {@link DynamicTest}.
 *
 * @param <K> the Map's key type
 */
public abstract class MapBenchmark<K> {

    private final MapSupplier implementation;
    private final Supplier<Map<K, Long>> mapSupplier;
    private final String keyLabel;
    private final String testLabel;
    private final Collection<K> keys;

    private Map<K, Long> map;

    protected MapBenchmark(final MapSupplier implementation, final Supplier<Map<K, Long>> mapSupplier,
            final String keyLabel, final String testLabel, final Collection<K> keys) {
        Objects.requireNonNull(implementation);
        Objects.requireNonNull(mapSupplier);
        Objects.requireNonNull(keyLabel);
        Objects.requireNonNull(testLabel);
        Objects.requireNonNull(keys);
        this.implementation = implementation;
        this.mapSupplier = mapSupplier;
        this.keyLabel = keyLabel;
        this.testLabel = testLabel;
        this.keys = keys;
    }

    public DynamicTest asDynamicTest() {
        return dynamicTest(getTestLabel(), this::execute);
    }

    /**
     * Create a new map and perform any other test setup. If your metric
     * requires more preparation other than instantiating a map, then
     * override this method. If you do allocate resources, be sure to
     * release them in {@link #destroy()}.
     * 
     * @see #destroy()
     */
    protected void init() {
        setMap(getMapSupplier().get());
    }

    /**
     * Prepares the test, runs the test, then cleans up resources. The test
     * invocation is also timed and memory consumption is measured. Finally,
     * the results are emitted to stdout.
     * @throws IOException 
     */
    protected void execute() throws IOException {
        init();
        final var runtime = Runtime.getRuntime();

        // make a best effort to proactively garbage collect resources to
        // reduce, but not eliminate, the likelihood that garbage collection
        // affects test performance
        runtime.gc();

        final var startMemory = runtime.totalMemory() - runtime.freeMemory();
        final var startNanos = System.nanoTime();
        benchmark(getMap());
        final var endNanos = System.nanoTime();
        final var endMemory = runtime.totalMemory() - runtime.freeMemory();

        // release memory references to improve the likelihood that GC will
        // happen before the next benchmark
        destroy();

        final var elapsedNanos = endNanos - startNanos;
        final var consumedMemory = endMemory - startMemory;

        final var out = getWriter();
        out.write(keyLabel);
        out.append('\t');
        out.write(testLabel);
        out.append('\t');
        out.write(getImplementation().name());
        out.append('\t');
        out.write(String.valueOf(getKeys().size()));
        out.append('\t');
        out.write(String.valueOf(elapsedNanos));
        out.append('\t');
        out.write(String.valueOf(consumedMemory));
        out.println();
        out.flush();
    }

    /**
     * Release memory to reduce the likelihood that garbage collection
     * interferes with the next test. If your test allocates additional
     * resources via the {@link #init()} method, then release them here.
     * 
     * @see #init()
     */
    protected void destroy() {
        getMap().clear();
        setMap(null);
    }

    /**
     * Perform the actual test. This method will automatically be timed and
     * memory consumption before and after will be measured. This method
     * should *not* set up any test fixtures or dispose of resources. Use
     * {@link #init()} and {@link #destroy()} for that.
     *
     * @param map a new map instance specifically for this test
     */
    protected abstract void benchmark(Map<K, Long> map);

    protected Supplier<Map<K, Long>> getMapSupplier() {
        return mapSupplier;
    }

    protected String getKeyLabel() {
        return keyLabel;
    }

    protected String getTestLabel() {
        return testLabel;
    }

    protected Map<K, Long> getMap() {
        return map;
    }

    protected void setMap(final Map<K, Long> map) {
        this.map = map;
    }

    protected MapSupplier getImplementation() {
        return implementation;
    }

    protected Collection<K> getKeys() {
        return keys;
    }

    protected abstract PrintWriter getWriter();
}