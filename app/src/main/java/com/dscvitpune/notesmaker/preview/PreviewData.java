package com.dscvitpune.notesmaker.preview;

public class PreviewData {
    private String mPreviewText;

    public PreviewData(String previewText) {
        mPreviewText = previewText;
    }

    public void changeText(String text) {
        mPreviewText = text;
    }

    public String getPreviewText() {
        return mPreviewText;
    }
}
