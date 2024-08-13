package me.yukitale.cryptoexchange.exchange.model;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Component;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;

import java.lang.reflect.Method;
import java.util.Optional;

@Component
public class CoinKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (method.getName().equals("deleteById")) {
            Long id = (Long) params[0];
            CoinRepository repository = (CoinRepository) target;
            Optional<Coin> optionalCoin = repository.findById(id);
            if (optionalCoin.isPresent()) {
                Coin coin = optionalCoin.get();
                return coin.getSymbol();
            }
        }
        return SimpleKey.EMPTY;
    }
}

