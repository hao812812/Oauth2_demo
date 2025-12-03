package com.example.pic.config;

import java.io.IOException;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@Component
public class TraceIdFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String traceId = UUID.randomUUID().toString();

		// 放入 MDC（讓 logback 可以取 %X{traceId}）
		MDC.put("traceId", traceId);

		try {
			chain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
