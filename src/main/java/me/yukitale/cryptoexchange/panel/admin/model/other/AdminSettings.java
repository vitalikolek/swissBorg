package me.yukitale.cryptoexchange.panel.admin.model.other;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.yukitale.cryptoexchange.panel.common.types.HomePageDesign;
import me.yukitale.cryptoexchange.panel.common.types.KycAcceptTimer;

@Entity
@Table(name = "admin_settings")
@Getter
@Setter
@NoArgsConstructor
public class AdminSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Size(max = 64)
    private String siteName;

    @NotBlank
    @Size(max = 250)
    private String siteTitle;

    @NotBlank
    @Size(max = 128)
    private String siteIcon;

    @Size(max = 512)
    private String supportWelcomeMessage;

    private String apiKey;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportWelcomeEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportPresetsEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean promoFormEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean promoHideEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean workerTopStats;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int homeDesign;

    @Column(columnDefinition = "TINYINT DEFAULT 0")
    private KycAcceptTimer kycAcceptTimer;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean smartDepositEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean showAddressAlways;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean showQrAlways;

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

    public HomePageDesign getHomePageDesign() {
        return getNewHomePageDesign();
    }

    public HomePageDesign getNewHomePageDesign() {
        return HomePageDesign.values()[this.homeDesign];
    }
}
