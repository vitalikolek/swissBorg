package me.yukitale.cryptoexchange.panel.common.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HomePageDesign {

    DESIGN_1("index"),
    DESIGN_2("index_2"),
    DESIGN_3("index_3"),
    DESIGN_4("index_4"),
    DESIGN_5("index_5"),
    DESIGN_6("index_6");

    private final String fileName;
}