package com.bloodpressure.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class TrainingDao_Impl implements TrainingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrainingRecord> __insertionAdapterOfTrainingRecord;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public TrainingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrainingRecord = new EntityInsertionAdapter<TrainingRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `training_records` (`id`,`date`,`startTime`,`totalDuration`,`completedGroups`,`totalGroups`,`groupDuration`,`restDuration`,`completed`,`notes`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrainingRecord entity) {
        statement.bindLong(1, entity.getId());
        final String _tmp = __converters.fromDate(entity.getDate());
        statement.bindString(2, _tmp);
        statement.bindLong(3, entity.getStartTime());
        statement.bindLong(4, entity.getTotalDuration());
        statement.bindLong(5, entity.getCompletedGroups());
        statement.bindLong(6, entity.getTotalGroups());
        statement.bindLong(7, entity.getGroupDuration());
        statement.bindLong(8, entity.getRestDuration());
        final int _tmp_1 = entity.getCompleted() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        if (entity.getNotes() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getNotes());
        }
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM training_records WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TrainingRecord record, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTrainingRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecent(final int limit,
      final Continuation<? super List<TrainingRecord>> $completion) {
    final String _sql = "SELECT * FROM training_records ORDER BY date DESC, startTime DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrainingRecord>>() {
      @Override
      @NonNull
      public List<TrainingRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfTotalDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDuration");
          final int _cursorIndexOfCompletedGroups = CursorUtil.getColumnIndexOrThrow(_cursor, "completedGroups");
          final int _cursorIndexOfTotalGroups = CursorUtil.getColumnIndexOrThrow(_cursor, "totalGroups");
          final int _cursorIndexOfGroupDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "groupDuration");
          final int _cursorIndexOfRestDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "restDuration");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<TrainingRecord> _result = new ArrayList<TrainingRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrainingRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final LocalDate _tmpDate;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final int _tmpTotalDuration;
            _tmpTotalDuration = _cursor.getInt(_cursorIndexOfTotalDuration);
            final int _tmpCompletedGroups;
            _tmpCompletedGroups = _cursor.getInt(_cursorIndexOfCompletedGroups);
            final int _tmpTotalGroups;
            _tmpTotalGroups = _cursor.getInt(_cursorIndexOfTotalGroups);
            final int _tmpGroupDuration;
            _tmpGroupDuration = _cursor.getInt(_cursorIndexOfGroupDuration);
            final int _tmpRestDuration;
            _tmpRestDuration = _cursor.getInt(_cursorIndexOfRestDuration);
            final boolean _tmpCompleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new TrainingRecord(_tmpId,_tmpDate,_tmpStartTime,_tmpTotalDuration,_tmpCompletedGroups,_tmpTotalGroups,_tmpGroupDuration,_tmpRestDuration,_tmpCompleted,_tmpNotes);
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
      final Continuation<? super List<TrainingRecord>> $completion) {
    final String _sql = "SELECT * FROM training_records WHERE date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromDate(from);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    final String _tmp_1 = __converters.fromDate(to);
    _statement.bindString(_argIndex, _tmp_1);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrainingRecord>>() {
      @Override
      @NonNull
      public List<TrainingRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfTotalDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "totalDuration");
          final int _cursorIndexOfCompletedGroups = CursorUtil.getColumnIndexOrThrow(_cursor, "completedGroups");
          final int _cursorIndexOfTotalGroups = CursorUtil.getColumnIndexOrThrow(_cursor, "totalGroups");
          final int _cursorIndexOfGroupDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "groupDuration");
          final int _cursorIndexOfRestDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "restDuration");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<TrainingRecord> _result = new ArrayList<TrainingRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrainingRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final LocalDate _tmpDate;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            _tmpDate = __converters.toDate(_tmp_2);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final int _tmpTotalDuration;
            _tmpTotalDuration = _cursor.getInt(_cursorIndexOfTotalDuration);
            final int _tmpCompletedGroups;
            _tmpCompletedGroups = _cursor.getInt(_cursorIndexOfCompletedGroups);
            final int _tmpTotalGroups;
            _tmpTotalGroups = _cursor.getInt(_cursorIndexOfTotalGroups);
            final int _tmpGroupDuration;
            _tmpGroupDuration = _cursor.getInt(_cursorIndexOfGroupDuration);
            final int _tmpRestDuration;
            _tmpRestDuration = _cursor.getInt(_cursorIndexOfRestDuration);
            final boolean _tmpCompleted;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp_3 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new TrainingRecord(_tmpId,_tmpDate,_tmpStartTime,_tmpTotalDuration,_tmpCompletedGroups,_tmpTotalGroups,_tmpGroupDuration,_tmpRestDuration,_tmpCompleted,_tmpNotes);
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
  public Object getCompletedCount(final LocalDate from, final LocalDate to,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM training_records WHERE date BETWEEN ? AND ? AND completed = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromDate(from);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    final String _tmp_1 = __converters.fromDate(to);
    _statement.bindString(_argIndex, _tmp_1);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(0);
            _result = _tmp_2;
          } else {
            _result = 0;
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
  public Object getTotalCompleted(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM training_records WHERE completed = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getTotalDuration(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(totalDuration) FROM training_records WHERE completed = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
