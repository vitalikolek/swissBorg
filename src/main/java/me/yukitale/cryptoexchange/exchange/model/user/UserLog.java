package me.yukitale.cryptoexchange.exchange.model.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.utils.GeoUtil;
import me.yukitale.cryptoexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "user_logs")
@NoArgsConstructor
@Getter
@Setter
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 128)
    private String action;

    @NotBlank
    @Size(max = 64)
    private String ip;

    @Size(max = 128)
    private String platform;

    @Column()
    private GeoUtil.GeoData geolocation;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public UserLog(String action, String ip, User user, String platform, long time) {
        this.action = action;
        this.ip = ip;
        this.date = new Timestamp(time);
        this.platform = platform;
        this.geolocation = GeoUtil.getGeo(this.ip);
        this.user = user;
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
}
