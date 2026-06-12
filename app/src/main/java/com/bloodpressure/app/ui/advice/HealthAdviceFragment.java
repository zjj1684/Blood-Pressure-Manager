package com.bloodpressure.app.ui.advice;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloodpressure.app.R;
import com.bloodpressure.app.data.db.ReportEntity;
import com.bloodpressure.app.ui.main.SharedBleViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthAdviceFragment extends Fragment {

    private HealthAdviceViewModel viewModel;
    private SharedBleViewModel sharedBleViewModel;
    private MaterialCardView cardReport;
    private TextView tvNickname;
    private TextView tvReportDate;
    private TextView tvConstitution;
    private TextView tvConfidence;
    private LinearLayout layoutConstitution2;
    private TextView tvConstitution2;
    private TextView tvConfidence2;
    private TextView tvBp;
    private TextView tvHr;
    private TextView tvDietaryAdvice;
    private LinearLayout layoutDietImages;
    private ImageView ivDiet1;
    private ImageView ivDiet2;
    private TextView tvAcupressureAdvice;
    private TextView tvEarAcupressureAdvice;
    private LinearLayout layoutEarImages;
    private LinearLayout layoutEarFront1;
    private TextView tvEarFrontLabel1;
    private ImageView ivEarFront1;
    private LinearLayout layoutEarFront2;
    private TextView tvEarFrontLabel2;
    private ImageView ivEarFront2;
    private ImageView ivEarBack;
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health_advice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HealthAdviceViewModel.class);
        sharedBleViewModel = new ViewModelProvider(requireActivity()).get(SharedBleViewModel.class);

        cardReport = view.findViewById(R.id.card_report);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvReportDate = view.findViewById(R.id.tv_report_date);
        tvConstitution = view.findViewById(R.id.tv_constitution);
        tvConfidence = view.findViewById(R.id.tv_confidence);
        layoutConstitution2 = view.findViewById(R.id.layout_constitution2);
        tvConstitution2 = view.findViewById(R.id.tv_constitution2);
        tvConfidence2 = view.findViewById(R.id.tv_confidence2);
        tvBp = view.findViewById(R.id.tv_bp);
        tvHr = view.findViewById(R.id.tv_hr);
        tvDietaryAdvice = view.findViewById(R.id.tv_dietary_advice);
        layoutDietImages = view.findViewById(R.id.layout_diet_images);
        ivDiet1 = view.findViewById(R.id.iv_diet1);
        ivDiet2 = view.findViewById(R.id.iv_diet2);
        tvAcupressureAdvice = view.findViewById(R.id.tv_acupressure_advice);
        tvEarAcupressureAdvice = view.findViewById(R.id.tv_ear_acupressure_advice);
        layoutEarImages = view.findViewById(R.id.layout_ear_images);
        layoutEarFront1 = view.findViewById(R.id.layout_ear_front1);
        tvEarFrontLabel1 = view.findViewById(R.id.tv_ear_front_label1);
        ivEarFront1 = view.findViewById(R.id.iv_ear_front1);
        layoutEarFront2 = view.findViewById(R.id.layout_ear_front2);
        tvEarFrontLabel2 = view.findViewById(R.id.tv_ear_front_label2);
        ivEarFront2 = view.findViewById(R.id.iv_ear_front2);
        ivEarBack = view.findViewById(R.id.iv_ear_back);
        recyclerReports = view.findViewById(R.id.recycler_reports);

        MaterialButton btnGenerate = view.findViewById(R.id.btn_generate);

        recyclerReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReportAdapter();
        recyclerReports.setAdapter(adapter);

        btnGenerate.setOnClickListener(v -> {
            Integer sys = sharedBleViewModel.getSystolic().getValue();
            Integer dia = sharedBleViewModel.getDiastolic().getValue();
            Integer hr = sharedBleViewModel.getHeartRate().getValue();
            viewModel.generateReport(
                    sys != null ? sys : 0,
                    dia != null ? dia : 0,
                    hr != null ? hr : 0
            );
        });

        viewModel.getCurrentReport().observe(getViewLifecycleOwner(), report -> {
            if (report != null) {
                showReport(report);
            }
        });

        viewModel.getAllReports().observe(getViewLifecycleOwner(), reports -> {
            adapter.setReports(reports);
        });

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReport(ReportEntity report) {
        cardReport.setVisibility(View.VISIBLE);
        tvNickname.setText(report.nickname != null ? report.nickname : "--");
        tvReportDate.setText(sdf.format(new Date(report.createdAt)));

        // 主要体质 + 置信度
        tvConstitution.setText(report.constitutionType);
        int color1 = getResources().getColor(HealthAdviceContent.getColorRes(report.constitutionType));
        tvConstitution.setTextColor(color1);
        tvConstitution.setBackground(createBadgeBg(color1));
        tvConfidence.setTextColor(color1);
        tvConfidence.setText(getString(R.string.label_confidence, report.confidence));

        // 次要体质（可选）
        if (report.constitutionType2 != null && !report.constitutionType2.isEmpty()) {
            layoutConstitution2.setVisibility(View.VISIBLE);
            tvConstitution2.setText(report.constitutionType2);
            int color2 = getResources().getColor(HealthAdviceContent.getColorRes(report.constitutionType2));
            tvConstitution2.setTextColor(color2);
            tvConstitution2.setBackground(createBadgeBg(color2));
            tvConfidence2.setTextColor(color2);
            tvConfidence2.setText(getString(R.string.label_confidence, report.confidence2));
        } else {
            layoutConstitution2.setVisibility(View.GONE);
        }

        // 血压 + 心率
        if (report.systolic > 0 && report.diastolic > 0) {
            tvBp.setText(getString(R.string.bp_format, report.systolic, report.diastolic));
        } else {
            tvBp.setText(R.string.bp_unavailable);
        }

        if (report.heartRate > 0) {
            tvHr.setText(getString(R.string.hr_format, report.heartRate));
        } else {
            tvHr.setText(R.string.hr_unavailable);
        }

        // 调理建议
        tvDietaryAdvice.setText(report.dietaryAdvice);
        showDietImages(report);
        tvAcupressureAdvice.setText(report.acupressureAdvice);
        tvEarAcupressureAdvice.setText(report.earAcupressureAdvice);

        // 耳穴图片
        showEarImages(report);
    }

    private void showDietImages(ReportEntity report) {
        int res1 = HealthAdviceContent.getDietaryImageRes(report.constitutionType);
        if (res1 != 0) {
            ivDiet1.setImageResource(res1);
            ivDiet1.setVisibility(View.VISIBLE);
        } else {
            ivDiet1.setVisibility(View.GONE);
        }

        if (report.constitutionType2 != null && !report.constitutionType2.isEmpty()) {
            int res2 = HealthAdviceContent.getDietaryImageRes(report.constitutionType2);
            if (res2 != 0) {
                ivDiet2.setImageResource(res2);
                ivDiet2.setVisibility(View.VISIBLE);
            } else {
                ivDiet2.setVisibility(View.GONE);
            }
        } else {
            ivDiet2.setVisibility(View.GONE);
        }
    }

    private void showEarImages(ReportEntity report) {
        // 第一个体质的耳前穴位图
        int earFrontRes1 = HealthAdviceContent.getEarFrontImageRes(report.constitutionType);
        ivEarFront1.setImageResource(earFrontRes1);
        tvEarFrontLabel1.setText(report.constitutionType + " - " + getString(R.string.label_ear_front));
        layoutEarFront1.setVisibility(View.VISIBLE);

        // 第二个体质的耳前穴位图（可选）
        if (report.constitutionType2 != null && !report.constitutionType2.isEmpty()) {
            int earFrontRes2 = HealthAdviceContent.getEarFrontImageRes(report.constitutionType2);
            ivEarFront2.setImageResource(earFrontRes2);
            tvEarFrontLabel2.setText(report.constitutionType2 + " - " + getString(R.string.label_ear_front));
            layoutEarFront2.setVisibility(View.VISIBLE);
        } else {
            layoutEarFront2.setVisibility(View.GONE);
        }

        // 耳背穴位图（通用）
        ivEarBack.setImageResource(HealthAdviceContent.getEarBackImageRes());
    }

    private GradientDrawable createBadgeBg(int typeColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(50f);
        bg.setColor(Color.argb(25, Color.red(typeColor), Color.green(typeColor), Color.blue(typeColor)));
        bg.setStroke(1, typeColor);
        return bg;
    }

    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

        private List<ReportEntity> reports;

        void setReports(List<ReportEntity> reports) {
            this.reports = reports;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReportEntity report = reports.get(position);
            holder.tvDate.setText(sdf.format(new Date(report.createdAt)));

            StringBuilder constitutionText = new StringBuilder(report.constitutionType);
            constitutionText.append(" ").append(report.confidence).append("%");
            if (report.constitutionType2 != null && !report.constitutionType2.isEmpty()) {
                constitutionText.append(" / ").append(report.constitutionType2)
                        .append(" ").append(report.confidence2).append("%");
            }
            holder.tvConstitution.setText(constitutionText.toString());
            holder.tvConstitution.setTextColor(
                    getResources().getColor(HealthAdviceContent.getColorRes(report.constitutionType)));

            if (report.systolic > 0 && report.diastolic > 0) {
                holder.tvBpSummary.setText(getString(R.string.bp_format, report.systolic, report.diastolic));
            } else if (report.heartRate > 0) {
                holder.tvBpSummary.setText(getString(R.string.hr_format, report.heartRate));
            } else {
                holder.tvBpSummary.setText("--");
            }

            holder.itemView.setOnClickListener(v -> viewModel.setCurrentReport(report));
        }

        @Override
        public int getItemCount() {
            return reports != null ? reports.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvDate;
            final TextView tvConstitution;
            final TextView tvBpSummary;

            ViewHolder(View view) {
                super(view);
                tvDate = view.findViewById(R.id.tv_date);
                tvConstitution = view.findViewById(R.id.tv_constitution);
                tvBpSummary = view.findViewById(R.id.tv_bp_summary);
            }
        }
    }
}
