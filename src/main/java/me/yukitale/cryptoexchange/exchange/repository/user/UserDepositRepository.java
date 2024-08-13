package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.exchange.model.user.UserDeposit;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public interface UserDepositRepository extends JpaRepository<UserDeposit, Long> {

    @Query("SELECT sum(d.amount) FROM UserDeposit d WHERE d.coinType = :coinType AND d.user.id = :userId AND d.completed = :completed")
    double findDepositAmountSumByCoinType(@Param("coinType") CoinType coinType, @Param("userId") long userId, @Param("completed") boolean completed);

    @Query("SELECT sum(d.price) FROM UserDeposit d WHERE d.user.id = :userId AND d.completed = :completed")
    double findDepositPriceSum(@Param("userId") long userId, @Param("completed") boolean completed);

    //worker start
    @Query("SELECT d.coinType, count(d.id) AS amount, sum(d.price) FROM UserDeposit d WHERE d.worker IS NOT NULL AND d.worker.id = :workerId AND d.date >= :startDate GROUP BY d.coinType ORDER BY amount DESC")
    List<Object[]> findDepositsByCoinType(@Param("workerId") long workerId, @Param("startDate") Date startDate);

    default Map<CoinType, Pair<Long, Double>> findDepositsByCoinTypeAsMap(@Param("workerId") long workerId, @Param("startDate") Date startDate) {
        Map<CoinType, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCoinType(workerId, startDate)) {
            deposits.put((CoinType) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }

    @Query("SELECT d.coinType, count(d.id) AS amount, sum(d.price) FROM UserDeposit d WHERE d.worker IS NOT NULL AND d.worker.id = :workerId GROUP BY d.coinType ORDER BY amount DESC")
    List<Object[]> findDepositsByCoinType(@Param("workerId") long workerId);

    default Map<CoinType, Pair<Long, Double>> findDepositsByCoinTypeAsMap(@Param("workerId") long workerId) {
        Map<CoinType, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCoinType(workerId)) {
            deposits.put((CoinType) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, count(d.id) AS amount, sum(d.price) FROM UserDeposit d WHERE d.worker IS NOT NULL AND d.worker.id = :workerId AND d.date >= :startDate GROUP BY d.countryCode ORDER BY amount DESC")
    List<Object[]> findDepositsByCountries(@Param("workerId") long workerId, @Param("startDate") Date startDate);

    default Map<String, Pair<Long, Double>> findDepositsByCountriesAsMap(@Param("workerId") long workerId, @Param("startDate") Date startDate) {
        Map<String, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCountries(workerId, startDate)) {
            deposits.put((String) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, count(d.id) AS amount, sum(d.price) FROM UserDeposit d WHERE d.worker IS NOT NULL AND d.worker.id = :workerId GROUP BY d.countryCode ORDER BY amount DESC")
    List<Object[]> findDepositsByCountries(@Param("workerId") long workerId);

    default Map<String, Pair<Long, Double>> findDepositsByCountriesAsMap(@Param("workerId") long workerId) {
        Map<String, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCountries(workerId)) {
            deposits.put((String) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }
    //worker end

    @Query("SELECT d.coinType, count(d.id) AS amount, sum(d.price) FROM UserDeposit d WHERE d.date >= :startDate GROUP BY d.coinType ORDER BY amount DESC")
    List<Object[]> findDepositsByCoinType(@Param("startDate") Date startDate);

    default Map<CoinType, Pair<Long, Double>> findDepositsByCoinTypeAsMap(@Param("startDate") Date startDate) {
        Map<CoinType, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCoinType(startDate)) {
            deposits.put((CoinType) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }

    @Query("SELECT d.coinType, count(d.id) AS amount, sum(d.price) FROM UserDeposit d GROUP BY d.coinType ORDER BY amount DESC")
    List<Object[]> findDepositsByCoinType();

    default Map<CoinType, Pair<Long, Double>> findDepositsByCoinTypeAsMap() {
        Map<CoinType, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCoinType()) {
            deposits.put((CoinType) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, count(d.id) AS amount, sum(d.price) FROM UserDeposit d WHERE d.date >= :startDate GROUP BY d.countryCode ORDER BY amount DESC")
    List<Object[]> findDepositsByCountries(@Param("startDate") Date startDate);

    default Map<String, Pair<Long, Double>> findDepositsByCountriesAsMap(@Param("startDate") Date startDate) {
        Map<String, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCountries(startDate)) {
            deposits.put((String) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, count(d.id) AS amount, sum(d.price) FROM UserDeposit d GROUP BY d.countryCode ORDER BY amount DESC")
    List<Object[]> findDepositsByCountries();

    default Map<String, Pair<Long, Double>> findDepositsByCountriesAsMap() {
        Map<String, Pair<Long, Double>> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsByCountries()) {
            deposits.put((String) objects[0], Pair.of((long) objects[1], (double) objects[2]));
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, count(d.id) AS amount FROM UserDeposit d WHERE d.user.promocodeName = :promocodeName AND d.date >= :startDate GROUP BY d.countryCode ORDER BY amount")
    List<Object[]> findDepositsCountByPromocodeName(@Param("promocodeName") String promocodeName, @Param("startDate") Date startDate);

    default Map<String, Long> findDepositsCountByPromocodeNameAsMap(String promocodeName, @Param("startDate") Date startDate) {
        Map<String, Long> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsCountByPromocodeName(promocodeName, startDate)) {
            deposits.put((String) objects[0], (Long) objects[1]);
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, sum(d.price) AS price FROM UserDeposit d WHERE d.user.promocodeName = :promocodeName AND d.date >= :startDate GROUP BY d.countryCode ORDER BY price")
    List<Object[]> findDepositsPriceByPromocodeName(@Param("promocodeName") String promocodeName, @Param("startDate") Date startDate);

    default Map<String, Double> findDepositsPriceByPromocodeNameAsMap(String promocodeName, @Param("startDate") Date startDate) {
        Map<String, Double> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsPriceByPromocodeName(promocodeName, startDate)) {
            deposits.put((String) objects[0], (Double) objects[1]);
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, count(d.id) AS amount FROM UserDeposit d WHERE d.user.promocodeName = :promocodeName GROUP BY d.countryCode ORDER BY amount")
    List<Object[]> findDepositsCountByPromocodeName(@Param("promocodeName") String promocodeName);

    default Map<String, Long> findDepositsCountByPromocodeNameAsMap(String promocodeName) {
        Map<String, Long> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsCountByPromocodeName(promocodeName)) {
            deposits.put((String) objects[0], (Long) objects[1]);
        }

        return deposits;
    }

    @Query("SELECT d.countryCode, sum(d.price) AS price FROM UserDeposit d WHERE d.user.promocodeName = :promocodeName GROUP BY d.countryCode ORDER BY price")
    List<Object[]> findDepositsPriceByPromocodeName(@Param("promocodeName") String promocodeName);

    default Map<String, Double> findDepositsPriceByPromocodeNameAsMap(String promocodeName) {
        Map<String, Double> deposits = new LinkedHashMap<>();
        for (Object[] objects : findDepositsPriceByPromocodeName(promocodeName)) {
            deposits.put((String) objects[0], (Double) objects[1]);
        }

        return deposits;
    }

    @Transactional
    @Modifying
    @Query("UPDATE UserDeposit d SET d.worker = NULL WHERE d.worker.id = :workerId")
    void removeWorkerForAll(@Param("workerId") long workerId);

    long countByCompleted(boolean completed);

    long countByCompletedAndWorkerId(boolean completed, long workerId);

    List<UserDeposit> findByCompletedAndBotReceivedOrderById(boolean completed, boolean botReceived);

    List<UserDeposit> findByCompletedOrderByIdDesc(boolean completed, Pageable pageable);

    Optional<UserDeposit> findByTransactionId(long transactionId);

    Optional<UserDeposit> findByTxId(long txId);

    Optional<UserDeposit> findByIdAndWorkerId(long id, long workerId);

    List<UserDeposit> findByWorkerIdOrderByTxIdDesc(long workerId);

    boolean existsByTransactionId(long transactionId);

    List<UserDeposit> findByUserIdAndCoinType(long userId, CoinType type);

    //start admin stats
    @Query("SELECT SUM(d.price) FROM UserDeposit d WHERE d.completed = TRUE AND d.date >= :startDate")
    Double sumPriceDateGreaterThan(@Param("startDate") Date startDate);

    @Query("SELECT SUM(d.price) FROM UserDeposit d WHERE d.completed = TRUE")
    Double sumPrice();

    @Query("SELECT DATE(d.date), SUM(d.price) " +
            "FROM UserDeposit d " +
            "WHERE d.completed = TRUE " +
            "GROUP BY DATE(d.date)")
    List<Object[]> getSumPricePerDay();

    default Map<Date, Double> getSumPricePerDayAsMap() {
        Map<Date, Double> map = new LinkedHashMap<>();
        for (Object[] objects : getSumPricePerDay()) {
            map.put((Date) objects[0], (Double) objects[1]);
        }

        return map;
    }
    //end admin stats

    //start worker stats
    @Query("SELECT SUM(d.price) FROM UserDeposit d WHERE d.completed = TRUE AND d.worker IS NOT NULL AND d.worker.id = :workerId AND d.date >= :startDate")
    Double sumPriceByWorkerIdDateGreaterThan(@Param("workerId") long workerId, @Param("startDate") Date startDate);

    @Query("SELECT SUM(d.price) FROM UserDeposit d WHERE d.completed = TRUE AND d.worker IS NOT NULL AND d.worker.id = :workerId")
    Double sumPriceByWorkerId(@Param("workerId") long workerId);

    @Query("SELECT DATE(d.date), SUM(d.price) " +
            "FROM UserDeposit d " +
            "WHERE d.completed = TRUE AND d.worker IS NOT NULL AND d.worker.id = :workerId " +
            "GROUP BY DATE(d.date)")
    List<Object[]> getSumPricePerDayByWorkerId(@Param("workerId") long workerId);

    default Map<Date, Double> getSumPricePerDayByWorkerIdAsMap(long workerId) {
        Map<Date, Double> map = new LinkedHashMap<>();
        for (Object[] objects : getSumPricePerDayByWorkerId(workerId)) {
            map.put((Date) objects[0], (Double) objects[1]);
        }

        return map;
    }

    @Query("SELECT d.worker, SUM(d.price) as sumPrice, COUNT(d) " +
            "FROM UserDeposit d " +
            "WHERE d.worker IS NOT NULL AND d.completed = TRUE AND d.date >= :startDate " +
            "GROUP by d.worker " +
            "ORDER BY sumPrice DESC")
    List<Object[]> sumWorkerPriceByDateGreaterThan(@Param("startDate") Date startDate);

    default Map<Worker, Pair<Double, Long>> sumWorkerPriceByDateGreaterThanAsMap(Date startDate) {
        Map<Worker, Pair<Double, Long>> map = new LinkedHashMap<>();
        for (Object[] objects : sumWorkerPriceByDateGreaterThan(startDate)) {
            map.put((Worker) objects[0], Pair.of((Double) objects[1], (Long) objects[2]));
        }

        return map;
    }

    @Query("SELECT d.worker, SUM(d.price) as sumPrice, COUNT(d) " +
            "FROM UserDeposit d " +
            "WHERE d.worker IS NOT NULL AND d.completed = TRUE " +
            "GROUP by d.worker " +
            "ORDER BY sumPrice DESC")
    List<Object[]> sumWorkerPrice();

    default Map<Worker, Pair<Double, Long>> sumWorkerPriceAsMap() {
        Map<Worker, Pair<Double, Long>> map = new LinkedHashMap<>();
        for (Object[] objects : sumWorkerPrice()) {
            map.put((Worker) objects[0], Pair.of((Double) objects[1], (Long) objects[2]));
        }

        return map;
    }
    //end worker stats

    @Query("SELECT SUM(d.price) FROM UserDeposit d WHERE d.user.id = :userId")
    Double sumPriceByUserId(@Param("userId") long userId);
}
