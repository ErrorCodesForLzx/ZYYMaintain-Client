package com.ecsoft.zyymaintain.network;

import com.ecsoft.zyymaintain.config.GlobalConfiguration;
import com.ecsoft.zyymaintain.network.util.OKHTTPUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;

/**
 * 网络层 登录服务
 */
public class LoginService {
    String url = GlobalConfiguration.serverUrl;
    /**
     * 发送Login请求
     * @return 返回响应JSON文本
     */
    public String doLogin(String userName,String passWord){
        HashMap<String,String> params = new HashMap<>();
        params.put("userName",userName);
        params.put("passWord",passWord);
        return OKHTTPUtil.sendGet(url + "/auth/user/login", params);
    }
    public String tokenCheck(String userName,String token){
        HashMap<String,String> params = new HashMap<>();
        params.put("userName",userName);
        params.put("token",token);
        return OKHTTPUtil.sendGet(url + "/auth/user/authToken", params);
    }
}
