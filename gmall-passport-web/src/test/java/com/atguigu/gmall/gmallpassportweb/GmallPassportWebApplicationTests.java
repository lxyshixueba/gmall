package com.atguigu.gmall.gmallpassportweb;

import com.atguigu.gmall.config.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

    @Value("${my.key.self}")
    private String mykey;

    @Test
    public void contextLoads() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("lxy", "1");
        map.put("lxya", "2");
        String encode = JwtUtil.encode(mykey, map, "192.168.213.221");
        System.out.println(encode);
        Map<String, Object> decode = JwtUtil.decode(encode,mykey , "192.168.213.221");
        System.out.println(decode);
    }

}
