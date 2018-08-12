/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.movement.movements;

import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.utils.BlockStateInterface;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class MovementDiagonal extends Movement {

    private static final double SQRT_2 = Math.sqrt(2);

    public MovementDiagonal(BlockPos start, EnumFacing dir1, EnumFacing dir2) {
        this(start, start.offset(dir1), start.offset(dir2), dir2);
        // super(start, start.offset(dir1).offset(dir2), new BlockPos[]{start.offset(dir1), start.offset(dir1).up(), start.offset(dir2), start.offset(dir2).up(), start.offset(dir1).offset(dir2), start.offset(dir1).offset(dir2).up()}, new BlockPos[]{start.offset(dir1).offset(dir2).down()});
    }

    public MovementDiagonal(BlockPos start, BlockPos dir1, BlockPos dir2, EnumFacing drr2) {
        this(start, dir1.offset(drr2), dir1, dir2);
    }

    public MovementDiagonal(BlockPos start, BlockPos end, BlockPos dir1, BlockPos dir2) {
        super(start, end, new BlockPos[]{dir1, dir1.up(), dir2, dir2.up(), end, end.up()}, new BlockPos[]{end.down()});
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
            case RUNNING:
                break;
            default:
                return state;
        }
        if (playerFeet().equals(dest)) {
            state.setStatus(MovementState.MovementStatus.SUCCESS);
            return state;
        }
        if (!BlockStateInterface.isLiquid(playerFeet())) {
            player().setSprinting(true);
        }
        MovementHelper.moveTowards(state, dest);
        return state;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        double lastPos = MovementHelper.getMiningDurationTicks(context.getToolSet(), positionsToBreak[4]) + MovementHelper.getMiningDurationTicks(context.getToolSet(), positionsToBreak[5]);
        if (lastPos != 0) {
            return COST_INF;
        }
        if (!MovementHelper.canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        double optionA = MovementHelper.getMiningDurationTicks(context.getToolSet(), positionsToBreak[0]) + MovementHelper.getMiningDurationTicks(context.getToolSet(), positionsToBreak[1]);
        double optionB = MovementHelper.getMiningDurationTicks(context.getToolSet(), positionsToBreak[2]) + MovementHelper.getMiningDurationTicks(context.getToolSet(), positionsToBreak[3]);
        if (optionA != 0 && optionB != 0) {
            return COST_INF;
        }
        if (optionA == 0) {
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[2]))) {
                return COST_INF;
            }
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[3]))) {
                return COST_INF;
            }
        }
        if (optionB == 0) {
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[0]))) {
                return COST_INF;
            }
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(positionsToBreak[1]))) {
                return COST_INF;
            }
        }
        double multiplier = 1;
        if (optionA != 0 || optionB != 0) {
            multiplier = 1.5; // TODO tune
        }
        return multiplier * SQRT_2 * (BlockStateInterface.isWater(src) || BlockStateInterface.isWater(dest) ? WALK_ONE_IN_WATER_COST : WALK_ONE_BLOCK_COST);
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }
}
