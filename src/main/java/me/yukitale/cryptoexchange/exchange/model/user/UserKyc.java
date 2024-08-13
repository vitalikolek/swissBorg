package me.yukitale.cryptoexchange.exchange.model.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import me.yukitale.cryptoexchange.utils.StringUtil;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.Date;

@Entity
@Table(name = "user_kyc")
@Getter
@Setter
@NoArgsConstructor
public class UserKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Size(min = 1, max = 64)
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 64)
    private String lastName;

    @NotBlank
    @Size(min = 1, max = 64)
    private String country;

    @NotBlank
    @Size(min = 6, max = 128)
    private String address;

    @NotBlank
    @Size(min = 5, max = 24)
    private String phoneNumber;

    private String birthDate;

    private IdType idType;

    @NotBlank
    @Size(min = 6, max = 24)
    private String idNumber;

    @NotBlank
    @Size(max = 128)
    private String documentImage;

    @NotBlank
    @Size(max = 128)
    private String selfieImage;

    private boolean accepted;

    private long autoAccept;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean kyc3Sended;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date date;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public UserKyc(String firstName, String lastName, String country, String address, String phoneNumber,
                   String birthDate, IdType idType, String idNumber, String documentImage, String selfieImage, User user) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.country = country;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.idType = idType;
        this.idNumber = idNumber;
        this.date = new Date();
        this.documentImage = documentImage;
        this.selfieImage = selfieImage;
        this.user = user;
    }

    public boolean isAccepted() {
        return this.accepted || (this.autoAccept > 0 && this.autoAccept < System.currentTimeMillis());
    }

    public String getFormattedDate() {
        long lastActivity = date.getTime();
        long diff = (System.currentTimeMillis() - lastActivity) / 1000L;
        if (diff < 60) {
            return diff + " сек. назад";
        } else if (diff > 86400) {
            return StringUtil.formatDate(new Date(lastActivity));
        } else if (diff > 3600) {
            return diff / 3600 + "ч. назад";
        } else {
            return diff / 60 + " мин. назад";
        }
    }

    @AllArgsConstructor
    @Getter
    public enum IdType {

        PASSPORT("Passport"),
        DRIVER_LICENSE("Driver License"),
        ID_CARD("Government-Issued ID card");

        private final String title;

        public static IdType getByTitle(String title) {
            return Arrays.stream(values())
                    .filter(type -> type.getTitle().equals(title))
                    .findFirst()
                    .orElse(PASSPORT);
        }
    }
}
