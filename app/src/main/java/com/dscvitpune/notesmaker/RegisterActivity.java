package com.dscvitpune.notesmaker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notesmaker.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.theartofdev.edmodo.cropper.CropImage;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {
    private static final int SELECT_PICTURE = 0;
    private static final String TAG = "Register";
    EditText rEmailId, rPassword, rName;
    CircleImageView profilePicture;
    Uri pfp; //profile Picture
    FirebaseAuth fAuth;
    Button Next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);
        fAuth = FirebaseAuth.getInstance();
        Next = findViewById(R.id.button_next);
        rEmailId = findViewById(R.id.edit_text_emailId);
        rPassword = findViewById(R.id.edit_text_password);
        rName = findViewById(R.id.edit_text_name);
        profilePicture = findViewById(R.id.profile_photo);

        profilePicture.setOnClickListener(new SelectImage());
        pfp = null;


        Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = rEmailId.getText().toString().trim();
                String password = rPassword.getText().toString().trim();
                String name = rName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    rName.setError("Name is mandatory");
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    rEmailId.setError("Email Id is mandatory");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    rPassword.setError("Password is mandatory");
                    return;
                }
                if (password.length() < 6) {
                    rPassword.setError("Password should be longer than 6 characters");
                    return;
                }


                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    //as
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            final FirebaseUser user = fAuth.getCurrentUser();
                            assert user != null;
                            user.sendEmailVerification()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                              @Override
                                                              public void onSuccess(Void aVoid) {
                                                                  Toast.makeText(RegisterActivity.this, "Verification Message has been sent", Toast.LENGTH_LONG).show();
                                                                  UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                                          .setDisplayName(rName.getText().toString())
                                                                          .setPhotoUri(pfp)
                                                                          .build();
                                                                  user.updateProfile(profileUpdates);
                                                              }
                                                          }
                                    ).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "On Failure: Message not sent" + e.getMessage());
                                    Toast.makeText(RegisterActivity.this, "Verification Message not sent", Toast.LENGTH_LONG).show();
                                    user.delete();
                                }
                            });
                            fAuth.signOut();
                            Toast.makeText(RegisterActivity.this, "User Created", Toast.LENGTH_LONG).show();
                            Intent mainIntent = new Intent(getApplicationContext(), LoginActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(mainIntent);
                        } else {
                            Toast.makeText(RegisterActivity.this, "Error!" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri image = data.getData();

                CropImage.activity(image)
                        .start(this);
            }


            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    pfp = result.getUri();
                    profilePicture.setImageURI(pfp);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
            }
        }
    }

    public class SelectImage implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
        }
    }
}










