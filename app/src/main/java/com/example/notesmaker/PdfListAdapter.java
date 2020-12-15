package com.example.notesmaker;

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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

class PdfListAdapter extends RecyclerView.Adapter<PdfListAdapter.PdfViewHolder> implements Filterable {

    List<File> moviesList;
    List<File> moviesListAll;
    private OnItemClickListener mListener;
    private Context context;

    @Override
    public Filter getFilter() {
        return filter;
    }

    Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<File> filteredList = new ArrayList<>();
            if(constraint.toString().isEmpty()){
             filteredList.addAll(moviesListAll);
            }
            else{
                for(File movie: moviesListAll){
                    if(movie.getName().toLowerCase().contains((constraint.toString().toLowerCase()))){
                        filteredList.add(movie);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values =filteredList;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
           moviesList = (List<File>) results.values;
           notifyDataSetChanged();
        }
    };


    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    File[] PDFs;

    public PdfListAdapter(Context context, File[] PDFs) {
        Arrays.sort(PDFs);
        this.PDFs = PDFs;
        this.context = context;

        this.moviesList = Arrays.asList(PDFs);
        this.moviesListAll = new ArrayList<>(moviesList);

    }


    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pdf_item_layout, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        Date date = new Date(moviesList.get(position).lastModified());
        DateFormat formatter = new SimpleDateFormat("dd/MM/yy  HH:mm:ss");
        formatter.setTimeZone(TimeZone.getDefault());
        holder.pdfDate.setText(formatter.format(date));
        holder.pdfSize.setText(getSize(moviesList.get(position).length()));
        holder.pdfName.setText(moviesList.get(position).getName());

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
        return moviesList.size();
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

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch(item.getItemId())
            {
                case R.id.men_rename:
                    ((NotesActivity)context).renameNote(getAdapterPosition());
                    break;
                case R.id.men_share:
                    ((NotesActivity)context).shareNote(getAdapterPosition());

                    break;
                case R.id.men_delete:
                    ((NotesActivity)context).deleteNote(getAdapterPosition());
                    break;
            }
        return true;
        }
    }
}
