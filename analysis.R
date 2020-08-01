#     Copyright © 2020 Carlos Macasaet
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        https://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.

library( 'dplyr' )
library( 'ggplot2' )
library( 'svglite' )

data <-
  read.table( 'data.tsv',
              sep='\t',
              col.names=c( 'keyType', 'test', 'implementation', 'numKeys', 'timeNanos', 'memory' ) )
data$timeSeconds <- data$timeNanos / 1000000000

# filter out tree-based implementations
data <- data %>% filter( implementation != 'JDK_TREE_MAP' )

int_data <- data %>% filter( keyType == 'int64' )

ggplot( int_data %>% filter( test=='randomShuffleInserts' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Random Shuffle Inserts (non-negative keys)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-shuffle-inserts.svg' )

ggplot( int_data %>% filter( test=='randomShuffleFullInserts' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Random Shuffle Full Inserts (full key range)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-shuffle-full-inserts.svg' )

ggplot( int_data %>% filter( test=='randomShuffleFullDeletes' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Random Shuffle Full Deletes (full key range)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-shuffle-full-deletes.svg' )

ggplot( int_data %>% filter( test=='randomShuffleReads' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Random Shuffle Reads (non-negative keys)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-shuffle-reads.svg' )

ggplot( int_data %>% filter( test=='randomShuffleFullReads' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Random Shuffle Full Reads (full key range)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-shuffle-full-reads.svg' )

ggplot( int_data %>% filter( test=='randomShuffleFullReadMisses' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Random Shuffle Full Read Misses (full key range)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-shuffle-full-read-misses.svg' )

ggplot( int_data %>% filter( test=='randomShuffleFullReadsAfterDeletingHalf' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Random Shuffle Full Read After Deleting Half (full key range)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-shuffle-full-reads-after-deleting-half.svg' )

ggplot( int_data %>% filter( test=='randomFullIteration' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Map iteration (full key range)",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/int64-random-full-iteration.svg' )

ggplot( int_data %>% filter( test=='randomShuffleFullInserts' ),
        aes( x=numKeys, y=memory, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Int64::Memory usage after inserts (full key range)",
        x="Number of keys",
        y="Bytes" )
ggsave( 'images/int64-insert-memory.svg' )

small_string_data <- data %>% filter( keyType=='smallString' )

ggplot( small_string_data %>% filter( test=='inserts' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Small Strings::Inserts",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/small_string-inserts.svg' )

ggplot( small_string_data %>% filter( test=='deletes' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Small Strings::Deletes",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/small_string-deletes.svg' )

ggplot( small_string_data %>% filter( test=='reads' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Small Strings::Reads",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/small_string-reads.svg' )

ggplot( small_string_data %>% filter( test=='readMisses' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Small Strings::Read Misses",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/small_string-read-misses.svg' )

ggplot( small_string_data %>% filter( test=='readsAfterDeletingHalf' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Small Strings::Reads After Deleting Half",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/small_string-reads-after-deleting-half.svg' )

large_string_data <- data %>% filter( keyType=='largeString' )

ggplot( large_string_data %>% filter( test=='inserts' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Large Strings::Inserts",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/large_string-inserts.svg' )

ggplot( large_string_data %>% filter( test=='deletes' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Large Strings::Deletes",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/large_string-deletes.svg' )

ggplot( large_string_data %>% filter( test=='reads' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Large Strings::Reads",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/large_string-reads.svg' )

ggplot( large_string_data %>% filter( test=='readMisses' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Large Strings::Read Misses",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/large_string-read-misses.svg' )

ggplot( large_string_data %>% filter( test=='readsAfterDeletingHalf' ),
        aes( x=numKeys, y=timeSeconds, group=implementation ) ) +
  geom_line( aes( color=implementation ) ) +
  geom_point( aes( color=implementation ) ) +
  labs( title="Large Strings::Reads After Deleting Half",
        x="Number of keys",
        y="Time (seconds)" )
ggsave( 'images/large_string-reads-after-deleting-half.svg' )
