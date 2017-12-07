package com.akolb;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.akolb.HMSClient.makeTable;
import static com.akolb.Main.createSchema;

/**
 * Actual benchmark code.
 */
class HMSBenchmarks {
  private static final Logger LOG = LoggerFactory.getLogger(HMSBenchmarks.class);

  static DescriptiveStatistics benchmarkListDatabases(MicroBenchmark benchmark,
                                                      final HMSClient client) {
    return benchmark.measure(client::getAllDatabasesNoException);
  }

  static DescriptiveStatistics benchmarkListAllTables(MicroBenchmark benchmark,
                                                      final HMSClient client,
                                                   final String dbName) {
    return benchmark.measure(() -> client.getAllTablesNoException(dbName));
  }

  static DescriptiveStatistics benchmarkTableCreate(MicroBenchmark bench,
                                                    final HMSClient client,
                                                    final String dbName,
                                                    final String tableName) {
    Table table = makeTable(dbName, tableName, null, null);

    return bench.measure(null,
        () -> client.createTableNoException(table),
        () -> client.dropTableNoException(dbName, tableName));
  }

  static DescriptiveStatistics benchmarkDeleteCreate(MicroBenchmark bench,
                                                     final HMSClient client,
                                                     final String dbName,
                                                     final String tableName) {
    Table table = makeTable(dbName, tableName, null, null);

    return bench.measure(
        () -> client.createTableNoException(table),
        () -> client.dropTableNoException(dbName, tableName),
        null);
  }

  static DescriptiveStatistics benchmarkNetworkLatency(MicroBenchmark bench,
                                                       final String server, int port) {
    return bench.measure(
        () -> {
          //noinspection EmptyTryBlock
          try (Socket socket = new Socket(server, port)) {
          } catch (IOException e) {
            LOG.error("socket connection failed", e);
          }
        });
  }

  static DescriptiveStatistics benchmarkGetTable(MicroBenchmark bench,
                                                 final HMSClient client,
                                                 final String dbName,
                                                 final String tableName) {
    createPartitionedTable(client, dbName, tableName);
    try {
      return bench.measure(() -> client.getTableNoException(dbName, tableName));
    } finally {
      client.dropTableNoException(dbName, tableName);
    }
  }

  static DescriptiveStatistics benchmarkListTables(MicroBenchmark bench,
                                                   HMSClient client,
                                                   String dbName,
                                                   int count) {
    // Create a bunch of tables
    String format = "tmp_table_%d";
    try {
      createManyTables(client, count, dbName, format);
      return bench.measure(() -> client.getAllTablesNoException(dbName));
    } finally {
      dropManyTables(client, count, dbName, format);
    }
  }

  static DescriptiveStatistics benchmarkCreatePartition(MicroBenchmark bench,
                                                        final HMSClient client,
                                                        final String dbName,
                                                        final String tableName) {
    createPartitionedTable(client, dbName, tableName);
    final List<String> values = Collections.singletonList("d1");
    try {
      Table t = client.getTable(dbName, tableName);
      Partition partition = HMSClient.makePartition(t, values);
      return bench.measure(null,
          () -> client.createPartitionNoException(partition),
          () -> client.dropPartitionNoException(dbName, tableName, values));
    } catch (TException e) {
      e.printStackTrace();
      return new DescriptiveStatistics();
    } finally {
      client.dropTableNoException(dbName, tableName);
    }
  }

  static DescriptiveStatistics benchmarkListPartition(MicroBenchmark bench,
                                                      final HMSClient client,
                                                      final String dbName,
                                                      final String tableName) {
    createPartitionedTable(client, dbName, tableName);
    try {
      client.addManyPartitions(dbName, tableName,
          Collections.singletonList("d"), 1);

      return bench.measure(() -> client.listPartitionsNoException(dbName, tableName));
    } catch (TException e) {
      e.printStackTrace();
      return new DescriptiveStatistics();
    } finally {
      client.dropTableNoException(dbName, tableName);
    }
  }

  static DescriptiveStatistics benchmarkListManyPartitions(MicroBenchmark bench,
                                                           final HMSClient client,
                                                           final String dbName,
                                                           final String tableName,
                                                           int howMany) {
    createPartitionedTable(client, dbName, tableName);
    try {
      client.addManyPartitions(dbName, tableName,
          Collections.singletonList("d"), howMany);
      LOG.debug("Created {} partitions", howMany);
      LOG.debug("started benchmark... ");
      return bench.measure(() -> client.listPartitionsNoException(dbName, tableName));
    } catch (TException e) {
      e.printStackTrace();
      return new DescriptiveStatistics();
    } finally {
      client.dropTableNoException(dbName, tableName);
    }
  }

  static DescriptiveStatistics benchmarkGetPartitions(final MicroBenchmark bench,
                                                      final HMSClient client,
                                                      final String dbName,
                                                      final String tableName,
                                                      int howMany) {
    createPartitionedTable(client, dbName, tableName);
    try {
      client.addManyPartitions(dbName, tableName,
          Collections.singletonList("d"), howMany);
      LOG.debug("Created {} partitions", howMany);
      LOG.debug("started benchmark... ");
      return bench.measure(() -> client.getPartitionsNoException(dbName, tableName));
    } catch (TException e) {
      e.printStackTrace();
      return new DescriptiveStatistics();
    } finally {
      client.dropTableNoException(dbName, tableName);
    }
  }

  static DescriptiveStatistics benchmarkDropPartition(MicroBenchmark bench,
                                                      final HMSClient client,
                                                      final String dbName,
                                                      final String tableName) {
    createPartitionedTable(client, dbName, tableName);
    final List<String> values = Collections.singletonList("d1");
    try {
      Table t = client.getTable(dbName, tableName);
      Partition partition = HMSClient.makePartition(t, values);
      return bench.measure(
          () -> client.createPartitionNoException(partition),
          () -> client.dropPartitionNoException(dbName, tableName, values),
          null);
    } catch (TException e) {
      e.printStackTrace();
      return new DescriptiveStatistics();
    } finally {
      client.dropTableNoException(dbName, tableName);
    }
  }


  private static void createManyTables(HMSClient client, int howMany, String dbName, String format) {
    List<FieldSchema> columns = createSchema(new ArrayList<>(Arrays.asList("name", "string")));
    List<FieldSchema> partitions = createSchema(new ArrayList<>(Arrays.asList("date", "string")));
    IntStream.range(0, howMany)
        .forEach(i ->
            client.createTableNoException(makeTable(dbName,
                String.format(format, i), columns, partitions)));
  }

  private static void dropManyTables(HMSClient client, int howMany, String dbName, String format) {
    IntStream.range(0, howMany)
        .forEach(i ->
            client.dropTableNoException(dbName, String.format(format, i)));
  }

  // Create a simple table with a single column and single partition
  private static void createPartitionedTable(HMSClient client, String dbName, String tableName) {
    client.createTableNoException(makeTable(dbName, tableName,
        createSchema(Collections.singletonList("name:string")),
        createSchema(Collections.singletonList("date"))));
  }

  static DescriptiveStatistics benchmarkGetNotificationId(MicroBenchmark benchmark,
                                                      final HMSClient client) {
    return benchmark.measure(client::getCurrentNotificationIdNoException);
  }

}
