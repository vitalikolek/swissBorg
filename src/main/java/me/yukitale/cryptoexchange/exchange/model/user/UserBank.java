package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_banks")
@Getter
@Setter
@NoArgsConstructor
public class UserBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 36)
    @NotBlank
    private String iban;

    @Size(max = 32)
    @NotBlank
    private String firstName;

    @Size(max = 32)
    @NotBlank
    private String lastName;

    @Size(max = 32)
    @NotBlank
    private String vatNumber;

    @Size(max = 32)
    @NotBlank
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public UserBank(String iban, String firstName, String lastName, String vatNumber, String currency, User user) {
        this.iban = iban;
        this.firstName = firstName;
        this.lastName = lastName;
        this.vatNumber = vatNumber;
        this.currency = currency;
        this.user = user;
    }
}
