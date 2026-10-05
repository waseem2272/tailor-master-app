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
import java.util.List;
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
            model.addAttribute("labors", labors);
            model.addAttribute("balanceMap", laborPaymentService.getLaborBalances());
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            logger.info("Retrieved {} labors.", labors.size());
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
            logger.info("Labor saved successfully. ID: {}, Name: {}", savedLabor.getId(), savedLabor.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Labor added successfully.");
            return "redirect:/labors";
        } catch (Exception e) {
            logger.error("Error occurred while saving labor: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while saving labor. Please try again.");
            return "redirect:/labors";
        }
    }

    @GetMapping("/edit/{id}")
    public String showUpdateLaborForm(@PathVariable String id,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        logger.info("Displaying update labor form for labor ID: {}", id);
        try {
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
            logger.info("Labor loaded for edit. ID: {}, Name: {}", laborId, labor.get().getName());
            return "labor/edit";
        } catch (Exception e) {
            logger.error("Error preparing labor edit form for encrypted ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while preparing the update labor.");
            return "redirect:/labors";
        }
    }

    @PostMapping("/update")
    public String updateLabor(@Valid @ModelAttribute("labor") Labor labor,
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
            Long laborId = validation.validateAndFetchLabor(encryptedLaborId, redirectAttributes);
            if (laborId == null) {
                logger.warn("Invalid labor ID provided for update: {}", encryptedLaborId);
                return "redirect:/labors";
            }
            Optional<Labor> existingLabor = laborService.getLaborById(laborId);
            if (existingLabor.isEmpty()) {
                logger.warn("Labor not found with ID {} for update.", laborId);
                redirectAttributes.addFlashAttribute("errorMessage", "Labor not found.");
                return "redirect:/labors";
            }
            labor.setId(laborId);
            Labor updatedLabor = laborService.updateLabor(labor);
            logger.info("Labor updated successfully. ID: {}, Name: {}", updatedLabor.getId(), updatedLabor.getName());
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
    public String showLaborDetails(@PathVariable String id,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        logger.info("Displaying details for labor ID (encrypted): {}", id);
        try {
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
            laborPayment.setPaymentDate(LocalDate.now());
            BigDecimal totalWork = laborPaymentService.getTotalWorkByLaborId(laborId);
            BigDecimal totalSalary = laborPaymentService.getTotalSalaryByLaborId(laborId);
            BigDecimal advancePaid = laborPaymentService.getTotalAdvanceByLaborId(laborId);
            BigDecimal totalPaid = laborPaymentService.getTotalPaidByLaborId(laborId);
            BigDecimal remainingBalance = laborPaymentService.getLaborBalance(laborId);
            model.addAttribute("labor", labor.get());
            model.addAttribute("payments", payments);
            model.addAttribute("totalWork", totalWork);
            model.addAttribute("totalSalary", totalSalary);
            model.addAttribute("advancePaid", advancePaid);
            model.addAttribute("totalPaid", totalPaid);
            model.addAttribute("remainingBalance", remainingBalance);
            model.addAttribute("laborPayment", laborPayment);
            model.addAttribute("paymentTypes", LaborPaymentType.values());
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            model.addAttribute("isUpdate", false);
            logger.info("Fetched labor details. Labor ID: {}, Work: {}, Salary: {}, Advance: {}, Total Paid: {}, Balance: {}",
                    laborId, totalWork, totalSalary, advancePaid, totalPaid, remainingBalance);
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
        logger.info("Creating labor payment for labor ID: {}", laborId);
        try {
            Long parsedLaborId = Long.parseLong(laborId);
            Labor labor = laborService.getLaborById(parsedLaborId)
                    .orElseThrow(() -> new IllegalArgumentException("Labor not found."));
            laborPayment.setLabor(labor);
            laborPayment.setPaymentDate(LocalDate.now());
            LaborPayment savedLaborPayment = laborPaymentService.savePayment(amount, laborPayment);
            logger.info("Labor payment saved successfully. Payment ID: {}, Labor ID: {}, Type: {}, Amount: {}",
                    savedLaborPayment.getId(), parsedLaborId, savedLaborPayment.getPaymentType(), savedLaborPayment.getAmount());
            redirectAttributes.addFlashAttribute("successMessage", "Payment recorded successfully.");
        } catch (NumberFormatException e) {
            logger.error("Invalid labor ID: {}", laborId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid labor ID.");
            return "redirect:/labors";
        } catch (Exception e) {
            logger.error("Error saving labor payment for labor ID {}: {}", laborId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving labor payment: " + e.getMessage());
            return "redirect:/labors";
        }
        return "redirect:/labors/details/" + new ThymeleafUtil().encryptId(laborPayment.getLabor().getId());
    }

    @GetMapping("/payments/get/{id}")
    @ResponseBody
    public ResponseEntity<LaborPayment> getLaborPayment(@PathVariable Long id) {
        logger.info("Request to fetch labor payment. Payment ID: {}", id);
        Optional<LaborPayment> laborPayment = laborPaymentService.getLaborPayment(id);
        if (laborPayment.isPresent()) {
            logger.info("Labor payment fetched successfully. Payment ID: {}", id);
            return ResponseEntity.ok(laborPayment.get());
        }
        logger.warn("Labor payment not found. Payment ID: {}", id);
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/payments/update")
    public String updateLaborPayment(@RequestParam BigDecimal amount,
                                     @ModelAttribute("laborPayment") LaborPayment laborPayment,
                                     @RequestParam("laborId") Long laborId,
                                     RedirectAttributes redirectAttributes) {
        logger.info("Request to update labor payment. Payment ID: {}, Labor ID: {}", laborPayment.getId(), laborId);
        try {
            Labor labor = laborService.getLaborById(laborId)
                    .orElseThrow(() -> new IllegalArgumentException("Labor not found."));
            laborPayment.setLabor(labor);
            laborPayment.setPaymentDate(LocalDate.now());
            LaborPayment updatedPayment = laborPaymentService.savePayment(amount, laborPayment);
            logger.info("Labor payment updated successfully. Payment ID: {}, Labor ID: {}, Type: {}, Amount: {}",
                    updatedPayment.getId(), laborId, updatedPayment.getPaymentType(), updatedPayment.getAmount());
            redirectAttributes.addFlashAttribute("successMessage", "Payment updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating labor payment. Payment ID: {}, Labor ID: {}: {}",
                    laborPayment.getId(), laborId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating labor payment: " + e.getMessage());
            return "redirect:/labors";
        }
        return "redirect:/labors/details/" + new ThymeleafUtil().encryptId(laborPayment.getLabor().getId());
    }

    @DeleteMapping("/payments/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        logger.info("Request to delete labor payment. Payment ID: {}", id);
        try {
            laborPaymentService.deleteById(id);
            logger.info("Labor payment deleted successfully. Payment ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to delete labor payment. Payment ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed");
        }
    }
}
