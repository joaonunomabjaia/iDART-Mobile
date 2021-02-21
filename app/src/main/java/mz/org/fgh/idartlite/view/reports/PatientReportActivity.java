package mz.org.fgh.idartlite.view.reports;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.highsoft.highcharts.Common.HIChartsClasses.HIChart;
import com.highsoft.highcharts.Common.HIChartsClasses.HIColumn;
import com.highsoft.highcharts.Common.HIChartsClasses.HIOptions;
import com.highsoft.highcharts.Common.HIChartsClasses.HIPlotOptions;
import com.highsoft.highcharts.Common.HIChartsClasses.HISubtitle;
import com.highsoft.highcharts.Common.HIChartsClasses.HITitle;
import com.highsoft.highcharts.Common.HIChartsClasses.HITooltip;
import com.highsoft.highcharts.Common.HIChartsClasses.HIXAxis;
import com.highsoft.highcharts.Common.HIChartsClasses.HIYAxis;
import com.highsoft.highcharts.Core.HIChartView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import mz.org.fgh.idartlite.R;
import mz.org.fgh.idartlite.base.activity.BaseActivity;
import mz.org.fgh.idartlite.base.viewModel.BaseViewModel;
import mz.org.fgh.idartlite.util.DateUtilities;
import mz.org.fgh.idartlite.util.Utilities;
import mz.org.fgh.idartlite.viewmodel.patient.PatientReportVM;

public class PatientReportActivity extends BaseActivity {

    private String start;
    private String end;
    private HIChartView chartView;
    private EditText edtStart;
    private EditText edtEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report);
        chartView = (HIChartView) findViewById(R.id.hc_view);

        chartView.setVisibility(View.GONE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtStart = findViewById(R.id.start);
        edtEnd = findViewById(R.id.end);
        ImageView search = findViewById(R.id.buttonSearch);

        edtStart.setOnClickListener(view -> {
            int mYear, mMonth, mDay;

            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(PatientReportActivity.this, (view1, year, monthOfYear, dayOfMonth) -> {
                edtStart.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                start =dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;
            }, mYear, mMonth, mDay);
            datePickerDialog.show();
        });

        edtEnd.setOnClickListener(view -> {
            int mYear, mMonth, mDay;

            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(PatientReportActivity.this, (view12, year, monthOfYear, dayOfMonth) -> {
                edtEnd.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                end = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;
            }, mYear, mMonth, mDay);
            datePickerDialog.show();
        });

        search.setOnClickListener(v -> {
            if (!Utilities.stringHasValue(start) || !Utilities.stringHasValue(end) ){
                Utilities.displayAlertDialog(PatientReportActivity.this, "Por favor indicar o período por analisar!").show();
            }else
            if (DateUtilities.dateDiff(DateUtilities.createDate(end, DateUtilities.DATE_FORMAT), DateUtilities.createDate(start, DateUtilities.DATE_FORMAT), DateUtilities.DAY_FORMAT) < 0){
                Utilities.displayAlertDialog(PatientReportActivity.this, "A data inicio deve ser menor que a data fim.").show();
            }else
            if ((int) (DateUtilities.dateDiff(DateUtilities.getCurrentDate(), DateUtilities.createDate(start, DateUtilities.DATE_FORMAT), DateUtilities.DAY_FORMAT)) < 0){
                Utilities.displayAlertDialog(PatientReportActivity.this, "A data inicio deve ser menor que a data corrente.").show();
            }
            else
            if ((int) DateUtilities.dateDiff(DateUtilities.getCurrentDate(), DateUtilities.createDate(end, DateUtilities.DATE_FORMAT), DateUtilities.DAY_FORMAT) < 0){
                Utilities.displayAlertDialog(PatientReportActivity.this, "A data fim deve ser menor que a data corrente.").show();
            }else {
                Utilities.hideSoftKeyboard(PatientReportActivity.this);
                generateGraph(DateUtilities.createDate(start, DateUtilities.DATE_FORMAT), DateUtilities.createDate(end, DateUtilities.DATE_FORMAT));
            }
        });
    }

    private void generateGraph(Date start, Date end) {
        start = DateUtilities.createDate(edtStart.getText().toString(), DateUtilities.DATE_FORMAT);
        end = DateUtilities.createDate(edtEnd.getText().toString(), DateUtilities.DATE_FORMAT);

        List<String> sanitaryUnits = getRelatedViewModel().getSanitaryUnityListOnPeriod(start, end);

        HIOptions options = new HIOptions();

        HIChart chart = new HIChart();
        chart.setType("column");
        options.setChart(chart);

        HITitle title = new HITitle();

        title.setText("Número de Pacientes Referidos da US");
        HISubtitle subtitle = new HISubtitle();
        subtitle.setText(start+" - "+end);
        options.setTitle(title);
        options.setSubtitle(subtitle);

        final HIYAxis hiyAxis = new HIYAxis();
        hiyAxis.setMin(0);
        hiyAxis.setTitle(new HITitle());
        hiyAxis.getTitle().setText("Quantidade");
        options.setYAxis(new ArrayList(){{add(hiyAxis);}});

        HIColumn patientData = new HIColumn();
        patientData.setName("Pacientes");
        ArrayList serieData = new ArrayList<>();

        final HIXAxis hixAxis = new HIXAxis();
        ArrayList categories = new ArrayList<>();
        for (String s : sanitaryUnits){
            categories.add(s);
            serieData.add(getRelatedViewModel().countNewPatientByPeriod(start, end, s));
        }

        patientData.setData(serieData);

        hixAxis.setCategories(categories);
        options.setXAxis(new ArrayList(){{add(hixAxis);}});

        HITooltip tooltip = new HITooltip();
        tooltip.setHeaderFormat("<span style=\"font-size:15px\">{point.key}</span><table>");
        tooltip.setPointFormat("<tr><td style=\"color:{series.color};padding:0\">{series.name}: </td>" + "<td style=\"padding:0\"><b>{point.y}</b></td></tr>");
        tooltip.setFooterFormat("</table>");
        tooltip.setShared(true);
        tooltip.setUseHTML(true);
        options.setTooltip(tooltip);

        HIPlotOptions plotOptions = new HIPlotOptions();
        plotOptions.setColumn(new HIColumn());
        plotOptions.getColumn().setPointPadding(0.2);
        plotOptions.getColumn().setBorderWidth(0);
        options.setPlotOptions(plotOptions);

        ArrayList series = new ArrayList<>();
        series.add(patientData);

        options.setSeries(series);

        chartView.setOptions(options);

        chartView.reload();

        chartView.setVisibility(View.VISIBLE);
    }

    @Override
    public PatientReportVM getRelatedViewModel() {
        return (PatientReportVM) super.getRelatedViewModel();
    }

    @Override
    public BaseViewModel initViewModel() {
        return new ViewModelProvider(this).get(PatientReportVM.class);
    }
}