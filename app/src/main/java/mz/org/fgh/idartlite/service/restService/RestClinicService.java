package mz.org.fgh.idartlite.service.restService;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import androidx.collection.ArrayMap;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.internal.LinkedTreeMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mz.org.fgh.idartlite.base.BaseService;
import mz.org.fgh.idartlite.model.Clinic;
import mz.org.fgh.idartlite.model.User;
import mz.org.fgh.idartlite.rest.RESTServiceHandler;
import mz.org.fgh.idartlite.service.ClinicService;
import mz.org.fgh.idartlite.service.PharmacyTypeService;

public class RestClinicService extends BaseService {
    private static final String TAG = "RestClinicService";
    private static ClinicService clinicService;
    private static PharmacyTypeService pharmacyTypeService;

    public RestClinicService(Application application, User currentUser) {
        super(application, currentUser);
    }

    public List<Clinic> restGetAllClinic() {

        String url = BaseService.baseUrl + "/clinic?facilitytype=neq.Unidade%20Sanitária&mainclinic=eq.false";
        clinicService = new ClinicService(getApplication(), null);
        pharmacyTypeService = new PharmacyTypeService(getApplication(), null);
        ArrayList<Clinic> clinicList = new ArrayList<>();
        RESTServiceHandler handler = new RESTServiceHandler();

        if (RESTServiceHandler.getServerStatus(BaseService.baseUrl)) {
            getRestServiceExecutor().execute(() -> {

                Map<String, Object> params = new ArrayMap<String, Object>();
                handler.addHeader("Content-Type", "Application/json");

                handler.objectRequest(url, Request.Method.GET, params, Object[].class, new Response.Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] clinics) {

                        if (clinics.length > 0) {
                            for (Object clinic : clinics) {
                                try {
                                    LinkedTreeMap<String, Object> itemresult = (LinkedTreeMap<String, Object>) clinic;

                                    Clinic clinicRest = new Clinic();
                                    clinicRest.setCode(Objects.requireNonNull(itemresult.get("code")).toString());
                                    clinicRest.setClinicName(Objects.requireNonNull(itemresult.get("clinicname")).toString());
                                    clinicRest.setPharmacyType(pharmacyTypeService.getPharmacyType(Objects.requireNonNull(itemresult.get("facilitytype")).toString()));
                                    clinicRest.setAddress(Objects.requireNonNull(itemresult.get("province")).toString().concat(Objects.requireNonNull(itemresult.get("district")).toString()));
                                    clinicRest.setPhone(Objects.requireNonNull(itemresult.get("telephone")).toString());
                                    clinicRest.setUuid(Objects.requireNonNull(itemresult.get("uuid")).toString());
                                    clinicList.add(clinicRest);

                                    Log.i(TAG, "onResponse: " + clinic);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                } finally {
                                    continue;
                                }
                            }
                        } else
                            Log.w(TAG, "Response Sem Info." + clinics.length);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Response", error.getMessage());
                    }
                });
            });

        } else {
            Log.e(TAG, "Response Servidor Offline");
            Toast.makeText(getApplication(),"Servidor offline, por favor tente mais tarde",Toast.LENGTH_LONG).show();
        }

        return clinicList;
    }
}