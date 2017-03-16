package com.lhy.netlibrary.okhttp;

import com.lhy.netlibrary.IProgressListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by lhy on 2017/2/25
 */
public class ProgressResponseBody extends ResponseBody {
    private final ResponseBody responseBody;
    private final IProgressListener progressListener;
    private BufferedSource bufferedSource;

    public ProgressResponseBody(ResponseBody responseBody,
                                IProgressListener progressListener) {
        this.responseBody = responseBody;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            long currentBytes = 0L;
            long contentLength = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();
                }
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                currentBytes += bytesRead != -1 ? bytesRead : 0;
                if (null != progressListener) {
                    progressListener.progress(currentBytes, contentLength, bytesRead == -1);
                }
                return bytesRead;
            }


        };
    }

}
