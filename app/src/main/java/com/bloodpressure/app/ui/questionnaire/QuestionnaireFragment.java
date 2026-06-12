package com.bloodpressure.app.ui.questionnaire;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bloodpressure.app.R;
import com.bloodpressure.app.data.db.QuestionnaireEntity;
import com.google.android.material.button.MaterialButton;

public class QuestionnaireFragment extends Fragment {

    private static final int STEP_COVER = -1;
    private static final int STEP_USER_INFO = 0;
    private static final int STEP_Q1 = 1;
    private static final int STEP_Q2 = 2;
    private static final int STEP_Q3 = 3;
    private static final int STEP_Q4 = 4;
    private static final int TOTAL_STEPS = 5;

    private static final int[][] QUESTION_TEXT_RES = {
            {},
            {R.string.q1_tanshi},
            {R.string.q2_yinxu},
            {R.string.q3_qiyu},
            {R.string.q4_pinghe}
    };

    private QuestionnaireViewModel viewModel;
    private FrameLayout stepContainer;
    private MaterialButton btnPrev;
    private MaterialButton btnNext;

    private EditText etNickname;
    private RadioGroup rgGender;
    private EditText etAge;
    private EditText etHeight;
    private EditText etWeight;
    private RadioGroup currentQuestionRg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questionnaire, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(QuestionnaireViewModel.class);

