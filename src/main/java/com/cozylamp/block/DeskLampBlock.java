package com.cozylamp.block;

import com.cozylamp.ModRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * Настольная лампа.
 *  - ПКМ: включить / выключить
 *  - ПКМ с зажатым Shift (пустые руки): сменить яркость (3 уровня тёплого света)
 *  - Редстоун-сигнал включает лампу, пропадание сигнала выключает
 *  - Включённая лампа пускает в конус света мягкие пылинки
 */
public class DeskLampBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DeskLampBlock> CODEC = simpleCodec(DeskLampBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty BRIGHTNESS = IntegerProperty.create("brightness", 1, 3);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(5, 0, 9, 11, 2, 15),          // основание
            Block.box(7, 2, 11, 9, 13, 13),         // стойка
            Block.box(7.5, 12.25, 4.5, 8.5, 13.25, 12), // рычаг
            Block.box(4.5, 7.5, 0.5, 11.5, 13.5, 7.5)); // абажур

    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public DeskLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(POWERED, false)
                .setValue(BRIGHTNESS, 3));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** Уровень света: 7 / 11 / 14 в зависимости от яркости. */
    public static int lightLevel(BlockState state) {
        if (!state.getValue(LIT)) return 0;
        return switch (state.getValue(BRIGHTNESS)) {
            case 1 -> 7;
            case 2 -> 11;
            default -> 14;
        };
    }

    // ---------- состояние / размещение ----------

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, POWERED, BRIGHTNESS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean powered = ctx.getLevel().hasNeighborSignal(ctx.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection())
                .setValue(POWERED, powered)
                .setValue(LIT, powered);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Стоит на чём угодно, у чего есть коллизия: столы, полки, тумбы, заборы, мебель из модов
        BlockPos below = pos.below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    // ---------- взаимодействие ----------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            int next = state.getValue(BRIGHTNESS) % 3 + 1;
            BlockState newState = state.setValue(BRIGHTNESS, next).setValue(LIT, true);
            if (!level.isClientSide) {
                level.setBlock(pos, newState, Block.UPDATE_ALL);
                level.playSound(null, pos, ModRegistry.LAMP_CLICK, SoundSource.BLOCKS, 0.6f, 0.75f + 0.12f * next);
                level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
                String bars = "\u25A0".repeat(next) + "\u25A1".repeat(3 - next);
                player.displayClientMessage(Component.translatable("message.cozylamp.brightness", bars), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        boolean turnOn = !state.getValue(LIT);
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(LIT, turnOn), Block.UPDATE_ALL);
            level.playSound(null, pos, ModRegistry.LAMP_CLICK, SoundSource.BLOCKS, 0.7f, turnOn ? 1.0f : 0.8f);
            level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean moved) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;

        BlockState next = state.setValue(POWERED, powered);
        if (state.getValue(LIT) != powered) {
            next = next.setValue(LIT, powered);
            level.playSound(null, pos, ModRegistry.LAMP_CLICK, SoundSource.BLOCKS, 0.7f, powered ? 1.0f : 0.8f);
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
    }

    // ---------- атмосфера: пылинки в свете лампы ----------

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(3) != 0) return;

        Direction f = state.getValue(FACING);
        // центр абажура смещён от центра блока на 0.25 в сторону, куда смотрит лампа
        double cx = pos.getX() + 0.5 + f.getStepX() * 0.25;
        double cz = pos.getZ() + 0.5 + f.getStepZ() * 0.25;

        double x = cx + (random.nextDouble() - 0.5) * 0.75;
        double y = pos.getY() + 0.05 + random.nextDouble() * 0.45;
        double z = cz + (random.nextDouble() - 0.5) * 0.75;

        float scale = 0.3f + random.nextFloat() * 0.15f;
        DustParticleOptions mote = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.45f), scale);
        level.addParticle(mote, x, y, z, 0.0, 0.002, 0.0);
    }

    // ---------- форма ----------

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            map.put(d, rotate(SHAPE_NORTH, d));
        }
        return map;
    }

    private static VoxelShape rotate(VoxelShape shape, Direction dir) {
        VoxelShape[] out = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double[] r = switch (dir) {
                case EAST  -> new double[]{1 - z2, 1 - z1, x1, x2};
                case SOUTH -> new double[]{1 - x2, 1 - x1, 1 - z2, 1 - z1};
                case WEST  -> new double[]{z1, z2, 1 - x2, 1 - x1};
                default    -> new double[]{x1, x2, z1, z2};
            };
            out[0] = Shapes.or(out[0], Shapes.box(r[0], y1, r[2], r[1], y2, r[3]));
        });
        return out[0];
    }
}
