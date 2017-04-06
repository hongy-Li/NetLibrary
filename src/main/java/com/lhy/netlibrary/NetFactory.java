package com.lhy.netlibrary;


import android.content.Context;

import com.lhy.netlibrary.okhttp.OKHttpUtils;

/**
 * Created by lhy on 2017/2/28
 */
public class NetFactory {
    public enum NetType {
        OK_HTTP
    }

    public static <T extends BaseNet> T createNetUtils(Context context, NetType type) {
        BaseNet baseNet = null;
        switch (type) {
            case OK_HTTP:
                baseNet = OKHttpUtils.getInstance(context);
                break;
        }
        return (T) baseNet;
    }

    ;
}
