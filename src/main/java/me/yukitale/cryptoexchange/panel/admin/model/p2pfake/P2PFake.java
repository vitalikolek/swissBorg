package me.yukitale.cryptoexchange.panel.admin.model.p2pfake;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "p2p_fakes")
@Getter
@Setter
@NoArgsConstructor
public class P2PFake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String avatar;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String orders;

    @Column(nullable = false)
    private String limits;

    @Column(nullable = false)
    private String paymentMethod;
}
