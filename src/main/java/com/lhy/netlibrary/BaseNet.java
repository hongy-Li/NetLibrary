package com.lhy.netlibrary;


import java.util.Map;

/**
 * Created by lhy on 2017/2/28
 */
public interface BaseNet {
    void getRequest(String url, Map<String, Object> params, IRequestListener listener);

    void postRequest(String url, Map<String, Object> params, IRequestListener listener);

    void downloadFileWithProgress(String url, String filePath, String filename, IProgressListener listener);

    void uploadFileWithProgress(String url, String filePath, String fileName, IProgressListener listener);
}
