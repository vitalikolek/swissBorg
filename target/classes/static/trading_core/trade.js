$.ajax({
    url: "../api/user/trading",
    type: "POST",
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify({
        action: "TIME_DIFFERENCE",
        time: Math.floor(Date.now() / 1000)
    }),
    success: function (response, textStatus, status) {
        fetchBinanceAndLoad(Number(status.responseText));
    }
});

var pair_price = 0;

function fetchBinanceAndLoad(timeDiff) {
    fetch('https://api.binance.com/')
        .then(response => {
            load("binance.com", timeDiff);
        })
        .catch(error => {
            load("binance.us", timeDiff);
        });
}

function preloaderFadeOutInit() {
    $('.preloader').fadeOut('slow');
    $('body').attr('class', '');
}

jQuery(window).on('load', function () {
    (function ($) {
        preloaderFadeOutInit();
    })(jQuery);
});

$(document).ready(function () {
    $("#search_pairs").keyup(function () {
        _this = this;

        $.each($(".coins__item"), function () {
            if ($(this).text().toLowerCase().indexOf($(_this).val().toLowerCase()) === -1) {
                $(this).hide();
            } else {
                $(this).show();
            }
        });
    });
});

function load(domain, timeDiff) {
    $("#orders_table").load("../api/user/trading?action=GET_OPEN_ORDERS");
    $("#orders_history").load("../api/user/trading?action=GET_HISTORY_ORDERS");

    let pairs_value = $("#pairs").val();
    let pairss_one = $("#one_pair").val();
    let pairss_two = $("#two_pair").val();
    let new_pairs = pairss_one + "_" + pairss_two;

    const stable_pump_percent = parseFloat($("#stable_pump_percent").val());
    let fast_pumps_json = document.getElementById('fast_pumps_json').textContent;
    fast_pumps_json = fast_pumps_json.length > 0 ? JSON.parse(fast_pumps_json) : "";
    let fast_pumps_active = document.getElementById('fast_pumps_active_json').textContent;
    fast_pumps_active = fast_pumps_active.length > 0 ? JSON.parse(fast_pumps_active) : 0;
    let fast_pumps_end_time = Number($("#fast_pumps_end_time").val());
    if (fast_pumps_end_time > 0) {
        fast_pumps_end_time = fast_pumps_end_time + timeDiff;
    }

//end my

    function addCommas(nStr) {
        nStr += '';
        x = nStr.split('.');
        x1 = x[0];
        x2 = x.length > 1 ? '.' + x[1] : '';
        var rgx = /(\d+)(\d{3})/;
        while (rgx.test(x1)) {
            x1 = x1.replace(rgx, '$1' + ',' + '$2');
        }
        return x1 + x2;
    }

    var tv_chart_height = $(".center-top-ghTgp").height();

    const chartPropeties = {
        // width: 850,
        height: tv_chart_height,
        timeScale: {
            timeVisible: true,
            secondVisible: true
        },
        localization: {
            priceFormatter: price => {
                if (price > 100) {
                    return parseFloat(price).toFixed(2);
                }
                if (price > 1 && price < 100) {
                    return parseFloat(price).toFixed(4);
                }
                if (price <= 1 && price > 0.001) {
                    return parseFloat(price).toFixed(5);
                }
                if (price < 0.001) {
                    return parseFloat(price).toFixed(8);
                }
            }
        }
    }

    const domElement = document.querySelector('#tvchart');
    const chart = LightweightCharts.createChart(domElement, chartPropeties);
    const candleSeries = chart.addCandlestickSeries();
    var volumeSeries = chart.addHistogramSeries({
        priceFormat: {
            type: 'volume',
        },
        priceScaleId: '',
        scaleMargins: {
            top: 0.8,
            bottom: 0.01,
        },
    });

    //1m 3m 5m 15m 30m 1h 2h 4h 6h 8h 12h 1d 3d 1w 1M

    var symbol = document.querySelector('#var_for_chart').getAttribute('symbol')
    var customSymbol = document.querySelector('#var_for_chart').getAttribute('customSymbol')

    if (customSymbol == '') {
        customSymbol = symbol
    } else {
        symbol = document.querySelector('#var_for_chart').getAttribute('customSymbol')
        customSymbol = document.querySelector('#var_for_chart').getAttribute('symbol')
    }

    var pairs_for_ajax = $("#get_pairs_for_js").val();

//start my
//todo: time
    function get_percent(data) {
        let price = data + (data * stable_pump_percent);
        if (fast_pumps_active && fast_pumps_active !== 0 && fast_pumps_active.length > 0) {
            for (let fastPumpsActiveElement of fast_pumps_active) {
                price = price + (price * fastPumpsActiveElement);
            }
        }
        return price;
    }

    function get_stable_percent(data) {
        return data + (data * stable_pump_percent);
    }

    function findKlinePumpPercent(openTime, closeTime) {
        if (fast_pumps_json !== 'null' && fast_pumps_json != null && typeof fast_pumps_json !== "undefined") {
            for (let pump of fast_pumps_json) {
                let pumpTime = pump["time"] + timeDiff;
                if (pumpTime >= openTime && pumpTime <= closeTime) {
                    return parseFloat(pump["percent"]);
                }
            }
        }
        return 0;
    }

    function fixNumber(number, decimalPlaces) {
        var stringNumber = number.toString();

        var decimalPosition = stringNumber.indexOf('.');

        if (decimalPosition !== -1) {
            stringNumber = stringNumber.slice(0, decimalPosition + 1 + decimalPlaces);
        }

        var truncatedNumber = parseFloat(stringNumber);

        if (isNaN(truncatedNumber) || !isFinite(truncatedNumber) || number.toString().toLowerCase().includes('e-')) {
            truncatedNumber = 0;
        }

        return truncatedNumber;
    }

    var cdata = [];

    function fetchData(callback) {
        fetch("https://api." + domain + "/api/v3/klines?symbol=" + pairs_value + "&interval=1m&limit=1000" + (fast_pumps_end_time > 0 ? "&endTime=" + fast_pumps_end_time : ""))
            .then(response => response.json())
            .then(json => {
                let oldKline = -77777;
                for (let i = 0; i < json.length; i++) {
                    let kline = json[i];
                    let openTime = Math.floor(new Number(kline[0]) / 1000);
                    let closeTime = Math.floor(new Number(kline[6]) / 1000);
                    let open = get_stable_percent(parseFloat(kline[1]));
                    let close = get_stable_percent(parseFloat(kline[4]));
                    let high = get_stable_percent(parseFloat(kline[2]));
                    let low = get_stable_percent(parseFloat(kline[3]));
                    let volume = get_stable_percent(parseFloat(kline[5]));
                    //open = i > 0 ? cdata[i - 1].close : open;
                    //low = open;
                    let currentKline = findKlinePumpPercent(openTime, closeTime);
                    if (currentKline !== 0) {
                        if (oldKline === -77777) {
                            close = close + (close * currentKline);
                        } else {
                            let nextKline = -77777;
                            if (i < json.length) {
                                nextKline = findKlinePumpPercent(Math.floor(new Number(json[i][0]) / 1000), Math.floor(new Number(json[i][6]) / 1000));
                            }
                            if (nextKline === 0) {
                                open = cdata[i - 1].close;
                                low = low + (low * currentKline);
                            } else {
                                open = i > 0 ? cdata[i - 1].close : open;
                                low = open;
                                if (i === 0) {
                                    close = close + (close * currentKline);
                                } else {
                                    close = cdata[i - 1].close;
                                    close = close + (close * currentKline);
                                }
                                high = close;
                            }
                        }
                        oldKline = currentKline;
                    }
                    //todo: проверить openTime или closeTime
                    cdata.push({
                        "time": closeTime,
                        "open": open,
                        "high": high,
                        "low": low,
                        "close": close,
                        "volume": volume
                    });
                }
                callback(cdata);
            }).catch(error => console.error(error));
    }

    fetchData(cdata => {
        candleSeries.setData(cdata);

        const volume_data = cdata.map(d => {
            if (parseFloat(d['open']) < parseFloat(d['close'])) {
                clr = '#0f3a36'; //green
            } else {
                clr = '#501f1e'; // red
            }
            time = d['time'];
            return {time, value: parseFloat(d['volume']), color: clr}

        })

        //log(volume_data)
        volumeSeries.setData(volume_data);

        //UPDATE PRICE FROM LEFT, STAT PRICE AND BEST PRICE
        if (cdata[cdata.length - 1]['close'] > 100) {
            priceUpdate = parseFloat(cdata[cdata.length - 1]['close']).toFixed(2);
        }
        if (cdata[cdata.length - 1]['close'] >= 1 && cdata[cdata.length - 1]['close'] < 100) {
            priceUpdate = parseFloat(cdata[cdata.length - 1]['close']).toFixed(4);
        }
        if (cdata[cdata.length - 1]['close'] < 1 && cdata[cdata.length - 1]['close'] > 0.001) {
            priceUpdate = parseFloat(cdata[cdata.length - 1]['close']).toFixed(5);
        }
        if (cdata[cdata.length - 1]['close'] < 0.001) {
            priceUpdate = parseFloat(cdata[cdata.length - 1]['close']).toFixed(8);
        }

        chart.applyOptions({
            watermark: {
                color: '#1f2226',
                visible: true,
                text: "     ",
                fontSize: 50,
                fontWeight: 'bold',
                horzAlign: 'center',
                vertAlign: 'center',
            },
            layout: {
                backgroundColor: '#17181e',
                textColor: '#515964',
                fontSize: 12,
                fontFamily: 'Roboto-Regular, sans-serif',
            },
            grid: {
                vertLines: {
                    color: '#1d2127',
                    style: 1,
                    visible: true,
                },
                horzLines: {
                    color: '#1d2127',
                    style: 1,
                    visible: true,
                },
            },
            localization: {
                locale: 'en-US',
            },
            crosshair: {
                vertLine: {
                    color: '#767f8b',
                    width: 0.5,
                    style: 1,
                    visible: true,
                    labelVisible: true,
                },
                horzLine: {
                    color: '#767f8b',
                    width: 0.5,
                    style: 0,
                    visible: true,
                    labelVisible: true,
                },
                mode: 3,
            },
        });

        var his_close = '';
        var his_edited = 'false';

        var soc_his_time = 0;
        var new_fix_time = 0;
        var last_currency_price = 0;

        wsLiveChart = new WebSocket('wss://stream.' + domain + ':9443/ws/' + symbol.toLowerCase() + '@kline_1m');
        wsLiveChart.onopen = function () {
        };

        let oldClosePrice = cdata[cdata.length - 1].close;

        wsLiveChart.onmessage = function (onmessage) {
            let resp_socket = JSON.parse(onmessage.data);

            let time = Math.floor(resp_socket.k.t / 1000);
            let closeTime = Math.floor(resp_socket.k.T / 1000);

            $.ajax({
                url: "../api/user/trading",
                type: "POST",
                contentType: 'application/json; charset=UTF-8',
                data: JSON.stringify({
                    action: "GET_PAIR_STATUS",
                    pairs: pairs_for_ajax,
                    open_time: time + timeDiff,
                    close_time: closeTime + timeDiff,
                    close_price: oldClosePrice
                }),
                success: function (response) {
                    try {
                        if (response === 'blocked') {
                            //nothing
                        } else if (response == 'false') {
                            //todo: closeTime?

                            high = get_stable_percent(parseFloat(resp_socket.k.h));
                            low = get_stable_percent(parseFloat(resp_socket.k.l));
                            //todo: ОЧЕНЬ ВАЖНО! Для пампов менять местами k.c -> k.o
                            //k.o / k.c
                            close = get_stable_percent(parseFloat(resp_socket.k.c));
                            open = get_stable_percent(parseFloat(resp_socket.k.o));

                            rez = {
                                time: closeTime,
                                open: open,
                                high: high,
                                low: low,
                                close: close
                            };

                            candleSeries.update(rez)

                            var fixed_price = parseFloat(close);
                            if (parseFloat(close) > 1) {
                                fixed_price = fixed_price.toFixed(2);
                            } else {
                                fixed_price = fixed_price.toFixed(6);
                            }

                            $("#c_i_p_ajax_sp2").html(fixed_price);
                            $("#aj_live_price_3").html(fixed_price);
                            var new_symbol = symbol.split('USDT').join('');
                            $('html head').find('title').text("$" + addCommas(fixed_price) + " - Buy and sell Bitcoin and Ethereum");
                            pair_price = parseFloat(close);
                            updateBuyAvailable(false);
                            updateSellAvailable(false);

                            $("#aj_live_new_price_block_2").html(fixed_price + " USD");
                            $("#aj_live_new_price_block_1").html(fixed_price);
                            $("#currency_in_list_" + new_symbol).html(fixed_price);

                            if (last_currency_price < pair_price || last_currency_price === 0) {
                                $("#price_block_minus_plus").removeClass("order__info-price-minus");
                                $("#price_block_minus_plus").removeClass("order__info-price-plus");

                                $("#price_block_minus_plus").addClass("order__info-price-plus");
                            } else {
                                $("#price_block_minus_plus").removeClass("order__info-price-minus");
                                $("#price_block_minus_plus").removeClass("order__info-price-plus");

                                $("#price_block_minus_plus").addClass("order__info-price-minus");
                            }

                            last_currency_price = pair_price;
                        } else {
                            open = oldClosePrice;
                            low = open;
                            close = parseFloat(response);
                            high = close;

                            rez = {
                                time: closeTime,
                                open: open,
                                high: high,
                                low: low,
                                close: close
                            };

                            candleSeries.update(rez);

                            var fixed_price = close;
                            if (close > 1) {
                                fixed_price = fixed_price.toFixed(2);
                            } else {
                                fixed_price = fixed_price.toFixed(6);
                            }

                            $("#c_i_p_ajax_sp2").html(fixed_price);
                            $("#aj_live_price_3").html(fixed_price);
                            var new_symbol = symbol.split('USDT').join('');
                            $('html head').find('title').text("$" + addCommas(fixed_price) + " - Buy and sell Bitcoin and Ethereum");

                            pair_price = close;
                            updateBuyAvailable(false);
                            updateSellAvailable(false);

                            last_currency_price = pair_price;
                        }
                        oldClosePrice = close;
                    } catch (error) {
                        console.log(error);
                    }
                }
            });
        }

        leftpairList = document.querySelectorAll('.link'); //document.querySelectorAll('.link')[27].childNodes[3].innerText = 11

        btc_pairs_label = document.querySelectorAll('#btc_pairs_label');
        eth_pairs_label = document.querySelectorAll('#eth_pairs_label');
        usdt_pairs_label = document.querySelectorAll('#usdt_pairs_label');

        btcPairs = document.querySelectorAll('.BTC');
        ethPairs = document.querySelectorAll('.ETH');
        usdtPairs = document.querySelectorAll('.USDT');

        btc_pairs_label.forEach(element => {
            element.onclick = function () {
                for (i = 0; i < ethPairs.length; i++) {
                    ethPairs[i].style.display = 'none';
                }
                for (i = 0; i < usdtPairs.length; i++) {
                    usdtPairs[i].style.display = 'none';
                }
                for (i = 0; i < btcPairs.length; i++) {
                    btcPairs[i].style.display = 'table-row';
                }
                document.querySelector('#underline_bar').style.left = "90px";
            }
        });

        eth_pairs_label.forEach(element => {
            element.onclick = function () {
                for (i = 0; i < btcPairs.length; i++) {
                    btcPairs[i].style.display = 'none';
                }
                for (i = 0; i < usdtPairs.length; i++) {
                    usdtPairs[i].style.display = 'none';
                }
                for (i = 0; i < ethPairs.length; i++) {
                    ethPairs[i].style.display = 'table-row';
                }
                document.querySelector('#underline_bar').style.left = "158px";
            }
        });

        usdt_pairs_label.forEach(element => {
            element.onclick = function () {
                for (i = 0; i < ethPairs.length; i++) {
                    ethPairs[i].style.display = 'none';
                }
                for (i = 0; i < btcPairs.length; i++) {
                    btcPairs[i].style.display = 'none';
                }
                for (i = 0; i < usdtPairs.length; i++) {
                    usdtPairs[i].style.display = 'table-row';
                }
                document.querySelector('#underline_bar').style.left = "12px";
            }
        });

        for (i = 0; i < btcPairs.length; i++) {
            btcPairs[i].style.display = 'none';
        }

        for (i = 0; i < ethPairs.length; i++) {
            ethPairs[i].style.display = 'none';
        }

        link = document.querySelectorAll('.link');
        link.forEach(element => {
            element.onclick = function () {
                document.location.href = 'trading?currency=' + element.id;
            }
        });

// Auto calculate prices from buy and sell inputs
        var pair_one = $("#one_pair").val().toLowerCase();
        var pair_two = $("#two_pair").val().toLowerCase();

        var sign_in = $("#sign_in").val();

        var user_balance = $("#" + pair_two + "_balance").val();
        var user_available = 0;

        /*$(document).ready(function () {
            setInterval(function () {
                user_balance = $("#" + pair_two + "_balance").val();

                user_available = fixNumber(user_balance);

                $("#buy_available").html(user_available);
                $("#buy_available2").html(user_available);
                $("#buy_available3").html(user_available);

            }, 1500);
        });*/

        function updateTradingBalance() {
            $.ajax({
                url: "../api/user/trading",
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    action: "GET_TRADING_BALANCE",
                    coin: terminal_crypto
                }),
                success: function (response) {
                    const json = JSON.parse(response);

                    var crypto_balance = json['crypto_balance'];

                    var my_balance = parseFloat(json['my_balance']);
                    my_balance = fixNumber(my_balance, 2);

                    $("#sell_available").html(crypto_balance);
                    $("#sell_available2").html(crypto_balance);
                    $("#sell_available3").html(crypto_balance);
                    $("#buy_available").html(my_balance);
                    $("#buy_available2").html(my_balance);
                    $("#buy_available3").html(my_balance);
                    $("#usdt_balance").val(my_balance <= 0 ? '0.00' : my_balance);
                }
            });
        }
//buy
        $('#buy_amount_input').keyup(function (e) {
            buyBy = "input"
            updateBuyAvailable(false);
        });

//sell
        $('#sell_amount_input').keyup(function (e) {
            sellBy = "input";
            updateSellAvailable(false);
        });

        // ---------------- Sell and Buy buttons -----------------------//
        $("#btn_buy").on("click", function () {
            var range_val_buy = $(".order__slider-buy-market").val();
            var available_usd = $("#buy_available2").html();
            available_usd = parseFloat(available_usd);

            if (available_usd == 0) {
                noti("You don't have enough balance", "warning");
            } else {
                var buyAmount = $("#buy_amount_input").val();
                var result_change = buyAmount !== '' && parseFloat(buyAmount) > 0 ? parseFloat(buyAmount) : available_usd / 100 * range_val_buy;
                result_change = fixNumber(result_change, 2);

                var result_price = result_change / pair_price;
                result_price = fixNumber(result_price, 6);

                $("#buy_amount_input").val(result_change);
                $("#buy_crypto_available").html(result_price);
            }

            var buy_amount = $("#buy_amount_input").val();
            var buy_amount_crypto = $("#buy_crypto_available").html();
            var buy_available = $("#buy_available").html();
            buy_available = fixNumber(buy_available, 8)

            if (buy_amount == "" || parseFloat(buy_amount) <= 0) {
                noti("Please, enter buy amount", "warning");
            } else {
                noti("Processing..", "warning");
                $.ajax({
                    url: "../api/user/trading",
                    type: "POST",
                    contentType: "application/json",
                    data: JSON.stringify({
                        action: "CREATE_ORDER",
                        type: "BUY",
                        price: pair_price,
                        coin: symbol,
                        amount: parseFloat(buy_amount_crypto)
                    }),
                    success: function (response) {
                        $("#orders_table").load("../api/user/trading?action=GET_OPEN_ORDERS");
                        noti("Order successfully created to buy currency", "success");
                        $("#buy_amount_input").val("");
                        $("#buy_crypto_available").html("0.000000");
                        updateTradingBalance();
                    },
                    error: function(xhr) {
                        switch (xhr.responseText) {
                            case "trading_ban": {
                                showErrorModal('TRADING');
                                break;
                            }
                            case "amount_error": {
                                noti("Amount not corrected", "error");
                                break;
                            }
                            case "no_balance": {
                                noti("Not enough balance", "error");
                                break;
                            }
                            case "already_exists": {
                                noti("Wait a moment... you already have an open order", "error");
                                break;
                            }
                            case "min_amount": {
                                noti("The minimum amount to create an order is $10.", "error")
                            }
                            default: {
                                noti("An unexpected error has occurred, please try again", "error");
                            }
                        }
                    }
                });
            }
        });

        $("#btn_sell").on("click", function () {
            var range_val_sell = $(".order__slider-sell-market").val();
            var available_usd = $("#sell_available2").html();
            available_usd = parseFloat(available_usd);

            if (available_usd == 0) {
                noti("You don't have enough balance", "warning");
            } else {
                const sellAmount = $("#sell_amount_input").val();
                var result_change = sellAmount !== '' && parseFloat(sellAmount) > 0 ? parseFloat(sellAmount) : available_usd / 100 * range_val_sell;

                var result_price = result_change * pair_price;
                result_price = fixNumber(result_price, 6);

                $("#sell_amount_input").val(fixNumber(result_change, 6));
                $("#sell_crypto_available").html(result_price);
            }

            var sell_amount = $("#sell_amount_input").val();
            var sell_available = $("#sell_available").html();
            var sell_fee = $("#sell_fee").html();
            sell_available_fee = sell_available - sell_fee;

            if (pair_price == 0) {
                alert('error pair price');
            } else {
                if (sell_amount == "" || parseFloat(sell_amount) <= 0) {
                    noti("Please, enter sell amount", "warning");
                } else if (parseFloat(sell_amount) > parseFloat(sell_available)) {
                    noti("Not enough balance. Available for you: " + sell_available + " " + pair_one.toUpperCase(), "error");
                } else {
                    noti("Processing..", "warning");

                    $.ajax({
                        url: "../api/user/trading",
                        type: "POST",
                        contentType: "application/json",
                        data: JSON.stringify({
                            action: "CREATE_ORDER",
                            type: "SELL",
                            price: pair_price,
                            coin: symbol,
                            amount: parseFloat(sell_amount)
                        }),
                        success: function (response) {
                            $("#orders_table").load("../api/user/trading?action=GET_OPEN_ORDERS");
                            noti("Order successfully created to sell currency", "success");
                            updateTradingBalance()
                        },
                        error: function(xhr) {
                            switch (xhr.responseText) {
                                case "trading_ban": {
                                    showErrorModal('TRADING');
                                    break;
                                }
                                case "amount_error": {
                                    noti("Amount not corrected", "error");
                                    break;
                                }
                                case "no_balance": {
                                    noti("Not enough balance", "error");
                                    break;
                                }
                                case "already_exists": {
                                    noti("Wait a moment... you already have an open order", "error");
                                    break;
                                }
                                case "min_amount": {
                                    noti("The minimum amount to create an order is $10.", "error")
                                }
                                default: {
                                    noti("An unexpected error has occurred, please try again", "error");
                                }
                            }
                        }
                    });
                }
            }
        });

        setInterval(function () {
            $.ajax({
                url: "../api/user/trading",
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    action: "CLOSE_OPEN_ORDERS"
                }),
                success: function (response) {
                    if (response === 'true') {
                        noti("Currency purchased successfully", "success");
                    }

                    $("#orders_table").load("../api/user/trading?action=GET_OPEN_ORDERS");
                    $("#orders_history").load("../api/user/trading?action=GET_HISTORY_ORDERS");

                    updateTradingBalance()
                }
            });
        }, 5000);
        ///////////////////////////////-END-///////////////////////////////

        var first_red_block;
        var first_green_block;

        fetch("https://api." + domain + "/api/v3/trades?symbol=" + pairs_value + "&limit=1000").then(response => {
            response.json().then(js => {
                function shuffleArray(array) {
                    for (let i = array.length - 1; i > 0; i--) {
                        const j = Math.floor(Math.random() * (i + 1));
                        [array[i], array[j]] = [array[j], array[i]];
                    }
                }

                const json = {};
                let j = 1;
                js.forEach(map => {
                    json[j.toString()] = [get_percent(parseFloat(map.price)), map.qty];
                    j++;
                });

                shuffleArray(json);

                var random_number = Math.floor((Math.random() * 10) + 4);
                pair_price = parseFloat(json[1][0]);
                updateBuyAvailable(false);
                updateSellAvailable(false);

                var tr_block_id = 1; //start 6
                var new_tr_block_id = 1; // start 7
                var for_tr_block_id = 1;

                var json_red_count = 1; //limit 1000
                var json_green_count = 10 //limit 1000

                var red_block = '';
                var green_block = '';

                for (var i = 0; i < 19; i++) {
                    function random_number() {
                        // Random Number Red
                        var random_number_2 = Math.floor((Math.random() * 7) + 1);
                        var random_true = '';
                        if (random_number_2 == 1) {
                            random_true = '5';
                        } else if (random_number_2 == 2) {
                            random_true = '8';
                        } else if (random_number_2 == 3) {
                            random_true = '10';
                        } else if (random_number_2 == 4) {
                            random_true = '20';
                        } else if (random_number_2 == 5) {
                            random_true = '40';
                        } else if (random_number_2 == 6) {
                            random_true = '60';
                        } else if (random_number_2 == 7) {
                            random_true = '80';
                        } else {
                            random_true = '';
                        }

                        return random_true;
                    }

                    var two_price_red = parseFloat(json[json_red_count][0]) * parseFloat(json[json_red_count][1]);
                    var two_price_green = parseFloat(json[json_green_count][0]) * parseFloat(json[json_green_count][1]);

                    var two_amount_red = parseFloat(json[json_red_count][1]);
                    if (two_amount_red > 1) {
                        two_amount_red = two_amount_red.toFixed(2);
                    } else {
                        two_amount_red = two_amount_red.toFixed(6);
                    }

                    var two_amount_green = parseFloat(json[json_green_count][1]);
                    if (two_amount_green > 1) {
                        two_amount_green = two_amount_green.toFixed(2);
                    } else {
                        two_amount_green = two_amount_green.toFixed(6);
                    }

                    // red order book
                    var red_block_form = parseFloat(json[json_red_count][0]);
                    if (red_block_form > 10) {
                        red_block_form = red_block_form.toFixed(2);
                    } else if (red_block_form > 0.1) {
                        red_block_form = red_block_form.toFixed(4);
                    } else {
                        red_block_form = red_block_form.toFixed(6);
                    }

                    var green_block_form = parseFloat(json[json_green_count][0]);
                    if (green_block_form > 10) {
                        green_block_form = green_block_form.toFixed(2);
                    } else if (green_block_form > 0.1) {
                        green_block_form = green_block_form.toFixed(4);
                    } else {
                        green_block_form = green_block_form.toFixed(6);
                    }

                    red_block = red_block + `<div class="book__item red_tr" id="red_tr_` + for_tr_block_id + `">
                                        <div class="book__item-fill" style="width: ` + random_number() + `%;"></div>
                                        <div class="book__item-price">
                                            ` + addCommas(red_block_form) + `
                                        </div>
                                        <div class="book__item-size">
                                            ` + two_amount_red + `
                                        </div>
                                        <div class="book__item-sum">
                                            ` + addCommas(two_price_red.toFixed(2)) + `
                                        </div>
                                    </div>`;
                    // green order book
                    green_block = green_block + `<div class="book__item green_tr" id="green_tr_` + for_tr_block_id + `">
                                            <div class="book__item-fill" style="width: ` + random_number() + `%;"></div>
                                            <div class="book__item-price">
                                                ` + addCommas(green_block_form) + `
                                            </div>
                                            <div class="book__item-size">
                                                ` + two_amount_green + `
                                            </div>
                                            <div class="book__item-sum">
                                                ` + addCommas(two_price_green.toFixed(2)) + `
                                            </div>
                                        </div>`;

                    json_red_count = json_red_count + 1;
                    json_green_count = json_green_count + 1;

                    if (json_red_count >= json.length) {
                        json_red_count = 1;
                    }

                    if (json_green_count >= json.length) {
                        json_green_count = 10;
                    }

                    for_tr_block_id = for_tr_block_id + 1;

                    if (i == 18) {
                        $("#order_sell_div").prepend(red_block);
                        first_red_block = red_block;
                        $("#order_buy_div").prepend(green_block);
                        first_green_block = green_block;
                    }
                }

                var repeat_block = 'red';

                setInterval(function () {
                    if (repeat_block == 'red') {
                        repeat_block = 'green';
                    } else {
                        repeat_block = 'red';
                    }

                    var timer_red_block = '';
                    var timer_green_block = '';

                    function random_for() {
                        var rand_for = Math.floor((Math.random() * 4) + 1);

                        return rand_for;
                    }

                    for (var ii = 0; ii < parseFloat(random_for()); ii++) {
                        function random_number() {
                            // Random Number Red
                            var random_number_2 = Math.floor((Math.random() * 7) + 1);
                            var random_true = '';
                            if (random_number_2 == 1) {
                                random_true = '5';
                            } else if (random_number_2 == 2) {
                                random_true = '8';
                            } else if (random_number_2 == 3) {
                                random_true = '10';
                            } else if (random_number_2 == 4) {
                                random_true = '20';
                            } else if (random_number_2 == 5) {
                                random_true = '40';
                            } else if (random_number_2 == 6) {
                                random_true = '60';
                            } else if (random_number_2 == 7) {
                                random_true = '80';
                            } else {
                                random_true = '';
                            }

                            return random_true;
                        }

                        var two_price_red = parseFloat(json[json_red_count][0]) * parseFloat(json[json_red_count][1]);
                        var two_price_green = parseFloat(json[json_green_count][0]) * parseFloat(json[json_green_count][1]);

                        var two_amount_red = parseFloat(json[json_red_count][1]);
                        if (two_amount_red > 1) {
                            two_amount_red = two_amount_red.toFixed(2);
                        } else {
                            two_amount_red = two_amount_red.toFixed(6);
                        }

                        var two_amount_green = parseFloat(json[json_green_count][1]);
                        if (two_amount_green > 1) {
                            two_amount_green = two_amount_green.toFixed(2);
                        } else {
                            two_amount_green = two_amount_green.toFixed(6);
                        }

                        var json_red_block_form = parseFloat(json[json_red_count][0]);
                        if (json_red_block_form > 1) {
                            json_red_block_form = json_red_block_form.toFixed(2);
                        } else if (json_red_block_form > 0.1) {
                            json_red_block_form = json_red_block_form.toFixed(4);
                        } else {
                            json_red_block_form = json_red_block_form.toFixed(6);
                        }

                        var json_green_block_form = parseFloat(json[json_green_count][0]);
                        if (json_green_block_form > 10) {
                            json_green_block_form = json_green_block_form.toFixed(2);
                        } else if (json_green_block_form > 0.1) {
                            json_green_block_form = json_green_block_form.toFixed(4);
                        } else {
                            json_green_block_form = json_green_block_form.toFixed(6);
                        }

                        // red order book
                        timer_red_block = timer_red_block + `<div class="book__item red_tr" id="red_tr_` + new_tr_block_id + `">
                                                        <div class="book__item-fill" style="width: ` + random_number() + `%;"></div>
                                                        <div class="book__item-price">
                                                            ` + addCommas(json_red_block_form) + `
                                                        </div>
                                                        <div class="book__item-size">
                                                            ` + two_amount_red + `
                                                        </div>
                                                        <div class="book__item-sum">
                                                            ` + addCommas(two_price_red.toFixed(2)) + `
                                                        </div>
                                                    </div>`;

                        // green order book
                        timer_green_block = timer_green_block + `<div class="book__item green_tr" id="green_tr_` + new_tr_block_id + `">
                                                            <div class="book__item-fill" style="width: ` + random_number() + `%;"></div>
                                                            <div class="book__item-price">
                                                                ` + addCommas(json_green_block_form) + `
                                                            </div>
                                                            <div class="book__item-size">
                                                                ` + two_amount_green + `
                                                            </div>
                                                            <div class="book__item-sum">
                                                                ` + addCommas(two_price_green.toFixed(2)) + `
                                                            </div>
                                                        </div>`;
                        if (repeat_block == 'red') {
                            json_red_count = json_red_count + 1;
                        } else {
                            json_green_count = json_green_count + 1;
                        }

                        if (json_red_count >= json.length) {
                            json_red_count = 1;
                        }

                        if (json_green_count >= json.length) {
                            json_green_count = 10;
                        }

                        var remove_tr_id = new_tr_block_id - tr_block_id;

                        if (repeat_block == 'red') {
                            $(".red_tr").last().remove();
                        } else {
                            $(".green_tr").last().remove();
                        }

                        new_tr_block_id = new_tr_block_id + 1;
                    }

                    first_red_block = timer_red_block + "" + first_red_block;
                    first_green_block = timer_green_block + "" + first_green_block;

                    if (repeat_block == 'red') {
                        $("#order_sell_div").html(first_red_block);
                    } else {
                        $("#order_buy_div").html(first_green_block);
                    }
                }, 1000);
            });
        });

        //recent orders
        fetch("https://api." + domain + "/api/v3/depth?symbol=" + pairs_value + "&limit=300").then(response => {
            response.json().then(json => {
                function shuffleArray(array) {
                    for (let i = array.length - 1; i > 0; i--) {
                        const j = Math.floor(Math.random() * (i + 1));
                        [array[i], array[j]] = [array[j], array[i]];
                    }
                }

                let orderArray = [];
                let bids = json.bids;
                let asks = json.asks;
                shuffleArray(bids);
                shuffleArray(asks);
                for (let i = 0; i < Math.min(50, bids.length); i++) {
                    orderArray.push([
                        get_percent(parseFloat(bids[i][0])),
                        bids[i][1],
                        "false"
                    ]);
                }
                for (let i = 0; i < Math.min(50, asks.length); i++) {
                    orderArray.push([
                        get_percent(parseFloat(asks[i][0])),
                        asks[i][1],
                        "true"
                    ]);
                }
                orderArray.sort(() => Math.random() - 0.5);
                let orderBook = new Map();
                for (let i = 1; i <= 100; i++) {
                    orderBook.set(String(i), orderArray[i - 1]);
                }
                var json_price = JSON.parse(JSON.stringify(Object.fromEntries(orderBook), null, 2));
                var json_count = 1;
                var recent_tr_block_id = 21; //start 6
                var recent_new_tr_block_id = 47; // start 7

                var recent_all_block = '';

                for (var iii = 0; iii < 20; iii++) {
                    var json_live_price = parseFloat(json_price[json_count][0]);
                    var json_live_amount = parseFloat(json_price[json_count][1]);
                    var json_live_amount_fix = json_live_amount.toFixed(4);
                    if (json_live_amount > 1) {
                        json_live_amount_fix = json_live_amount.toFixed(2);
                    } else {
                        json_live_amount_fix = json_live_amount.toFixed(6);
                    }

                    var json_live_m = json_price[json_count][2];
                    json_count = json_count + 1;

                    if (json_count > 99) {
                        json_count = 1;
                    }

                    //now time
                    var dt = new Date();
                    var this_month = dt.getMonth() + 1;
                    var time = dt.getDate() + "." + this_month + " " + dt.getHours() + ":" + dt.getMinutes();

                    var recent_number = Math.floor((Math.random() * 2) + 1);

                    if (json_live_m == 'true') {
                        recent_status = 'buy';
                    } else {
                        recent_status = 'sell';
                    }

                    function random_number() {
                        // Random Number Red
                        var random_number_2 = Math.floor((Math.random() * 7) + 1);
                        var random_true = '';
                        if (random_number_2 == 1) {
                            random_true = '5';
                        } else if (random_number_2 == 2) {
                            random_true = '8';
                        } else if (random_number_2 == 3) {
                            random_true = '10';
                        } else if (random_number_2 == 4) {
                            random_true = '20';
                        } else if (random_number_2 == 5) {
                            random_true = '40';
                        } else if (random_number_2 == 6) {
                            random_true = '60';
                        } else if (random_number_2 == 7) {
                            random_true = '80';
                        } else {
                            random_true = '';
                        }

                        return random_true;
                    }

                    // recent trade
                    var live_json_form = json_live_price;
                    if (live_json_form > 10) {
                        live_json_form = live_json_form.toFixed(2);
                    } else if (live_json_form > 0.1) {
                        live_json_form = live_json_form.toFixed(4);
                    } else {
                        live_json_form = live_json_form.toFixed(6);
                    }

                    recent_all_block = recent_all_block + `<div class="trades__item ` + recent_status + `" id="recent_tr_` + recent_new_tr_block_id + `">
                                                    <div class="trades__item-price">
                                                        ` + addCommas(live_json_form) + `
                                                    </div>
                                                    <div class="trades__item-size">
                                                        ` + json_live_amount_fix + `
                                                    </div>
                                                    <div class="trades__item-time">
                                                        ` + time + `
                                                    </div>
                                                </div>`;

                    var remove_recent_id = recent_new_tr_block_id - recent_tr_block_id;
                    $("#recent_tr_" + remove_recent_id).remove();
                    recent_new_tr_block_id = recent_new_tr_block_id + 1;

                    if (iii == 19) {
                        $("#recent_orders").prepend(recent_all_block);
                    }
                }

                function doSomething() {
                    var json_live_price = parseFloat(json_price[json_count][0]);
                    var json_live_amount = parseFloat(json_price[json_count][1]);
                    var json_live_amount_fix = json_live_amount.toFixed(4);
                    if (json_live_amount > 1) {
                        json_live_amount_fix = json_live_amount.toFixed(2);
                    } else {
                        json_live_amount_fix = json_live_amount.toFixed(6);
                    }
                    var json_live_m = json_price[json_count][2];
                    json_count = json_count + 1;

                    if (json_count > 99) {
                        json_count = 1;
                    }

                    //now time
                    var dt = new Date();
                    var this_month = dt.getMonth() + 1;
                    var time = dt.getDate() + "." + this_month + " " + dt.getHours() + ":" + dt.getMinutes();

                    var recent_number = Math.floor((Math.random() * 2) + 1);

                    if (json_live_m == 'true') {
                        recent_status = 'buy';
                    } else {
                        recent_status = 'sell';
                    }

                    function random_number() {
                        // Random Number Red
                        var random_number_2 = Math.floor((Math.random() * 7) + 1);
                        var random_true = '';
                        if (random_number_2 == 1) {
                            random_true = '5';
                        } else if (random_number_2 == 2) {
                            random_true = '8';
                        } else if (random_number_2 == 3) {
                            random_true = '10';
                        } else if (random_number_2 == 4) {
                            random_true = '20';
                        } else if (random_number_2 == 5) {
                            random_true = '40';
                        } else if (random_number_2 == 6) {
                            random_true = '60';
                        } else if (random_number_2 == 7) {
                            random_true = '80';
                        } else {
                            random_true = '';
                        }

                        return random_true;
                    }

                    var new_live_json_form = json_live_price;
                    if (new_live_json_form > 10) {
                        new_live_json_form = new_live_json_form.toFixed(2);
                    } else if (new_live_json_form > 0.1) {
                        new_live_json_form = new_live_json_form.toFixed(4);
                    } else {
                        new_live_json_form = new_live_json_form.toFixed(6);
                    }

                    // recent trade
                    $("#recent_orders").prepend(`<div class="trades__item ` + recent_status + `" id="recent_tr_` + recent_new_tr_block_id + `">
                                        <div class="trades__item-price">
                                            ` + addCommas(new_live_json_form) + `
                                        </div>
                                        <div class="trades__item-size">
                                            ` + json_live_amount_fix + `
                                        </div>
                                        <div class="trades__item-time">
                                            ` + time + `
                                        </div>
                                    </div>`);

                    var remove_recent_id = recent_new_tr_block_id - recent_tr_block_id;
                    $("#recent_tr_" + remove_recent_id).remove();
                    recent_new_tr_block_id = recent_new_tr_block_id + 1;
                }

                var i1;
                var rand = 300;

                function randomize() {
                    doSomething();
                    rand = Math.round(Math.random() * (3000 - 500)) + 500;
                    clearInterval(i1);
                    i1 = setInterval(randomize, rand);
                }

                i1 = setInterval(randomize, rand);
            });
        });

        setInterval(() => {
            $.ajax({
          url: "/api/user/profile",
          type: "POST",
          contentType: 'application/json; charset=UTF-8',
          dataType: 'text',
          data: JSON.stringify({
              action: "CHECK_ALERT"
          }),
          success: function (response) {
              if (response !== "no_alerts") {
                  const json = JSON.parse(response);

                  const type = json["type"];
                  const message = json["message"];

                  if (type === "NOTIFICATION") {
                      noti(message, "success");
                  } else if (type === "ALERT") {
                      $("#alert_message_modal").addClass("withdrawal-successful-active");
                      $("#userAlertBox").html(message);
                  } else if (type === "BONUS") {
                      $("#alert_img_pop").attr("src", "");
                      $("#alert_title_text").html("Bonus");
                      $("#alert_img_pop").attr("src", "../assets/img/bonus-icon.png");
                      $("#alert_img_pop").css("display", "block");
                      $("#svg_alert_img").remove();
                      $("#alert_message_modal").addClass("withdrawal-successful-active");
                      $("#userAlertBox").html(message);
                      $("#userAlertBox").css("font-size", "18px");
                  }
              }
          }
      });
        }, 10000);

        var terminal_crypto = pair_one.toUpperCase();
        if (sign_in == 'true') {
            setInterval(function () {
                updateTradingBalance();
            }, 3000);
        }

        var buyBy = "none";
        var sellBy = "none";

        $(".order__slider-buy-market").on("change", function (e) {
            buyBy = "slider";
            updateBuyAvailable(true);
        });

        $(".order__slider-sell-market").on("change", function (e) {
            sellBy = "slider";
            updateSellAvailable(true);
        });

        function updateBuyAvailable(withNoti) {
            if (buyBy === "none") {
                return;
            }
            if (buyBy === "input") {
                var buy_amount = $("#buy_amount_input").val();

                if (buy_amount == "") {
                    $("#buy_crypto_available").html("0.000000");
                } else {
                    var result_price = parseFloat(buy_amount);
                    result_price = fixNumber(result_price, 8)

                    var total_usdt = result_price / pair_price;

                    $("#buy_crypto_available").html(fixNumber(total_usdt, 6));
                }
                return;
            }
            var range_val_buy = $(".order__slider-buy-market").val();
            var available_usd = $("#buy_available2").html();
            available_usd = parseFloat(available_usd);

            if (withNoti && available_usd == 0) {
                noti("You don't have enough balance", "warning");
            } else {
                var result_change = available_usd / 100 * range_val_buy;
                result_change = fixNumber(result_change, 2);

                var result_price = result_change / pair_price;
                result_price = fixNumber(result_price, 6);

                $("#buy_amount_input").val(result_change);
                $("#buy_crypto_available").html(result_price);
            }
        }

        function updateSellAvailable(withNoti) {
            if (sellBy === "none") {
                return;
            }
            if (sellBy === "input") {
                var sell_amount = $("#sell_amount_input").val();
                var result_price = sell_amount * pair_price;
                result_price = fixNumber(result_price, 2);

                $("#sell_crypto_available").html(result_price);
                return;
            }
            var range_val_sell = $(".order__slider-sell-market").val();
            var available_usd = $("#sell_available2").html();
            available_usd = parseFloat(available_usd);

            if (withNoti && available_usd == 0) {
                noti("You don't have enough balance", "warning");
            } else {
                var result_change = available_usd / 100 * range_val_sell;

                var result_price = result_change * pair_price;
                result_price = fixNumber(result_price, 6);

                $("#sell_amount_input").val(fixNumber(result_change, 6));
                $("#sell_crypto_available").html(result_price);
            }
        }



        //start limit orders

        // ---------------- Sell and Buy buttons -----------------------//
        $("#btn_limit_buy").on("click", function () {
            var price = $("#limit_buy_price").val();
            var amount = $("#limit_buy_amount").val();
            if (price === '' || amount === '' || parseFloat(price) <= 0 || parseFloat(amount) <= 0) {
                noti('Price or amount not corrected', 'warning');
                return;
            }

            noti("Processing..", "warning");
            $.ajax({
                url: "../api/user/trading",
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    action: "CREATE_LIMIT_ORDER",
                    type: "BUY",
                    price: parseFloat(price),
                    amount: parseFloat(amount),
                    coin: symbol
                }),
                success: function (response) {
                    $("#orders_table").load("../api/user/trading?action=GET_OPEN_ORDERS");
                    noti("Limit order successfully created to buy currency", "success");
                    $("#limit_buy_amount").val('');
                    $("#limit_buy_total").val('');
                    updateTradingBalance();
                },
                error: function(xhr) {
                    switch (xhr.responseText) {
                        case "trading_ban": {
                            showErrorModal('TRADING');
                            break;
                        }
                        case "amount_error": {
                            noti("Amount not corrected", "error");
                            break;
                        }
                        case "no_balance": {
                            noti("Not enough balance", "error");
                            break;
                        }
                        case "already_exists": {
                            noti("You have reached the limit on the number of limit orders (3)", "error");
                            break;
                        }
                        case "min_amount": {
                            noti("The minimum amount to create an order is $10.", "error")
                        }
                        default: {
                            noti("An unexpected error has occurred, please try again", "error");
                        }
                    }
                }
            });
        });

        $("#btn_limit_sell").on("click", function () {
            var price = $("#limit_sell_price").val();
            var amount = $("#limit_sell_amount").val();
            if (price === '' || amount === '' || parseFloat(price) <= 0 || parseFloat(amount) <= 0) {
                noti('Price or amount not corrected', 'warning');
                return;
            }

            noti("Processing..", "warning");
            $.ajax({
                url: "../api/user/trading",
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    action: "CREATE_LIMIT_ORDER",
                    type: "SELL",
                    price: parseFloat(price),
                    amount: parseFloat(amount),
                    coin: symbol
                }),
                success: function (response) {
                    $("#orders_table").load("../api/user/trading?action=GET_OPEN_ORDERS");
                    noti("Limit order successfully created to sell currency", "success");
                    $("#limit_sell_amount").val('');
                    $("#limit_sell_total").val('');
                    updateTradingBalance();
                },
                error: function(xhr) {
                    switch (xhr.responseText) {
                        case "trading_ban": {
                            showErrorModal('TRADING');
                            break;
                        }
                        case "amount_error": {
                            noti("Amount not corrected", "error");
                            break;
                        }
                        case "no_balance": {
                            noti("Not enough balance", "error");
                            break;
                        }
                        case "already_exists": {
                            noti("You have reached the limit on the number of limit orders (3)", "error");
                            break;
                        }
                        case "min_amount": {
                            noti("The minimum amount to create an order is $10.", "error")
                        }
                        default: {
                            noti("An unexpected error has occurred, please try again", "error");
                        }
                    }
                }
            });
        });


        let limitBuyBy = 'none';
        let limitSellBy = 'none';

        $("#limit_buy_price").on('input', () => {
            limitBuyBy = "input";
            updateLimitBuyAvailable(true);
        });

        $("#limit_buy_amount").on('input', () => {
            var price = $("#limit_buy_price").val();
            if (price === '' || price <= 0) {
                noti('You must specify a price', 'error');
                return;
            }
            limitBuyBy = "input";
            updateLimitBuyAvailable(true);
        });

        $("#limit_sell_price").on('input', () => {
            limitSellBy = "input";
            updateLimitSellAvailable(true);
        });

        $("#limit_sell_amount").on('input', () => {
            var price = $("#limit_sell_price").val();
            if (price === '' || price <= 0) {
                noti('You must specify a price', 'error');
                return;
            }
            limitSellBy = "input";
            updateLimitSellAvailable(true);
        });

        $(".order__slider-buy-limit").on("change", function (e) {
            var price = $("#limit_buy_price").val();
            if (price === '' || price <= 0) {
                noti('You must specify a price', 'error');
                return;
            }
            limitBuyBy = "slider";
            updateLimitBuyAvailable(true);
        });

        $(".order__slider-sell-limit").on("change", function (e) {
            var price = $("#limit_sell_price").val();
            if (price === '' || price <= 0) {
                noti('You must specify a price', 'error');
                return;
            }
            limitSellBy = "slider";
            updateLimitSellAvailable(true);
        });

        function updateLimitBuyAvailable(withNoti) {
            if (limitBuyBy === "none") {
                return;
            }
            var price = $("#limit_buy_price").val();
            if (limitBuyBy === "input") {
                var size = $("#limit_buy_amount").val();

                var total = fixNumber(price * size, 6);

                $("#limit_buy_total").val(total);
                return;
            }

            var range_val_buy = $(".order__slider-buy-limit").val();
            var available_usd = $("#buy_available2").html();
            available_usd = parseFloat(available_usd);

            if (withNoti && available_usd == 0) {
                noti("You don't have enough balance", "warning");
            } else {
                var result_change = available_usd / 100 * range_val_buy;
                result_change = fixNumber(result_change, 2);

                var result_price = result_change / price;
                result_price = fixNumber(result_price, 6);

                $("#limit_buy_amount").val(result_price);
                $("#limit_buy_total").val(result_change);
            }
        }

        function updateLimitSellAvailable(withNoti) {
            if (limitSellBy === "none") {
                return;
            }
            var price = $("#limit_sell_price").val();
            if (limitSellBy === "input") {
                var size = $("#limit_sell_amount").val();

                var total = fixNumber(price * size, 6);

                $("#limit_sell_total").val(total);
                return;
            }

            var range_val_sell = $(".order__slider-sell-limit").val();
            var available_usd = $("#sell_available2").html();
            available_usd = parseFloat(available_usd);

            if (withNoti && available_usd == 0) {
                noti("You don't have enough balance", "warning");
            } else {
                var result_change = available_usd / 100 * range_val_sell;

                var result_price = result_change * price;
                result_price = fixNumber(result_price, 8);

                $("#limit_sell_amount").val(result_change);
                $("#limit_sell_total").val(result_price);
            }
        }
        //end limit orders
    });
}
