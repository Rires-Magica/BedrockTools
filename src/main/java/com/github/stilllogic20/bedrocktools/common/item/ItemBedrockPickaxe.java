package com.github.stilllogic20.bedrocktools.common.item;

import java.util.Objects;

import com.github.stilllogic20.bedrocktools.BedrockToolsMod;
import com.github.stilllogic20.bedrocktools.common.BedrockToolsMaterial;
import com.github.stilllogic20.bedrocktools.common.init.Items;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemBedrockPickaxe extends ItemPickaxe {

    private static final float ATTACK_DAMAGE = 22F;
    private static final String NAME = "bedrock_pickaxe";

    static enum ItemMode {
        NORMAL(20F),
        MIDDLE(12F),
        SLOW(8F),
        FAST(128F),
        INSANE(Float.MAX_VALUE),
        OFF(0F);

        private final float efficiency;

        private ItemMode(float efficiency) {
            this.efficiency = efficiency;
        }

        public ItemMode next() {
            final ItemMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

    }

    private volatile ItemMode mode = ItemMode.NORMAL;

    public ItemBedrockPickaxe() {
        super(BedrockToolsMaterial.BEDROCK);
        setCreativeTab(Items.CREATIVE_TAB);
        setTranslationKey(NAME);
        setRegistryName(BedrockToolsMod.MODID, NAME);
        setHarvestLevel("pickaxe", -1);
    }

    @Override
    public boolean canHarvestBlock(IBlockState blockState) {
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack item, IBlockState blockState) {
        final ItemMode mode = this.mode;
        assert mode != null;
        return mode.efficiency;
    }

    @Override
    public boolean hitEntity(ItemStack item, EntityLivingBase target, EntityLivingBase attacker) {
        if (attacker instanceof EntityPlayer) {
            target.attackEntityFrom(DamageSource.OUT_OF_WORLD, ATTACK_DAMAGE * 0.9F);
            target.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), ATTACK_DAMAGE * 0.1F);
        } else {
            target.attackEntityFrom(DamageSource.causeMobDamage(attacker), ATTACK_DAMAGE);
        }
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack item = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                mode = mode.next();
                player.sendMessage(
                        new TextComponentString(
                                String.format("[BedrockTools] Mode: %s (%.0f)", mode.name(), mode.efficiency)));
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, item);
        }
        return new ActionResult<>(EnumActionResult.PASS, item);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state != null && !worldIn.isRemote) {
            final Block block = state.getBlock();
            if (Objects.equals(block, Blocks.BEDROCK)) {
                block.onBlockHarvested(worldIn, pos, state, player);
                worldIn.setBlockToAir(pos);
                worldIn.playEvent(null, 2001, pos, Block.getStateId(state));
                block.breakBlock(worldIn, pos, state);
                worldIn.spawnEntity(new EntityItem(worldIn, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(block)));
            }
        }
        return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }

}