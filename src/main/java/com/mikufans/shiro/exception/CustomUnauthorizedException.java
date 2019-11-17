package com.mikufans.shiro.exception;

public class CustomUnauthorizedException extends RuntimeException
{
    public CustomUnauthorizedException()
    {
        super();
    }

    public CustomUnauthorizedException(String msg)
    {
        super(msg);
    }
}
