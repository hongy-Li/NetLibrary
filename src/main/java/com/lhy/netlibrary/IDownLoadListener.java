package com.lhy.netlibrary;

/**
 * Created by lhy on 2017/2/25.
 */
public interface IDownLoadListener extends IRequestListener {
    void progress(long currentBytes, long totalBytes, boolean done);
}
