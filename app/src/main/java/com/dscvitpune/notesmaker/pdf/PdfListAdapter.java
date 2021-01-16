package com.dscvitpune.notesmaker.pdf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dscvitpune.notesmaker.NotesActivity;
import com.example.notesmaker.R;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class PdfListAdapter extends RecyclerView.Adapter<PdfListAdapter.PdfViewHolder> implements Filterable {

    List<File> pdfList;
    List<File> pdfListAll;
    Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<File> filteredList = new ArrayList<>();
            if (constraint.toString().isEmpty()) {
                filteredList.addAll(pdfListAll);
            } else {
                for (File pdf : pdfListAll) {
                    if (pdf.getName().toLowerCase().contains((constraint.toString().toLowerCase()))) {
                        filteredList.add(pdf);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            pdfList = (List<File>) results.values;
            notifyDataSetChanged();
        }
    };
    File[] PDFs;
    private OnItemClickListener mListener;
    private final Context context;


    public PdfListAdapter(Context context, File[] PDFs) {
        Arrays.sort(PDFs);
        this.PDFs = PDFs;
        this.context = context;
        this.pdfList = Arrays.asList(PDFs);
        this.pdfListAll = new ArrayList<>(pdfList);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pdf_item_layout, parent, false);
        return new PdfViewHolder(view);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        Date date = new Date(pdfList.get(position).lastModified());
        DateFormat formatter = new SimpleDateFormat("dd/MM/yy  HH:mm:ss");
        formatter.setTimeZone(TimeZone.getDefault());
        holder.pdfDate.setText(formatter.format(date));
        holder.pdfSize.setText(getSize(pdfList.get(position).length()));
        holder.pdfName.setText(pdfList.get(position).getName());
    }

    private String getSize(long sizeBytes) {
        long MB = 1024L * 1024L;
        long KB = 1024L;

        String size = (sizeBytes) + "Bytes";
        if (sizeBytes > (2 * MB)) {
            size = (sizeBytes / MB) + "MB";
        } else if (sizeBytes > (2 * KB)) {
            size = (sizeBytes / KB) + "KB";
        }
        return size;
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public class PdfViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        TextView pdfName, pdfSize, pdfDate;
        Button dotBtn;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);

            pdfName = itemView.findViewById(R.id.cloudFileName);
            pdfDate = itemView.findViewById(R.id.cloudFileTime);
            pdfSize = itemView.findViewById(R.id.cloudFileSize);
            dotBtn = itemView.findViewById(R.id.cloudFileMenu);

            dotBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopupMenu(v);

                }
            });


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onItemClick(position);
                        }
                    }
                }
            });

        }

        private void showPopupMenu(View v) {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.inflate(R.menu.pop_up_menu);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.men_rename:
                    ((NotesActivity) context).renameNote(getAdapterPosition());
                    break;
                case R.id.men_share:
                    ((NotesActivity) context).shareNote(getAdapterPosition());

                    break;
                case R.id.men_delete:
                    ((NotesActivity) context).deleteNote(getAdapterPosition());
                    break;
            }
            return true;
        }
    }
}
