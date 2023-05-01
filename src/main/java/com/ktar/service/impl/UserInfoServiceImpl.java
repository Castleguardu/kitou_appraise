package com.ktar.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.ktar.dto.LoginFormDTO;
import com.ktar.dto.Result;
import com.ktar.entity.User;
import com.ktar.entity.UserInfo;
import com.ktar.mapper.UserInfoMapper;
import com.ktar.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktar.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {


}
