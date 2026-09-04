package com.example.tailormaster.validation;

import com.example.tailormaster.entity.Customer;

import java.util.*;

public class Utility {

    // validation error messages
    public static final String PRODUCT_ERROR_CODE = "error.products";
    public static final String PRODUCT_ERROR_MESSAGE = "Please select at least one product";
    public static final String CHEST = "Chest is required";
    public static final String SLEEVE = "Sleeve Length is required";
    public static final String SHOULDER = "Shoulder Length is required";
    public static final String HIPS = "Hips is required";
    public static final String WAIST = "Waist is required";

    // Utility method to populate customer measurements
//    public static CustomerMeasurement populateCustomerMeasurement(Customer customer, Product product, CustomerRegistrationDTO registrationDTO) {
//        CustomerMeasurement measurement = new CustomerMeasurement();
//
//        measurement.setCustomer(customer);
//        measurement.setProduct(product);
//
//        measurement.setFrontPocket(registrationDTO.isFrontPocket());
//        measurement.setOneSidePocket(registrationDTO.isOneSidePocket());
//        measurement.setTwoSidePocket(registrationDTO.isTwoSidePocket());
//        measurement.setShalwarZipPocket(registrationDTO.isShalwarZipPocket());
//        measurement.setShalwarSadaPocket(registrationDTO.isShalwarSadaPocket());
//        measurement.setTwoPajamaPocket(registrationDTO.isTwoPajamaPocket());
//        measurement.setSingleSilai(registrationDTO.isSingleSilai());
//        measurement.setDoubleSilai(registrationDTO.isDoubleSilai());
//        measurement.setChamakSilai(registrationDTO.isChamakSilai());
//        measurement.setFlopPocket(registrationDTO.isFlopPocket());
//        measurement.setNormalButton(registrationDTO.isNormalButton());
//        measurement.setTouchButton(registrationDTO.isTouchButton());
//        measurement.setMetalButton(registrationDTO.isMetalButton());
//
//        measurement.setQameezLength(registrationDTO.getQameezLength());
//        measurement.setQameezType(registrationDTO.getQameezType());
//        measurement.setBazoo(registrationDTO.getBazoo());
//        measurement.setKuhni(registrationDTO.getKuhni());
//        measurement.setAarmHole(registrationDTO.getAarmHole());
//        measurement.setKuff(registrationDTO.getKuff());
//        measurement.setGolMoori(registrationDTO.getGolMoori());
//        measurement.setGalla(registrationDTO.getGalla());
//        measurement.setGallaType(registrationDTO.getGallaType());
//        measurement.setTera(registrationDTO.getTera());
//        measurement.setPatiLambai(registrationDTO.getPatiLambai());
//
//        measurement.setChati(registrationDTO.getChati());
//        measurement.setChati360(registrationDTO.getChati360());
//        measurement.setKamar(registrationDTO.getKamar());
//        measurement.setKamar360(registrationDTO.getKamar360());
//        measurement.setDaman(registrationDTO.getDaman());
//        measurement.setDaman360(registrationDTO.getDaman360());
//        measurement.setShalwar(registrationDTO.getShalwar());
//        measurement.setShalwarGher(registrationDTO.getShalwarGher());
//        measurement.setShalwarPaincha(registrationDTO.getShalwarPaincha());
//        measurement.setPajama(registrationDTO.getPajama());
//        measurement.setPajamaWaist(registrationDTO.getPajamaWaist());
//        measurement.setPajamaPaincha(registrationDTO.getPajamaPaincha());
//        measurement.setLasticPlusDori(registrationDTO.getLasticPlusDori());
//
//        measurement.setDeegar(registrationDTO.getDeegar());
//
//        // Generate barcode
//        String barcode = generateBarcode(customer);
//        measurement.setBarcode(barcode);
//
//        return measurement;
//    }

