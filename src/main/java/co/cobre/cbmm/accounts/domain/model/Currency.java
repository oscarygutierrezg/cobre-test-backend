package co.cobre.cbmm.accounts.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing supported currency codes
 */
public enum Currency {
    USD("USD", "US Dollar", "$"),
    COP("COP", "Colombian Peso", "$"),
    MXN("MXN", "Mexican Peso", "$"),
    EUR("EUR", "Euro", "€"),
    GBP("GBP", "British Pound", "£"),
    BRL("BRL", "Brazilian Real", "R$"),
    ARS("ARS", "Argentine Peso", "$"),
    CLP("CLP", "Chilean Peso", "$"),
    PEN("PEN", "Peruvian Sol", "S/");

    private final String code;
    private final String name;
    private final String symbol;

    Currency(String code, String name, String symbol) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * Parse currency from string code
     */
    public static Currency fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }

        String upperCode = code.toUpperCase();
        for (Currency currency : values()) {
            if (currency.code.equals(upperCode)) {
                return currency;
            }
        }

        throw new IllegalArgumentException("Unsupported currency code: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}
