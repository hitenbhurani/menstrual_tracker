package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();

        // 1. HARDCODED YOUR EXACT WEB CLIENT ID HERE
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("199892000795-t7vhgudfo5h5u4k3f9ljhfmu19avgqpg.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // Routing to Register Activity
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            String email = getTrimmedText(etEmail);
            String rawPassword = etPassword.getText() == null ? "" : etPassword.getText().toString();
            String password = rawPassword.trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            signInWithEmailPassword(email, password, rawPassword);
        });

        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                // 2. ERROR CATCHER: This will show the exact code if it fails
                int statusCode = e.getStatusCode();
                Toast.makeText(this, "Google sign in failed. Code: " + statusCode, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            // CREATE THE LOCAL ROOM DATABASE ROW FOR GOOGLE USERS!
                            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                                com.miniflo.femcare.data.UserEntity entity = new com.miniflo.femcare.data.UserEntity(user.getEmail());
                                entity.name = user.getDisplayName() != null ? user.getDisplayName() : "FemCare User";
                                com.miniflo.femcare.data.AppDatabase.getInstance(getApplicationContext()).userDao().insertUser(entity);
                            });
                        }
                        routeAfterSignIn();
                    } else {
                        Toast.makeText(LoginActivity.this, "Firebase Auth with Google failed", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private String getTrimmedText(TextInputEditText inputEditText) {
        if (inputEditText == null || inputEditText.getText() == null) {
            return "";
        }
        return inputEditText.getText().toString().trim();
    }

    private void signInWithEmailPassword(String email, String trimmedPassword, String rawPassword) {
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(this, methodsTask -> {
                    if (methodsTask.isSuccessful() && methodsTask.getResult() != null) {
                        List<String> methods = methodsTask.getResult().getSignInMethods();
                        if (methods != null && !methods.isEmpty() && !methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                            Toast.makeText(
                                    LoginActivity.this,
                                    "This account is linked to Google Sign-In. Please use the Google button.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }
                    }

                    attemptEmailPasswordLogin(email, trimmedPassword, rawPassword);
                });
    }

    private void attemptEmailPasswordLogin(String email, String trimmedPassword, String rawPassword) {
        mAuth.signInWithEmailAndPassword(email, trimmedPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        routeAfterSignIn();
                        return;
                    }

                    Exception firstError = task.getException();
                    boolean retryWithRawPassword = !rawPassword.equals(trimmedPassword)
                            && firstError instanceof FirebaseAuthInvalidCredentialsException;

                    if (retryWithRawPassword) {
                        mAuth.signInWithEmailAndPassword(email, rawPassword)
                                .addOnCompleteListener(this, retryTask -> {
                                    if (retryTask.isSuccessful()) {
                                        routeAfterSignIn();
                                    } else {
                                        showEmailLoginError(retryTask.getException());
                                    }
                                });
                    } else {
                        showEmailLoginError(firstError);
                    }
                });
    }

    private void showEmailLoginError(Exception error) {
        if (error instanceof FirebaseAuthInvalidUserException) {
            Toast.makeText(this, "No account found for this email.", Toast.LENGTH_LONG).show();
            return;
        }

        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(
                    this,
                    "Incorrect email or password. If this account was created with Google, use Google Sign-In.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String message = error != null && error.getMessage() != null ? error.getMessage() : "Unknown error";
        Toast.makeText(this, "Auth Failed: " + message, Toast.LENGTH_SHORT).show();
    }

    private void routeAfterSignIn() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            openAndClearTask(MainActivity.class);
            return;
        }

        String email = user.getEmail().trim();
        FirebaseFirestore.getInstance().collection("users").document(email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Boolean onboardingField = snapshot.getBoolean("onboardingComplete");
                    boolean onboardingComplete = onboardingField == null || onboardingField;

                    SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("onboarding_complete", onboardingComplete).apply();

                    if (onboardingComplete) {
                        openAndClearTask(DashboardActivity.class);
                    } else {
                        openAndClearTask(MainActivity.class);
                    }
                })
                .addOnFailureListener(e -> openAndClearTask(DashboardActivity.class));
    }

    private void openAndClearTask(Class<?> destination) {
        if (destination == DashboardActivity.class) {
            BackgroundTaskScheduler.scheduleAll(this);
            BackgroundTaskScheduler.enqueueImmediateSync(this, "login_success");
        }

        Intent intent = new Intent(LoginActivity.this, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}