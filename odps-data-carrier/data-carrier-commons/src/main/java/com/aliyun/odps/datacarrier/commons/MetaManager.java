package com.aliyun.odps.datacarrier.commons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Jon (wangzhong.zw@alibaba-inc.com)
 *
 * MetaManager reads/writes metadata from/to a directory. The structure is as follows:
 *
 *  [output directory]
 *  |______hive-meta-carrier
 *         |______global.json
 *         |______table_meta
 *         |      |______[database name].json
 *         |      |______[database name]
 *         |             |______[table name].json
 *         |
 *         |______partition_meta
 *                |______[database name]
 *                       |______[table name].json
 *
 * The format of global.json is as follows:
 * {
 *   "datasourceType" : xxx,
 *   "odpsVersion" : "xxx"
 * }
 *
 * The format of [table name].json is as follows:
 * {
 *    "tableName" : "table_1",
 *    "odpsTableName" : "odps_table_1",
 *    "lifeCycle" : 10,
 *    "comment" : "first table",
 *    "ifNotExist" : true,
 *    "columns" : [
 *        {
 *            "name" : "column_1",
 *            "odpsColumnName" : "odps_column_1",
 *            "type" : "bigint",
 *            "comment" : "xxx"
 *        },
 *        ...
 *    ],
 *    "partitionColumns" : [
 *        {
 *            "name" : "column_1",
 *            "odpsColumnName" : "odps_column_1",
 *            "type" : "bigint",
 *            "comment" : "xxx"
 *        },
 *        ...
 *    ]
 * }
 *
 * The format of table partition spec is as follows:
 * {
 *    "databaseName" : "db_1",
 *    "tableName" : "table_1",
 *    "partitions" : [
 *        {
 *            "partitionSpec" : "xxx",
 *            "location" : "xxx",
 *            "creationTime" : "xxx"
 *        },
 *        ...
 *    ]
 * }
 */
public class MetaManager {
  public static class GlobalMetaModel{
    public String datasourceType;
    public String odpsVersion = "1.0";
  }

  public static class DatabaseMetaModel {
    public String databaseName;
    public String odpsProjectName;
  }

  public static class TableMetaModel {
    public String tableName;
    public String odpsTableName;
    public Integer lifeCycle;
    public String comment;
    public Boolean ifNotExist = true;
    public List<ColumnMetaModel> columns = new ArrayList<>();
    public List<ColumnMetaModel> partitionColumns = new ArrayList<>();
  }

  public static class ColumnMetaModel {
    public String columnName;
    public String odpsColumnName;
    public String type;
    public String comment;
  }

  public static class PartitionMetaModel {
    public String partitionSpec;
    public String location;
    public String createTime;
  }

  public static class TablePartitionMetaModel {
    public String tableName;
    public List<PartitionMetaModel> partitions = new ArrayList<>();
  }

  private static final String GLOBAL = "global";
  private static final String TABLE_META_DIR = "table_meta";
  private static final String PARTITION_META_DIR = "partition_meta";
  private static final String JSON_SUFFIX = ".json";

  private String root;

