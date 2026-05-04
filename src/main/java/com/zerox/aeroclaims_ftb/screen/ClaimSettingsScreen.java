package com.zerox.aeroclaims_ftb.screen;

import com.zerox.aeroclaims_ftb.Aeroclaims_ftb;
import com.zerox.aeroclaims_ftb.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class ClaimSettingsScreen extends AbstractContainerScreen<ClaimSettingsMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Aeroclaims_ftb.MODID, "textures/screen/claim-menu.png");

    private static final int TEXTURE_W = 190;
    private static final int TEXTURE_H = 178;

    private static final int COLOR_TITLE = 0xDCE7F2;
    private static final int COLOR_TEXT = 0xA8B4C2;
    private static final int COLOR_OK = 0x7ACB8A;
    private static final int COLOR_ERR = 0xD86A6A;

    private static final long REFRESH_COOLDOWN_MS = 30_000L;
    private static final Map<BlockPos, Long> refreshCooldowns = new HashMap<>();
    private static final Map<BlockPos, Boolean> activateUsedInCooldown = new HashMap<>();

    private static final int BTN_X = 16;
    private static final int BTN_H = 18;

    private static final int TEAM_Y = 43;
    private static final int ALLIES_Y = 64;
    private static final int PUBLIC_Y = 85;
    private static final int ACTION_Y = 112;
    private static final int CLAIM_ROW_Y = 132;

    private static final int SMALL_BTN = 13;

    private Button partyButton;
    private Button alliesButton;
    private Button othersButton;
    private Button refreshButton;
    private Button actionButton;
    private Button minusButton;
    private Button plusButton;

    private boolean inActivateMode = false;

    public ClaimSettingsScreen(ClaimSettingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = TEXTURE_W;
        imageHeight = TEXTURE_H;
    }

    @Override
    protected void init() {
        super.init();

        int fullButtonW = 158;

        partyButton = FtbButton.create(leftPos + BTN_X, topPos + TEAM_Y, fullButtonW, BTN_H, partyText(), b -> {
            menu.setAllowParty(!menu.isAllowParty());
            b.setMessage(partyText());
            sendPermissions();
        });

        alliesButton = FtbButton.create(leftPos + BTN_X, topPos + ALLIES_Y, fullButtonW, BTN_H, alliesText(), b -> {
            menu.setAllowAllies(!menu.isAllowAllies());
            b.setMessage(alliesText());
            sendPermissions();
        });

        othersButton = FtbButton.create(leftPos + BTN_X, topPos + PUBLIC_Y, fullButtonW, BTN_H, othersText(), b -> {
            menu.setAllowOthers(!menu.isAllowOthers());
            b.setMessage(othersText());
            sendPermissions();
        });

        refreshButton = FtbButton.create(leftPos + 16, topPos + ACTION_Y, 68, BTN_H, refreshText(), b -> sendRefresh());

        actionButton = FtbButton.create(leftPos + 94, topPos + ACTION_Y, 79, BTN_H, activateText(), b -> sendActionButtonClick());

        minusButton = FtbButton.create(leftPos + 16, topPos + CLAIM_ROW_Y, SMALL_BTN, 16, Component.literal("-"), b -> sendAdjust(-1));

        plusButton = FtbButton.create(leftPos + 142, topPos + CLAIM_ROW_Y, SMALL_BTN, 16, Component.literal("+"), b -> sendAdjust(+1));

        addRenderableWidget(partyButton);
        addRenderableWidget(alliesButton);
        addRenderableWidget(othersButton);
        addRenderableWidget(refreshButton);
        addRenderableWidget(actionButton);
        addRenderableWidget(minusButton);
        addRenderableWidget(plusButton);

        updateRefreshButton();
        updateActionButton();
        refreshClaimButtons();
    }

    public void syncFromMenu() {
        partyButton.setMessage(partyText());
        alliesButton.setMessage(alliesText());
        othersButton.setMessage(othersText());
        updateRefreshButton();
        updateActionButton();
        refreshClaimButtons();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        g.blit(BACKGROUND, leftPos, topPos, 0, 0, TEXTURE_W, TEXTURE_H, TEXTURE_W, TEXTURE_H);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        String title = Component.translatable("screen.aeroclaims_ftb.claim_settings.title").getString();
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 14, COLOR_TITLE, false);

        int claimTextY = CLAIM_ROW_Y + 4;

        String claimsText = menu.getClaimsForBlock() + " claims";
        g.drawString(font, claimsText, 38, claimTextY, COLOR_TEXT, false);

        String blocksText = blocksText();
        int blocksColor = blocksOverLimit() ? COLOR_ERR : COLOR_TEXT;
        g.drawString(font, blocksText, imageWidth - 16 - font.width(blocksText), claimTextY, blocksColor, false);

        int infoY = 148;

        boolean active = menu.isClaimActive();
        String prefix = Component.translatable("screen.aeroclaims_ftb.claim_settings.privacy_prefix").getString();
        String status = Component.translatable(active
                ? "screen.aeroclaims_ftb.claim_settings.status.active"
                : "screen.aeroclaims_ftb.claim_settings.status.disabled").getString();

        g.drawString(font, prefix, 16, infoY, COLOR_TEXT, false);
        g.drawString(font, status, 16 + font.width(prefix), infoY, active ? COLOR_OK : COLOR_ERR, false);

        try {
            String ownerName = Minecraft.getInstance()
                    .getConnection()
                    .getPlayerInfo(menu.getOwner())
                    .getProfile()
                    .getName();

            g.drawString(font,
                    Component.translatable("screen.aeroclaims_ftb.claim_settings.owner", ownerName).getString(),
                    16, infoY + 11, COLOR_TEXT, false);
        } catch (Exception ignored) {
        }

        int nameY = infoY + 22;

        if (!menu.isOnShip()) {
            g.drawString(font,
                    Component.translatable("screen.aeroclaims_ftb.claim_settings.not_on_subclaim").getString(),
                    16, nameY, COLOR_ERR, false);
        } else if (menu.getShipName() != null && !menu.getShipName().isEmpty()) {
            for (FormattedCharSequence line : font.split(
                    Component.translatable("screen.aeroclaims_ftb.claim_settings.ship", menu.getShipName()),
                    130)) {
                g.drawString(font, line, 16, nameY, COLOR_TEXT, false);
                nameY += font.lineHeight;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g, mx, my, partialTick);
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);

        if (!refreshButton.active && menu.isOnShip() && !onCooldown()) {
            updateRefreshButton();
        }

        if (menu.isOnShip()) {
            updateActionButton();
        }
    }

    private void refreshClaimButtons() {
        if (!menu.isOnShip()) {
            minusButton.active = false;
            plusButton.active = false;
            return;
        }

        plusButton.active = menu.getFreeSlots() >= 1;
        minusButton.active = menu.getClaimsForBlock() >= 1 && canReduceByOne();
    }

    private boolean canReduceByOne() {
        if (!menu.isClaimActive()) return true;

        int count = menu.getShipBlockCount();

        if (count == SyncClaimStatePacket.SHIP_BLOCK_COUNT_UNKNOWN) return true;

        return count <= (menu.getClaimsForBlock() - 1) * menu.getBlocksPerClaim();
    }

    private void updateRefreshButton() {
        if (!menu.isOnShip()) {
            refreshButton.active = false;
            refreshButton.setMessage(Component.translatable("screen.aeroclaims_ftb.claim_settings.not_on_subclaim"));
        } else if (onCooldown()) {
            refreshButton.active = false;
            refreshButton.setMessage(Component.translatable("screen.aeroclaims_ftb.claim_settings.refresh_wait"));
        } else {
            refreshButton.active = true;
            refreshButton.setMessage(refreshText());
        }
    }

    private void updateActionButton() {
        if (!menu.isOnShip()) {
            inActivateMode = false;
            actionButton.active = false;
            actionButton.setMessage(activateText());
            return;
        }

        boolean nowCooldown = onCooldown();

        if (nowCooldown) {
            inActivateMode = true;
            actionButton.setMessage(activateText());

            boolean alreadyUsed = Boolean.TRUE.equals(activateUsedInCooldown.get(menu.getCenter()));
            actionButton.active = !alreadyUsed && blocksKnownAndOk();
        } else {
            inActivateMode = false;
            actionButton.setMessage(deactivateText());
            actionButton.active = menu.isClaimActive();
        }
    }

    private void sendPermissions() {
        PacketDistributor.sendToServer(new UpdateClaimSettingsPacket(
                menu.getCenter(),
                menu.isAllowParty(),
                menu.isAllowAllies(),
                menu.isAllowOthers()
        ));
    }

    private void sendAdjust(int delta) {
        PacketDistributor.sendToServer(new AdjustBlockClaimsPacket(menu.getCenter(), delta));

        menu.setClaimsForBlock(Math.max(0, menu.getClaimsForBlock() + delta));
        menu.setFreeSlots(Math.max(0, menu.getFreeSlots() - delta));

        refreshClaimButtons();
    }

    private void sendRefresh() {
        if (onCooldown()) return;

        PacketDistributor.sendToServer(new RefreshClaimPacket(menu.getCenter()));

        refreshCooldowns.put(menu.getCenter(), System.currentTimeMillis());
        activateUsedInCooldown.put(menu.getCenter(), false);

        refreshButton.active = false;
        refreshButton.setMessage(Component.translatable("screen.aeroclaims_ftb.claim_settings.refresh_wait"));

        inActivateMode = true;
        actionButton.setMessage(activateText());
        actionButton.active = blocksKnownAndOk();
    }

    private void sendActionButtonClick() {
        if (inActivateMode) {
            sendActivate();
        } else {
            sendDeactivate();
        }
    }

    private void sendActivate() {
        if (!onCooldown() || blocksOverLimit()) return;

        PacketDistributor.sendToServer(new ActivateClaimPacket(menu.getCenter()));

        activateUsedInCooldown.put(menu.getCenter(), true);
        actionButton.active = false;
    }

    private void sendDeactivate() {
        if (onCooldown()) return;

        PacketDistributor.sendToServer(new DeactivateClaimPacket(menu.getCenter()));

        actionButton.active = false;
    }

    private boolean onCooldown() {
        Long last = refreshCooldowns.get(menu.getCenter());
        return last != null && System.currentTimeMillis() - last < REFRESH_COOLDOWN_MS;
    }

    private boolean blocksKnownAndOk() {
        return menu.getShipBlockCount() != SyncClaimStatePacket.SHIP_BLOCK_COUNT_UNKNOWN
                && !blocksOverLimit();
    }

    private String blocksText() {
        int count = menu.getShipBlockCount();

        if (!menu.isOnShip()) return "—";

        if (count == SyncClaimStatePacket.SHIP_BLOCK_COUNT_UNKNOWN) {
            return Component.translatable("screen.aeroclaims_ftb.claim_settings.blocks_unknown").getString();
        }

        int limit = menu.getBlockLimit();

        return Component.translatable("screen.aeroclaims_ftb.claim_settings.blocks_usage", count, limit).getString();
    }

    private boolean blocksOverLimit() {
        int count = menu.getShipBlockCount();
        int limit = menu.getBlockLimit();

        return menu.isOnShip()
                && count != SyncClaimStatePacket.SHIP_BLOCK_COUNT_UNKNOWN
                && limit > 0
                && count > limit;
    }

    private Component refreshText() {
        return Component.translatable("screen.aeroclaims_ftb.claim_settings.refresh");
    }

    private Component activateText() {
        return Component.translatable("screen.aeroclaims_ftb.claim_settings.activate");
    }

    private Component deactivateText() {
        return Component.translatable("screen.aeroclaims_ftb.claim_settings.deactivate");
    }

    private Component partyText() {
        return Component.translatable(menu.isAllowParty()
                ? "screen.aeroclaims_ftb.claim_settings.party.allowed"
                : "screen.aeroclaims_ftb.claim_settings.party.denied");
    }

    private Component alliesText() {
        return Component.translatable(menu.isAllowAllies()
                ? "screen.aeroclaims_ftb.claim_settings.allies.allowed"
                : "screen.aeroclaims_ftb.claim_settings.allies.denied");
    }

    private Component othersText() {
        return Component.translatable(menu.isAllowOthers()
                ? "screen.aeroclaims_ftb.claim_settings.others.allowed"
                : "screen.aeroclaims_ftb.claim_settings.others.denied");
    }

    private static class FtbButton extends Button {

        protected FtbButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        }

        public static FtbButton create(int x, int y, int width, int height, Component message, OnPress onPress) {
            return new FtbButton(x, y, width, height, message, onPress);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x1 = getX();
            int y1 = getY();
            int x2 = x1 + width;
            int y2 = y1 + height;

            boolean hovered = isHoveredOrFocused();

            int bg = !active ? 0x9930363F : hovered ? 0xCC4A5664 : 0xCC3A424D;
            int border = !active ? 0xAA1A1E24 : hovered ? 0xFF8FBFE8 : 0xFF5E6B78;

            g.fill(x1, y1, x2, y2, 0xFF090B0F);
            g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, bg);

            g.fill(x1, y1, x2, y1 + 1, border);
            g.fill(x1, y2 - 1, x2, y2, border);
            g.fill(x1, y1, x1 + 1, y2, border);
            g.fill(x2 - 1, y1, x2, y2, border);

            if (hovered && active) {
                g.fill(x1 + 2, y1 + 2, x2 - 2, y1 + 3, 0x663FA9F5);
            }

            renderString(g, Minecraft.getInstance().font, active ? 0xFFE8EEF5 : 0xFF8A939E);
        }
    }
}
