package com.example.client.Exception;

public class BaseException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// 錯誤訊息
	private final String message;

	public BaseException(String message) {
		super(message);
		this.message = message;

	}

	public BaseException(String message, Throwable cause) {
		super(message, cause);
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}

}
