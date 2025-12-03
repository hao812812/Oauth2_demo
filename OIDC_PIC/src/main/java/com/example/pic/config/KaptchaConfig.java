package com.example.pic.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;

@Configuration
public class KaptchaConfig {

	@Bean
	public DefaultKaptcha captchaProducer() {
		Properties properties = new Properties();
		properties.setProperty("kaptcha.textproducer.char.length", "5");
		properties.setProperty("kaptcha.textproducer.char.string", "abcde2345678gfynmnpwx");
		properties.setProperty("kaptcha.image.width", "130");
		properties.setProperty("kaptcha.image.height", "50");
		properties.setProperty("kaptcha.textproducer.font.size", "40");

		Config config = new Config(properties);
		DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
		defaultKaptcha.setConfig(config);
		return defaultKaptcha;
	}
}
