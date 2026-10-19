package dev.anvilborn.forge;

public record Aspect(String id, String family, String effect, double amount, int weight, boolean harmful) {
    public String translationKey() { return "aspect.anvilborn." + id; }
    public boolean supports(String category) { return family.equals("any") || family.equals(category); }
}
