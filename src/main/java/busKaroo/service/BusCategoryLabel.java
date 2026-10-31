package busKaroo.service;

import busKaroo.model.Bus;

/** Customer-facing category for both current and legacy bus category records. */
public final class BusCategoryLabel {
    private BusCategoryLabel() {}

    public static String from(Bus bus) {
        String category = bus.getCategory() == null ? "" : bus.getCategory().getName();
        if (category.equalsIgnoreCase("Normal") || category.equalsIgnoreCase("Luxury") || category.equalsIgnoreCase("Sleeper")) {
            return category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
        }
        String details = (category + " " + bus.getModel() + " " + bus.getManufacturer()).toLowerCase();
        if (details.contains("sleeper")) return "Sleeper";
        if (details.contains("luxury") || details.contains("premium") || details.contains("electric coach")
                || details.contains("volvo") || details.contains("scania")
                || details.matches("(?s).*\\bac\\b.*")) return "Luxury";
        return "Normal";
    }
}
