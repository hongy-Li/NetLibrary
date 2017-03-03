package com.lhy.netlibrary.okhttp;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.lhy.netlibrary.BaseNet;
import com.lhy.netlibrary.IDownLoadListener;
import com.lhy.netlibrary.IRequestListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * Created by lhy on 2017/2/25
 */
public class OKHttpUtils implements BaseNet {
    private OKHttpUtils() {
        mCommonClient = new OkHttpClient();
    }

    public static OKHttpUtils getInstance() {
        return OKHttpUtilsHolder.sInstance;
    }


    private static class OKHttpUtilsHolder {
        private static final OKHttpUtils sInstance = new OKHttpUtils();
    }

    private static final String TAG = "OKHttpUtils";
    private OkHttpClient mCommonClient;
    private Map<String, Call> mCall = new HashMap<>();//用于断点续传

    public void downLoad(final boolean isAutoResume, String url, String path, String filename, final long startPoints, final IDownLoadListener downLoadListener) {
        // 拦截器，用上ProgressResponseBody
        if (TextUtils.isEmpty(path)) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        }
        if (TextUtils.isEmpty(filename)) {
            filename = getFileNameFromUrl(url);
        }
        File fileDir = new File(path);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        final File file = new File(fileDir, filename);
        Request request;
        Call call;
        if (isAutoResume) { //支持断点续传
            request = new Request.Builder()
                    .url(url)
                    .header("RANGE", "bytes=" + startPoints + "-")//断点续传要用到的，指示下载的区间
                    .build();
            call = getProgressClient(true, downLoadListener).newCall(request);
            mCall.put(url, call);
        } else { //不支持断点续传
            request = new Request.Builder()
                    .url(url)
                    .build();
            call = getProgressClient(true, downLoadListener).newCall(request);
        }
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (isAutoResume) {
                    save(response, startPoints, file);
                } else {
                    save(response, file);
                }
            }
        });
    }

    private void onPause(String url) {
        Call call = mCall.get(url);
        if (call != null) {
            call.cancel();
        }
    }

    private void save(Response response, File file) {
        try {
            BufferedSink sink = Okio.buffer(Okio.sink(file));
            sink.writeAll(response.body().source());
            sink.close();
            Log.i(TAG, "Path=" + file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save1(Response response, File file) {
        InputStream inputStream = response.body().byteStream();
        long total = response.body().contentLength();
        Log.i(TAG, "total=" + total);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[2048];
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
            fileOutputStream.flush();
        } catch (IOException e) {
            Log.i(TAG, "IOException");
            e.printStackTrace();
        }
        Log.d(TAG, "文件下载成功");
    }

    private void save(Response response, long startsPoint, File file) {
        ResponseBody body = response.body();
        InputStream in = body.byteStream();
        FileChannel channelOut = null;
        // 随机访问文件，可以指定断点续传的起始位置
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rwd");
            //Chanel NIO中的用法，由于RandomAccessFile没有使用缓存策略，直接使用会使得下载速度变慢，亲测缓存下载3.3秒的文件，用普通的RandomAccessFile需要20多秒。
            channelOut = randomAccessFile.getChannel();
            // 内存映射，直接使用RandomAccessFile，是用其seek方法指定下载的起始位置，使用缓存下载，在这里指定下载位置。
            MappedByteBuffer mappedBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE, startsPoint, body.contentLength());
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                mappedBuffer.put(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                if (channelOut != null) {
                    channelOut.close();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private OkHttpClient getProgressClient(boolean isDownOrUpload, final IDownLoadListener progressListener) {
        // 拦截器，用上ProgressResponseBody
        if (mCommonClient == null) {
            mCommonClient = new OkHttpClient();
        }
        if (isDownOrUpload) {
            Interceptor interceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                            .build();
                }
            };


            return new OkHttpClient.Builder()
                    .addNetworkInterceptor(interceptor)
                    .build();
        }
        return mCommonClient;
    }

    /**
     * 截取路径中 最后一个 / 后的内容
     *
     * @param url
     * @return
     */
    public String getFileNameFromUrl(String url) {
        int separatorIndex = url.lastIndexOf("/");
        return ((separatorIndex < 0) ? url : url.substring(separatorIndex + 1, url.length()));
    }

    /**
     * get异步请求
     */
    public void getAsynHttp(String url, Map<String, Object> params, IRequestListener listener) {
        url = changeURL(url, params);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call mcall = getProgressClient(false, null).newCall(request);
        mcall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (null != response.cacheResponse()) {
                    String str = response.cacheResponse().toString();
                    Log.i(TAG, "cache---" + str);
                } else {

                    String str = response.networkResponse().toString();
                    Log.i(TAG, "network---" + str);
                    Log.i(TAG, response.body().string());
                    Log.i(TAG, response.body().contentType().toString());
                    Log.i(TAG, response.cacheControl().toString());
//                Log.i(TAG, response.cacheResponse().toString());
                    Log.i(TAG, response.challenges().toString());
                    Log.i(TAG, response.code() + "");
//                Log.i(TAG, response.handshake().toString());
                    Log.i(TAG, response.headers().toString());
                    Log.i(TAG, response.isRedirect() + "");
                    Log.i(TAG, response.isSuccessful() + "");
                    Log.i(TAG, response.message() + "");
                    Log.i(TAG, response.request().method() + "");
                }
            }
        });
    }

    /**
     * post异步请求
     */
    public void postAsynHttp(String url, Map<String, Object> params) throws Exception {
        JSONObject json = new JSONObject();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                json.put(key, value);
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString()))
                .build();
        Call call = getProgressClient(false, null).newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "e=" + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                String res = response.toString();
                response.body().contentType();
                Log.i(TAG, body);
                Log.i(TAG, response.body().contentType().toString());
                Log.i(TAG, response.cacheControl().toString());
