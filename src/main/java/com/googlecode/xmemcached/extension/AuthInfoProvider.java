package com.googlecode.xmemcached.extension;

import net.rubyeye.xmemcached.auth.AuthInfo;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public interface AuthInfoProvider {

    default Map<InetSocketAddress, AuthInfo> getAuthInfoMap(){
        return new HashMap<>();
    };

}