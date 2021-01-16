package com.dscvitpune.notesmaker.cloud;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.notesmaker.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class CloudNotes extends AppCompatActivity {

    RecyclerView cloudNotes;
    LinearLayoutManager linearLayoutManager;
    CloudFileAdapter cloudFileAdapter;
    TextView cloudMessage;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseStorage storage;
    StorageReference pdfStorage;
    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_notes);

        swipeRefreshLayout = findViewById(R.id.cloudListRefresh);
        cloudMessage = findViewById(R.id.cloudMessage);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        if (mUser != null) {
            cloudMessage.setVisibility(View.INVISIBLE);
            storage = FirebaseStorage.getInstance();
            pdfStorage = storage.getReference();
            pdfStorage = pdfStorage.child(mUser.getUid());
            pdfStorage = pdfStorage.child("PDFs");
            getFiles();
            swipeRefreshLayout.setEnabled(true);

            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getFiles();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        } else {
            swipeRefreshLayout.setEnabled(false);
            cloudMessage.setVisibility(View.VISIBLE);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        cloudNotes = findViewById(R.id.cloudNotes);
        linearLayoutManager = new LinearLayoutManager(this);


    }

    public void getFiles() {
        pdfStorage.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                cloudFileAdapter = new CloudFileAdapter(CloudNotes.this, listResult.getItems());
                cloudNotes.setLayoutManager(linearLayoutManager);
                cloudNotes.setAdapter(cloudFileAdapter);
            }
        });
    }

    public void shareNote(StorageReference storageReference) {
        File PDF = null;
        try {
            PDF = File.createTempFile(storageReference.getName(), ".pdf", getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
        final File finalPDF = PDF;

        storageReference.getFile(PDF).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                Uri uri = FileProvider.getUriForFile(CloudNotes.this, CloudNotes.this.getPackageName() + ".provider", finalPDF);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share it"));
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
                cloudFileAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}