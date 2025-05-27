package com.example.tailormaster.entity;

import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @ToString
@Entity
public class CustomerMeasurement extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

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

    private String chest;
    private String sleeveLength;
    private String shoulder;
    private String hips;
    private String waist;

    private String barcode;

    public void generateBarcode() {
        this.barcode = customer.getFullName() + customer.getPhoneNumber() + "-" + UUID.randomUUID();
    }

    // Constructors, Getters, and Setters
}
