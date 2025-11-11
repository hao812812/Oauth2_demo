package com.example.pic.Controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.code.kaptcha.Producer;

import Dto.commonRes;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/captcha")
public class captchaController {
	
	@Autowired
	private Producer captchaProducer;
	
	@GetMapping("/generate")
	public void getCaptcha(HttpServletResponse response, HttpSession session) throws IOException {
		
		response.setContentType("image/jpeg");
		
		String text = captchaProducer.createText();//產生隨機文字
		BufferedImage image = captchaProducer.createImage(text);//文字畫成圖
		
		session.setAttribute("verifInput", text.toLowerCase());//文字存入session
		
		ServletOutputStream out = response.getOutputStream();
		ImageIO.write(image, "jpg", out);
		out.flush();
		out.close();
		
	}
	
	@PostMapping("/verify")
	public commonRes<Void> verify(@RequestBody Map<String, String> request,HttpSession session){
		String inputText = request.get("captcha");
		
		String sessionText =(String)session.getAttribute("verifInput");
		
                boolean isValid = sessionText != null && sessionText.equalsIgnoreCase(inputText);

                if(!isValid) {
                	return commonRes.error("驗證失敗");
                }
                return commonRes.success("驗證成功");
        
	}

}
