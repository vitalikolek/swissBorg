package me.yukitale.cryptoexchange.panel.supporter.model.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.model.SupportPreset;
import me.yukitale.cryptoexchange.panel.supporter.model.Supporter;

@Entity
@Table(name = "supporters_support_presets")
@Getter
@Setter
@NoArgsConstructor
public class SupporterSupportPreset extends SupportPreset {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_id", nullable = false)
    private Supporter supporter;
}
