package com.bloodpressure.app.ui.questionnaire;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bloodpressure.app.data.db.QuestionnaireEntity;
import com.bloodpressure.app.data.repository.QuestionnaireRepository;

import java.util.List;

public class QuestionnaireViewModel extends AndroidViewModel {

    private final QuestionnaireRepository repository;
    private final LiveData<List<QuestionnaireEntity>> allQuestionnaires;

    int currentStep = 0;
    boolean showingCover = true;
    boolean submitted = false;
    QuestionnaireEntity submittedEntity = null;
    String savedNickname = "";
    int savedGenderIndex = -1; // 0=male, 1=female, -1=none
    String savedAge = "";
    String savedHeight = "";
    String savedWeight = "";
    int[] savedAnswers = new int[]{-1, -1, -1, -1};

    public QuestionnaireViewModel(Application application) {
        super(application);
        repository = new QuestionnaireRepository(application);
        allQuestionnaires = repository.getAll();
    }

    public LiveData<List<QuestionnaireEntity>> getAllQuestionnaires() {
        return allQuestionnaires;
    }

    public void save(QuestionnaireEntity entity) {
        repository.insert(entity);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
