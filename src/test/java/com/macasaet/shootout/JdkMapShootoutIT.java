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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

@Tag("benchmark")
public class JdkMapShootoutIT extends MapShootout {

    private static PrintWriter outputSink;

    public JdkMapShootoutIT() throws FileNotFoundException {
        super(Arrays.asList(JdkHashTables.JDK_HASH_MAP,
            JdkHashTables.JDK_LINKED_HASH_MAP,
            JdkSearchTrees.JDK_TREE_MAP));

        setWriter(outputSink);
    }

    @BeforeAll
    public static void setUp() throws FileNotFoundException {
        outputSink = new PrintWriter("data.tsv");
    }

    @AfterAll
    public static void tearDown() throws IOException {
        outputSink.flush();
        outputSink.close();
    }
}