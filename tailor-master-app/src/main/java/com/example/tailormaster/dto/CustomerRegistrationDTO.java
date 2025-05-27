package com.example.tailormaster.dto;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.validation.MeasurementValidationGroup;
import com.example.tailormaster.validation.measurement.ValidCustomerMeasurement;
import com.example.tailormaster.validation.products.ValidateSelectedProducts;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter @Setter @ToString
public class CustomerRegistrationDTO {

    @Valid
    private Customer customer;

    private boolean frontPocket;
    private boolean oneSidePocket;
    private boolean twoSidePocket;

    private boolean shalwarZipPocket;
    private boolean shalwarSadaPocket;
    private boolean twoPajamaPocket;

    private boolean singleSilai;
    private boolean doubleSilai;
    private boolean chamakSilai;

    private boolean flopPocket;
    private boolean normalButton;

    private boolean touchButton;
    private boolean metalButton;

    private String qameezLength;
    private String qameezType;

    private String bazoo;
    private String kuhni;
    private String aarmHole;

    private String kuff;
    private String golMoori;
    private String galla;
    private String gallaType;

    private String tera;
    private String patiLambai;

    private String chati;
    private String chati360;

    private String kamar;
    private String kamar360;

    private String daman;
    private String daman360;

    private String shalwar;
    private String shalwarGher;
    private String shalwarPaincha;

    private String pajama;
    private String pajamaWaist;
    private String pajamaPaincha;

    private String lasticPlusDori;
//    private String halfLasticPlusDori;

    private String deegar;

    private CustomerMeasurement measurement;

//    @Valid
//    @ValidateSelectedProducts(groups = MeasurementValidationGroup.class)
    private List<Product> products = new ArrayList<>();
//    @ValidCustomerMeasurement(groups = MeasurementValidationGroup.class)
    private Map<Long, CustomerMeasurement> customerMeasurements = new HashMap<>();

    // Store selected product IDs separately for easier pre-selection
    private Set<Long> selectedProductIds = new HashSet<>();

    public void addCustomerMeasurement(Long productId, CustomerMeasurement measurement) {
        customerMeasurements.put(productId, measurement);
    }
}