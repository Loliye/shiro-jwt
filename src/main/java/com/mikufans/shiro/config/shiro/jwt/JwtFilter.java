package com.mikufans.shiro.config.shiro.jwt;

import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.mikufans.shiro.exception.CustomExcetption;
import com.mikufans.shiro.model.common.Constant;
import com.mikufans.shiro.model.common.ResponseBean;
import com.mikufans.shiro.util.JwtUtil;
import com.mikufans.shiro.util.RedisUtil;
import com.mikufans.shiro.util.common.JsonConverUtil;
import com.mikufans.shiro.util.common.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * jwt过滤
 */
@Slf4j
public class JwtFilter extends BasicHttpAuthenticationFilter
{
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
    {
        //查看当前header是否携带authentication属性token，有就进行登陆授权
        if (this.isLoginAttempt(request, response))
        {
            try
            {
                //进行shiro登陆
                this.executeLogin(request, response);
            } catch (Exception e)
            {
                String msg = e.getMessage();
                Throwable throwable = e.getCause();
                if (throwable instanceof SignatureVerificationException)
                {
                    // 该异常为JWT的AccessToken认证失败(Token或者密钥不正确)
                    msg = "Token或者密钥不正确(" + throwable.getMessage() + ")";
                } else if (throwable instanceof TokenExpiredException)
                {
                    // 该异常为JWT的AccessToken已过期，判断RefreshToken未过期就进行AccessToken刷新
                    if (this.refreshToken(request, response))
                    {
                        return true;
                    } else
                    {
                        msg = "Token已过期(" + throwable.getMessage() + ")";
                    }
                } else
                {
                    // 应用异常不为空
                    if (throwable != null)
                    {
                        // 获取应用异常msg
                        msg = throwable.getMessage();
                    }
                }
                // Token认证失败直接返回Response信息
                this.response401(response, msg);


                return false;

            }
        } else
        {
            //没有携带token
            HttpServletRequest httpServletRequest = WebUtils.toHttp(request);

            String httpMethod = httpServletRequest.getMethod();

            String requestUri = httpServletRequest.getRequestURI();

            log.info("当前请求{} authorization属性（token）为空  请求类型{}", requestUri, httpMethod);

            Boolean mustLoginFlag = false;
            if (mustLoginFlag)
            {
                this.response401(response, "请先登录");
                return false;
            }
        }
        return true;
    }

    /**
     * 这里我们详细说明下为什么重写
     * 可以对比父类方法，只是将executeLogin方法调用去除了
     * 如果没有去除将会循环调用doGetAuthenticationInfo方法
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception
    {
        this.sendChallenge(request, response);
        return false;
    }

    /**
     * 检测header中里面是否包含authorization，有就进行token验证
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response)
    {
        String token = this.getAuthzHeader(request);
        return token != null;
    }

    /**
     * 进行accessToken登陆授权认证
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception
    {
        //拿到当前的authorization中accessToken  (shiro中getAuthzHeader已经实现)
        JwtToken token = new JwtToken(this.getAuthzHeader(request));

        //提交给UserRealm进行认证
        this.getSubject(request, response).login(token);

        return true;
    }

    /**
     * 对跨域提供支持
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception
    {
        HttpServletRequest httpServletRequest = WebUtils.toHttp(request);
        HttpServletResponse httpServletResponse = WebUtils.toHttp(response);
        httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", httpServletRequest.getHeader("Access-Control-Request-Headers"));
        // 跨域时会首先发送一个OPTIONS请求，这里我们给OPTIONS请求直接返回正常状态
        if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name()))
        {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            return false;
        }
        return super.preHandle(request, response);
    }

    /**
     * accessToken 刷新，进行判断RefreshToken是否过期，为过期就返回新的accessToken且继续正常访问
     *
     * @param request
     * @param response
     * @return
     */
    private boolean refreshToken(ServletRequest request, ServletResponse response)
    {
        String token = this.getAuthzHeader(request);
        String accout = JwtUtil.getClaim(token, Constant.ACCOUNT);

        if (RedisUtil.exists(Constant.PREFIX_SHIRO_REFRESH_TOKEN + accout))
        {
            String currentTime = RedisUtil.getObject(Constant.PREFIX_SHIRO_REFRESH_TOKEN + accout).toString();

            if (JwtUtil.getClaim(token, Constant.CURRENT_TIME_MILLIS).equals(currentTime))
            {
                String current = String.valueOf(System.currentTimeMillis());

                PropertiesUtil.readProperties("config.properties");
                String refreshTokenExpireTime = PropertiesUtil.getProperty("refreshTokenExpireTime");
                // 设置RefreshToken中的时间戳为当前最新时间戳，且刷新过期时间重新为30分钟过期(配置文件可配置refreshTokenExpireTime属性)
                RedisUtil.setObject(Constant.PREFIX_SHIRO_REFRESH_TOKEN + accout, current, Integer.parseInt(refreshTokenExpireTime));
                // 刷新AccessToken，设置时间戳为当前最新时间戳
                token = JwtUtil.sign(accout, current);
                // 将新刷新的AccessToken再次进行Shiro的登录
                JwtToken jwtToken = new JwtToken(token);
                // 提交给UserRealm进行认证，如果错误他会抛出异常并被捕获，如果没有抛出异常则代表登入成功，返回true
                this.getSubject(request, response).login(jwtToken);
                // 最后将刷新的AccessToken存放在Response的Header中的Authorization字段返回
                HttpServletResponse httpServletResponse = WebUtils.toHttp(response);
                httpServletResponse.setHeader("Authorization", token);
                httpServletResponse.setHeader("Access-Control-Expose-Headers", "Authorization");
                return true;
            }
        }
        return false;
    }


    /**
     * 不用转发  直接返回response信息
     *
     * @param response
     * @param msg
     */
    public void response401(ServletResponse response, String msg)
    {
        HttpServletResponse httpServletResponse = WebUtils.toHttp(response);
        httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setContentType("application/json;charset=utf-8");

        PrintWriter out = null;
        try
        {
            out = httpServletResponse.getWriter();
        } catch (IOException e)
        {
            log.error("直接返回Response信息出现IOException异常:" + e.getMessage());
            throw new CustomExcetption("直接返回Response信息出现IOException异常:" + e.getMessage());
        }
        String data = JsonConverUtil.objectToJson(new ResponseBean(HttpStatus.UNAUTHORIZED.value(), "无权访问(unauthorized)" + msg, null));
        out.append(data);

    }
}
