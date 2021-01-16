package com.dscvitpune.notesmaker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dscvitpune.notesmaker.cloud.CloudNotes;
import com.dscvitpune.notesmaker.pdf.PdfActivity;
import com.dscvitpune.notesmaker.pdf.PdfListAdapter;
import com.dscvitpune.notesmaker.preview.PreviewActivity;
import com.example.notesmaker.R;
import com.dscvitpune.notesmaker.pdf.PDF;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.ml.quaterion.text2summary.Text2Summary;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import kotlin.jvm.internal.Ref.ObjectRef;

public class NotesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    public static FirebaseAuth mAuth;
    public static StorageReference pdfStorage;
    ImageView pfp; //Profile Photo
    TextView username, userEmail;
    NavigationView navigationView;
    RecyclerView PDFList;
    PdfListAdapter pdfListAdapter;
    LinearLayoutManager linearLayoutManager;
    File[] allPdfList;
    FirebaseUser mUser;
    FirebaseStorage storage;
    StorageReference userStorage;
    private DrawerLayout drawer;
    private Uri imgUri;

    @Override
    protected void onStart() {
        checkAuthentication();
        super.onStart();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void checkAuthentication() {
        Menu menu = navigationView.getMenu();

        MenuItem login = menu.findItem(R.id.nav_logout);
        FirebaseUser mFirebaseUser = mAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            login.setTitle("Log In");
            login.setIcon(getResources().getDrawable(R.drawable.login));
        } else {
            login.setTitle("Log Out");
            login.setIcon(getResources().getDrawable(R.drawable.logout));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        checkPermission();

        if (mUser != null) {
            storage = FirebaseStorage.getInstance();
            userStorage = storage.getReference();
            userStorage = userStorage.child(mUser.getUid());
            pdfStorage = userStorage.child("PDFs");
        }

        PDFList = findViewById(R.id.pdfList);
        initRecyclerView();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);


        View header = navigationView.getHeaderView(0);
        pfp = header.findViewById(R.id.userProfilePhoto);
        username = header.findViewById(R.id.username);
        userEmail = header.findViewById(R.id.userEmail);


        navigationView.setNavigationItemSelectedListener(this);

        if (mUser != null) {
            updateHeader();
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        FloatingActionButton add = findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    CropImage.startPickImageActivity(NotesActivity.this);
                }
            }
        });
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    NotesActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_STORAGE_PERMISSION
            );
        } else {
            return true;
        }
        return false;
    }

    private void updateHeader() {
        username.setText(mUser.getDisplayName());
        userEmail.setText(mUser.getEmail());

        if (mUser.getPhotoUrl() != null) {
            pfp.setImageURI(mUser.getPhotoUrl());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (pdfListAdapter != null) {
                    pdfListAdapter.getFilter().filter(newText);
                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void initRecyclerView() {
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyPdf";
            File directory = new File(path);
            File[] allFiles = directory.listFiles();

            allPdfList = getPDFs(allFiles);
            pdfListAdapter = new PdfListAdapter(NotesActivity.this, allPdfList);
            linearLayoutManager = new LinearLayoutManager(this);

            pdfListAdapter.setOnItemClickListener(new PdfListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    Intent intent = new Intent(NotesActivity.this, PdfActivity.class);
                    // passing current pdf from here
                    String pdfpath = allPdfList[position].getPath();

                    Log.d("chk", pdfpath);

                    intent.putExtra("PdfPath", pdfpath);
                    startActivity(intent);
                }
            });

            PDFList.setLayoutManager(linearLayoutManager);
            PDFList.setAdapter(pdfListAdapter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteNote(final int position) {
        if (allPdfList[position].exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
            builder.setMessage("Do you want to delete this Note?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            allPdfList[position].delete();
//                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            initRecyclerView();
                        }
                    })
                    .setNegativeButton("No", null);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    private File[] getPDFs(File[] allFiles) {
        int i = 0;
        File[] PDFs = new File[allFiles.length];
        for (File f : allFiles) {
            if (f.isFile() && f.getPath().endsWith(".pdf")) {
                PDFs[i] = f;
                i++;
            }
        }
        return PDFs;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            initRecyclerView();
        } else {
            Toast.makeText(this, "Permission Denied!!!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            imgUri = imageUri;
            startCrop(imageUri);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                imgUri = result.getUri();
                if (imgUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imgUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        getTextFromBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(this, "Image captured successfully !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCrop(Uri imageUri) {
        CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON).setMultiTouchEnabled(true).start(this);
    }

    private void getTextFromBitmap(Bitmap imageBitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector textDetector = FirebaseVision.getInstance().getVisionTextDetector();
        textDetector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                displayTextFromImage(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(NotesActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void displayTextFromImage(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        String text = "";
        if (blockList.size() == 0) {
            Toast.makeText(this, "No Text found in image", Toast.LENGTH_SHORT).show();
        } else {
            for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()) {
                text += block.getText() + "\n";
            }

            Intent previewActivity = new Intent(NotesActivity.this, PreviewActivity.class);
            previewActivity.putExtra("Text", text);
            startActivity(previewActivity);

        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_sync:
                startActivity(new Intent(getApplicationContext(), CloudNotes.class));
                Toast.makeText(this, "Sync", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;
            case R.id.nav_logout:
                if (menuItem.getTitle() == "Log Out") {
                    mAuth.signOut();
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                    checkAuthentication();
                    break;
                } else {
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                }
                break;
            case R.id.nav_read:
                Toast.makeText(this, "Read", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_contact:
                Toast.makeText(this, "Contact", Toast.LENGTH_SHORT).show();
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void renameNote(final int adapterPosition) {

        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(NotesActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.rename_dialog, null);

        // declaring edit text
        final EditText renEdit = mView.findViewById(R.id.rename_edit);
        // setting view
        builder.setView(mView);

        // prevents off screen touches
        builder.setCancelable(false);

        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String finalText = renEdit.getText().toString() + ".pdf";
                if (allPdfList[adapterPosition].exists()) {
                    File from = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyPdf", allPdfList[adapterPosition].getName());
                    File to = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyPdf", finalText);
                    from.renameTo(to);
                }
                initRecyclerView();
            }
        });

        // setting neg btn
        builder.setNegativeButton("Cancel", null);

        //show
        builder.show();


    }

    public void shareNote(int adapterPosition) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        Uri uri = FileProvider.getUriForFile(NotesActivity.this, NotesActivity.this.getPackageName() + ".provider", allPdfList[adapterPosition]);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share it"));
    }
}