package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.user.User;
import org.antlr.v4.runtime.misc.Triple;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsernameIgnoreCase(String username);

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  //worker start
  @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId GROUP BY u.regCountryCode ORDER by registrations DESC")
  List<Object[]> findRegistrationsByCountries(@Param("workerId") long workerId);

  default Map<String, Long> findRegistrationsByCountriesAsMap(@Param("workerId") long workerId) {
    Map<String, Long> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByCountries(workerId)) {
      registrations.put((String) objects[0], (Long) objects[1]);
    }

    return registrations;
  }

  @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId AND u.registered >= :startDate GROUP BY u.regCountryCode ORDER by registrations DESC")
  List<Object[]> findRegistrationsByCountries(@Param("workerId") long workerId, @Param("startDate") Date startDate);

  default Map<String, Long> findRegistrationsByCountriesAsMap(@Param("workerId") long workerId, @Param("startDate") Date startDate) {
    Map<String, Long> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByCountries(workerId, startDate)) {
      registrations.put((String) objects[0], (Long) objects[1]);
    }

    return registrations;
  }

  //refers start
  @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.deposits) FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId GROUP BY u.referrer ORDER by registrations DESC")
  List<Object[]> findRegistrationsByRefers(@Param("workerId") long workerId);

  default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap(@Param("workerId") long workerId) {
    Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByRefers(workerId)) {
      String referrer = (String) objects[0];
      if (referrer.isEmpty()) {
        referrer = "N/A";
      }

      long users = (long) objects[1];
      long depositsCount = (long) objects[2];
      double depositsPrice = (double) objects[3];

      registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
    }

    return registrations;
  }

  @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.deposits) FROM User u WHERE u.worker IS NOT NULL AND u.worker.id = :workerId AND u.registered >= :startDate GROUP BY u.referrer ORDER by registrations DESC")
  List<Object[]> findRegistrationsByRefers(@Param("workerId") long workerId, @Param("startDate") Date startDate);

  default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap(@Param("workerId") long workerId, @Param("startDate") Date startDate) {
    Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByRefers(workerId, startDate)) {
      String referrer = (String) objects[0];
      if (referrer.isEmpty()) {
        referrer = "N/A";
      }

      long users = (long) objects[1];
      long depositsCount = (long) objects[2];
      double depositsPrice = (double) objects[3];

      registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
    }

    return registrations;
  }
  //refers end
  //worker end

  //refers start
  @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.deposits) FROM User u GROUP BY u.referrer ORDER by registrations DESC")
  List<Object[]> findRegistrationsByRefers();

  default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap() {
    Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByRefers()) {
      String referrer = (String) objects[0];
      if (referrer.isEmpty()) {
        referrer = "N/A";
      }

      long users = (long) objects[1];
      long depositsCount = (long) objects[2];
      double depositsPrice = (double) objects[3];

      registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
    }

    return registrations;
  }

  @Query("SELECT u.referrer, count(u.id) AS registrations, sum(u.depositsCount), sum(u.deposits) FROM User u WHERE u.registered >= :startDate GROUP BY u.referrer ORDER by registrations DESC")
  List<Object[]> findRegistrationsByRefers(@Param("startDate") Date startDate);

  default Map<String, Triple<Long, Long, Double>> findRegistrationsByRefersAsMap(@Param("startDate") Date startDate) {
    Map<String, Triple<Long, Long, Double>> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByRefers(startDate)) {
      String referrer = (String) objects[0];
      if (referrer.isEmpty()) {
        referrer = "N/A";
      }

      long users = (long) objects[1];
      long depositsCount = (long) objects[2];
      double depositsPrice = (double) objects[3];

      registrations.put(referrer, new Triple<>(users, depositsCount, depositsPrice));
    }

    return registrations;
  }
  //refers end

  @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u GROUP BY u.regCountryCode ORDER by registrations DESC")
  List<Object[]> findRegistrationsByCountries();

  default Map<String, Long> findRegistrationsByCountriesAsMap() {
    Map<String, Long> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByCountries()) {
      registrations.put((String) objects[0], (Long) objects[1]);
    }

    return registrations;
  }

  @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.registered >= :startDate GROUP BY u.regCountryCode ORDER by registrations DESC")
  List<Object[]> findRegistrationsByCountries(@Param("startDate") Date startDate);

  default Map<String, Long> findRegistrationsByCountriesAsMap(@Param("startDate") Date startDate) {
    Map<String, Long> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByCountries(startDate)) {
      registrations.put((String) objects[0], (Long) objects[1]);
    }

    return registrations;
  }

  @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.promocodeName = :promocodeName AND u.registered >= :startDate GROUP BY u.regCountryCode ORDER by registrations DESC")
  List<Object[]> findRegistrationsByPromocodeName(@Param("promocodeName") String promocodeName, @Param("startDate") Date startDate);

  default Map<String, Long> findRegistrationsByPromocodeNameAsMap(String promocodeName, Date startDate) {
    Map<String, Long> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByPromocodeName(promocodeName, startDate)) {
      registrations.put((String) objects[0], (Long) objects[1]);
    }

    return registrations;
  }

  @Query("SELECT u.regCountryCode, count(u.id) AS registrations FROM User u WHERE u.promocodeName = :promocodeName GROUP BY u.regCountryCode ORDER by registrations DESC")
  List<Object[]> findRegistrationsByPromocodeName(@Param("promocodeName") String promocodeName);

  default Map<String, Long> findRegistrationsByPromocodeNameAsMap(String promocodeName) {
    Map<String, Long> registrations = new LinkedHashMap<>();
    for (Object[] objects : findRegistrationsByPromocodeName(promocodeName)) {
      registrations.put((String) objects[0], (Long) objects[1]);
    }

    return registrations;
  }

  @Query(value = "SELECT user_id FROM user_roles WHERE role_id = 3;", nativeQuery = true)
  List<Long> findAdminIds();

  @Query(value = "SELECT user_id FROM user_roles WHERE role_id = 2;", nativeQuery = true)
  List<Long> findWorkerIds();

  @Query(value = "SELECT user_id FROM user_roles WHERE role_id = 4;", nativeQuery = true)
  List<Long> findSupporterIds();

  List<User> findAllByLastIpOrRegIpOrderByIdDesc(String lastIp, String regIp);

  List<User> findAllByRoleTypeOrderByLastActivityDesc(int roleType, Pageable pageable);

  List<User> findAllByOrderByLastActivityDesc(Pageable pageable);

  List<User> findAllByWorkerIdOrderByLastActivityDesc(long workerId, Pageable pageable);

  List<User> findAllByLastOnlineGreaterThan(long lastActivity);

  List<User> findAllByRoleTypeAndLastOnlineGreaterThanOrderByLastActivityDesc(int roleType, long lastActivity, Pageable pageable);

  List<User> findAllByLastOnlineGreaterThanOrderByLastActivityDesc(long lastActivity, Pageable pageable);

  List<User> findAllByWorkerIdAndLastOnlineGreaterThanOrderByLastActivityDesc(long workerId, long lastActivity, Pageable pageable);

  Optional<User> findByRoleTypeAndUsername(int roleType, String username);

  Optional<User> findByRoleTypeAndEmail(int roleType, String email);

  Optional<User> findByEmailAndWorkerId(String email, long workerId);

  Optional<User> findByUsernameAndWorkerId(String username, long workerId);

  Optional<User> findByIdAndWorkerId(long id, long workerId);

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByEmail(String email);

  Long countByRoleType(int roleType);

  Long countByLastOnlineGreaterThan(long lastActivity);

  Long countByRoleTypeAndLastOnlineGreaterThan(int roleType, long lastActivity);

  Long countByWorkerIdAndLastOnlineGreaterThan(long workerId, long lastActivity);

  Long countByWorkerId(long workerId);

  boolean existsByIdAndWorkerId(long id, long workerId);

  @Transactional
  @Modifying
  @Query("UPDATE User u SET u.worker = null WHERE u.worker IS NOT NULL AND u.worker.id = :workerId")
  void removeWorkerFromUsers(@Param("workerId") long workerId);

  //start admin stats
  @Query("SELECT DATE(u.registered), COUNT(u) " +
          "FROM User u " +
          "GROUP BY DATE(u.registered)")
  List<Object[]> getUsersCountPerDay();

  default Map<Date, Long> getUsersCountPerDayAsMap() {
    Map<Date, Long> map = new LinkedHashMap<>();
    for (Object[] objects : getUsersCountPerDay()) {
      map.put((Date) objects[0], (Long) objects[1]);
    }

    return map;
  }
  //end admin stats

  //start worker stats
  @Query("SELECT DATE(u.registered), COUNT(u) " +
          "FROM User u " +
          "WHERE u.worker IS NOT NULL AND u.worker.id = :workerId " +
          "GROUP BY DATE(u.registered)")
  List<Object[]> getUsersCountPerDayByWorkerId(@Param("workerId") long workerId);

  default Map<Date, Long> getUsersCountPerDayByWorkerIdAsMap(long workerId) {
    Map<Date, Long> map = new LinkedHashMap<>();
    for (Object[] objects : getUsersCountPerDayByWorkerId(workerId)) {
      map.put((Date) objects[0], (Long) objects[1]);
    }

    return map;
  }

  @Query("SELECT u.worker.id, COUNT(u) " +
          "FROM User u " +
          "WHERE u.worker IS NOT NULL AND u.registered >= :startDate GROUP BY u.worker")
  List<Object[]> getWorkerUsersCountByDateGreaterThan(@Param("startDate") Date startDate);

  default Map<Long, Long> getWorkerUsersCountByDateGreaterThanAsMap(Date startDate) {
    Map<Long, Long> map = new LinkedHashMap<>();
    for (Object[] objects : getWorkerUsersCountByDateGreaterThan(startDate)) {
      map.put((Long) objects[0], (Long) objects[1]);
    }

    return map;
  }

  @Query("SELECT u.worker.id, COUNT(u) " +
          "FROM User u " +
          "WHERE u.worker IS NOT NULL " +
          "GROUP BY u.worker")
  List<Object[]> getWorkerUsersCount();

  default Map<Long, Long> getWorkerUsersCountAsMap() {
    Map<Long, Long> map = new LinkedHashMap<>();
    for (Object[] objects : getWorkerUsersCount()) {
      map.put((Long) objects[0], (Long) objects[1]);
    }

    return map;
  }

  //end worker stats
}
