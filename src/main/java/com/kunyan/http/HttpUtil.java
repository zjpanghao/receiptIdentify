package com.kunyan.http;

import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HttpUtil {
    public static String get(String serverUrl)  {
        DataOutputStream out = null;
        DataInputStream in = null;
        try {

            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setDoInput(true);
            connection.connect();

//            out = new DataOutputStream(connection.getOutputStream());
//            JsonObject jsonObject = new JsonObject();
//            jsonObject.addProperty("image", Base64.getEncoder().encodeToString(data));
//            out.writeBytes(jsonObject.toString());
//            out.flush();
            int statusCode = connection.getResponseCode();
            if (statusCode == 200) {
                in = new DataInputStream(connection.getInputStream());
                byte [] buffer = new byte[100];
                int n = 0;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((n =in.read(buffer, 0, 100)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, n);
                }
                return byteArrayOutputStream.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String  post(String serverUrl, Map<String, String> params) throws IOException{
        DataOutputStream  out = null;
        DataInputStream in = null;
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.connect();

            out = new DataOutputStream(connection.getOutputStream());
            JsonObject jsonObject = new JsonObject();
            for(Map.Entry<String, String> entry : params.entrySet()) {
                jsonObject.addProperty(entry.getKey(), entry.getValue());
            }
            out.writeBytes(jsonObject.toString());
            out.flush();
            int statusCode = connection.getResponseCode();
            if (statusCode == 200) {
                in = new DataInputStream(connection.getInputStream());
                byte [] buffer = new byte[100];
                int n = 0;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((n =in.read(buffer, 0, 100)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, n);
                }
                return new String(byteArrayOutputStream.toByteArray());
            }
        } catch (MalformedURLException e) {
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
        return null;
    }
}
