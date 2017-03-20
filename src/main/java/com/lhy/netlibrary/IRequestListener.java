package com.lhy.netlibrary;

/**
 * Created by lhy on 2017/2/28.
 */
public interface IRequestListener {
        void onSucceed(String msg);
        void onFailed(int code, Exception e);
}
