package com.example.tailormaster.controller.labor;

import com.example.tailormaster.entity.labor.Labor;
import com.example.tailormaster.entity.labor.LaborPayment;
import com.example.tailormaster.enums.LaborPaymentType;
import com.example.tailormaster.service.labor.LaborPaymentService;
import com.example.tailormaster.service.labor.LaborService;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Validation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Controller
@RequestMapping("/labors")
public class LaborController {

    private static final Logger logger = LogManager.getLogger(LaborController.class);

    private final LaborService laborService;
    private final LaborPaymentService laborPaymentService;
    private final Validation validation;

    @GetMapping
    public String listLabors(Model model) {
        logger.info("User accessed the labor list page.");
        try {
            List<Labor> labors = laborService.getAllLabors();
            model.addAttribute("labors",  labors);
            model.addAttribute("balanceMap", laborPaymentService.getLaborBalances());
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            logger.info("Retrieved labors: {} ", labors);
        } catch (Exception e) {
            logger.error("Error retrieving labors for the list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading labors.");
        }
        return "labor/labors";
    }

    @GetMapping("/create")
    public String showAddLaborForm(Model model) {
        model.addAttribute("labor", new Labor());
        return "labor/create";
    }

    @PostMapping("/save")
    public String saveLabor(@Valid @ModelAttribute("labor") Labor labor,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            logger.warn("Validation failed while saving labor: {}", bindingResult.getAllErrors());
            return "labor/create";
        }

        try {
            Labor savedLabor = laborService.saveLabor(labor);
            logger.info("Labor saved successfully: {}", savedLabor.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Labor added successfully.");
            return "redirect:/labors";
        } catch (Exception e) {
            logger.error("Error occurred while saving labor: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while saving labor. Please try again.");
            return "redirect:/labors";
        }
    }

    // Show update labor form
    @GetMapping("/edit/{id}")
    public String showUpdateLaborForm(@PathVariable String id, Model model,
                                        RedirectAttributes redirectAttributes) {
        logger.info("Displaying update labor form for labor ID: {}", id);

        try {
            // Decrypt and validate labor ID
            Long laborId = validation.validateAndFetchLabor(id, redirectAttributes);
            if (laborId == null) {
                logger.warn("Invalid labor ID provided for edit: {}", id);
                return "redirect:/labors";
            }

            Optional<Labor> labor = laborService.getLaborById(laborId);
            if (labor.isEmpty()) {
                logger.warn("Labor not found with ID {} for editing.", laborId);
                redirectAttributes.addFlashAttribute("errorMessage", "Labor not found.");
                return "redirect:/labors";
            }

            model.addAttribute("labor", labor.get());
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            logger.info("Populated Labor for edit form: {}", labor);
            return "labor/edit";

        } catch (Exception e) {
            logger.error("An error occurred while preparing the update labor form for encrypted ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while preparing the update labor.");
            return "redirect:/labors";
        }
    }

    // update Labor
    @PostMapping("/update")
    public String updateLabor(
            @Valid @ModelAttribute Labor labor,
            BindingResult bindingResult,
            @RequestParam("encryptedLaborId") String encryptedLaborId,
            RedirectAttributes redirectAttributes,
            Model model) {

        logger.info("Attempting to update labor with encrypted ID: {}", encryptedLaborId);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation failed while updating labor: {}", bindingResult.getAllErrors());
            return "labor/edit";
        }

        try {
            // Decrypt and validate labor ID
            Long laborId = validation.validateAndFetchLabor(encryptedLaborId, redirectAttributes);
            if (laborId == null) {
                logger.warn("Invalid labor ID provided for update: {}", encryptedLaborId);
                return "redirect:/labors";
            }
            logger.debug("Decrypted Labor ID for update: {}", laborId);

            // Fetch labor details (since ID is valid)
            Optional<Labor> existingLabor = laborService.getLaborById(laborId);
            if (existingLabor.isEmpty()) {
                logger.warn("Labor not found with ID {} for update.", laborId);
                redirectAttributes.addFlashAttribute("errorMessage", "Labor not found.");
                return "redirect:/labors";
            }

            labor.setId(laborId);

            Labor updatedLabor = laborService.updateLabor(labor);
            logger.info("Labor updated successfully: {}", updatedLabor);
            redirectAttributes.addFlashAttribute("successMessage", "Labor updated successfully!");
            redirectAttributes.addFlashAttribute("laborId", updatedLabor.getId());
            return "redirect:/labors";

        } catch (Exception e) {
            logger.error("Error updating Labor with encrypted ID {}: {}", encryptedLaborId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating Labor: " + e.getMessage());
            return "redirect:/labors";
        }
    }

