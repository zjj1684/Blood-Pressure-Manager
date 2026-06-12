package com.bloodpressure.app.ui.advice;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bloodpressure.app.data.db.QuestionnaireEntity;
import com.bloodpressure.app.data.db.ReportEntity;
import com.bloodpressure.app.data.repository.QuestionnaireRepository;
import com.bloodpressure.app.data.repository.ReportRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthAdviceViewModel extends AndroidViewModel {

    private final QuestionnaireRepository questionnaireRepository;
    private final ReportRepository reportRepository;
    private final LiveData<List<ReportEntity>> allReports;
    private final MutableLiveData<ReportEntity> currentReport = new MutableLiveData<>(null);
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HealthAdviceViewModel(@NonNull Application application) {
        super(application);
        questionnaireRepository = new QuestionnaireRepository(application);
        reportRepository = new ReportRepository(application);
        allReports = reportRepository.getAll();
    }

    public LiveData<List<ReportEntity>> getAllReports() {
        return allReports;
    }

    public LiveData<ReportEntity> getCurrentReport() {
        return currentReport;
    }

    public LiveData<String> getErrorEvent() {
        return errorEvent;
    }

    public void generateReport(int systolic, int diastolic, int heartRate) {
        executor.execute(() -> {
            QuestionnaireEntity latest = questionnaireRepository.getLatest();
            if (latest == null) {
                errorEvent.postValue("请先完成问卷以生成报告");
                return;
            }

            List<HealthAdviceContent.ConstitutionResult> results =
                    HealthAdviceContent.classifyTop(
                            latest.scorePinghe, latest.scoreYinxu,
                            latest.scoreTanshi, latest.scoreQiyu);

            HealthAdviceContent.ConstitutionResult primary = results.get(0);
            String type2 = null;
            if (results.size() > 1) {
                type2 = results.get(1).type;
            }

            ReportEntity report = new ReportEntity();
            report.nickname = latest.nickname;
            report.createdAt = System.currentTimeMillis();
            report.constitutionType = primary.type;
            report.confidence = primary.confidence;
            report.systolic = systolic;
            report.diastolic = diastolic;
            report.heartRate = heartRate;
            report.dietaryAdvice = HealthAdviceContent.getDietaryAdvice(primary.type, type2);
            report.acupressureAdvice = HealthAdviceContent.getAcupressureAdvice(primary.type, type2);
            report.earAcupressureAdvice = HealthAdviceContent.getEarAcupressureAdvice(primary.type, type2);

            if (results.size() > 1) {
                HealthAdviceContent.ConstitutionResult secondary = results.get(1);
                report.constitutionType2 = secondary.type;
                report.confidence2 = secondary.confidence;
            }

            reportRepository.insert(report);
            currentReport.postValue(report);
        });
    }

    public void setCurrentReport(ReportEntity report) {
        currentReport.setValue(report);
    }

    public void deleteReport(long id) {
        reportRepository.delete(id);
    }
}
