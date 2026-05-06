package com.fcar.modules.catalog.controller.admin;

import com.fcar.modules.branch.entity.Branch;
import com.fcar.modules.catalog.entity.CarColor;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarImportHistory;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.catalog.entity.CarModel;
import com.fcar.modules.branch.repository.BranchRepository;
import com.fcar.modules.catalog.repository.CarColorRepository;
import com.fcar.modules.catalog.repository.CarDefinitionRepository;
import com.fcar.modules.catalog.repository.CarImportHistoryRepository;
import com.fcar.modules.catalog.repository.CarInventoryRepository;
import com.fcar.modules.catalog.service.CarInventoryMergeService;
import com.fcar.modules.catalog.service.display.CarDefinitionLabelFormatter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/** Phân công: Hiếu — quản lý kho xe, nhập xe (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/cars")
public class AdminCarInventoryController {

    private final CarDefinitionRepository carDefinitionRepository;
    private final CarInventoryRepository carInventoryRepository;
    private final BranchRepository branchRepository;
    private final CarImportHistoryRepository carImportHistoryRepository;
    private final CarColorRepository carColorRepository;
    private final CarInventoryMergeService carInventoryMergeService;
    private final ObjectMapper objectMapper;
    private final CarDefinitionLabelFormatter carDefinitionLabelFormatter;

    @GetMapping("/import")
    public String showImportPage(
            @RequestParam(required = false) String historyYear,
            @RequestParam(required = false) String historyMonth,
            @RequestParam(required = false) String historyDay,
            @RequestParam(required = false) Long inventoryModelId,
            @RequestParam(required = false) String openHistory,
            Model model) {
        Integer y = parseIntOrNull(historyYear);
        Integer m = parseIntOrNull(historyMonth);
        Integer d = parseIntOrNull(historyDay);
        addImportCommonAttributes(model, y, m, d, inventoryModelId);
        ImportNewForm newForm = new ImportNewForm();
        newForm.setQuantity(1);
        model.addAttribute("newForm", newForm);
        model.addAttribute("openHistoryModal", "1".equals(openHistory));
        return "admin/cars/import";
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Chuẩn hóa bộ lọc: không có năm → bỏ tháng/ngày; ngày không hợp lệ → bỏ ngày (lọc theo cả tháng). */
    private static Integer[] normalizeHistoryFilter(Integer y, Integer m, Integer d) {
        if (y == null) {
            return new Integer[]{null, null, null};
        }
        if (m == null) {
            return new Integer[]{y, null, null};
        }
        if (d == null) {
            return new Integer[]{y, m, null};
        }
        try {
            LocalDate.of(y, m, d);
            return new Integer[]{y, m, d};
        } catch (DateTimeException e) {
            return new Integer[]{y, m, null};
        }
    }

    private void addImportCommonAttributes(Model model, Integer historyYear, Integer historyMonth, Integer historyDay,
                                           Long inventoryModelId) {
        Integer[] f = normalizeHistoryFilter(historyYear, historyMonth, historyDay);
        Integer y = f[0];
        Integer m = f[1];
        Integer d = f[2];

        List<CarDefinition> definitions = carDefinitionRepository.findAllWithRelations();
        model.addAttribute("definitions", definitions);

        List<CarColor> colorList = carColorRepository.findByCarDefinitionIn(definitions);
        Map<Long, List<String>> definitionColors = new HashMap<>();
        for (CarColor cc : colorList) {
            CarDefinition def = cc.getCarDefinition();
            if (def == null || def.getId() == null) continue;
            definitionColors.computeIfAbsent(def.getId(), k -> new ArrayList<>())
                    .add(cc.getColorValue() != null ? cc.getColorValue() : "");
        }
        model.addAttribute("definitionColors", definitionColors);

        model.addAttribute("branches", branchRepository.findByDeletedFalse());

        List<CarModel> inventoryModelOptions = carInventoryRepository.findDistinctModelsInInventory();
        model.addAttribute("inventoryModelOptions", inventoryModelOptions);
        model.addAttribute("inventoryFilterModelId", inventoryModelId);
        model.addAttribute("inventory", carInventoryRepository.findAllWithRelationsFiltered(inventoryModelId));

        LocalDate[] range = computeHistoryDateRange(y, m, d);
        model.addAttribute("history", carImportHistoryRepository.findAllWithRelationsFiltered(range[0], range[1]));

        model.addAttribute("historyFilterYear", y);
        model.addAttribute("historyFilterMonth", m);
        model.addAttribute("historyFilterDay", d);

        int cy = Year.now().getValue();
        List<Integer> historyYears = new ArrayList<>();
        for (int i = cy - 10; i <= cy + 1; i++) {
            historyYears.add(i);
        }
        model.addAttribute("historyYears", historyYears);

        putHistoryFilterCalendarJson(model);
    }

    /** Dữ liệu cho dropdown tháng/ngày phụ thuộc năm (theo dữ liệu lịch sử + fallback lịch). */
    private void putHistoryFilterCalendarJson(Model model) {
        List<LocalDate> distinctDates = carImportHistoryRepository.findDistinctImportDates();
        Map<Integer, TreeSet<Integer>> monthsByYear = new TreeMap<>();
        Map<String, TreeSet<Integer>> daysByYearMonth = new HashMap<>();
        for (LocalDate d : distinctDates) {
            monthsByYear.computeIfAbsent(d.getYear(), k -> new TreeSet<>()).add(d.getMonthValue());
            String key = d.getYear() + "-" + d.getMonthValue();
            daysByYearMonth.computeIfAbsent(key, k -> new TreeSet<>()).add(d.getDayOfMonth());
        }
        Map<Integer, List<Integer>> monthsJson = new LinkedHashMap<>();
        monthsByYear.forEach((yr, set) -> monthsJson.put(yr, new ArrayList<>(set)));
        Map<String, List<Integer>> daysJson = new LinkedHashMap<>();
        daysByYearMonth.forEach((k, set) -> daysJson.put(k, new ArrayList<>(set)));
        try {
            model.addAttribute("historyMonthsByYearJson", objectMapper.writeValueAsString(monthsJson));
            model.addAttribute("historyDaysByYearMonthJson", objectMapper.writeValueAsString(daysJson));
        } catch (JsonProcessingException e) {
            model.addAttribute("historyMonthsByYearJson", "{}");
            model.addAttribute("historyDaysByYearMonthJson", "{}");
        }
    }

    private static LocalDate[] computeHistoryDateRange(Integer y, Integer m, Integer d) {
        if (y == null) {
            return new LocalDate[]{null, null};
        }
        if (m == null) {
            return new LocalDate[]{LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31)};
        }
        if (d == null) {
            LocalDate first = LocalDate.of(y, m, 1);
            return new LocalDate[]{first, first.withDayOfMonth(first.lengthOfMonth())};
        }
        LocalDate exact = LocalDate.of(y, m, d);
        return new LocalDate[]{exact, exact};
    }

    private String redirectImport(Integer historyYear, Integer historyMonth, Integer historyDay, Long inventoryModelId) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/admin/cars/import");
        if (historyYear != null) {
            b.queryParam("historyYear", historyYear);
        }
        if (historyMonth != null) {
            b.queryParam("historyMonth", historyMonth);
        }
        if (historyDay != null) {
            b.queryParam("historyDay", historyDay);
        }
        if (inventoryModelId != null) {
            b.queryParam("inventoryModelId", inventoryModelId);
        }
        return "redirect:" + b.build().toUriString();
    }

    private String importFormWithErrors(Model model, ImportNewForm form, BindingResult bindingResult,
                                        String importSource, Integer historyYear, Integer historyMonth, Integer historyDay,
                                        Long inventoryModelId) {
        addImportCommonAttributes(model, historyYear, historyMonth, historyDay, inventoryModelId);
        model.addAttribute("newForm", form);
        model.addAttribute("importErrors", bindingResult.getAllErrors().stream()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : err.getCode())
                .toList());
        if ("stock".equals(importSource)) {
            model.addAttribute("openStockImportModal", true);
            maybeAddStockImportDisplayLabels(model, form);
        }
        model.addAttribute("openHistoryModal", false);
        return "admin/cars/import";
    }

    /** Nhãn hiển thị modal Nhập xe (dòng kho) khi validation lỗi. */
    private void maybeAddStockImportDisplayLabels(Model model, ImportNewForm form) {
        if (form.getCarDefinitionId() == null) {
            return;
        }
        carDefinitionRepository.findByIdWithRelations(form.getCarDefinitionId()).ifPresent(def ->
                model.addAttribute("stockImportCarLabel", carDefinitionLabelFormatter.format(def)));
        if (form.getBranchId() != null) {
            branchRepository.findById(form.getBranchId())
                    .ifPresent(b -> model.addAttribute("stockImportBranchLabel", b.getName()));
        }
        if (form.getColorCode() != null) {
            model.addAttribute("stockImportColorLabel", form.getColorCode());
        }
    }

    @PostMapping("/import/new")
    public String importNew(@Valid @ModelAttribute("newForm") ImportNewForm form,
                            BindingResult bindingResult,
                            Model model,
                            @RequestParam(value = "importSource", required = false) String importSource,
                            @RequestParam(required = false) Integer historyYear,
                            @RequestParam(required = false) Integer historyMonth,
                            @RequestParam(required = false) Integer historyDay,
                            @RequestParam(required = false) Long inventoryModelId,
                            RedirectAttributes redirectAttributes) {
        LocalDate today = LocalDate.now();
        if (form.getQuantity() == null || form.getQuantity() < 1) {
            bindingResult.rejectValue("quantity", "quantity.invalid", "Số lượng phải >= 1");
        }
        if (form.getPurchasePrice() == null) {
            bindingResult.rejectValue("purchasePrice", "price.invalid", "Giá nhập không được để trống");
        }
        if (bindingResult.hasErrors()) {
            return importFormWithErrors(model, form, bindingResult, importSource, historyYear, historyMonth, historyDay,
                    inventoryModelId);
        }

        LocalDate importDate = today;

        CarDefinition def = carDefinitionRepository.findById(form.getCarDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException("Car definition not found"));
        Branch branch = branchRepository.findById(form.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

        String colorInput = form.getColorCode() == null ? "" : form.getColorCode().trim();
        CarColor carColor = carColorRepository
                .findFirstByCarDefinitionAndColorValueIgnoreCase(def, colorInput)
                .orElse(null);
        if (carColor == null) {
            bindingResult.rejectValue("colorCode", "color.invalid",
                    "Màu không hợp lệ hoặc không thuộc mẫu xe đã chọn");
            return importFormWithErrors(model, form, bindingResult, importSource, historyYear, historyMonth, historyDay,
                    inventoryModelId);
        }

        Optional<CarInventory> existingOpt =
                carInventoryRepository.findByCarDefinitionAndBranchAndCarColor(def, branch, carColor);
        if (existingOpt.isPresent()) {
            CarInventory inv = existingOpt.get();
            inv.setQuantity(inv.getQuantity() + form.getQuantity());
            carInventoryRepository.save(inv);
        } else {
            CarInventory inventory = new CarInventory();
            inventory.setCarDefinition(def);
            inventory.setCarColor(carColor);
            inventory.setBranch(branch);
            inventory.setQuantity(form.getQuantity());
            inventory.setDisabled(false);
            carInventoryRepository.save(inventory);
        }

        CarImportHistory h = new CarImportHistory();
        h.setCarDefinition(def);
        h.setCarColor(carColor);
        h.setBranch(branch);
        h.setPurchasePrice(form.getPurchasePrice());
        h.setQuantity(form.getQuantity());
        h.setImportDate(importDate);
        carImportHistoryRepository.save(h);

        redirectAttributes.addFlashAttribute("success", "Nhập xe thành công.");
        Integer[] hf = normalizeHistoryFilter(historyYear, historyMonth, historyDay);
        return redirectImport(hf[0], hf[1], hf[2], inventoryModelId);
    }

    @PostMapping("/inventory/{id}/toggle-status")
    public String toggleInventoryStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Integer historyYear,
            @RequestParam(required = false) Integer historyMonth,
            @RequestParam(required = false) Integer historyDay,
            @RequestParam(required = false) Long inventoryModelId,
            RedirectAttributes redirectAttributes) {
        CarInventory inventory = carInventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
        inventory.setDisabled(!inventory.isDisabled());
        carInventoryRepository.save(inventory);
        Integer[] hf = normalizeHistoryFilter(historyYear, historyMonth, historyDay);
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái dòng kho.");
        return redirectImport(hf[0], hf[1], hf[2], inventoryModelId);
    }

    @GetMapping("/inventory/{id}/edit")
    public String editInventory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer historyYear,
            @RequestParam(required = false) Integer historyMonth,
            @RequestParam(required = false) Integer historyDay,
            @RequestParam(required = false) Long inventoryModelId,
            Model model) {
        CarInventory inventory = carInventoryRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

        InventoryEditForm form = new InventoryEditForm();
        form.setBranchId(inventory.getBranch() != null ? inventory.getBranch().getId() : null);
        form.setQuantity(inventory.getQuantity());

        model.addAttribute("inventory", inventory);
        model.addAttribute("form", form);
        model.addAttribute("branches", branchRepository.findByDeletedFalse());
        model.addAttribute("returnHistoryYear", historyYear);
        model.addAttribute("returnHistoryMonth", historyMonth);
        model.addAttribute("returnHistoryDay", historyDay);
        model.addAttribute("returnInventoryModelId", inventoryModelId);
        return "admin/cars/inventory-edit";
    }

    @PostMapping("/inventory/{id}")
    public String updateInventory(@PathVariable Long id,
                                  @Valid @ModelAttribute("form") InventoryEditForm form,
                                  BindingResult bindingResult,
                                  @RequestParam(required = false) Integer historyYear,
                                  @RequestParam(required = false) Integer historyMonth,
                                  @RequestParam(required = false) Integer historyDay,
                                  @RequestParam(required = false) Long inventoryModelId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("inventoryEditErrors", bindingResult.getAllErrors().stream()
                    .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : err.getCode())
                    .toList());
            CarInventory inventory = carInventoryRepository.findByIdWithRelations(id)
                    .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
            model.addAttribute("inventory", inventory);
            model.addAttribute("branches", branchRepository.findByDeletedFalse());
            model.addAttribute("returnHistoryYear", historyYear);
            model.addAttribute("returnHistoryMonth", historyMonth);
            model.addAttribute("returnHistoryDay", historyDay);
            model.addAttribute("returnInventoryModelId", inventoryModelId);
            return "admin/cars/inventory-edit";
        }

        CarInventory inventory = carInventoryRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

        Branch branch = branchRepository.findById(form.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

        Optional<CarInventory> duplicate = carInventoryRepository.findByCarDefinitionAndBranchAndCarColor(
                inventory.getCarDefinition(), branch, inventory.getCarColor());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            carInventoryMergeService.mergeInto(inventory, duplicate.get());
            Integer[] hf = normalizeHistoryFilter(historyYear, historyMonth, historyDay);
            redirectAttributes.addFlashAttribute("success", "Đã gộp và cập nhật kho thành công.");
            return redirectImport(hf[0], hf[1], hf[2], inventoryModelId);
        }

        inventory.setBranch(branch);
        inventory.setQuantity(form.getQuantity());
        carInventoryRepository.save(inventory);

        Integer[] hf = normalizeHistoryFilter(historyYear, historyMonth, historyDay);
        redirectAttributes.addFlashAttribute("success", "Cập nhật kho thành công.");
        return redirectImport(hf[0], hf[1], hf[2], inventoryModelId);
    }

    @Data
    public static class ImportNewForm {

        @NotNull(message = "Vui lòng chọn mẫu xe")
        private Long carDefinitionId;

        @NotNull(message = "Vui lòng chọn cửa hàng")
        private Long branchId;

        @NotNull
        private BigDecimal purchasePrice;

        @NotBlank(message = "Vui lòng chọn màu xe")
        private String colorCode;

        @NotNull
        @Min(1)
        private Integer quantity;
    }

    @Data
    public static class InventoryEditForm {

        @NotNull(message = "Vui lòng chọn cửa hàng")
        private Long branchId;

        @NotNull
        @Min(0)
        private Integer quantity;
    }
}
