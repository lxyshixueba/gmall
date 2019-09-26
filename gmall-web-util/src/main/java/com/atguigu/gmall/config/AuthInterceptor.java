package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    /**
     * 创建拦截器，目的：在用户登录的时候将token存入cookie中
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    //在进入拦截器之前 这个拦截器的作用？ 验证用户的登录信息？在用户进入控制器之前做到 验证用户是否登陆过？
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getParameter("newToken");
        if(token!=null){
            //TODO 如果url中的token不为空将它存入 cookie中方便去取得
            //设置了cookie的过期时间
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);
            //这样其他的 控制器 在得到cookie之后也可以得到用户的登录信息了
        }
        if(token==null){
            //这种情况下登录之后就 拿不到 token的话那么 已经存在了cookie中了
            token = CookieUtil.getCookieValue(request, "token", false);
            //拿到了token之后应该做什么呢？
        }
        //TODO 获取到用户的昵称 然后存到 域中
        //读取token
        if(token!=null){
            Map map = getUserMapByToken(token);
            if(map!=null&&map.size()>0){
                String nickName = (String) map.get("nickName");
                request.setAttribute("nickName", nickName);
            }
        }

        HandlerMethod handlerMethod = (HandlerMethod)handler;
        LoginRequire loginRequireAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        //判断方法上有没有 LoginRequire这个注解，如果有，就进true，没有就进false
        if(loginRequireAnnotation!=null){
            //方法上有注解，需要 进行登录
            String salt = request.getHeader("x-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?newToken=" + token + "&salt=" + salt);
            if("success".equals(result)){
                //表示用户验证登录成功
                Map map = getUserMapByToken(token);
                String userId =(String) map.get("userId");
                request.setAttribute("userId",userId);
                return true;
            }else {
                if(loginRequireAnnotation.autoRedirect()){
                    //获取请求地址
                    String requestURL = request.getRequestURL().toString();  //http://item.gmall.com/38.html?
                    //编码url
                    String encodeURL  = URLEncoder.encode(requestURL, "UTF-8");
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    //不走拦截器了
                    return false;
                }
            }
        }
        return true;
    }

    private Map getUserMapByToken(String token) {
        // JwtUtil 工具类解密
        // base64 编码得到用户数据
        // eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ
        String tokenUserInfo  = StringUtils.substringBetween(token, ".");
        System.out.println(tokenUserInfo); // eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0
        // 创建对象
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        // 获取字节数组
        byte[] bytes = base64UrlCodec.decode(tokenUserInfo);
        // 字节数组转换为字符串
        String mapJson = null;
        try {
            mapJson = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return JSON.parseObject(mapJson,Map.class);
    }

    //在进入控制器之后返回视图之前
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    //在返回视图之后
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

}