        stepContainer = view.findViewById(R.id.step_container);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);

        btnPrev.setOnClickListener(v -> onPrev());
        btnNext.setOnClickListener(v -> onNext());

        if (viewModel.submitted && viewModel.submittedEntity != null) {
            showResult(viewModel.submittedEntity);
        } else if (viewModel.showingCover) {
            goToStep(STEP_COVER);
        } else {
            goToStep(viewModel.currentStep);
        }
    }

    private void goToStep(int step) {
        saveCurrentStepData();
        viewModel.currentStep = step;
        stepContainer.removeAllViews();

        if (step == STEP_COVER) {
            buildCoverStep();
        } else if (step == STEP_USER_INFO) {
            buildUserInfoStep();
        } else if (step >= STEP_Q1 && step <= STEP_Q4) {
            buildQuestionStep(step);
        }

        updateNavButtons();
    }

    private void buildCoverStep() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(dp(32), dp(80), dp(32), dp(32));

        // 标题
        TextView title = new TextView(requireContext());
        title.setText(R.string.cover_title);
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(0xFF333333);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(24);
        layout.addView(title, titleLp);

        // 说明文字
        TextView desc = new TextView(requireContext());
        desc.setText(R.string.cover_description);
        desc.setTextSize(15);
        desc.setGravity(Gravity.CENTER);
        desc.setTextColor(0xFF666666);
        desc.setLineSpacing(dp(4), 1);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descLp.bottomMargin = dp(48);
        layout.addView(desc, descLp);

        // 开始填写按钮
        MaterialButton btnStart = new MaterialButton(requireContext());
        btnStart.setText(R.string.btn_start_questionnaire);
        btnStart.setTextSize(16);
        btnStart.setCornerRadius(dp(24));
        btnStart.setPadding(dp(32), dp(14), dp(32), dp(14));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnStart.setOnClickListener(v -> {
            viewModel.showingCover = false;
            goToStep(STEP_USER_INFO);
        });
        layout.addView(btnStart, btnLp);

        stepContainer.addView(layout);
    }

    private void buildUserInfoStep() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        // 标题
        TextView title = new TextView(requireContext());
        title.setText(R.string.section_user_info);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(24);
        layout.addView(title, titleLp);

        // 昵称
        etNickname = new EditText(requireContext());
        etNickname.setHint(R.string.hint_nickname);
        etNickname.setInputType(InputType.TYPE_CLASS_TEXT);
        etNickname.setTextSize(16);
        etNickname.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams nicknameLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nicknameLp.bottomMargin = dp(16);
        layout.addView(etNickname, nicknameLp);

        // 性别
        TextView genderLabel = new TextView(requireContext());
        genderLabel.setText(R.string.label_gender);
        genderLabel.setTextSize(14);
        LinearLayout.LayoutParams genderLabelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        genderLabelLp.bottomMargin = dp(4);
        layout.addView(genderLabel, genderLabelLp);

        rgGender = new RadioGroup(requireContext());
        rgGender.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbMale = new RadioButton(requireContext());
        rbMale.setText(R.string.gender_male);
        rbMale.setId(View.generateViewId());
        RadioButton rbFemale = new RadioButton(requireContext());
        rbFemale.setText(R.string.gender_female);
        rbFemale.setId(View.generateViewId());
        rgGender.addView(rbMale);
        rgGender.addView(rbFemale);
        LinearLayout.LayoutParams genderLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        genderLp.bottomMargin = dp(16);
        layout.addView(rgGender, genderLp);

        // 年龄
        etAge = new EditText(requireContext());
        etAge.setHint(R.string.hint_age);
        etAge.setInputType(InputType.TYPE_CLASS_NUMBER);
        etAge.setTextSize(16);
        etAge.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams ageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ageLp.bottomMargin = dp(16);
        layout.addView(etAge, ageLp);

        // 身高体重
        LinearLayout hwRow = new LinearLayout(requireContext());
        hwRow.setOrientation(LinearLayout.HORIZONTAL);

        etHeight = new EditText(requireContext());
        etHeight.setHint(R.string.hint_height);
        etHeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etHeight.setTextSize(16);
        etHeight.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams heightLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        heightLp.setMarginEnd(dp(8));
        hwRow.addView(etHeight, heightLp);

        etWeight = new EditText(requireContext());
        etWeight.setHint(R.string.hint_weight);
        etWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etWeight.setTextSize(16);
        etWeight.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams weightLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        weightLp.setMarginStart(dp(8));
        hwRow.addView(etWeight, weightLp);

        layout.addView(hwRow);
        stepContainer.addView(layout);

        // 恢复已保存的值
        if (!viewModel.savedNickname.isEmpty()) {
            etNickname.setText(viewModel.savedNickname);
        }
        if (viewModel.savedGenderIndex >= 0) {
            ((RadioButton) rgGender.getChildAt(viewModel.savedGenderIndex)).setChecked(true);
        }
        if (!viewModel.savedAge.isEmpty()) {
            etAge.setText(viewModel.savedAge);
        }
        if (!viewModel.savedHeight.isEmpty()) {
            etHeight.setText(viewModel.savedHeight);
        }
        if (!viewModel.savedWeight.isEmpty()) {
            etWeight.setText(viewModel.savedWeight);
        }
    }

    private void buildQuestionStep(int step) {
        int qIndex = step - STEP_Q1;

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        // 步骤指示
        TextView indicator = new TextView(requireContext());
        indicator.setText(getString(R.string.step_indicator, step, TOTAL_STEPS - 1));
        indicator.setTextSize(13);
        indicator.setTextColor(0xFF999999);
        LinearLayout.LayoutParams indLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        indLp.bottomMargin = dp(24);
        layout.addView(indicator, indLp);

        // 题目文字
        TextView questionText = new TextView(requireContext());
        questionText.setText(QUESTION_TEXT_RES[step][0]);
        questionText.setTextSize(18);
        questionText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        qLp.bottomMargin = dp(24);
        layout.addView(questionText, qLp);

        // 选项
        currentQuestionRg = new RadioGroup(requireContext());
        currentQuestionRg.setOrientation(RadioGroup.VERTICAL);

        int[] optResIds = {R.string.opt_1, R.string.opt_2, R.string.opt_3, R.string.opt_4, R.string.opt_5};
        for (int i = 0; i < 5; i++) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setText(optResIds[i]);
            rb.setTextSize(16);
            rb.setId(View.generateViewId());
            rb.setPadding(dp(12), dp(12), dp(12), dp(12));
            RadioGroup.LayoutParams rbLp = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            rbLp.bottomMargin = dp(4);
            currentQuestionRg.addView(rb, rbLp);
        }

        LinearLayout.LayoutParams rgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(currentQuestionRg, rgLp);

        stepContainer.addView(layout);

        // 恢复已选答案
        if (viewModel.savedAnswers[qIndex] != -1) {
            currentQuestionRg.check(currentQuestionRg.getChildAt(viewModel.savedAnswers[qIndex]).getId());
        }
    }

    private void saveCurrentStepData() {
        int step = viewModel.currentStep;

        if (step == STEP_USER_INFO) {
            if (etNickname != null) {
                viewModel.savedNickname = etNickname.getText().toString().trim();
            }
            if (rgGender != null) {
                int checkedId = rgGender.getCheckedRadioButtonId();
                if (checkedId != -1) {
                    for (int i = 0; i < rgGender.getChildCount(); i++) {
                        if (rgGender.getChildAt(i).getId() == checkedId) {
                            viewModel.savedGenderIndex = i;
                            break;
                        }
                    }
                }
            }
            if (etAge != null) {
                viewModel.savedAge = etAge.getText().toString().trim();
            }
            if (etHeight != null) {
                viewModel.savedHeight = etHeight.getText().toString().trim();
            }
            if (etWeight != null) {
                viewModel.savedWeight = etWeight.getText().toString().trim();
            }
        } else if (step >= STEP_Q1 && step <= STEP_Q4 && currentQuestionRg != null) {
            int qIndex = step - STEP_Q1;
            int checkedId = currentQuestionRg.getCheckedRadioButtonId();
            if (checkedId != -1) {
                for (int i = 0; i < currentQuestionRg.getChildCount(); i++) {
                    if (currentQuestionRg.getChildAt(i).getId() == checkedId) {
                        viewModel.savedAnswers[qIndex] = i;
                        break;
                    }
                }
            }
        }
    }

    private void updateNavButtons() {
        int step = viewModel.currentStep;
        View navBar = getView() != null ? getView().findViewById(R.id.nav_bar) : null;
        if (step == STEP_COVER) {
            if (navBar != null) navBar.setVisibility(View.GONE);
        } else {
            if (navBar != null) navBar.setVisibility(View.VISIBLE);
            btnPrev.setVisibility(step == STEP_USER_INFO ? View.INVISIBLE : View.VISIBLE);
            btnNext.setText(step == STEP_Q4 ? R.string.btn_submit : R.string.btn_next);
        }
    }

    private void onPrev() {
        int step = viewModel.currentStep;
        if (step > STEP_USER_INFO) {
            goToStep(step - 1);
        }
    }

    private void onNext() {
        int step = viewModel.currentStep;

        if (step == STEP_USER_INFO) {
            if (!validateUserInfo()) return;
        } else if (step >= STEP_Q1 && step <= STEP_Q4) {
            if (!validateCurrentQuestion()) return;
        }

        saveCurrentStepData();

        if (step < STEP_Q4) {
            goToStep(step + 1);
        } else {
            submit();
        }
    }

    private boolean validateUserInfo() {
        if (etNickname.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_nickname_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), R.string.err_gender_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etAge.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_age_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etHeight.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_height_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etWeight.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_weight_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validateCurrentQuestion() {
        if (currentQuestionRg == null || currentQuestionRg.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), R.string.err_question_incomplete, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void submit() {
        saveCurrentStepData();

        QuestionnaireEntity entity = new QuestionnaireEntity();
        entity.nickname = viewModel.savedNickname;
        entity.gender = viewModel.savedGenderIndex == 0
                ? getString(R.string.gender_male) : getString(R.string.gender_female);
        entity.height = Float.parseFloat(viewModel.savedHeight);
        entity.weight = Float.parseFloat(viewModel.savedWeight);
        entity.age = Integer.parseInt(viewModel.savedAge);
        entity.scoreTanshi = viewModel.savedAnswers[0] + 1;
        entity.scoreYinxu = viewModel.savedAnswers[1] + 1;
        entity.scoreQiyu = viewModel.savedAnswers[2] + 1;
        entity.scorePinghe = viewModel.savedAnswers[3] + 1;
        entity.createdAt = System.currentTimeMillis();
        viewModel.save(entity);
        viewModel.submitted = true;
        viewModel.submittedEntity = entity;

        showResult(entity);
    }

    private void showResult(QuestionnaireEntity entity) {
        View navBar = getView().findViewById(R.id.nav_bar);
        navBar.setVisibility(View.GONE);

        stepContainer.removeAllViews();
        LayoutInflater.from(requireContext()).inflate(R.layout.fragment_questionnaire_result, stepContainer, true);

        TextView tvSummary = stepContainer.findViewById(R.id.tv_user_summary);
        tvSummary.setText(getString(R.string.result_user_summary,
                entity.nickname, entity.gender, entity.age, entity.height, entity.weight));

        int[] scoreValues = {
                entity.scoreTanshi, entity.scoreYinxu, entity.scoreQiyu,
                entity.scorePinghe
        };
        int[] tvIds = {
                R.id.tv_score_q1, R.id.tv_score_q2, R.id.tv_score_q3,
                R.id.tv_score_q4
        };
        int[] progressIds = {
                R.id.progress_q1, R.id.progress_q2, R.id.progress_q3,
                R.id.progress_q4
        };

        for (int i = 0; i < 4; i++) {
            TextView tv = stepContainer.findViewById(tvIds[i]);
            tv.setText(getString(R.string.score_format, scoreValues[i]));

            ProgressBar pb = stepContainer.findViewById(progressIds[i]);
            pb.setProgress(scoreValues[i]);
        }

        MaterialButton btnBack = stepContainer.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            viewModel.submitted = false;
            viewModel.submittedEntity = null;
            viewModel.showingCover = false;

            navBar.setVisibility(View.VISIBLE);
            goToStep(STEP_USER_INFO);
        });

        MaterialButton btnRestart = stepContainer.findViewById(R.id.btn_restart);
        btnRestart.setOnClickListener(v -> {
            viewModel.currentStep = 0;
            viewModel.submitted = false;
            viewModel.submittedEntity = null;
            viewModel.showingCover = true;
            viewModel.savedNickname = "";
            viewModel.savedGenderIndex = -1;
            viewModel.savedAge = "";
            viewModel.savedHeight = "";
            viewModel.savedWeight = "";
            viewModel.savedAnswers = new int[]{-1, -1, -1, -1};

            navBar.setVisibility(View.VISIBLE);
            goToStep(STEP_COVER);
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
