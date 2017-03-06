package com.lhy.netlibrary;

import org.json.JSONException;

import java.util.Map;

/**
 * Created by lhy on 2017/2/28.
 */
public interface BaseNet {
    void getHttp(String url, Map<String, Object> params, IRequestListener listener);

    void postHttp(String url, Map<String, Object> params, IRequestListener listener) throws  Exception;
}
