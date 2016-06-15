package com.ks.utils;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class MemcacheUtil {

	private static MemcachedClient memClient = null;
	
	public static MemcachedClient getInstance(){
		if(memClient==null){
			try{
				MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(Constant.MEMCACHE_HOST_PORT),Constant.MEMCACHE_WEIGHT);  
				memClient =builder.build();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return memClient;
	}
	
	
}
