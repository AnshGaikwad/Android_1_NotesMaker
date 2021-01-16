package com.dscvitpune.notesmaker.preview;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dscvitpune.notesmaker.NotesActivity;
import com.example.notesmaker.R;
import com.dscvitpune.notesmaker.pdf.PDF;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kotlin.jvm.internal.Ref;

public class PreviewActivity extends AppCompatActivity {
    public static StorageReference pdfStorage;
    SharedPreferences preferences;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseStorage storage;
    StorageReference userStorage;
    private ArrayList<PreviewData> mPreviewDataList;
    private String originalString;
    private Boolean isSummarised;
    private Uri imgUri;
    private RecyclerView mRecyclerView;
    private PreviewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Button buttonAdd, buttonSaveToPDF;
    private EditText editTextPDFName;

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (mUser != null) {
            storage = FirebaseStorage.getInstance();
            userStorage = storage.getReference();
            userStorage = userStorage.child(mUser.getUid());
            pdfStorage = userStorage.child("PDFs");
        }

        createPreviewDataList();
        buildRecyclerView();
        Bundle extras = getIntent().getExtras();
        String text;
        if (extras != null) {
            text = extras.getString("Text");
            insertItem(0, text);
        }


        buttonAdd = findViewById(R.id.btnAddPara);
        buttonSaveToPDF = findViewById(R.id.btnSavePDF);
        editTextPDFName = findViewById(R.id.prevEditTextTitle);
        editTextPDFName.setText(getName("Document"));

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.startPickImageActivity(PreviewActivity.this);
                /*if (imgUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imgUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        getTextFromBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }*/
            }
        });
        buttonSaveToPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullPDFString = "";
                for (PreviewData data : mPreviewDataList) {
                    fullPDFString += data.getPreviewText() + "\n";
                }
                String PDFName = editTextPDFName.getText().toString();
                if (PDFName == null) {
                    PDFName = getName("Document");
                }
                saveToPDF(fullPDFString, PDFName);
            }
        });
    }

    private void saveToPDF(String text, String name) {

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            if (text.isEmpty()) {
                Toast.makeText(this, "Nothing to save, Text is empty", Toast.LENGTH_SHORT).show();
            } else {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyPdf";

                PDF pdf = new PDF();
                pdf.addParagraph(text);
                File temp = null;
                try {
                    String tempName = name;
                    if (name.length() <= 3) {
                        tempName = name + "111";
                    }
                    temp = File.createTempFile(tempName, ".pdf", getCacheDir());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final File file = pdf.makeDocument(temp);

                if (mUser != null) {
                    if (preferences.getBoolean("CloudUpload", false)) {
                        if (file != null) {
                            StorageReference tempFile = pdfStorage.child(name + ".pdf");
                            tempFile.putFile(Uri.fromFile(file)).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            });
                        }
                        if (!preferences.getBoolean("LocalStorage", false)) {
                            File dir;

                            dir = new File(path);
                            if (!dir.exists())
                                dir.mkdirs();
                            File finalPDF = new File(path, name + ".pdf");
                            try {
                                copy(file, finalPDF);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        File dir;

                        dir = new File(path);
                        if (!dir.exists())
                            dir.mkdirs();
                        File finalPDF = new File(path, name + ".pdf");
                        try {
                            copy(file, finalPDF);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    File dir;

                    dir = new File(path);
                    if (!dir.exists())
                        dir.mkdirs();
                    File finalPDF = new File(path, name + ".pdf");
                    try {
                        copy(file, finalPDF);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(this, "Note Saved as a PDF in " + path, Toast.LENGTH_SHORT).show();

            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Storage Permission Needed");
                alert.setMessage("We need storage permission to store the PDF on your device. Please grant storage permission.");
                alert.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 01);
                    }
                });
                alert.show();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 01);
            }
        }
        startActivity(new Intent(PreviewActivity.this, NotesActivity.class));
        finish();
    }

    String getName(String name) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss", Locale.ENGLISH);
        Date now = new Date();

        String pdfName = (name + "_" + formatter.format(now));
        return pdfName;
    }

    private void getTextFromBitmap(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextDetector textDetector = FirebaseVision.getInstance().getVisionTextDetector();
        textDetector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                displayTextFromImage(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PreviewActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            int position = mAdapter.getItemCount();
            insertItem(position, text);
        }
    }

    public void insertItem(int position, String text) {
        mPreviewDataList.add(position, new PreviewData(text));
        mAdapter.notifyItemInserted(position);
        mAdapter.notifyDataSetChanged();
    }

    public void removeItem(int position) {
        mPreviewDataList.remove(position);
        mAdapter.notifyItemRemoved(position);
        mAdapter.notifyDataSetChanged();
    }

    public void changeItem(int position, String text) {
        mPreviewDataList.get(position).changeText(text);
        mAdapter.notifyItemChanged(position);
        mAdapter.notifyDataSetChanged();
    }

    public void createPreviewDataList() {
        mPreviewDataList = new ArrayList<>();
        /*mPreviewDataList.add(new PreviewData("Great job!"));
        mPreviewDataList.add(new PreviewData("Amazing job!"));
        mPreviewDataList.add(new PreviewData("Amazing Work!"));*/
    }

    public void buildRecyclerView() {
        mRecyclerView = findViewById(R.id.previewRecyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new PreviewAdapter(mPreviewDataList);
        isSummarised = false;

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new PreviewAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int position) {
                removeItem(position);
            }

            @Override
            public void onSummariseClick(int position) {
                isSummarised = true;
                originalString = mPreviewDataList.get(position).getPreviewText();
                String summarisedText = summariseText(originalString);
                changeItem(position, summarisedText);
            }

            @Override
            public void onResetClick(int position) {
                if (isSummarised) {
                    changeItem(position, originalString);
                }
            }
        });
    }

    private String summariseText(String text) {

        final Ref.ObjectRef summary = new Ref.ObjectRef();

        summary.element = Text2Summary.Companion.summarize(text, 0.4F);

        return (String) summary.element;
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
}