    // Utility method to populate CustomerRegistrationDTO
//    public static CustomerRegistrationDTO populateCustomerRegistrationDTO(List<Product> products,
//                                                                          Customer customer,
//                                                                          CustomerMeasurement customerMeasurement) {
//        CustomerRegistrationDTO measurement = new CustomerRegistrationDTO();
//
//        measurement.setCustomer(customer);
//
//        if (customerMeasurement != null) {
//            measurement.setFrontPocket(customerMeasurement.isFrontPocket());
//            measurement.setOneSidePocket(customerMeasurement.isOneSidePocket());
//            measurement.setTwoSidePocket(customerMeasurement.isTwoSidePocket());
//            measurement.setShalwarZipPocket(customerMeasurement.isShalwarZipPocket());
//            measurement.setShalwarSadaPocket(customerMeasurement.isShalwarSadaPocket());
//            measurement.setTwoPajamaPocket(customerMeasurement.isTwoPajamaPocket());
//            measurement.setSingleSilai(customerMeasurement.isSingleSilai());
//            measurement.setDoubleSilai(customerMeasurement.isDoubleSilai());
//            measurement.setChamakSilai(customerMeasurement.isChamakSilai());
//            measurement.setFlopPocket(customerMeasurement.isFlopPocket());
//            measurement.setNormalButton(customerMeasurement.isNormalButton());
//            measurement.setTouchButton(customerMeasurement.isTouchButton());
//            measurement.setMetalButton(customerMeasurement.isMetalButton());
//
//            measurement.setQameezLength(customerMeasurement.getQameezLength());
//            measurement.setQameezType(customerMeasurement.getQameezType());
//            measurement.setBazoo(customerMeasurement.getBazoo());
//            measurement.setKuhni(customerMeasurement.getKuhni());
//            measurement.setAarmHole(customerMeasurement.getAarmHole());
//            measurement.setKuff(customerMeasurement.getKuff());
//            measurement.setGolMoori(customerMeasurement.getGolMoori());
//            measurement.setGalla(customerMeasurement.getGalla());
//            measurement.setGallaType(customerMeasurement.getGallaType());
//            measurement.setTera(customerMeasurement.getTera());
//            measurement.setPatiLambai(customerMeasurement.getPatiLambai());
//
//            measurement.setChati(customerMeasurement.getChati());
//            measurement.setChati360(customerMeasurement.getChati360());
//            measurement.setKamar(customerMeasurement.getKamar());
//            measurement.setKamar360(customerMeasurement.getKamar360());
//            measurement.setDaman(customerMeasurement.getDaman());
//            measurement.setDaman360(customerMeasurement.getDaman360());
//            measurement.setShalwar(customerMeasurement.getShalwar());
//            measurement.setShalwarGher(customerMeasurement.getShalwarGher());
//            measurement.setShalwarPaincha(customerMeasurement.getShalwarPaincha());
//            measurement.setPajama(customerMeasurement.getPajama());
//            measurement.setPajamaWaist(customerMeasurement.getPajamaWaist());
//            measurement.setPajamaPaincha(customerMeasurement.getPajamaPaincha());
//            measurement.setLasticPlusDori(customerMeasurement.getLasticPlusDori());
//
//            measurement.setDeegar(customerMeasurement.getDeegar());
//        }
//
//        // Fetch products the customer has selected
//        Set<Long> selectedProductIds = customer.getMeasurements()
//                .stream()
//                .map(measurement1 -> measurement1.getProduct().getId())
//                .collect(Collectors.toSet());
//
//        // Ensure all active products are listed, marking selected ones
//        List<Product> productsList = new ArrayList<>();
//        for (Product product : products) {
//            productsList.add(product); // Add all active products (both selected & unselected)
//        }
//        measurement.setProducts(productsList);
//
//        // Populate customer measurements
//        Map<Long, CustomerMeasurement> measurementMap = new HashMap<>();
//        for (CustomerMeasurement measurement2 : customer.getMeasurements()) {
//            measurementMap.put(measurement2.getProduct().getId(), measurement2);
//        }
//        measurement.setCustomerMeasurements(measurementMap);
//
//        // Pass the selected product IDs for Thymeleaf to check the right boxes
//        measurement.setSelectedProductIds(selectedProductIds);
//
//        return measurement;
//    }

