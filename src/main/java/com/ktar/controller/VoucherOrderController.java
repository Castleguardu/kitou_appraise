package com.ktar.controller;


import com.ktar.dto.Result;
import com.ktar.service.IVoucherOrderService;
import com.ktar.service.IVoucherService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;
    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        System.out.println("为什么一直不进来？？？");
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
