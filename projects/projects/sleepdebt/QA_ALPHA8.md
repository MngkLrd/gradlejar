# Sleep Debt 3.0.0-alpha.8 quick QA

1. In Creative, the Sleep Debt tab appears before the vanilla Food & Drinks tab
   with the Sleep Journal icon and localized "Sleep Debt" title.
2. The tab contains exactly the 29 registered items, once each, including
   Sleep Journal, Sleepless spawn egg, all 6 block-items and all drinks.
3. Switch to Survival and run /sleepdebt debug preset tired, exhausted, critical.
   Check status and a new entry in the rest journal; no game restart needed.
4. Use /sleepdebt debug preset rested to clear all three primary needs.
5. Check that /sleepdebt debug set 950 850 900 still works.
6. Use /tick rate 100 then /tick rate 20 and verify client and server clocks.
7. In a night area with loaded surrounding chunks, check world-space mirages
   at debt >=650; ensure screen is closed and masking caffeine has worn off.
8. The Sleepless AI only targets players whose debt is >=620; its natural
   spawn also requires nighttime and low light. Test spawn egg independently.
9. /sleepdebt showcase creates a camp where solid ground and space exist.
10. Dedicated-server startup must not classload client-only Creative screen code.
