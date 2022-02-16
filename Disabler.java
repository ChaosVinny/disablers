import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S2APacketParticles;
import org.lwjgl.input.Keyboard;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Disabler extends Module {


    private final TimerUtil timer = new TimerUtil();
    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final ModeValue<Mode> mode = new ModeValue<>("Mode", Mode.VERUS, this);
    private final ModeValue<PACKET_TYPE> packet = new ModeValue<>("VPacket", PACKET_TYPE.EXTRA, this);
    private long nextTime;

    @Override
    public String getName() {
        return "Disabler";
    }

    @Override
    public String getDescription() {
        return "Disabilita Alcuni AntiCheat";
    }

    @Override
    public int getKey() {
        return Keyboard.KEY_G;
    }

    @Override
    public Category getCategory() {
        return Category.FUN;
    }

    @Override
    public void onEnable() {
        if (this.mode.getValue().equals(Mode.ALICE)) {
            MotionUtil.sendDirect(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, Double.MAX_VALUE, mc.thePlayer.posZ, false));
            mc.thePlayer.setPosition(mc.thePlayer.posX, Double.MAX_VALUE, mc.thePlayer.posZ);
            mc.renderGlobal.loadRenderers();
        }
        this.timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.packetQueue.clear();
        super.onDisable();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof Update) {
            switch (this.mode.getValue()) {
                case VERUS:
                    if ((mc.thePlayer != null) && mc.thePlayer.ticksExisted < 5) {
                        this.packetQueue.clear();
                        this.timer.reset();
                    }
                    if (timer.sleep(nextTime) && mc.thePlayer.isMovingOnGround() && !mc.thePlayer.isCollidedVertically) {
                        this.nextTime = (long) (310L + Math.random());
                        if (!packetQueue.isEmpty() && packetQueue.size() > 42) {
                            MotionUtil.sendDirect(packetQueue.poll());
                            packetQueue.clear();
                        }
                        timer.reset();
                    }
                    break;
                case DUPLICATE:
                    if (mc.thePlayer.ticksExisted % 3 == 1) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                    }
                    break;
            }
        }
        if (event instanceof PacketSend) {
            if (mc.isSingleplayer()) return;
            Packet<?> packet = ((PacketSend) event).getPacket();
            switch (this.mode.getValue()) {
                case RIDING:
                    if (packet instanceof C03PacketPlayer) {
                        MotionUtil.sendDirect(new C18PacketSpectate(mc.thePlayer.getGameProfile().getId()));
                        MotionUtil.sendDirect(new C0CPacketInput());
                        final PlayerCapabilities capabilities = new PlayerCapabilities();
                        capabilities.allowFlying = true;
                        capabilities.disableDamage = true;
                        capabilities.isFlying = true;
                        capabilities.isCreativeMode = true;
                        capabilities.allowEdit = true;
                        capabilities.setFlySpeed(Float.POSITIVE_INFINITY);
                        capabilities.setPlayerWalkSpeed(Float.POSITIVE_INFINITY);
                        MotionUtil.sendDirect(new C13PacketPlayerAbilities(capabilities));
                    }
                    if (packet instanceof C0FPacketConfirmTransaction || packet instanceof C00PacketKeepAlive) {
                        ((PacketSend) event).setCancelled(true);
                    }
                    break;
                case ALICE:
                    if (packet instanceof C03PacketPlayer) {
                        MotionUtil.sendDirect(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
                    }
                    break;
                case NEGATVITY:
                    if (packet instanceof C03PacketPlayer) {
                        C03PacketPlayer position = (C03PacketPlayer) packet;
                        if (mc.thePlayer.ticksExisted % 12 == 0) {
                            position.setY(mc.thePlayer.posY - 11.0D);
                            position.setOnGround(true);
                        }
                    }
                    break;
                case VERUS:
                    if(packet instanceof C00PacketKeepAlive){
                        for (int i = 0; i < 2; i++) {
                            this.packetQueue.add(packet);
                        }
                        ((PacketSend) event).setCancelled(true);
                    }
                    if (packet instanceof C0FPacketConfirmTransaction) {
                        final C0FPacketConfirmTransaction CONFIRM = (C0FPacketConfirmTransaction) packet;
                        boolean block = mc.currentScreen instanceof GuiInventory;
                        if (block && CONFIRM.getUid() > 0 && CONFIRM.getUid() < 100) return;
                        for (int i = 0; i < 4; i++) {
                            this.packetQueue.add(CONFIRM);
                        }
                        ((PacketSend) event).setCancelled(true);
                    }
                    if (packet instanceof C03PacketPlayer) {
                        MotionUtil.sendDirect(new C18PacketSpectate(mc.thePlayer.getGameProfile().getId()));
                        MotionUtil.sendDirect(new C0CPacketInput());
                        final double offset = -0.015625f;
                        if (mc.currentScreen instanceof GuiContainer) return;
                        boolean canTicked = mc.thePlayer.ticksExisted % 64 == 0;
                        boolean canSendPacket = canTicked && intentionalMove() && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isJumping && !mc.thePlayer.isCollidedHorizontally && mc.thePlayer.hurtTime <= 0 && !doHittingProcess();
                        if (canSendPacket) {
                            switch (this.packet.getValue()) {
                                case EXTRA:
                                    MotionUtil.sendDirect(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, offset, mc.thePlayer.posZ, mc.thePlayer.onGround));
                                    break;
                                case CURRENT:
                                    ((C03PacketPlayer) packet).setY(offset);
                                    ((C03PacketPlayer) packet).setOnGround(false);
                                    ((C03PacketPlayer) packet).setMoving(false);
                                    break;
                            }
                        }
                        //if(mc.thePlayer.ticksExisted % 33 == 0)
                            /*
                                private int entityID;
                            private int motionX;
                            private int motionY;
                                    private int motionZ;
                             */
                        // mc.getNetHandler().getNetworkManager().sendPacket(new S12PacketEntityVelocity(mc.thePlayer.getEntityId(),mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ));
                        //(double xIn, double yIn, double zIn, float yawIn, float pitchIn, Set<S08PacketPlayerPosLook.EnumFlags> p_i45993_9_
                        // mc.getNetHandler().getNetworkManager().dispatchPacket(new S08PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,  null), null);
                        //mc.getNetHandler().addToSendQueue(new S08PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,  null));
                        setPremissionFly();
                    }
                    break;
                case SPECTATE:
                    if (packet instanceof C03PacketPlayer) {
                        MotionUtil.sendDirect(new C18PacketSpectate(mc.thePlayer.getGameProfile().getId()));
                        MotionUtil.sendDirect(new C0CPacketInput());
                    }
                    if (packet instanceof C00PacketKeepAlive || packet instanceof C0FPacketConfirmTransaction)
                        ((PacketSend) event).setCancelled(true);
                    break;
            }
        }
        if (event instanceof PacketReceive) {
            if (mc.isSingleplayer()) return;
            Packet<?> packet = ((PacketReceive) event).getPacket();
            switch (this.mode.getValue()) {
                case VERUS:
                    if (packet instanceof S2APacketParticles) {
                        ((PacketReceive) event).setCancelled(true);
                    }
                    if (packet instanceof S08PacketPlayerPosLook) {
                        S08PacketPlayerPosLook packet8 = (S08PacketPlayerPosLook) packet;
                        packet8.y += 1.0E-4D;//yaw or y?
                    }
                    break;
                case ALICE:
                    if (packet instanceof S08PacketPlayerPosLook && mc.thePlayer.ticksExisted % 33 == 1) {
                        ((PacketReceive) event).setCancelled(true);
                        mc.timer.elapsedPartialTicks = 0.65F;
                        mc.thePlayer.posX = 0.12D;
                        mc.thePlayer.posY = Math.toRadians(mc.thePlayer.rotationYaw);
                    }
                    break;
            }
        }
    }

    private boolean intentionalMove() {
        return !(!mc.gameSettings.keyBindForward && !mc.gameSettings.keyBindBack.pressed && !mc.gameSettings.keyBindLeft.pressed && !mc.gameSettings.keyBindRight.pressed);
    }

    private void setPremissionFly() {
        PlayerCapabilities pc = new PlayerCapabilities();
        pc.disableDamage = true;
        pc.isFlying = true;
        pc.allowFlying = true;
        pc.isCreativeMode = true;
        mc.thePlayer.sendQueue.addToSendQueue(new C13PacketPlayerAbilities(pc));
    }

    public boolean doHittingProcess() {
        return (mc.thePlayer.isBlocking() || mc.thePlayer.isSwingInProgress || mc.thePlayer.isUsingItem() || mc.thePlayer.isOnLadder() || mc.thePlayer.isEating() || mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiInventory || mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiChest);
    }

    public enum Mode {VERUS, RIDING, SPECTATE, ALICE, NEGATVITY, DUPLICATE}

    public enum PACKET_TYPE {EXTRA, CURRENT}
}