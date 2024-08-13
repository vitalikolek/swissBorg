package me.yukitale.cryptoexchange.panel.common.service;

import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminDepositCoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.panel.common.model.DepositCoin;

import java.util.List;

@Service
public class DepositCoinService {

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    public List<? extends DepositCoin> getDepositCoins(User user) {
        return user.getWorker() == null ? adminDepositCoinRepository.findAll() : user.getWorker().getDepositCoins();
    }
}
