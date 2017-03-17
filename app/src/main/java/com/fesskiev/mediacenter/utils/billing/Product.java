package com.fesskiev.mediacenter.utils.billing;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Product {

    @SerializedName("productId")
    @Expose
    private String productId;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("price")
    @Expose
    private String price;
    @SerializedName("price_amount_micros")
    @Expose
    private int priceAmountMicros;
    @SerializedName("price_currency_code")
    @Expose
    private String priceCurrencyCode;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("description")
    @Expose
    private String description;

    @Override
    public String toString() {
        return "InAppProduct{" +
                "productId='" + productId + '\'' +
                ", type='" + type + '\'' +
                ", price='" + price + '\'' +
                ", priceAmountMicros=" + priceAmountMicros +
                ", priceCurrencyCode='" + priceCurrencyCode + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public String getSku() {
        return productId;
    }

    public String getType() {
        return type;
    }
}
