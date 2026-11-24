# Anvil Right Click Repair 0.1.0
Minecraft 1.21.1 / NeoForge / Java 21 / Gradle 9.2.1 / Mojang mappings.
Hold damaged equipment in the main hand and its normal anvil repair ingredient in the offhand. Right-click any vanilla anvil. One material and one level restore up to 25% maximum durability; 12% chance of anvil wear. Creative is free. Existing components, enchantments and prior-work value are unchanged. No prior-work surcharge is charged by this shortcut. Ordinary use without the valid pair is unchanged; sneaking bypasses quick repair and retains vanilla sneak interaction. No custom repair recipes or combining duplicate tools. No client-only code or custom packets.
Build: gradle clean build
JAR: build/libs/anvilrightclickrepair-neoforge-1.21.1-0.1.0.jar
Runtime tests: diamond pickaxe + diamond; iron armor + iron ingot; wrong ingredient; fully repaired item; zero XP; last ingredient; normal GUI; creative; chipped/damaged anvil; enchanted item; dedicated server; protected block; modded item with normal repair ingredient. Runtime tests pending.
