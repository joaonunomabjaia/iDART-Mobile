package mz.org.fgh.idartlite.viewmodel.patient;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mz.org.fgh.idartlite.BR;
import mz.org.fgh.idartlite.R;
import mz.org.fgh.idartlite.base.model.BaseModel;
import mz.org.fgh.idartlite.base.service.IBaseService;
import mz.org.fgh.idartlite.base.viewModel.SearchVM;
import mz.org.fgh.idartlite.model.Clinic;
import mz.org.fgh.idartlite.model.patient.PatientAttribute;
import mz.org.fgh.idartlite.model.patient.Patient;
import mz.org.fgh.idartlite.rest.service.Patient.RestPatientService;
import mz.org.fgh.idartlite.searchparams.AbstractSearchParams;
import mz.org.fgh.idartlite.searchparams.PatientSearchParams;
import mz.org.fgh.idartlite.service.episode.EpisodeService;
import mz.org.fgh.idartlite.service.episode.IEpisodeService;
import mz.org.fgh.idartlite.service.patient.IPatientAttributeService;
import mz.org.fgh.idartlite.service.patient.IPatientService;
import mz.org.fgh.idartlite.service.patient.PatientAttributeService;
import mz.org.fgh.idartlite.service.patient.PatientService;
import mz.org.fgh.idartlite.util.Utilities;
import mz.org.fgh.idartlite.view.patientPanel.PatientPanelActivity;
import mz.org.fgh.idartlite.view.patientSearch.SearchPatientActivity;
import mz.org.fgh.idartlite.viewmodel.prescription.PrescriptionVM;

public class PatientVM extends SearchVM<Patient> {

    private Patient patient;
    private IPatientService patientService;
    private IEpisodeService episodeService;
    private IPatientAttributeService attributeService;

    public PatientVM(@NonNull Application application) {
        super(application);

        patientService = (PatientService) getServiceProvider().get(PatientService.class);
        episodeService = new EpisodeService(application,getCurrentUser());
        attributeService = new PatientAttributeService(application, getCurrentUser());

    }

    @Override
    protected IBaseService initRelatedService() {
        return null;
    }

    @Override
    protected BaseModel initRecord() {
        return null;
    }

    @Override
    protected void initFormData() {

    }

    public List<Patient> searchPatient(String param, Clinic clinic, long offset, long limit) throws SQLException {
        return patientService.searchPatientByParamAndClinic(param,clinic, offset, limit);
    }

    public void loadPatientEpisodes() throws SQLException {
        this.patient.setEpisodes(episodeService.getAllEpisodesByPatient(this.patient));
        
        if (!Utilities.listHasElements(this.patient.getEpisodes())){
            throw new RuntimeException(getRelatedActivity().getString(R.string.no_episode_found));
        }
    }

    @Override
    public void initSearch(){
        if(!Utilities.stringHasValue(getSearchParams().getSearchParam())) {
            Utilities.displayAlertDialog(getRelatedActivity(),getRelatedActivity().getString(R.string.nid_or_name_is_mandatory)).show();
        }else {
            getLoadingDialog().startLoadingDialog();
            super.initSearch();
        }
    }

    @Override
    protected void doOnNoRecordFound() {
        getLoadingDialog().dismisDialog();
        Utilities.displayAlertDialog(getRelatedActivity(),getRelatedActivity().getString(R.string.no_search_results)).show();
    }

    @Override
    public void displaySearchResults() {
        Utilities.hideSoftKeyboard(getRelatedActivity());
        getLoadingDialog().dismisDialog();
        ((SearchPatientActivity)getRelatedActivity()).displaySearchResult();
    }

    @Override
    public PatientSearchParams getSearchParams() {
        return (PatientSearchParams) super.getSearchParams();
    }

    @Override
    public AbstractSearchParams<Patient> initSearchParams() {
        return new PatientSearchParams();
    }

    @Override
    public List<Patient> doSearch(long offset, long limit) throws SQLException {
        return searchPatient(this.getSearchParams().getSearchParam().trim(), getCurrentClinic(), offset, limit);
    }

    @Override
    public void doOnlineSearch(long offset, long limit) throws SQLException {
        super.doOnlineSearch(offset, limit);
        RestPatientService.restSearchPatientByNidOrNameOrSurname(getSearchParams().getSearchParam(), getSearchParams().getSearchParam(), getSearchParams().getSearchParam(), offset, limit, this);
    }

    public List<Patient> getAllPatient() throws SQLException {
        return patientService.getALLPatient();
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @Override
    public void preInit() {

    }

    @Bindable
    public Clinic getClinic(){
        return getCurrentClinic();
    }

    @Bindable
    public String getSearchParam() {
        return getSearchParams().getSearchParam();
    }

    public void setSearchParam(String searchParam) {
        getSearchParams().setSearchParam(searchParam);
        notifyPropertyChanged(BR.searchParam);
    }

    public void downloadSelected(Patient patient) {
        setPatient(patient);
        patient.setAttributes(new ArrayList<>());
        patient.getAttributes().add(PatientAttribute.fastCreateFaltoso(patient));
        try {
            Patient existingPatient = patientService.checkExistsPatientWithNID(patient.getNid());
            if (existingPatient == null) {
                patientService.saveFaltoso(patient);
                Utilities.displayConfirmationDialog(getRelatedActivity(), "Paciente carregado com sucesso. Gostaria de ir aos detalhes do mesmo?", getRelatedActivity().getString(R.string.yes), getRelatedActivity().getString(R.string.no), PatientVM.this).show();
            } else {
                existingPatient.setAttributes(attributeService.getAllOfPatient(existingPatient));
                if (existingPatient.isFaltosoOrAbandono()) {
                    Utilities.displayAlertDialog(getRelatedActivity(),"O paciente seleccionado ja foi carregado.").show();
                } else {
                    //patientService.changePatienToFaltosoOrAbandono(existingPatient);
                    Utilities.displayAlertDialog(getRelatedActivity(),"Paciente carregado com sucesso.").show();
                }

            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public void doOnConfirmed() {
        goToPatientPanel(getPatient());
    }

    public void goToPatientPanel (Patient patient) {
        Map<String, Object> params = new HashMap<>();
        try {
            patient.setAttributes(attributeService.getAllOfPatient(patient));
            params.put("patient", patient);
            params.put("user", getCurrentUser());
            params.put("clinic", getCurrentClinic());
            params.put("clinicSector", getCurrentClinicSector());
            getRelatedActivity().nextActivity(PatientPanelActivity.class,params);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
