package me.yukitale.cryptoexchange.exchange.payload.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class Login2FARequest {

    @NotBlank
    private String token;

    @NotBlank
    private String code;
}
