package mz.org.fgh.idartlite.dao.dispense;

import android.app.Application;
import android.database.Cursor;

import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RawRowMapper;
import com.j256.ormlite.stmt.ColumnArg;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mz.org.fgh.idartlite.base.databasehelper.IdartLiteDataBaseHelper;
import mz.org.fgh.idartlite.dao.generic.GenericDaoImpl;
import mz.org.fgh.idartlite.model.Dispense;
import mz.org.fgh.idartlite.model.Episode;
import mz.org.fgh.idartlite.model.Patient;
import mz.org.fgh.idartlite.model.Prescription;
import mz.org.fgh.idartlite.model.ReferedStockMoviment;
import mz.org.fgh.idartlite.model.Stock;
import mz.org.fgh.idartlite.model.TherapeuticLine;
import mz.org.fgh.idartlite.util.DateUtilities;

public class DispenseDaoImpl extends GenericDaoImpl<Dispense, Integer> implements IDispenseDao {

    public DispenseDaoImpl(Class dataClass) throws SQLException {
        super(dataClass);
    }

    public DispenseDaoImpl(ConnectionSource connectionSource, Class dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public DispenseDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    @Override
    public List<Dispense> getAllByPrescription(Prescription prescription) throws SQLException {
        return queryBuilder().where().eq(Dispense.COLUMN_PRESCRIPTION, prescription.getId()).and().eq(Dispense.COLUMN_VOIDED,false).query();
    }

    @Override
    public long countAllOfPrescription(Prescription prescription) throws SQLException {
        return queryBuilder().where().eq(Dispense.COLUMN_PRESCRIPTION, prescription.getId()).and().eq(Dispense.COLUMN_VOIDED,false).countOf();
    }

    public List<Dispense> getAllOfPatient(Application application, Patient patient) throws SQLException {

        QueryBuilder<Prescription, Integer> prescriptionQb = IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getPrescriptionDao().queryBuilder();
        prescriptionQb.where().eq(Prescription.COLUMN_PATIENT_ID, patient.getId());

        QueryBuilder<TherapeuticLine, Integer> therapeuticLineQb = IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getTherapeuticLineDao().queryBuilder();
        prescriptionQb.join(therapeuticLineQb);

        QueryBuilder<Dispense, Integer> dispenseQb = IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getDispenseDao().queryBuilder();
        dispenseQb.join(prescriptionQb);
        QueryBuilder<Dispense, Integer> dispenseQb =   IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getDispenseDao().queryBuilder();
        dispenseQb.join(prescriptionQb).where().eq(Dispense.COLUMN_VOIDED,false);

        List<Dispense> dispenses = dispenseQb.orderBy(Dispense.COLUMN_NEXT_PICKUP_DATE, false).query();

        System.out.println(dispenseQb.orderBy(Dispense.COLUMN_NEXT_PICKUP_DATE, false).prepareStatementString());

        return dispenses;
    }

    @Override
    public Dispense getLastDispensePrescription(Prescription prescription) throws SQLException {

        List<Dispense> dispenseList = null;
        QueryBuilder<Dispense, Integer> dispenseQb = queryBuilder();

        dispenseQb.where().eq(Dispense.COLUMN_PRESCRIPTION, prescription.getId()).and().eq(Dispense.COLUMN_VOIDED,false);

        dispenseList = dispenseQb.orderBy(Dispense.COLUMN_PICKUP_DATE, false).limit(1L).query();

        if (dispenseList.size() != 0)
            return dispenseList.get(0);

        return null;

    }

    @Override
    public List<Dispense> getDispensesBetweenStartDateAndEndDateWithLimit(Date startDate, Date endDate, long offset, long limit) throws SQLException {
        return queryBuilder().limit(limit)
                .offset(offset).where().ge(Dispense.COLUMN_PICKUP_DATE, startDate)
                .and()
                .le(Dispense.COLUMN_PICKUP_DATE, endDate).and().eq(Dispense.COLUMN_VOIDED,false).query();
    }


    @Override
    public List<Dispense> getDispensesBetweenStartDateAndEndDate(Date startDate, Date endDate) throws SQLException {
        return queryBuilder().where().ge(Dispense.COLUMN_PICKUP_DATE, startDate)
                .and()
                .le(Dispense.COLUMN_PICKUP_DATE, endDate).and().eq(Dispense.COLUMN_VOIDED,false).query();
    }

    @Override
    public List<Dispense> getAllDispensesByStatus(String status) throws SQLException {
        return queryBuilder().where().eq(Dispense.COLUMN_SYNC_STATUS, status).and().eq(Dispense.COLUMN_VOIDED,false).query();
    }

    @Override
    public List<Dispense> getDispensesBetweenNextPickppDateStartDateAndEndDateWithLimit(Date startDate, Date endDate, long offset, long limit) throws SQLException {
        return queryBuilder().orderBy(Dispense.COLUMN_NEXT_PICKUP_DATE, true).limit(limit)

    public List<Dispense> getDispensesBetweenNextPickppDateStartDateAndEndDateWithLimit(Application application,Date startDate, Date endDate, long offset, long limit) throws SQLException {

        QueryBuilder<Prescription, Integer> prescriptionQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getPrescriptionDao().queryBuilder();
        QueryBuilder<Episode, Integer> episodeQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getEpisodeDao().queryBuilder();

        episodeQb.where().isNotNull(Episode.COLUMN_STOP_REASON);
        QueryBuilder<Patient, Integer> patientQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getPatientDao().queryBuilder();
        patientQb.where().eq(Patient.COLUMN_ID, new ColumnArg(Patient.TABLE_NAME, Patient.COLUMN_ID)).and().not().in(Patient.COLUMN_ID,episodeQb.selectRaw("patient_id"));
        QueryBuilder<Dispense, Integer> dispenseQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getDispenseDao().queryBuilder();

        prescriptionQb.groupBy(Prescription.COLUMN_PATIENT_ID).join(patientQb);
        dispenseQb.join(prescriptionQb);
        dispenseQb.orderBy(Dispense.COLUMN_NEXT_PICKUP_DATE,true).limit(limit)
                .offset(offset).where().ge(Dispense.COLUMN_NEXT_PICKUP_DATE, startDate)
                .and()
                .le(Dispense.COLUMN_NEXT_PICKUP_DATE, endDate).and().eq(Dispense.COLUMN_VOIDED,false);
        return dispenseQb.query();
    }

    public List<Dispense> getAbsentPatientsBetweenNextPickppDateStartDateAndEndDateWithLimit(Application application,Date startDate, Date endDate, long offset, long limit) throws SQLException {

      QueryBuilder<Prescription, Integer> prescriptionQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getPrescriptionDao().queryBuilder();
        QueryBuilder<Episode, Integer> episodeQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getEpisodeDao().queryBuilder();

        episodeQb.where().isNotNull(Episode.COLUMN_STOP_REASON);
        QueryBuilder<Patient, Integer> patientQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getPatientDao().queryBuilder();
        patientQb.where().not().in(Patient.COLUMN_ID,episodeQb.selectRaw("patient_id"));
        QueryBuilder<Dispense, Integer> dispenseQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getDispenseDao().queryBuilder();
        QueryBuilder<Dispense, Integer> dispenseQb1 =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getDispenseDao().queryBuilder();
        QueryBuilder<Prescription, Integer> prescriptionInnerQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getPrescriptionDao().queryBuilder();
        QueryBuilder<Patient, Integer> patientInnerQb =  IdartLiteDataBaseHelper.getInstance(application.getApplicationContext()).getPatientDao().queryBuilder();

        prescriptionQb.groupBy(Prescription.COLUMN_PATIENT_ID).join(patientQb);
        prescriptionInnerQb.groupBy(Prescription.COLUMN_PATIENT_ID).join(patientInnerQb);
        dispenseQb.join(prescriptionQb);
        dispenseQb1.join(prescriptionInnerQb);
        dispenseQb.orderBy(Dispense.COLUMN_NEXT_PICKUP_DATE,true).limit(limit)
                .offset(offset).where().ge(Dispense.COLUMN_NEXT_PICKUP_DATE, startDate)
                .and()
                .le(Dispense.COLUMN_NEXT_PICKUP_DATE, endDate).and().in(Dispense.COLUMN_NEXT_PICKUP_DATE,dispenseQb1.selectRaw("max(next_pickup_date)")).and().eq(Dispense.COLUMN_VOIDED,false);
        System.out.println(dispenseQb.prepareStatementString());

       return dispenseQb.query();
    }

}