    // Utility method to populate CustomerMeasurement
//    public static CustomerMeasurement populateCustomerMeasurement(CustomerMeasurement measurement, CustomerRegistrationDTO customerRegistrationDTO) {
//
//        measurement.setFrontPocket(customerRegistrationDTO.isFrontPocket());
//        measurement.setOneSidePocket(customerRegistrationDTO.isOneSidePocket());
//        measurement.setTwoSidePocket(customerRegistrationDTO.isTwoSidePocket());
//        measurement.setShalwarZipPocket(customerRegistrationDTO.isShalwarZipPocket());
//        measurement.setShalwarSadaPocket(customerRegistrationDTO.isShalwarSadaPocket());
//        measurement.setTwoPajamaPocket(customerRegistrationDTO.isTwoPajamaPocket());
//        measurement.setSingleSilai(customerRegistrationDTO.isSingleSilai());
//        measurement.setDoubleSilai(customerRegistrationDTO.isDoubleSilai());
//        measurement.setChamakSilai(customerRegistrationDTO.isChamakSilai());
//        measurement.setFlopPocket(customerRegistrationDTO.isFlopPocket());
//        measurement.setNormalButton(customerRegistrationDTO.isNormalButton());
//        measurement.setTouchButton(customerRegistrationDTO.isTouchButton());
//        measurement.setMetalButton(customerRegistrationDTO.isMetalButton());
//
//        measurement.setQameezLength(customerRegistrationDTO.getQameezLength());
//        measurement.setQameezType(customerRegistrationDTO.getQameezType());
//        measurement.setBazoo(customerRegistrationDTO.getBazoo());
//        measurement.setKuhni(customerRegistrationDTO.getKuhni());
//        measurement.setAarmHole(customerRegistrationDTO.getAarmHole());
//        measurement.setKuff(customerRegistrationDTO.getKuff());
//        measurement.setGolMoori(customerRegistrationDTO.getGolMoori());
//        measurement.setGalla(customerRegistrationDTO.getGalla());
//        measurement.setGallaType(customerRegistrationDTO.getGallaType());
//        measurement.setTera(customerRegistrationDTO.getTera());
//        measurement.setPatiLambai(customerRegistrationDTO.getPatiLambai());
//
//        measurement.setChati(customerRegistrationDTO.getChati());
//        measurement.setChati360(customerRegistrationDTO.getChati360());
//        measurement.setKamar(customerRegistrationDTO.getKamar());
//        measurement.setKamar360(customerRegistrationDTO.getKamar360());
//        measurement.setDaman(customerRegistrationDTO.getDaman());
//        measurement.setDaman360(customerRegistrationDTO.getDaman360());
//        measurement.setShalwar(customerRegistrationDTO.getShalwar());
//        measurement.setShalwarGher(customerRegistrationDTO.getShalwarGher());
//        measurement.setShalwarPaincha(customerRegistrationDTO.getShalwarPaincha());
//        measurement.setPajama(customerRegistrationDTO.getPajama());
//        measurement.setPajamaWaist(customerRegistrationDTO.getPajamaWaist());
//        measurement.setPajamaPaincha(customerRegistrationDTO.getPajamaPaincha());
//        measurement.setLasticPlusDori(customerRegistrationDTO.getLasticPlusDori());
//
//        measurement.setDeegar(customerRegistrationDTO.getDeegar());
//
//        return measurement;
//    }

    // generate barcode
    private static String generateBarcode(Customer savedCustomer) {
        return savedCustomer.getFullName() + savedCustomer.getPhoneNumber() + "-" + UUID.randomUUID();
    }

    public static String detectImageMimeType(byte[] imageData) {
        if (imageData == null || imageData.length < 4) return null;

        // Check for PNG
        if (imageData[0] == (byte) 0x89 && imageData[1] == 0x50 &&
                imageData[2] == 0x4E && imageData[3] == 0x47) {
            return "image/png";
        }

        // Check for JPEG/JPG
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) {
            return "image/jpeg";
        }

        return null;
    }

    public static String generateShortCode(String shopName) {
        if (shopName == null || shopName.trim().isEmpty()) {
            return "";
        }

        String[] words = shopName.trim().split("\\s+");
        StringBuilder shortCode = new StringBuilder();

        int limit = Math.min(words.length, 3);
        for (int i = 0; i < limit; i++) {
            if (!words[i].isEmpty()) {
                shortCode.append(Character.toUpperCase(words[i].charAt(0)));
            }
        }

        return shortCode.toString();
    }

}
