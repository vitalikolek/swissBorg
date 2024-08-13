package me.yukitale.cryptoexchange.exchange.model.user.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;

@Entity
@Table(name = "user_error_messages")
@Getter
@Setter
@NoArgsConstructor
public class UserErrorMessage extends ErrorMessage {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
