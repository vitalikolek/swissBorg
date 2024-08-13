package me.yukitale.cryptoexchange.panel.worker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.types.HomePageDesign;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import me.yukitale.cryptoexchange.utils.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.Date;

@Entity
@Table(name = "worker_domains")
@NoArgsConstructor
@Getter
@Setter
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = true)
    private Worker worker;

    private long usersCount;

    private long depositsCount;

    private double deposits;

    @Size(min = 4, max = 128)
    @Column(unique = true, nullable = false)
    private String name;

    @Size(min = 1, max = 64)
    private String exchangeName;

    @Size(max = 128)
    private String title;

    @Size(max = 128)
    private String icon;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int homeDesign;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailRequiredEnabled;

    @Size(max = 64)
    private String server;

    private int port;

    @Size(max = 64)
    private String email;

    @Size(max = 64)
    private String password;

    @Size(max = 128)
    private String listingRequest;

    @Size(max = 128)
    private String partnership;

    @Size(max = 128)
    private String twitter;

    @Size(max = 128)
    private String telegram;

    @Size(max = 128)
    private String instagram;

    @Size(max = 128)
    private String facebook;

    @Size(max = 128)
    private String reddit;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean listingRequestEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean partnershipEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean twitterEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean telegramEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean instagramEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean facebookEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean redditEnabled;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date added;

    public HomePageDesign getHomePageDesign() {
        return getNewHomePageDesign();
    }

    public HomePageDesign getNewHomePageDesign() {
        return HomePageDesign.values()[this.homeDesign];
    }

    @Transient
    public String getFormattedAdded() {
        long lastActivity = added.getTime();
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

    @Transient
    public MyDecimal formattedDeposits() {
        return new MyDecimal(this.deposits, true);
    }

    @Transient
    public boolean isEmailValid() {
        return StringUtils.isNotBlank(this.server) && port > 0 && port < 65535 && StringUtils.isNotBlank(this.email) && StringUtils.isNotBlank(this.password);
    }
}
