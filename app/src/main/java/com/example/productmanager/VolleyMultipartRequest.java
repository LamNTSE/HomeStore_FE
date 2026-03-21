package com.example.productmanager;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;
    private final Map<String, String> headers;
    private final Map<String, DataPart> params;

    private final String boundary = "apiclient-" + System.currentTimeMillis();
    private final String mimeType = "multipart/form-data; boundary=" + boundary;

    public VolleyMultipartRequest(int method, String url,
                                  Map<String, String> headers,
                                  Map<String, DataPart> params,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.headers = headers;
        this.params = params;
    }

    @Override
    public String getBodyContentType() {
        return mimeType;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public byte[] getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, DataPart> entry : params.entrySet()) {
                    buildPart(dos, entry.getValue(), entry.getKey());
                }
            }

            // 🔥 END BOUNDARY (QUAN TRỌNG)
            dos.writeBytes("--" + boundary + "--\r\n");

            dos.flush();
            dos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    private void buildPart(DataOutputStream dos, DataPart dataFile, String inputName) throws IOException {

        dos.writeBytes("--" + boundary + "\r\n");

        dos.writeBytes("Content-Disposition: form-data; name=\""
                + inputName + "\"; filename=\""
                + dataFile.fileName + "\"\r\n");

        // 🔥 AUTO MIME TYPE
        String type = "image/jpeg";
        if (dataFile.fileName.endsWith(".png")) type = "image/png";

        dos.writeBytes("Content-Type: " + type + "\r\n\r\n");

        dos.write(dataFile.content);
        dos.writeBytes("\r\n");
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    public static class DataPart {
        public String fileName;
        public byte[] content;

        public DataPart(String name, byte[] data) {
            fileName = name;
            content = data;
        }
    }
}