package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import user.service.PaymentService;

public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentServiceMapper paymentServiceMapper;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentServiceMapper.insertSelective(paymentInfo);
    }
}
