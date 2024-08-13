package me.yukitale.cryptoexchange.panel.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class LegalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "TEXT", length = 65535)
    private String aml;

    @Column(columnDefinition = "TEXT", length = 65535)
    private String terms;

    @Column(columnDefinition = "TEXT", length = 65535)
    private String privacyNotice;

    @Column(columnDefinition = "TEXT", length = 65535)
    private String regulatory;

    @Column(columnDefinition = "TEXT", length = 65535)
    private String recovery;

    @Column(columnDefinition = "TEXT", length = 65535)
    private String benefits;
}
