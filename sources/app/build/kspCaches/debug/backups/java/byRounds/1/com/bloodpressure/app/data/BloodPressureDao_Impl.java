package com.bloodpressure.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.datetime.LocalDate;
import kotlinx.datetime.LocalTime;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BloodPressureDao_Impl implements BloodPressureDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BloodPressureRecord> __insertionAdapterOfBloodPressureRecord;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<BloodPressureRecord> __deletionAdapterOfBloodPressureRecord;

  private final SharedSQLiteStatement __preparedStmtOfUpdateRecord;

  public BloodPressureDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBloodPressureRecord = new EntityInsertionAdapter<BloodPressureRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `blood_pressure_records` (`id`,`systolic`,`diastolic`,`heartRate`,`date`,`time`,`period`,`medicationTaken`,`note`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BloodPressureRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSystolic());
        statement.bindLong(3, entity.getDiastolic());
        statement.bindLong(4, entity.getHeartRate());
        final String _tmp = __converters.fromDate(entity.getDate());
        statement.bindString(5, _tmp);
        final String _tmp_1 = __converters.fromTime(entity.getTime());
        statement.bindString(6, _tmp_1);
        final String _tmp_2 = __converters.fromPeriod(entity.getPeriod());
        statement.bindString(7, _tmp_2);
        final int _tmp_3 = entity.getMedicationTaken() ? 1 : 0;
        statement.bindLong(8, _tmp_3);
        statement.bindString(9, entity.getNote());
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfBloodPressureRecord = new EntityDeletionOrUpdateAdapter<BloodPressureRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `blood_pressure_records` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BloodPressureRecord entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateRecord = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE blood_pressure_records SET systolic = ?, diastolic = ?, heartRate = ?, date = ?, period = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final BloodPressureRecord record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfBloodPressureRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final BloodPressureRecord record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfBloodPressureRecord.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRecord(final long id, final int systolic, final int diastolic,
      final int heartRate, final LocalDate date, final MeasurementPeriod period,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateRecord.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, systolic);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, diastolic);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, heartRate);
        _argIndex = 4;
        final String _tmp = __converters.fromDate(date);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 5;
        final String _tmp_1 = __converters.fromPeriod(period);
        _stmt.bindString(_argIndex, _tmp_1);
        _argIndex = 6;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateRecord.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDate(final LocalDate date,
      final Continuation<? super List<BloodPressureRecord>> $completion) {
    final String _sql = "SELECT * FROM blood_pressure_records WHERE date = ? ORDER BY time ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromDate(date);
    _statement.bindString(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BloodPressureRecord>>() {
      @Override
      @NonNull
      public List<BloodPressureRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfPeriod = CursorUtil.getColumnIndexOrThrow(_cursor, "period");
          final int _cursorIndexOfMedicationTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationTaken");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<BloodPressureRecord> _result = new ArrayList<BloodPressureRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpHeartRate;
            _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            final LocalDate _tmpDate;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_1);
            final LocalTime _tmpTime;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfTime);
            _tmpTime = __converters.toTime(_tmp_2);
            final MeasurementPeriod _tmpPeriod;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfPeriod);
            _tmpPeriod = __converters.toPeriod(_tmp_3);
            final boolean _tmpMedicationTaken;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfMedicationTaken);
            _tmpMedicationTaken = _tmp_4 != 0;
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new BloodPressureRecord(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpHeartRate,_tmpDate,_tmpTime,_tmpPeriod,_tmpMedicationTaken,_tmpNote,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDateRange(final LocalDate from, final LocalDate to,
      final Continuation<? super List<BloodPressureRecord>> $completion) {
    final String _sql = "SELECT * FROM blood_pressure_records WHERE date BETWEEN ? AND ? ORDER BY date DESC, time ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromDate(from);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    final String _tmp_1 = __converters.fromDate(to);
    _statement.bindString(_argIndex, _tmp_1);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BloodPressureRecord>>() {
      @Override
      @NonNull
      public List<BloodPressureRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfPeriod = CursorUtil.getColumnIndexOrThrow(_cursor, "period");
          final int _cursorIndexOfMedicationTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationTaken");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<BloodPressureRecord> _result = new ArrayList<BloodPressureRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpHeartRate;
            _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            final LocalDate _tmpDate;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_2);
            final LocalTime _tmpTime;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfTime);
            _tmpTime = __converters.toTime(_tmp_3);
            final MeasurementPeriod _tmpPeriod;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfPeriod);
            _tmpPeriod = __converters.toPeriod(_tmp_4);
            final boolean _tmpMedicationTaken;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfMedicationTaken);
            _tmpMedicationTaken = _tmp_5 != 0;
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new BloodPressureRecord(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpHeartRate,_tmpDate,_tmpTime,_tmpPeriod,_tmpMedicationTaken,_tmpNote,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByPeriodAndDateRange(final MeasurementPeriod period, final LocalDate from,
      final LocalDate to, final Continuation<? super List<BloodPressureRecord>> $completion) {
    final String _sql = "SELECT * FROM blood_pressure_records WHERE period = ? AND date BETWEEN ? AND ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    final String _tmp = __converters.fromPeriod(period);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    final String _tmp_1 = __converters.fromDate(from);
    _statement.bindString(_argIndex, _tmp_1);
    _argIndex = 3;
    final String _tmp_2 = __converters.fromDate(to);
    _statement.bindString(_argIndex, _tmp_2);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BloodPressureRecord>>() {
      @Override
      @NonNull
      public List<BloodPressureRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfPeriod = CursorUtil.getColumnIndexOrThrow(_cursor, "period");
          final int _cursorIndexOfMedicationTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationTaken");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<BloodPressureRecord> _result = new ArrayList<BloodPressureRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpHeartRate;
            _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            final LocalDate _tmpDate;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_3);
            final LocalTime _tmpTime;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfTime);
            _tmpTime = __converters.toTime(_tmp_4);
            final MeasurementPeriod _tmpPeriod;
            final String _tmp_5;
            _tmp_5 = _cursor.getString(_cursorIndexOfPeriod);
            _tmpPeriod = __converters.toPeriod(_tmp_5);
            final boolean _tmpMedicationTaken;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfMedicationTaken);
            _tmpMedicationTaken = _tmp_6 != 0;
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new BloodPressureRecord(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpHeartRate,_tmpDate,_tmpTime,_tmpPeriod,_tmpMedicationTaken,_tmpNote,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDateAndPeriod(final LocalDate date, final MeasurementPeriod period,
      final Continuation<? super List<BloodPressureRecord>> $completion) {
    final String _sql = "SELECT * FROM blood_pressure_records WHERE date = ? AND period = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromDate(date);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    final String _tmp_1 = __converters.fromPeriod(period);
    _statement.bindString(_argIndex, _tmp_1);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BloodPressureRecord>>() {
      @Override
      @NonNull
      public List<BloodPressureRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfPeriod = CursorUtil.getColumnIndexOrThrow(_cursor, "period");
          final int _cursorIndexOfMedicationTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationTaken");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<BloodPressureRecord> _result = new ArrayList<BloodPressureRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpHeartRate;
            _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            final LocalDate _tmpDate;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_2);
            final LocalTime _tmpTime;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfTime);
            _tmpTime = __converters.toTime(_tmp_3);
            final MeasurementPeriod _tmpPeriod;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfPeriod);
            _tmpPeriod = __converters.toPeriod(_tmp_4);
            final boolean _tmpMedicationTaken;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfMedicationTaken);
            _tmpMedicationTaken = _tmp_5 != 0;
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new BloodPressureRecord(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpHeartRate,_tmpDate,_tmpTime,_tmpPeriod,_tmpMedicationTaken,_tmpNote,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecent(final int limit,
      final Continuation<? super List<BloodPressureRecord>> $completion) {
    final String _sql = "SELECT * FROM blood_pressure_records ORDER BY date DESC, time DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BloodPressureRecord>>() {
      @Override
      @NonNull
      public List<BloodPressureRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfPeriod = CursorUtil.getColumnIndexOrThrow(_cursor, "period");
          final int _cursorIndexOfMedicationTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationTaken");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<BloodPressureRecord> _result = new ArrayList<BloodPressureRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpHeartRate;
            _tmpHeartRate = _cursor.getInt(_cursorIndexOfHeartRate);
            final LocalDate _tmpDate;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp);
            final LocalTime _tmpTime;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfTime);
            _tmpTime = __converters.toTime(_tmp_1);
            final MeasurementPeriod _tmpPeriod;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfPeriod);
            _tmpPeriod = __converters.toPeriod(_tmp_2);
            final boolean _tmpMedicationTaken;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfMedicationTaken);
            _tmpMedicationTaken = _tmp_3 != 0;
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new BloodPressureRecord(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpHeartRate,_tmpDate,_tmpTime,_tmpPeriod,_tmpMedicationTaken,_tmpNote,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
