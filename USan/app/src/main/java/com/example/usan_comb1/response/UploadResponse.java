package com.example.usan_comb1.response;

import com.google.gson.annotations.SerializedName;

// 프로필 이미지 업로드
public class UploadResponse {
    @SerializedName("filename")
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}