//                Log.i(TAG, response.cacheResponse().toString());
                Log.i(TAG, response.challenges().toString());
                Log.i(TAG, response.code() + "");
//                Log.i(TAG, response.handshake().toString());
                Log.i(TAG, response.headers().toString());
                Log.i(TAG, response.isRedirect() + "");
                Log.i(TAG, response.isSuccessful() + "");
                Log.i(TAG, response.message() + "");
                Log.i(TAG, response.request().method() + "");
            }

        });
    }

    /**
     * 异步上传文件
     */
    public void postAsynFile(String filePath, String url, IDownLoadListener listener) {
//        File file = new File(filePath);
//        String filename="滚滚红尘";
//        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
//        RequestBody requestBody = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addPart(Headers.of(
//                        "Content-Disposition",
//                        "form-data; name=\"username\""),
//                        RequestBody.create(null, "HGR"))
//                .addPart(Headers.of(
//                        "Content-Disposition",
//                        "form-data; name=\"mFile\"; filename=\"" + filename + "\""), fileBody)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(url)
//                .post(new ProgressRequestBody(requestBody, listener));


        File file = new File(filePath);
//        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);

        RequestBody requestBody = new MultipartBody.Builder() //建立请求的内容
                .setType(MultipartBody.FORM)
                .addPart(fileBody)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(new ProgressRequestBody(requestBody, listener))
                .build();
        mCommonClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG, response.body().string());
            }
        });
    }

    private String changeURL(String url, Map<String, Object> params) {
        String lastUrl = url;
        if (params != null) {
            Set<String> keySet = params.keySet();
            if (keySet.size() > 0)
                for (String key : keySet) {
                    if (!lastUrl.contains("?")) {
                        Object value = params.get(key);
                        lastUrl = lastUrl + "?" + key + "=" + value;
                    } else {
                        Object value = params.get(key);
                        lastUrl = lastUrl + "&" + key + "=" + value;
                    }
                }
            else {
                lastUrl = url;
            }
        }
        Log.i(TAG, "lastUrl=" + lastUrl);
        return lastUrl;
    }

    public void setSupportCookie() {
        mCommonClient.newBuilder().cookieJar(new CookieJar() {
            private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url, cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url);
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        });
    }

    @Override
    public void getHttp(String url, Map<String, Object> params, IRequestListener listener) {
        url = changeURL(url, params);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call mcall = getProgressClient(false, null).newCall(request);
        mcall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (null != response.cacheResponse()) {
                    String str = response.cacheResponse().toString();
                    Log.i(TAG, "cache---" + str);
                } else {

                    String str = response.networkResponse().toString();
                    Log.i(TAG, "network---" + str);
                    Log.i(TAG, response.body().string());
                    Log.i(TAG, response.body().contentType().toString());
                    Log.i(TAG, response.cacheControl().toString());
//                Log.i(TAG, response.cacheResponse().toString());
                    Log.i(TAG, response.challenges().toString());
                    Log.i(TAG, response.code() + "");
//                Log.i(TAG, response.handshake().toString());
                    Log.i(TAG, response.headers().toString());
                    Log.i(TAG, response.isRedirect() + "");
                    Log.i(TAG, response.isSuccessful() + "");
                    Log.i(TAG, response.message() + "");
                    Log.i(TAG, response.request().method() + "");
                }
            }
        });
    }

    @Override
    public void postHttp(String url, Map<String, Object> params, IRequestListener listener) {

    }
}
