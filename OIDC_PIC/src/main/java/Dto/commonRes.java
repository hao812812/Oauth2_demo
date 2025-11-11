package Dto;

public class commonRes<T> {
	private boolean success;
	private String message;
	private T data;
	
	
    public commonRes(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public commonRes(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    
    // 靜態工廠方法 - 成功回應
    public static <T> commonRes<T> success(String message) {
        return new commonRes<>(true, message, null);
    }
    
    public static <T> commonRes<T> success(String message, T data) {
        return new commonRes<>(true, message, data);
    }
    
    // 靜態工廠方法 - 失敗回應
    public static <T> commonRes<T> error(String message) {
        return new commonRes<>(false, message, null);
    }
    
    public static <T> commonRes<T> error(String message, T data) {
        return new commonRes<>(false, message, data);
    }

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
    
}
