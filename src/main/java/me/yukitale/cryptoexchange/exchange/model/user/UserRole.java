package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  //@Column(length = 128)
  //@Size(max = 128)
  @Enumerated(EnumType.ORDINAL)
  private UserRoleType name;

  public UserRole(UserRoleType name) {
    this.name = name;
  }
}