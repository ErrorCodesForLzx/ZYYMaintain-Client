package com.ecsoft.zyymaintain.network.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Okhttp工具包
 */
public class OKHTTPUtil {
    /**
     * 发送GET请求
     * @param url 请求地址
     * @param params 参数列表
     * @return
     */
    public static String sendGet(String url, Map<String,String> params){
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        Request request;
        if (params.size() > 0) {
            request = new Request.Builder()
                    .url(url+"?"+mapToParamString(params))
                    .get()
                    .build();
        } else  {
            request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
        }
        Call call =  client.newCall(request);
        String responseStr = "";
        try {
            Response response = call.execute();
            responseStr = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseStr;

    }


    public static String upLoadImage(String url, File file){
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        RequestBody image = RequestBody.create(MediaType.parse("image/*"),file);
        RequestBody requestBody = new MultipartBody.Builder() // 创建请求体
                .setType(MultipartBody.ALTERNATIVE)
                .addFormDataPart("file",file.getName(),image)
                .build();
        Request request = new Request.Builder() // 创建请求对象
                .url(url)
                .post(requestBody)
                .build();
        Call call =  client.newCall(request);
        String responseStr = "";
        Response response = null;
        try {
            response = call.execute();
            responseStr = response.body().string();

        } catch (IOException e) {
            e.printStackTrace();
            responseStr = "{}";
        }
        return responseStr;
    }

    private static String mapToParamString(Map<String,String> params){
        StringBuilder generatedString = new StringBuilder();
        // 遍历MAP集合
        for (Map.Entry<String, String> item : params.entrySet()) {
            generatedString.append(item.getKey()).append("=").append(item.getValue()).append("&");
        }
        generatedString.replace(generatedString.length()-1,generatedString.length(),"");
        return generatedString.toString();
    }
}
