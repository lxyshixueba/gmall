package com.atguigu.gmall.gmallpassportweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import user.service.UserInfoService;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Value("${my.key.self}")
    private String mykey;

    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        // 保存上
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        // 取得ip地址
        String remoteAddr  = request.getHeader("X-forwarded-for");
        if (userInfo!=null) {
            UserInfo loginUser = userInfoService.login(userInfo);
            if (loginUser == null) {
                return "fail";
            } else {
                // 生成token
                Map map = new HashMap();
                map.put("userId", loginUser.getId());
                map.put("nickName", loginUser.getNickName());
                String token = JwtUtil.encode(mykey, map, remoteAddr);

                return token;
            }
        }
        return "fail";
    }

    //此方法用做 获取用户的token，通过token以得到用户的信息
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String newToken = request.getParameter("newToken");
        String salt = request.getParameter("salt");
        Map<String, Object> map = JwtUtil.decode(newToken,mykey , salt);
        if(map!=null&&map.size()>0){
            //用户已经登录过了
            String userId = (String) map.get("userId");
            String nickName = (String) map.get("nickName");
            //得到了用户的个人信息，进行验证？
            UserInfo userInfo = userInfoService.verify(userId);
            if(userInfo!=null){
                return "success";
            }
        }
        return "fail";
    }
}
