package me.yukitale.cryptoexchange.exchange.model.ban;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import me.yukitale.cryptoexchange.exchange.model.user.User;

import java.util.Date;

@Entity
@Table(name = "email_bans")
@Getter
@Setter
@NoArgsConstructor
public class EmailBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Size(max = 64)
    @Column(unique = true)
    private String email;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date date;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
