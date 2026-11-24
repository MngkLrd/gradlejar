from pathlib import Path
import sys
root=Path(sys.argv[1])
fabric=sys.argv[2]=='fabric'
pkg=root/'src/main/java/dev/minemods/campfirebatch'
probe=Path('tools/campfirebatch-runtime/RuntimeProbe.java').read_text()
if fabric:
    callback='return net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(player, level, hand, hit);'
else:
    callback='var event = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, hand, pos, hit); net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event); return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;'
(pkg/'RuntimeProbe.java').write_text(probe.replace('/* CALLBACK */', callback))
entry=pkg/'CampfireBatch.java'
s=entry.read_text()
if fabric:
    old='UseBlockCallback.EVENT.register(BatchLoader::interact);'
    new=old+'\n        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(RuntimeProbe::run);'
else:
    old='NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CampfireBatch::onRightClick);'
    new=old+'\n        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> RuntimeProbe.run(event.getServer()));'
    b=root/'build.gradle'
    g=b.read_text()
    assert 'runs { client { client() } }' in g
    b.write_text(g.replace('runs { client { client() } }','runs { client { client() }; server { server() } }'))
assert old in s
entry.write_text(s.replace(old,new))
