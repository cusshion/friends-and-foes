package com.faboslav.friendsandfoes.common.entity.ai.brain.task.coppergolem;

import com.faboslav.friendsandfoes.common.client.render.entity.animation.CopperGolemAnimations;
import com.faboslav.friendsandfoes.common.entity.CopperGolemEntity;
import com.faboslav.friendsandfoes.common.entity.ai.brain.CopperGolemBrain;
import com.faboslav.friendsandfoes.common.entity.pose.CopperGolemEntityPose;
import com.faboslav.friendsandfoes.common.init.FriendsAndFoesMemoryModuleTypes;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CopperGolemPressButtonTask extends Task<CopperGolemEntity>
{
	private final static float WITHING_DISTANCE = 2.0F;
	private int pressButtonTicks = 0;
	private int minPressButtonTick = 0;
	private int maxPressButtonTicks = 0;
	private boolean wasButtonPressed = false;
	private int heightDifference = 0;

	public CopperGolemPressButtonTask() {
		super(
			Map.of(
				FriendsAndFoesMemoryModuleTypes.COPPER_GOLEM_BUTTON_POS.get(), MemoryModuleState.VALUE_PRESENT,
				FriendsAndFoesMemoryModuleTypes.COPPER_GOLEM_IS_OXIDIZED.get(), MemoryModuleState.VALUE_ABSENT
			), 100
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, CopperGolemEntity copperGolem) {
		return copperGolem.getButtonPos() != null
			   && copperGolem.isButtonValidToBePressed(copperGolem.getButtonPos().getPos())
			   && copperGolem.getButtonPos().getPos().isWithinDistance(copperGolem.getPos(), WITHING_DISTANCE)
			   && !copperGolem.getNavigation().isFollowingPath();
	}

	@Override
	protected void run(ServerWorld world, CopperGolemEntity copperGolem, long time) {
		this.pressButtonTicks = 0;
		this.wasButtonPressed = false;

		GlobalPos buttonPos = copperGolem.getButtonPos();

		if (buttonPos == null) {
			return;
		}

		this.heightDifference = buttonPos.getPos().getY() - copperGolem.getBlockPos().getY();

		if (this.heightDifference >= 1) {
			this.maxPressButtonTicks = CopperGolemAnimations.getPressButtonUpKeyframeAnimation(copperGolem.getAnimationSpeedModifier()).getAnimationLengthInTicks();
		} else {
			this.maxPressButtonTicks = CopperGolemAnimations.getPressButtonDownKeyframeAnimation(copperGolem.getAnimationSpeedModifier()).getAnimationLengthInTicks();
		}

		this.minPressButtonTick = copperGolem.getRandom().nextBetween((int) (this.maxPressButtonTicks * 0.4), (int) (this.maxPressButtonTicks * 0.6));
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, CopperGolemEntity copperGolem, long time) {
		return this.pressButtonTicks <= this.maxPressButtonTicks
			   && !copperGolem.isOxidized();
	}

	protected void keepRunning(ServerWorld world, CopperGolemEntity copperGolem, long time) {
		this.pressButtonTicks++;

		if (this.heightDifference >= 1) {
			copperGolem.startPressButtonUpAnimation();
		} else {
			copperGolem.startPressButtonDownAnimation();
		}

		if (
			copperGolem.getOxidationLevel().ordinal() >= Oxidizable.OxidationLevel.WEATHERED.ordinal()
			&& copperGolem.getRandom().nextFloat() > 0.99
		) {
			copperGolem.setOxidationLevel(Oxidizable.OxidationLevel.OXIDIZED);
		}

		if (pressButtonTicks < this.minPressButtonTick) {
			return;
		}

		if (this.wasButtonPressed) {
			return;
		}

		GlobalPos buttonPos = copperGolem.getButtonPos();

		if (buttonPos == null) {
			return;
		}

		this.wasButtonPressed = this.tryToPressButton(copperGolem, buttonPos.getPos());
	}

	@Override
	protected void finishRunning(ServerWorld world, CopperGolemEntity copperGolem, long time) {
		copperGolem.setPose(CopperGolemEntityPose.IDLE);
		copperGolem.getBrain().forget(FriendsAndFoesMemoryModuleTypes.COPPER_GOLEM_BUTTON_POS.get());
		CopperGolemBrain.setPressButtonCooldown(copperGolem);
	}

	private boolean tryToPressButton(CopperGolemEntity copperGolem, BlockPos buttonPos) {
		BlockState buttonBlockState = this.getButtonBlockState(copperGolem, buttonPos);

		if (
			buttonBlockState == null
			|| buttonBlockState.get(AbstractButtonBlock.POWERED)
		) {
			return false;
		}

		AbstractButtonBlock buttonBlock = (AbstractButtonBlock) buttonBlockState.getBlock();

		buttonBlock.powerOn(
			buttonBlockState,
			copperGolem.getWorld(),
			buttonPos
		);
		copperGolem.getEntityWorld().playSound(null,
			buttonPos,
			buttonBlock.getClickSound(true),
			SoundCategory.BLOCKS,
			0.3F,
			0.6F
		);
		copperGolem.getEntityWorld().emitGameEvent(copperGolem,
			GameEvent.BLOCK_ACTIVATE,
			buttonPos
		);


		return true;
	}

	@Nullable
	private BlockState getButtonBlockState(CopperGolemEntity copperGolem, BlockPos buttonBlockPos) {
		ServerWorld serverWorld = (ServerWorld) copperGolem.getEntityWorld();
		BlockState blockState = serverWorld.getBlockState(buttonBlockPos);

		if (!(blockState.getBlock() instanceof AbstractButtonBlock)) {
			return null;
		}

		return blockState;
	}
}
