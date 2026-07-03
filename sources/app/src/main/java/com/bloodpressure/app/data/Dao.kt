package com.bloodpressure.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.datetime.LocalDate

@Dao
interface BloodPressureDao {
    @Insert
    suspend fun insert(record: BloodPressureRecord): Long

    @Delete
    suspend fun delete(record: BloodPressureRecord)

    @Query("SELECT * FROM blood_pressure_records WHERE date = :date ORDER BY time ASC")
    suspend fun getByDate(date: LocalDate): List<BloodPressureRecord>

    @Query("SELECT * FROM blood_pressure_records WHERE date BETWEEN :from AND :to ORDER BY date DESC, time ASC")
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<BloodPressureRecord>

    @Query("SELECT * FROM blood_pressure_records WHERE period = :period AND date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun getByPeriodAndDateRange(period: MeasurementPeriod, from: LocalDate, to: LocalDate): List<BloodPressureRecord>

    @Query("SELECT * FROM blood_pressure_records WHERE date = :date AND period = :period")
    suspend fun getByDateAndPeriod(date: LocalDate, period: MeasurementPeriod): List<BloodPressureRecord>

    @Query("SELECT * FROM blood_pressure_records ORDER BY date DESC, time DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<BloodPressureRecord>

    @Query("SELECT * FROM blood_pressure_records")
    suspend fun getAll(): List<BloodPressureRecord>

    @Query("UPDATE blood_pressure_records SET systolic = :systolic, diastolic = :diastolic, heartRate = :heartRate, date = :date, period = :period WHERE id = :id")
    suspend fun updateRecord(id: Long, systolic: Int, diastolic: Int, heartRate: Int, date: LocalDate, period: MeasurementPeriod)
}

@Dao
interface MedicationDao {
    @Insert
    suspend fun insert(medication: Medication): Long

    @Query("SELECT * FROM medications WHERE isActive = 1")
    suspend fun getActiveMedications(): List<Medication>

    @Query("SELECT * FROM medications")
    suspend fun getAllMedications(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Query("UPDATE medications SET name = :name, dosage = :dosage WHERE id = :id")
    suspend fun updateNameAndDosage(id: Long, name: String, dosage: String)

    @Query("UPDATE medications SET isActive = 0 WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MedicationRecordDao {
    @Insert
    suspend fun insert(record: MedicationRecord): Long

    @Delete
    suspend fun delete(record: MedicationRecord)

    @Query("SELECT * FROM medication_records WHERE date = :date")
    suspend fun getByDate(date: LocalDate): List<MedicationRecord>

    @Query("SELECT * FROM medication_records WHERE medicationId = :medicationId AND date = :date")
    suspend fun getByMedicationAndDate(medicationId: Long, date: LocalDate): MedicationRecord?

    @Query("UPDATE medication_records SET taken = :taken, takenAt = :takenAt WHERE id = :id")
    suspend fun updateTaken(id: Long, taken: Boolean, takenAt: Long?)

    @Query("SELECT * FROM medication_records WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<MedicationRecord>

    @Query("SELECT * FROM medication_records ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MedicationRecord>

    @Query("UPDATE medication_records SET taken = :taken, dosage = :dosage WHERE id = :id")
    suspend fun updateRecord(id: Long, taken: Boolean, dosage: String)

    @Query("UPDATE medication_records SET medicationId = :medicationId, date = :date, taken = :taken, dosage = :dosage WHERE id = :id")
    suspend fun updateMedRecord(id: Long, medicationId: Long, date: LocalDate, taken: Boolean, dosage: String)
}

@Dao
interface DosageChangeDao {
    @Insert
    suspend fun insert(change: DosageChange): Long

    @Query("SELECT * FROM dosage_changes WHERE medicationId = :medicationId ORDER BY date DESC")
    suspend fun getByMedication(medicationId: Long): List<DosageChange>
}
