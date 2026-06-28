package com.bloodpressure.app.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile BloodPressureDao _bloodPressureDao;

  private volatile MedicationDao _medicationDao;

  private volatile MedicationRecordDao _medicationRecordDao;

  private volatile DosageChangeDao _dosageChangeDao;

  private volatile TrainingDao _trainingDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `blood_pressure_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `systolic` INTEGER NOT NULL, `diastolic` INTEGER NOT NULL, `heartRate` INTEGER NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `period` TEXT NOT NULL, `medicationTaken` INTEGER NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `medications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `dosage` TEXT NOT NULL, `isActive` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `medication_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `medicationId` INTEGER NOT NULL, `date` TEXT NOT NULL, `taken` INTEGER NOT NULL, `dosage` TEXT NOT NULL, `takenAt` INTEGER, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `dosage_changes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `medicationId` INTEGER NOT NULL, `date` TEXT NOT NULL, `oldDosage` TEXT NOT NULL, `newDosage` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `training_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `totalDuration` INTEGER NOT NULL, `completedGroups` INTEGER NOT NULL, `totalGroups` INTEGER NOT NULL, `groupDuration` INTEGER NOT NULL, `restDuration` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `notes` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fc12721ceba908697fa383a2a8362d80')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `blood_pressure_records`");
        db.execSQL("DROP TABLE IF EXISTS `medications`");
        db.execSQL("DROP TABLE IF EXISTS `medication_records`");
        db.execSQL("DROP TABLE IF EXISTS `dosage_changes`");
        db.execSQL("DROP TABLE IF EXISTS `training_records`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsBloodPressureRecords = new HashMap<String, TableInfo.Column>(10);
        _columnsBloodPressureRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("systolic", new TableInfo.Column("systolic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("diastolic", new TableInfo.Column("diastolic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("heartRate", new TableInfo.Column("heartRate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("time", new TableInfo.Column("time", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("period", new TableInfo.Column("period", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("medicationTaken", new TableInfo.Column("medicationTaken", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureRecords.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBloodPressureRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBloodPressureRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBloodPressureRecords = new TableInfo("blood_pressure_records", _columnsBloodPressureRecords, _foreignKeysBloodPressureRecords, _indicesBloodPressureRecords);
        final TableInfo _existingBloodPressureRecords = TableInfo.read(db, "blood_pressure_records");
        if (!_infoBloodPressureRecords.equals(_existingBloodPressureRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "blood_pressure_records(com.bloodpressure.app.data.BloodPressureRecord).\n"
                  + " Expected:\n" + _infoBloodPressureRecords + "\n"
                  + " Found:\n" + _existingBloodPressureRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsMedications = new HashMap<String, TableInfo.Column>(4);
        _columnsMedications.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("dosage", new TableInfo.Column("dosage", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedications = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedications = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMedications = new TableInfo("medications", _columnsMedications, _foreignKeysMedications, _indicesMedications);
        final TableInfo _existingMedications = TableInfo.read(db, "medications");
        if (!_infoMedications.equals(_existingMedications)) {
          return new RoomOpenHelper.ValidationResult(false, "medications(com.bloodpressure.app.data.Medication).\n"
                  + " Expected:\n" + _infoMedications + "\n"
                  + " Found:\n" + _existingMedications);
        }
        final HashMap<String, TableInfo.Column> _columnsMedicationRecords = new HashMap<String, TableInfo.Column>(7);
        _columnsMedicationRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationRecords.put("medicationId", new TableInfo.Column("medicationId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationRecords.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationRecords.put("taken", new TableInfo.Column("taken", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationRecords.put("dosage", new TableInfo.Column("dosage", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationRecords.put("takenAt", new TableInfo.Column("takenAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationRecords.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedicationRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedicationRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMedicationRecords = new TableInfo("medication_records", _columnsMedicationRecords, _foreignKeysMedicationRecords, _indicesMedicationRecords);
        final TableInfo _existingMedicationRecords = TableInfo.read(db, "medication_records");
        if (!_infoMedicationRecords.equals(_existingMedicationRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "medication_records(com.bloodpressure.app.data.MedicationRecord).\n"
                  + " Expected:\n" + _infoMedicationRecords + "\n"
                  + " Found:\n" + _existingMedicationRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsDosageChanges = new HashMap<String, TableInfo.Column>(7);
        _columnsDosageChanges.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDosageChanges.put("medicationId", new TableInfo.Column("medicationId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDosageChanges.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDosageChanges.put("oldDosage", new TableInfo.Column("oldDosage", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDosageChanges.put("newDosage", new TableInfo.Column("newDosage", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDosageChanges.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDosageChanges.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDosageChanges = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDosageChanges = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDosageChanges = new TableInfo("dosage_changes", _columnsDosageChanges, _foreignKeysDosageChanges, _indicesDosageChanges);
        final TableInfo _existingDosageChanges = TableInfo.read(db, "dosage_changes");
        if (!_infoDosageChanges.equals(_existingDosageChanges)) {
          return new RoomOpenHelper.ValidationResult(false, "dosage_changes(com.bloodpressure.app.data.DosageChange).\n"
                  + " Expected:\n" + _infoDosageChanges + "\n"
                  + " Found:\n" + _existingDosageChanges);
        }
        final HashMap<String, TableInfo.Column> _columnsTrainingRecords = new HashMap<String, TableInfo.Column>(10);
        _columnsTrainingRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("totalDuration", new TableInfo.Column("totalDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("completedGroups", new TableInfo.Column("completedGroups", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("totalGroups", new TableInfo.Column("totalGroups", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("groupDuration", new TableInfo.Column("groupDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("restDuration", new TableInfo.Column("restDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("completed", new TableInfo.Column("completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrainingRecords.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrainingRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTrainingRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTrainingRecords = new TableInfo("training_records", _columnsTrainingRecords, _foreignKeysTrainingRecords, _indicesTrainingRecords);
        final TableInfo _existingTrainingRecords = TableInfo.read(db, "training_records");
        if (!_infoTrainingRecords.equals(_existingTrainingRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "training_records(com.bloodpressure.app.data.TrainingRecord).\n"
                  + " Expected:\n" + _infoTrainingRecords + "\n"
                  + " Found:\n" + _existingTrainingRecords);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "fc12721ceba908697fa383a2a8362d80", "bcbf0f92dcd3d23f5968352bd901f0d0");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "blood_pressure_records","medications","medication_records","dosage_changes","training_records");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `blood_pressure_records`");
      _db.execSQL("DELETE FROM `medications`");
      _db.execSQL("DELETE FROM `medication_records`");
      _db.execSQL("DELETE FROM `dosage_changes`");
      _db.execSQL("DELETE FROM `training_records`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BloodPressureDao.class, BloodPressureDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MedicationDao.class, MedicationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MedicationRecordDao.class, MedicationRecordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DosageChangeDao.class, DosageChangeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TrainingDao.class, TrainingDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BloodPressureDao bloodPressureDao() {
    if (_bloodPressureDao != null) {
      return _bloodPressureDao;
    } else {
      synchronized(this) {
        if(_bloodPressureDao == null) {
          _bloodPressureDao = new BloodPressureDao_Impl(this);
        }
        return _bloodPressureDao;
      }
    }
  }

  @Override
  public MedicationDao medicationDao() {
    if (_medicationDao != null) {
      return _medicationDao;
    } else {
      synchronized(this) {
        if(_medicationDao == null) {
          _medicationDao = new MedicationDao_Impl(this);
        }
        return _medicationDao;
      }
    }
  }

  @Override
  public MedicationRecordDao medicationRecordDao() {
    if (_medicationRecordDao != null) {
      return _medicationRecordDao;
    } else {
      synchronized(this) {
        if(_medicationRecordDao == null) {
          _medicationRecordDao = new MedicationRecordDao_Impl(this);
        }
        return _medicationRecordDao;
      }
    }
  }

  @Override
  public DosageChangeDao dosageChangeDao() {
    if (_dosageChangeDao != null) {
      return _dosageChangeDao;
    } else {
      synchronized(this) {
        if(_dosageChangeDao == null) {
          _dosageChangeDao = new DosageChangeDao_Impl(this);
        }
        return _dosageChangeDao;
      }
    }
  }

  @Override
  public TrainingDao trainingDao() {
    if (_trainingDao != null) {
      return _trainingDao;
    } else {
      synchronized(this) {
        if(_trainingDao == null) {
          _trainingDao = new TrainingDao_Impl(this);
        }
        return _trainingDao;
      }
    }
  }
}
