# Alpha 13 — focused horror regression QA
1. At night /sleepdebt debug spawn_sleepless, view face and chest straight-on:
   actual front-facing darkness/teeth/ribs should be visible. No UV misalignment.
2. Walk away: the new skitter event must replace (not overlay) Minecraft soul-sand
   steps; cadence slightly irregular, sometimes absent.
3. Let the hunter land four hits: one rarely triggered high-intensity screech
   should happen on the fourth; ordinary attacks use the existing impact cue.
4. Approach slowly: soundscape grows, with isolated, positional, infrequent
   throat rattles placed behind the camera and a low heartbeat at close range.
5. Set sleeplessNearVignette=false while sleeplessHorrorAudio=true: hear
   sounds without world fog. The inverse (audio=false, aura=true) must work.
6. Verify 8 audio files under assets/sleepdebt/sounds/sleepless in JAR, and
   references in sounds.json. All samples derived from pinned CC0 sources.
7. At sunrise/sleep/death: audio should stop and renderer fade out promptly.
8. Test headphones at Hostile and Ambient volume 30%, 70%, 100%, with subtitles.
9. Build and static checks are not substitute for actually inspecting sound
   levels, 3D anatomy, or modpack shader compatibility.
