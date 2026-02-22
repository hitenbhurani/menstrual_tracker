package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class UneasinessActivity extends AppCompatActivity {

    ArrayList<MaterialButton> symptomButtons = new ArrayList<>();
    TextInputLayout otherInputLayout;
    TextInputEditText otherInput;
    MaterialButton symp7; // "Other" button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uneasiness);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        MaterialButton symp1 = findViewById(R.id.symp1);
        MaterialButton symp2 = findViewById(R.id.symp2);
        MaterialButton symp3 = findViewById(R.id.symp3);
        MaterialButton symp4 = findViewById(R.id.symp4);
        MaterialButton symp5 = findViewById(R.id.symp5);
        MaterialButton symp6 = findViewById(R.id.symp6);
        symp7 = findViewById(R.id.symp7);

        otherInputLayout = findViewById(R.id.otherInputLayout);
        otherInput = findViewById(R.id.otherInput);
        Button nextButton = findViewById(R.id.nextButton);

        symptomButtons.add(symp1);
        symptomButtons.add(symp2);
        symptomButtons.add(symp3);
        symptomButtons.add(symp4);
        symptomButtons.add(symp5);
        symptomButtons.add(symp6);
        symptomButtons.add(symp7);

        for (MaterialButton button : symptomButtons) {
            button.setTag("unselected");
            button.setOnClickListener(v -> toggleButtonColor(button));
        }

        nextButton.setOnClickListener(v -> {

            boolean hasSelection = false;

            for (MaterialButton button : symptomButtons) {
                if (button.getTag().equals("selected")) {
                    hasSelection = true;
                    break;
                }
            }

            if (!hasSelection) {
                Toast.makeText(UneasinessActivity.this,
                        "Please select at least one option.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (symp7.getTag().equals("selected") &&
                    otherInput.getText().toString().trim().isEmpty()) {

                Toast.makeText(UneasinessActivity.this,
                        "Please specify your symptom in the text box.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 MARK ONBOARDING COMPLETE
            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("onboarding_complete", true);
            editor.apply();
            boolean test = prefs.getBoolean("onboarding_complete", false);
            Toast.makeText(this, "Saved now: " + test, Toast.LENGTH_LONG).show();

            // 🔥 GO TO DASHBOARD
            Intent intent = new Intent(UneasinessActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void toggleButtonColor(MaterialButton button) {

        if (button.getTag().equals("unselected")) {

            button.setTag("selected");
            button.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#F8BBD0")));
            button.setStrokeColor(
                    ColorStateList.valueOf(Color.parseColor("#F8BBD0")));

            if (button == symp7) {
                otherInputLayout.setVisibility(View.VISIBLE);
            }

        } else {

            button.setTag("unselected");
            button.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            button.setStrokeColor(
                    ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

            if (button == symp7) {
                otherInputLayout.setVisibility(View.GONE);
                otherInput.setText("");
            }
        }
    }
}