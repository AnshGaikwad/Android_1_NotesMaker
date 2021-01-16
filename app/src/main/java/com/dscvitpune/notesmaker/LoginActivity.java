package com.dscvitpune.notesmaker;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notesmaker.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText lEmailId, lPassword;
    Button Login, NewAcc;
    TextView ForgotPassword;
    FirebaseAuth fAuth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        lEmailId = findViewById(R.id.edit_text_emailId);
        lPassword = findViewById(R.id.edit_text_password);
        Login = findViewById(R.id.button_login);
        ForgotPassword = findViewById(R.id.button_forgotPwd);
        NewAcc = findViewById(R.id.button_newAcc);
        fAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();


        if (user != null) {
            Intent noteIntent = new Intent(LoginActivity.this, NotesActivity.class);
            startActivity(noteIntent);
            finish();
        } else {
            NewAcc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                }
            });
            Login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String Email = lEmailId.getText().toString().trim();
                    String Password = lPassword.getText().toString().trim();
                    if (TextUtils.isEmpty(Email)) {
                        lEmailId.setError("Email Id is mandatory");
                        return;
                    }
                    if (TextUtils.isEmpty(Password)) {
                        lPassword.setError("Password is mandatory");
                        return;
                    }
                    if (Password.length() < 6) {
                        lPassword.setError("Password should be longer than 6 characters");
                        return;
                    }
                    fAuth.signInWithEmailAndPassword(Email, Password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                String userId = fAuth.getCurrentUser().getUid();
                                FirebaseUser user = fAuth.getCurrentUser();
                                if (!user.isEmailVerified()) {
                                    Toast.makeText(LoginActivity.this, "Email isn't verified, try again after verification of Email ", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, "You have Logged in", Toast.LENGTH_LONG).show();
                                    // user will be logged in into the notes activity
                                    Intent noteIntent = new Intent(LoginActivity.this, NotesActivity.class);
                                    startActivity(noteIntent);
                                    finish();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Incorrect Email or Password" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });
            ForgotPassword.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      final EditText resetMail = new EditText(v.getContext());
                                                      AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext());
                                                      passwordResetDialog.setTitle("Reset Password?");
                                                      passwordResetDialog.setMessage("Enter your email to receive reset link");
                                                      passwordResetDialog.setView(resetMail);
                                                      passwordResetDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                          @Override
                                                          public void onClick(DialogInterface dialog, int which) {
                                                              String mail = resetMail.getText().toString();
                                                              fAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                  @Override
                                                                  public void onSuccess(Void aVoid) {
                                                                      Toast.makeText(LoginActivity.this, "Reset Link sent to your Emai", Toast.LENGTH_SHORT).show();
                                                                  }
                                                              }).addOnFailureListener(new OnFailureListener() {
                                                                  @Override
                                                                  public void onFailure(@NonNull Exception e) {
                                                                      Toast.makeText(LoginActivity.this, "Error!Reset Link is not sent" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                  }
                                                              });
                                                          }
                                                      });
                                                      passwordResetDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                          @Override
                                                          public void onClick(DialogInterface dialog, int which) {
                                                          }
                                                      });
                                                      passwordResetDialog.create().show();
                                                  }
                                              }
            );
        }
    }
}