  public MetaManager(String root) {
    this.root = root;
    File outputDir = new File(root);
    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw new IllegalArgumentException("Output directory does not exist and cannot be created.");
    }
    if (!outputDir.isDirectory()) {
      throw new IllegalArgumentException("Please specify an existing directory.");
    }
  }

  public GlobalMetaModel getGlobalMeta() throws IOException {
    Path filePath = Paths.get(this.root, GLOBAL + JSON_SUFFIX);
    String jsonString = DirUtils.readFromFile(filePath);
    return Constants.GSON.fromJson(jsonString, GlobalMetaModel.class);
  }

  public void setGlobalMeta(GlobalMetaModel globalMeta) throws IOException {
    Path filePath = Paths.get(this.root, GLOBAL + JSON_SUFFIX);
    String jsonString = Constants.GSON.toJson(globalMeta, GlobalMetaModel.class);
    DirUtils.writeToFile(filePath, jsonString);
  }

  public DatabaseMetaModel getDatabaseMeta(String databaseName) throws IOException {
    Path filePath = Paths.get(this.root, TABLE_META_DIR, databaseName + JSON_SUFFIX);
    String jsonString = DirUtils.readFromFile(filePath);
    return Constants.GSON.fromJson(jsonString, DatabaseMetaModel.class);
  }

  public void setDatabaseMeta(DatabaseMetaModel databaseMeta) throws IOException {
    Path filePath =
        Paths.get(this.root, TABLE_META_DIR, databaseMeta.databaseName + JSON_SUFFIX);
    String jsonString = Constants.GSON.toJson(databaseMeta, DatabaseMetaModel.class);
    DirUtils.writeToFile(filePath, jsonString);
  }

  public TableMetaModel getTableMeta(String databaseName, String tableName) throws IOException {
    Path filePath = Paths.get(this.root, TABLE_META_DIR, databaseName,
        tableName + JSON_SUFFIX);
    String jsonString = DirUtils.readFromFile(filePath);
    return Constants.GSON.fromJson(jsonString, TableMetaModel.class);
  }

  public void setTableMeta(String databaseName, TableMetaModel tableMetaModel)
      throws IOException {
    String jsonString = Constants.GSON.toJson(tableMetaModel, TableMetaModel.class);
    Path filePath = Paths.get(this.root, TABLE_META_DIR, databaseName,
        tableMetaModel.tableName + JSON_SUFFIX);
    DirUtils.writeToFile(filePath, jsonString);
  }

  public TablePartitionMetaModel getTablePartitionMeta(String databaseName, String tableName)
      throws IOException {
    Path filePath = Paths.get(this.root, PARTITION_META_DIR, databaseName,
        tableName + JSON_SUFFIX);
    String jsonString = DirUtils.readFromFile(filePath);
    return Constants.GSON.fromJson(jsonString, TablePartitionMetaModel.class);
  }

  public void setTablePartitionMeta(String databaseName,
      TablePartitionMetaModel tablePartitionMeta) throws IOException {
    Path filePath = Paths.get(this.root, PARTITION_META_DIR, databaseName,
        tablePartitionMeta.tableName + JSON_SUFFIX);
    String jsonString = Constants.GSON.toJson(tablePartitionMeta, TablePartitionMetaModel.class);
    DirUtils.writeToFile(filePath, jsonString);
  }

  public String[] listDatabases() {
    Path tableMetaDir = Paths.get(this.root, TABLE_META_DIR);
    return DirUtils.listDirs(tableMetaDir);
  }

  /**
   * Return table names in a given database, including both non-partition tables and partition
   * tables
   */
  public String[] listTables(String databaseName) {
    Path databaseDir = Paths.get(this.root, TABLE_META_DIR, databaseName);
    String[] tableMetaFiles = DirUtils.listFiles(databaseDir);

    // Remove .json
    for (int i = 0; i < tableMetaFiles.length; i++) {
      String tableMetaFile = tableMetaFiles[i];
      if (tableMetaFile.endsWith(JSON_SUFFIX)) {
        tableMetaFiles[i] =
            tableMetaFile.substring(0, tableMetaFile.length() - JSON_SUFFIX.length());
      } else {
        throw new IllegalArgumentException(
            "Table meta directory contains invalid file: " + tableMetaFile);
      }
    }
    return tableMetaFiles;
  }

  /**
   * Return only partition table names in a given database
   */
  public String[] listPartitionTables(String databaseName) {
    Path databaseDir = Paths.get(this.root, PARTITION_META_DIR, databaseName);
    if (!Files.exists(databaseDir)) {
      return new String[0];
    }
    String[] partitionMetaFiles = DirUtils.listFiles(databaseDir);

    // Remove .json
    for (int i = 0; i < partitionMetaFiles.length; i++) {
      String partitionMetaFile = partitionMetaFiles[i];
      if (partitionMetaFile.endsWith(JSON_SUFFIX)) {
        partitionMetaFiles[i] = partitionMetaFile.substring(
            partitionMetaFile.length() - JSON_SUFFIX.length());
      } else {
        throw new IllegalArgumentException(
            "Partition meta directory contains invalid file: " + partitionMetaFile);
      }
    }
    return partitionMetaFiles;
  }
}