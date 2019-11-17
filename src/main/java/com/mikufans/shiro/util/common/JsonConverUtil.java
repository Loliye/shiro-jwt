package com.mikufans.shiro.util.common;

import com.alibaba.fastjson.JSONObject;

public class JsonConverUtil
{
    public static <T> T jsonToObject(String pojo, Class<T> clazz)
    {
        return JSONObject.parseObject(pojo,clazz);
    }

    public static<T> String objectToJson(T t)
    {
        return JSONObject.toJSONString(t);
    }
}