    @GetMapping("/details/{id}")
    public String showLaborDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying details for labor ID (encrypted): {}", id);
        try {

            // Decrypt and validate customer ID
            Long laborId = validation.decryptAndValidateId(id);
            if (laborId == null) {
                logger.warn("Invalid labor ID provided: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid labor ID.");
                return "redirect:/labors";
            }
            logger.debug("Decrypted labor ID: {}", laborId);

            Optional<Labor> labor = laborService.getLaborById(laborId);
            if (labor.isEmpty()) {
                logger.warn("Labor not found with ID: {}", laborId);
                redirectAttributes.addFlashAttribute("errorMessage", "Labor not found.");
                return "redirect:/labors";
            }

            List<LaborPayment> payments = laborPaymentService.getPaymentsForLabor(laborId);

            LaborPayment laborPayment = new LaborPayment();
            laborPayment.setLabor(labor.get());

            BigDecimal regularPaid = laborPaymentService.getTotalRegularPaidByLaborId(laborId);
            BigDecimal advancePaid = laborPaymentService.getTotalAdvancePaidByLaborId(laborId);
            BigDecimal borrowPaid = laborPaymentService.getTotalBorrowPaidByLaborId(laborId);

            BigDecimal totalPaid = laborPaymentService.getTotalPaidByLaborId(laborId);

//            BigDecimal remainingBalance = laborPaymentService.getLaborBalance(laborId);
            BigDecimal remainingBalance = regularPaid.subtract(totalPaid);

            model.addAttribute("labor", labor.get());
            model.addAttribute("payments", payments);
            model.addAttribute("totalPaid", totalPaid);
            model.addAttribute("regularPaid", regularPaid);
            model.addAttribute("advancePaid", advancePaid);
            model.addAttribute("borrowPaid", borrowPaid);
            model.addAttribute("remainingBalance", remainingBalance);
            model.addAttribute("laborPayment", laborPayment); // for the modal form
            model.addAttribute("paymentTypes", LaborPaymentType.values());
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            model.addAttribute("isUpdate", false); // to toggle behavior in form
            logger.info("Fetched labor details: {}", labor.get());
            logger.info("Fetched labor payment details: {}", payments);
            logger.info("Fetched labor payment details breakup: Regular Paid :: {}, Advance Paid :: {}, " +
                    " Borrowed :: {}, Total Paid :: {}, Remaining Balance :: {}",
                    regularPaid, advancePaid, borrowPaid, totalPaid, remainingBalance);

        } catch (Exception e) {
            logger.error("Error loading labor details for ID (encrypted) {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading labor details: " + e.getMessage());
            return "redirect:/labors";
        }
        return "labor/labor-details";
    }

    @PostMapping("/payments")
    public String saveLaborPayment(@RequestParam BigDecimal amount,
                                   @ModelAttribute("laborPayment") LaborPayment laborPayment,
                                   @RequestParam("laborId") String laborId,
                                   RedirectAttributes redirectAttributes) {

        logger.info("Creating Labor payment: {}", laborPayment);
        try {

            Labor labor = laborService.getLaborById(Long.parseLong(laborId))
                    .orElseThrow(() -> new IllegalArgumentException("Labor not found."));

            laborPayment.setLabor(labor);
            laborPayment.setPaymentDate(LocalDate.now());
            LaborPayment savedLaborPayment = laborPaymentService.savePayment(amount, laborPayment);
            logger.info("Labor payment saved successfully: {}", savedLaborPayment);
            redirectAttributes.addFlashAttribute("successMessage", "Payment recorded successfully.");
        } catch (Exception e) {
            logger.error("Error saving labor payment for labor ID {}: {}", laborId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving labor payment: " + e.getMessage());
            return "redirect:/labors";
        }
        return "redirect:/labors/details/" + new ThymeleafUtil().encryptId(laborPayment.getLabor().getId()); // Redirect back to labor details
    }

    @GetMapping("/payments/get/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLaborPayment(@PathVariable Long id) {

        logger.info("Request for update labor payment for labor payment ID: {}", id);

        Optional<LaborPayment> laborPayment = laborPaymentService.getLaborPayment(id);

        return laborPayment
                .map(payment -> {

                    Map<String, Object> response = new HashMap<>();

                    response.put("id", payment.getId());
                    response.put("paymentType", payment.getPaymentType().name());
                    response.put("workAmount", payment.getWorkAmount());
                    response.put("deductions", payment.getDeductions());
                    response.put("remarks", payment.getRemarks());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/payments/update")
    public String updateLaborPayment(@RequestParam BigDecimal amount,
                                     @ModelAttribute("laborPayment") LaborPayment laborPayment,
                                     @RequestParam("laborId") Long laborId,
                                     RedirectAttributes redirectAttributes) {
        logger.info("Request for update labor payment for labor payment ID: {}", laborPayment.getId());
        try {
            Labor labor = laborService.getLaborById(laborId)
                    .orElseThrow(() -> new IllegalArgumentException("Labor not found."));

            laborPayment.setLabor(labor);
            laborPayment.setPaymentDate(LocalDate.now());
            LaborPayment updatedPayment = laborPaymentService.savePayment(amount, laborPayment);
            logger.info("Labor payment updated successfully with details: {}", updatedPayment);
            redirectAttributes.addFlashAttribute("successMessage", "Payment updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating labor payment for labor payment ID {}: {}", laborPayment.getId(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating labor payment: " + e.getMessage());
            return "redirect:/labors";
        }
        return "redirect:/labors/details/" + new ThymeleafUtil().encryptId(laborPayment.getLabor().getId());
    }

    @DeleteMapping("/payments/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        logger.info("Request for delete labor payment for labor payment ID: {}", id);
        try {
            laborPaymentService.deleteById(id); // make sure this method exists
            logger.info("Labor payment deleted successfully with labor payment ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to delete payment with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed");
        }
    }


}
