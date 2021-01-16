package com.dscvitpune.notesmaker.preview;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notesmaker.R;

import java.util.ArrayList;

public class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.PreviewViewHolder> {
    private final ArrayList<PreviewData> mPreviewDataList;
    private OnItemClickListener mListener;

    private String finalString;

    public PreviewAdapter(ArrayList<PreviewData> previewDataList) {
        mPreviewDataList = previewDataList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.preview_layout, parent, false);
        PreviewViewHolder previewViewHolder = new PreviewViewHolder(view, mListener);
        return previewViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final PreviewViewHolder holder, final int position) {
        PreviewData currentItem = mPreviewDataList.get(position);
        holder.mPreviewTextView.setText(currentItem.getPreviewText());
        holder.mPreviewEditText.setText(currentItem.getPreviewText());
        holder.mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalString = holder.mPreviewEditText.getText().toString();
                mPreviewDataList.get(position).changeText(finalString);
                holder.mPreviewTextView.setText(finalString);
                holder.mPreviewConstraintLayout.setVisibility(View.GONE);
                holder.mPreviewTextView.setVisibility(View.VISIBLE);
            }
        });
        holder.mPreviewTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.mPreviewConstraintLayout.setVisibility(View.VISIBLE);
                holder.mPreviewTextView.setVisibility(View.GONE);
            }
        });
        holder.mPreviewEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) {
                    mPreviewDataList.get(position).changeText(String.valueOf(s));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return mPreviewDataList.size();
    }

    public interface OnItemClickListener {
        void onDeleteClick(int position);

        void onSummariseClick(int position);

        void onResetClick(int position);
    }

    public static class PreviewViewHolder extends RecyclerView.ViewHolder {
        public TextView mPreviewTextView;
        public Button mSaveButton, mSummariseButton, mResetButton;
        public ImageButton mDeleteButton;
        public EditText mPreviewEditText;
        public ConstraintLayout mPreviewConstraintLayout;

        public PreviewViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            mPreviewTextView = itemView.findViewById(R.id.prevTextView);
            mSaveButton = itemView.findViewById(R.id.prevLayoutSave);
            mSummariseButton = itemView.findViewById(R.id.prevLayoutSummary);
            mResetButton = itemView.findViewById(R.id.prevLayoutReset);
            mDeleteButton = itemView.findViewById(R.id.prevLayoutDelete);
            mPreviewEditText = itemView.findViewById(R.id.prevEditText);
            mPreviewConstraintLayout = itemView.findViewById(R.id.prevEditTextLayout);

            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onDeleteClick(position);
                        }
                    }
                }
            });
            mSummariseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onSummariseClick(position);
                        }
                    }
                }
            });
            mResetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onResetClick(position);
                        }
                    }
                }
            });
        }
    }
}
