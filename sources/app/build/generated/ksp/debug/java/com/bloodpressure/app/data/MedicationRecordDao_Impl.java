package com.bloodpressure.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MedicationRecordDao_Impl implements MedicationRecordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MedicationRecord> __insertionAdapterOfMedicationRecord;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<MedicationRecord> __deletionAdapterOfMedicationRecord;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTaken;

  private final SharedSQLiteStatement __preparedStmtOfUpdateRecord;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMedRecord;

  public MedicationRecordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMedicationRecord = new EntityInsertionAdapter<MedicationRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `medication_records` (`id`,`medicationId`,`date`,`taken`,`dosage`,`takenAt`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MedicationRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getMedicationId());
        final String _tmp = __converters.fromDate(entity.getDate());
        statement.bindString(3, _tmp);
        final int _tmp_1 = entity.getTaken() ? 1 : 0;
        statement.bindLong(4, _tmp_1);
        statement.bindString(5, entity.getDosage());
        if (entity.getTakenAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getTakenAt());
        }
        statement.bindLong(7, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfMedicationRecord = new EntityDeletionOrUpdateAdapter<MedicationRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `medication_records` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MedicationRecord entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateTaken = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE medication_records SET taken = ?, takenAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateRecord = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE medication_records SET taken = ?, dosage = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateMedRecord = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE medication_records SET medicationId = ?, date = ?, taken = ?, dosage = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MedicationRecord record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMedicationRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MedicationRecord record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMedicationRecord.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTaken(final long id, final boolean taken, final Long takenAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTaken.acquire();
        int _argIndex = 1;
        final int _tmp = taken ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        if (takenAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, takenAt);
        }
        _argIndex = 3;
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
          __preparedStmtOfUpdateTaken.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRecord(final long id, final boolean taken, final String dosage,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateRecord.acquire();
        int _argIndex = 1;
        final int _tmp = taken ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, dosage);
        _argIndex = 3;
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
  public Object updateMedRecord(final long id, final long medicationId, final LocalDate date,
      final boolean taken, final String dosage, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMedRecord.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, medicationId);
        _argIndex = 2;
        final String _tmp = __converters.fromDate(date);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 3;
        final int _tmp_1 = taken ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp_1);
        _argIndex = 4;
        _stmt.bindString(_argIndex, dosage);
        _argIndex = 5;
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
          __preparedStmtOfUpdateMedRecord.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDate(final LocalDate date,
      final Continuation<? super List<MedicationRecord>> $completion) {
    final String _sql = "SELECT * FROM medication_records WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromDate(date);
    _statement.bindString(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationRecord>>() {
      @Override
      @NonNull
      public List<MedicationRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "taken");
          final int _cursorIndexOfDosage = CursorUtil.getColumnIndexOrThrow(_cursor, "dosage");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MedicationRecord> _result = new ArrayList<MedicationRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMedicationId;
            _tmpMedicationId = _cursor.getLong(_cursorIndexOfMedicationId);
            final LocalDate _tmpDate;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_1);
            final boolean _tmpTaken;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfTaken);
            _tmpTaken = _tmp_2 != 0;
            final String _tmpDosage;
            _tmpDosage = _cursor.getString(_cursorIndexOfDosage);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MedicationRecord(_tmpId,_tmpMedicationId,_tmpDate,_tmpTaken,_tmpDosage,_tmpTakenAt,_tmpCreatedAt);
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
  public Object getByMedicationAndDate(final long medicationId, final LocalDate date,
      final Continuation<? super MedicationRecord> $completion) {
    final String _sql = "SELECT * FROM medication_records WHERE medicationId = ? AND date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, medicationId);
    _argIndex = 2;
    final String _tmp = __converters.fromDate(date);
    _statement.bindString(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MedicationRecord>() {
      @Override
      @Nullable
      public MedicationRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "taken");
          final int _cursorIndexOfDosage = CursorUtil.getColumnIndexOrThrow(_cursor, "dosage");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final MedicationRecord _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMedicationId;
            _tmpMedicationId = _cursor.getLong(_cursorIndexOfMedicationId);
            final LocalDate _tmpDate;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_1);
            final boolean _tmpTaken;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfTaken);
            _tmpTaken = _tmp_2 != 0;
            final String _tmpDosage;
            _tmpDosage = _cursor.getString(_cursorIndexOfDosage);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new MedicationRecord(_tmpId,_tmpMedicationId,_tmpDate,_tmpTaken,_tmpDosage,_tmpTakenAt,_tmpCreatedAt);
          } else {
            _result = null;
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
      final Continuation<? super List<MedicationRecord>> $completion) {
    final String _sql = "SELECT * FROM medication_records WHERE date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromDate(from);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    final String _tmp_1 = __converters.fromDate(to);
    _statement.bindString(_argIndex, _tmp_1);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationRecord>>() {
      @Override
      @NonNull
      public List<MedicationRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "taken");
          final int _cursorIndexOfDosage = CursorUtil.getColumnIndexOrThrow(_cursor, "dosage");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MedicationRecord> _result = new ArrayList<MedicationRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMedicationId;
            _tmpMedicationId = _cursor.getLong(_cursorIndexOfMedicationId);
            final LocalDate _tmpDate;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_2);
            final boolean _tmpTaken;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfTaken);
            _tmpTaken = _tmp_3 != 0;
            final String _tmpDosage;
            _tmpDosage = _cursor.getString(_cursorIndexOfDosage);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MedicationRecord(_tmpId,_tmpMedicationId,_tmpDate,_tmpTaken,_tmpDosage,_tmpTakenAt,_tmpCreatedAt);
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
      final Continuation<? super List<MedicationRecord>> $completion) {
    final String _sql = "SELECT * FROM medication_records ORDER BY date DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationRecord>>() {
      @Override
      @NonNull
      public List<MedicationRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "taken");
          final int _cursorIndexOfDosage = CursorUtil.getColumnIndexOrThrow(_cursor, "dosage");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MedicationRecord> _result = new ArrayList<MedicationRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMedicationId;
            _tmpMedicationId = _cursor.getLong(_cursorIndexOfMedicationId);
            final LocalDate _tmpDate;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp);
            final boolean _tmpTaken;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfTaken);
            _tmpTaken = _tmp_1 != 0;
            final String _tmpDosage;
            _tmpDosage = _cursor.getString(_cursorIndexOfDosage);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MedicationRecord(_tmpId,_tmpMedicationId,_tmpDate,_tmpTaken,_tmpDosage,_tmpTakenAt,_tmpCreatedAt);
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
