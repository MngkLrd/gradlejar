package dev.timberfall.tree;
import dev.timberfall.block.NotchedLogBlock;
import dev.timberfall.entity.TreePiece;
import dev.timberfall.entity.TopplingTreeEntity;
import dev.timberfall.registry.TimberfallBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.List;
public final class ChopEvents {
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand()!=InteractionHand.MAIN_HAND || !event.getItemStack().is(ItemTags.AXES)) return;
        BlockPos pos=event.getPos();
        BlockState state=event.getLevel().getBlockState(pos);
        Block original=state.getBlock();
        String wood=TimberfallBlocks.woodType(original);
        boolean notched=wood!=null;
        if(!notched){
            if(!state.is(BlockTags.LOGS))return;
            Identifier id=BuiltInRegistries.BLOCK.getKey(original);
            String path=id.getPath();
            wood=path.endsWith("_log")?path.substring(0,path.length()-4):
                 path.endsWith("_stem")?path.substring(0,path.length()-5):"";
            if(!id.getNamespace().equals("minecraft") || TimberfallBlocks.notch(wood)==null)return;
            if(state.hasProperty(RotatedPillarBlock.AXIS) && state.getValue(RotatedPillarBlock.AXIS)!=Direction.Axis.Y)return;
        }
        Block notch=TimberfallBlocks.notch(wood);
        if(notch==null)return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if(!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level))return;
        String vanillaId=(wood.equals("crimson")||wood.equals("warped"))?wood+"_stem":wood+"_log";
        Block fullLog=BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(vanillaId));
        if(fullLog==null||fullLog==Blocks.AIR)return;
        int stage=notched?state.getValue(NotchedLogBlock.STAGE):0;
        Direction facing=notched?state.getValue(NotchedLogBlock.FACING):event.getHitVec().getDirection();
        if(!facing.getAxis().isHorizontal())facing=player.getDirection().getOpposite();
        if(stage<3){
            BlockState next=notch.defaultBlockState()
                .setValue(NotchedLogBlock.FACING,facing)
                .setValue(NotchedLogBlock.STAGE,stage+1);
            level.setBlock(pos,next,3);
            player.displayClientMessage(Component.literal("Timberfall  "+(stage+1)+"/3"),true);
            level.playSound(null,pos,SoundEvents.WOOD_HIT,SoundSource.BLOCKS,0.85f,0.8f+stage*0.08f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,fullLog.defaultBlockState()),
                pos.getX()+0.5,pos.getY()+0.55,pos.getZ()+0.5,12,0.32,0.23,0.32,0.08);
            event.getItemStack().hurtAndBreak(1,player,InteractionHand.MAIN_HAND);
            return;
        }
        List<TreePiece> pieces=TreeScanner.scan(level,pos,fullLog);
        if(pieces.isEmpty()){
            level.setBlock(pos,fullLog.defaultBlockState(),3);
            player.displayClientMessage(Component.literal("Timberfall: no natural upright trunk"),true);
            return;
        }
        TopplingTreeEntity falling=TopplingTreeEntity.create(level,pos,player.getDirection(),pieces);
        if(!level.addFreshEntity(falling))return;
        for(TreePiece part:pieces){
            BlockPos at=pos.offset(part.x(),part.y(),part.z());
            if(part.state().is(BlockTags.LEAVES))Block.dropResources(level.getBlockState(at),level,at);
            level.setBlock(at,Blocks.AIR.defaultBlockState(),3);
        }
        level.playSound(null,pos,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1.6f,0.7f);
        event.getItemStack().hurtAndBreak(Math.min(pieces.size(),20),player,InteractionHand.MAIN_HAND);
        player.displayClientMessage(Component.literal("Timberfall   TREE DOWN"),true);
    }
}