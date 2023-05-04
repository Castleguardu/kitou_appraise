package com.ktar.utils;


public interface ILOCK {
//    尝试获取锁
    boolean tryLock(long timeoutSec);

//    释放锁
    void unlock();
}
