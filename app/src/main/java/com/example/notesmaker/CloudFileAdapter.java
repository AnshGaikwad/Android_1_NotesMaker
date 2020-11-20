package com.example.notesmaker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

class CloudFileAdapter extends RecyclerView.Adapter<CloudFileAdapter.ViewHolder> {
    private Context mContext;
    private List<StorageReference> mList;


    public CloudFileAdapter(Context mContext, List<StorageReference> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pdf_item_layout, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final StorageReference mFile = mList.get(position);
        holder.cloudFileName.setText(mFile.getName());

        mFile.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
            Date date = new Date(storageMetadata.getCreationTimeMillis());
            DateFormat formatter = new SimpleDateFormat("dd/MM/yy  HH:mm:ss");
            formatter.setTimeZone(TimeZone.getDefault());
            holder.cloudFileSize.setText(formatter.format(date));
            holder.cloudFileTime.setText(getSize(storageMetadata.getSizeBytes()));
            }
        });


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File cache = mContext.getCacheDir();
                File PDF = null;
                try {
                    PDF = File.createTempFile("TempFile", null, cache);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final File finalPDF = PDF;
                mFile.getFile(PDF).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Intent intent = new Intent(mContext, PdfActivity.class);
                        intent.putExtra("PdfPath", finalPDF.getAbsolutePath());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                });
            }
        });

        holder.cloudFileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.inflate(R.menu.cloud_file_menu);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch(item.getItemId())
                            {
                                case R.id.men_share:
                                    ((CloudNotes)mContext).shareNote(mFile);
                                    break;

                                case R.id.men_delete:
                                    deleteFile(mFile);
                                    break;
                            }
                            return false;
                        }
                    });
                    popupMenu.show();
            }
        });
    }

    private String getSize(long sizeBytes) {
        long MB = 1024L * 1024L;
        long KB = 1024L;

        String size = (sizeBytes) + "Bytes";
        if (sizeBytes>(2*MB)){
            size = (sizeBytes/MB) + "MB";
        }else if (sizeBytes>(2*KB)){
            size = (sizeBytes/KB) + "KB";
        }
        return size;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView cloudFileName, cloudFileSize, cloudFileTime;
        Button cloudFileMenu;
        View mView;

        public ViewHolder(@NonNull View mItem) {
            super(mItem);
            cloudFileName =mItem.findViewById(R.id.cloudFileName);
            cloudFileSize = mItem.findViewById(R.id.cloudFileSize);
            cloudFileTime = mItem.findViewById(R.id.cloudFileTime);
            cloudFileMenu = mItem.findViewById(R.id.cloudFileMenu);

            mView = mItem;
        }
    }

    private boolean deleteFile(final StorageReference storageReference){
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle("Delete File on Cloud")
                .setMessage("Once you delete this file it will disappear forever.\n Are you sure you want to delete?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(mContext, "File Deleted Successfully", Toast.LENGTH_SHORT).show();
                                ((CloudNotes)mContext).getFiles();
                            }
                        });
                    }
                })
                .setNeutralButton("Cancel", null)
                .create();
        dialog.show();
        return false;
    }

}
