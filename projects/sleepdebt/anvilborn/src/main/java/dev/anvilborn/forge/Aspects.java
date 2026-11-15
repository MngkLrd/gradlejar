package dev.anvilborn.forge;

import net.minecraft.util.RandomSource;
import java.util.*;

public final class Aspects {
    private static final LinkedHashMap<String, Aspect> ALL = new LinkedHashMap<>();
    private static void add(String id, String family, String effect, double amount, int weight, boolean harmful) {
        ALL.put(id, new Aspect(id, family, effect, amount, weight, harmful));
    }
    static {
        // Ten aspects per family; each has a gameplay/stat effect and roll weight.
        add("cleaver","weapon","damage",0.055,12,false);
        add("bloodletter","weapon","bleed",0.04,8,false);
        add("executioner","weapon","execute",0.075,8,false);
        add("sunder","weapon","damage",0.085,6,false);
        add("frenzied","weapon","speed",0.05,10,false);
        add("duelist","weapon","speed",0.075,7,false);
        add("leeching","weapon","leech",0.045,6,false);
        add("crusher","weapon","knockback",0.11,9,false);
        add("ember_edge","weapon","fire",0.035,8,false);
        add("savage","weapon","damage",0.11,4,false);

        add("miner","tool","mining",0.075,13,false);
        add("geologist","tool","xp",0.15,10,false);
        add("nimble","tool","mining",0.13,8,false);
        add("conservator","tool","preserve",0.065,8,false);
        add("deep_drill","tool","deep_mining",0.18,7,false);
        add("precision","tool","mining",0.10,9,false);
        add("prospector","tool","xp",0.24,5,false);
        add("lightweight","tool","speed",0.025,11,false);
        add("steady_hand","tool","preserve",0.10,6,false);
        add("overdrive","tool","mining",0.22,4,false);

        add("bastion","armor","armor",0.055,10,false);
        add("reinforced","armor","toughness",0.09,8,false);
        add("fleetfoot","armor","movement",0.022,9,false);
        add("bulwark","armor","armor",0.085,6,false);
        add("vitality","armor","health",0.025,7,false);
        add("fortitude","armor","toughness",0.14,6,false);
        add("featherfall","armor","fall",0.055,8,false);
        add("stability","armor","resistance",0.08,7,false);
        add("warded","armor","guard",0.035,7,false);
        add("second_wind","armor","guard",0.07,4,false);

        add("marksman","ranged","ranged",0.07,10,false);
        add("hawk_eye","ranged","ranged",0.09,8,false);
        add("headhunter","ranged","execute",0.055,8,false);
        add("venom_tip","ranged","poison",0.06,7,false);
        add("barbed","ranged","bleed",0.055,9,false);
        add("incendiary","ranged","fire",0.05,6,false);
        add("hunter","ranged","ranged",0.11,8,false);
        add("sharpshooter","ranged","ranged",0.15,5,false);
        add("pursuit","ranged","slow",0.055,7,false);
        add("eagle_eye","ranged","ranged",0.19,4,false);

        add("balanced","any","luck",0.25,10,false);
        add("masterwork","any","preserve",0.05,7,false);
        add("steady","any","resistance",0.02,8,false);
        add("lucky","any","luck",0.45,5,false);
        add("tempered","any","health",0.01,8,false);
        add("flawed","any","damage",-0.05,9,true);
        add("brittle","any","preserve",-0.12,9,true);
        add("heavy","any","movement",-0.025,10,true);
        add("unstable","any","guard",-0.06,8,true);
        add("blunted","any","mining",-0.10,10,true);
    }
    public static final List<Aspect> VALUES = List.copyOf(ALL.values());
    public static Aspect byId(String id) { return ALL.get(id); }
    public static ForgeData.Roll roll(RandomSource random, String family, List<ForgeData.Roll> existing, boolean cursed, int level) {
        List<Aspect> pool = VALUES.stream()
            .filter(a -> a.supports(family) && a.harmful() == cursed)
            // Universal curses must still have a real impact on the rolled item category.
            .filter(a -> !a.harmful() || switch (family) {
                case "armor" -> a.effect().equals("movement") || a.effect().equals("guard");
                case "weapon" -> a.effect().equals("damage") || a.effect().equals("preserve") || a.effect().equals("movement");
                case "tool" -> a.effect().equals("mining") || a.effect().equals("preserve") || a.effect().equals("movement");
                case "ranged" -> a.effect().equals("preserve") || a.effect().equals("movement");
                default -> a.effect().equals("movement") || a.effect().equals("preserve");
            })
            .filter(a -> existing.stream().noneMatch(r -> r.id().equals(a.id())))
            .toList();
        int weight = pool.stream().mapToInt(Aspect::weight).sum();
        if (weight <= 0) return null;
        int index = random.nextInt(weight);
        for (Aspect a : pool) {
            index -= a.weight();
            if (index < 0) return new ForgeData.Roll(a.id(), level >= 7 ? 3 : level >= 3 ? 2 : 1);
        }
        return null;
    }
    private Aspects() {}
}
