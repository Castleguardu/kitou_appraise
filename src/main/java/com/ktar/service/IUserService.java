package com.ktar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ktar.dto.LoginFormDTO;
import com.ktar.dto.Result;
import com.ktar.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
