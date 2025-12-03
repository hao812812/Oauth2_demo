package com.example.pic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(RequestLogInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {

		req.setAttribute("startTime", System.currentTimeMillis());
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex)
			throws Exception {

		long start = (long) req.getAttribute("startTime");
		long duration = System.currentTimeMillis() - start;

		log.info("API: {} {} | Status={} | 耗時={}ms", req.getMethod(), req.getRequestURI(), res.getStatus(), duration);

	}

}
