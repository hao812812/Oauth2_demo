package com.example.pic.config;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.pic.Exception.BaseException;
import Dto.commonRes;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	/**
	 * 抓取自定例外
	 * @param e
	 * @return
	 */
	@ExceptionHandler(BaseException.class)
	public commonRes<Object> handleBaseException(BaseException e){
		
		return new commonRes<>(false,e.getMessage());	
	}
		
	/**
	 * 抓取全部例外
	 * @param e
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	public commonRes<Object> handleException(Exception e){
		
		return new commonRes<>(false,e.getMessage());	
	}
	

}
