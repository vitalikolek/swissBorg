package me.yukitale.cryptoexchange.common.types;

public enum CoinType {
    BTC(false),
    BCH(false),
    ETH(false),
    USDTERC20(false),
    ETC(false),
    XRP(true),
    LTC(false),
    ADA(false),
    DSH(false),
    ZCASH(false),
    DOGE(false),
    SOL(false),
    XMR(false),
    XTZ(false),
    XLM(true),
    EOS(true),
    TRX(false),
    USDTTRC20(false),
    BNB2(true),
    BNBBEP20(false),
    USDTBEP20(false),
    BUSDBEP20(false),
    USDCTRC20(false),
    BTG(false),
    USDPERC20(false),
    SHIBBEP20(false),
    CROERC(false);

    public final boolean requiresMemo;

    CoinType(boolean requiresMemo) {
        this.requiresMemo = requiresMemo;
    }

    public boolean requiresMemo() {
        return requiresMemo;
    }
}
