package me.yukitale.cryptoexchange.exchange.model.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user_api_keys")
@Getter
@Setter
@NoArgsConstructor
public class UserApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Size(max = 16)
    private String secretKey;

    private boolean enabled;

    private boolean spotTrading;

    private boolean futuresTrading;

    private boolean withdraw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
