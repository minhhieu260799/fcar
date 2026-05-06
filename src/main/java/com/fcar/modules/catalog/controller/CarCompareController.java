package com.fcar.modules.catalog.controller;

import com.fcar.modules.catalog.entity.CarAttribute;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.service.CarQueryService;
import com.fcar.modules.catalog.service.CarQueryService.ListedCarDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Phân công: Hiệp Hiếu — so sánh xe. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/cars")
public class CarCompareController {

    private final CarQueryService carQueryService;

    @GetMapping("/compare")
    public String compare(@RequestParam(value = "ids", required = false) String ids, Model model) {
        List<CarDefinition> definitions = new ArrayList<>();
        if (ids != null && !ids.isBlank()) {
            for (String part : ids.split(",")) {
                if (part.isBlank()) {
                    continue;
                }
                try {
                    Long id = Long.valueOf(part.trim());
                    carQueryService.findListedDefinitionForDetail(id).ifPresent(definitions::add);
                    if (definitions.size() >= 3) {
                        break;
                    }
                } catch (NumberFormatException ignored) {
                    // skip
                }
            }
        }

        Set<Long> selectedIds = definitions.stream().map(CarDefinition::getId).collect(Collectors.toCollection(HashSet::new));

        TreeSet<String> extraAttrNameSet = new TreeSet<>();
        Map<Long, Map<String, String>> extraAttrByDefId = new HashMap<>();
        for (CarDefinition d : definitions) {
            Map<String, String> byName = new HashMap<>();
            if (d.getAttributes() != null) {
                for (CarAttribute a : d.getAttributes()) {
                    if (a.getName() == null || a.getName().isBlank()) {
                        continue;
                    }
                    String key = a.getName().trim();
                    extraAttrNameSet.add(key);
                    byName.put(key, a.getValue() != null ? a.getValue() : "");
                }
            }
            extraAttrByDefId.put(d.getId(), byName);
        }
        List<String> compareExtraAttributeNames = new ArrayList<>(extraAttrNameSet);

        List<ListedCarDefinition> pickerSource = carQueryService.listDefinitionsForComparePicker(200);
        List<ListedCarDefinition> modalPickerCars = pickerSource.stream()
                .filter(p -> !selectedIds.contains(p.definition().getId()))
                .collect(Collectors.toList());

        List<CarDefinition> slotCars = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            slotCars.add(i < definitions.size() ? definitions.get(i) : null);
        }

        model.addAttribute("definitions", definitions);
        model.addAttribute("slotCars", slotCars);
        model.addAttribute("extraAttrByDefId", extraAttrByDefId);
        model.addAttribute("compareExtraAttributeNames", compareExtraAttributeNames);
        model.addAttribute("modalPickerCars", modalPickerCars);
        model.addAttribute("canAddMore", definitions.size() < 3);
        model.addAttribute("title", "So sánh xe");
        return "cars/compare";
    }
}
