package me.yukitale.cryptoexchange.exchange.payload.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class LoginRequest {

	@NotBlank
	private String email;

	@NotBlank
	private String password;

	@NotBlank
	private String captcha;
}
