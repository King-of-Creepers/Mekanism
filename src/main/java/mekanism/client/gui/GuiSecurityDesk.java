package mekanism.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.gui.element.GuiSecurityLight;
import mekanism.client.gui.element.GuiTextureOnlyElement;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.scroll.GuiTextScrollList;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.text.BackgroundType;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.lib.security.ISecurityTile.SecurityMode;
import mekanism.common.lib.security.SecurityFrequency;
import mekanism.common.network.PacketAddTrusted;
import mekanism.common.network.PacketGuiInteract;
import mekanism.common.network.PacketGuiInteract.GuiInteraction;
import mekanism.common.tile.TileEntitySecurityDesk;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.BooleanStateDisplay.OnOff;
import mekanism.common.util.text.OwnerDisplay;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GuiSecurityDesk extends GuiMekanismTile<TileEntitySecurityDesk, MekanismTileContainer<TileEntitySecurityDesk>> {

    private static final ResourceLocation PUBLIC = MekanismUtils.getResource(ResourceType.GUI, "public.png");
    private static final ResourceLocation PRIVATE = MekanismUtils.getResource(ResourceType.GUI, "private.png");
    private static final List<Character> SPECIAL_CHARS = Arrays.asList('-', '|', '_');
    private MekanismButton removeButton;
    private MekanismButton publicButton;
    private MekanismButton privateButton;
    private MekanismButton trustedButton;
    private MekanismButton overrideButton;
    private GuiTextScrollList scrollList;
    private GuiTextField trustedField;

    public GuiSecurityDesk(MekanismTileContainer<TileEntitySecurityDesk> container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        ySize += 64;
        dynamicSlots = true;
    }

    @Override
    protected void initPreSlots() {
        func_230480_a_(new GuiElementHolder(this, 141, 13, 26, 37));
        func_230480_a_(new GuiElementHolder(this, 141, 54, 26, 34));
        func_230480_a_(new GuiElementHolder(this, 141, 92, 26, 37));
    }

    @Override
    public void func_231160_c_() {
        super.func_231160_c_();
        func_230480_a_(new GuiSlot(SlotType.INNER_HOLDER_SLOT, this, 145, 17));
        func_230480_a_(new GuiSlot(SlotType.INNER_HOLDER_SLOT, this, 145, 96));
        func_230480_a_(new GuiSecurityLight(this, 144, 77, () -> tile.getFreq() == null || tile.ownerUUID == null ||
                                                                 !tile.ownerUUID.equals(getMinecraft().player.getUniqueID()) ? 2 : tile.getFreq().isOverridden() ? 0 : 1));
        func_230480_a_(new GuiTextureOnlyElement(PUBLIC, this, 145, 32, 18, 18));
        func_230480_a_(new GuiTextureOnlyElement(PRIVATE, this, 145, 111, 18, 18));
        func_230480_a_(scrollList = new GuiTextScrollList(this, 13, 13, 122, 42));
        func_230480_a_(removeButton = new TranslationButton(this, getGuiLeft() + 13, getGuiTop() + 81, 122, 20, MekanismLang.BUTTON_REMOVE, () -> {
            int selection = scrollList.getSelection();
            if (tile.getFreq() != null && selection != -1) {
                Mekanism.packetHandler.sendToServer(new PacketGuiInteract(GuiInteraction.REMOVE_TRUSTED, tile, selection));
                scrollList.clearSelection();
                updateButtons();
            }
        }));
        func_230480_a_(trustedField = new GuiTextField(this, 35, 68, 99, 11));
        trustedField.setMaxStringLength(PacketAddTrusted.MAX_NAME_LENGTH);
        trustedField.setBackground(BackgroundType.INNER_SCREEN);
        trustedField.setEnterHandler(this::setTrusted);
        trustedField.setInputValidator(c -> SPECIAL_CHARS.contains(c) || Character.isDigit(c) || Character.isLetter(c));
        trustedField.addCheckmarkButton(this::setTrusted);
        func_230480_a_(publicButton = new MekanismImageButton(this, getGuiLeft() + 13, getGuiTop() + 113, 40, 16, 40, 16, getButtonLocation("public"),
              () -> {
                  Mekanism.packetHandler.sendToServer(new PacketGuiInteract(GuiInteraction.SECURITY_DESK_MODE, tile, SecurityMode.PUBLIC.ordinal()));
                  updateButtons();
              }, getOnHover(MekanismLang.PUBLIC_MODE)));
        func_230480_a_(privateButton = new MekanismImageButton(this, getGuiLeft() + 54, getGuiTop() + 113, 40, 16, 40, 16, getButtonLocation("private"),
              () -> {
                  Mekanism.packetHandler.sendToServer(new PacketGuiInteract(GuiInteraction.SECURITY_DESK_MODE, tile, SecurityMode.PRIVATE.ordinal()));
                  updateButtons();
              }, getOnHover(MekanismLang.PRIVATE_MODE)));
        func_230480_a_(trustedButton = new MekanismImageButton(this, getGuiLeft() + 95, getGuiTop() + 113, 40, 16, 40, 16, getButtonLocation("trusted"),
              () -> {
                  Mekanism.packetHandler.sendToServer(new PacketGuiInteract(GuiInteraction.SECURITY_DESK_MODE, tile, SecurityMode.TRUSTED.ordinal()));
                  updateButtons();
              }, getOnHover(MekanismLang.TRUSTED_MODE)));
        func_230480_a_(overrideButton = new MekanismImageButton(this, getGuiLeft() + 146, getGuiTop() + 59, 16, 16, getButtonLocation("exclamation"),
              () -> {
                  Mekanism.packetHandler.sendToServer(new PacketGuiInteract(GuiInteraction.OVERRIDE_BUTTON, tile));
                  updateButtons();
              }, (onHover, matrix, xAxis, yAxis) -> {
            if (tile.getFreq() != null) {
                displayTooltip(matrix, MekanismLang.SECURITY_OVERRIDE.translate(OnOff.of(tile.getFreq().isOverridden())), xAxis, yAxis);
            }
        }));
        updateButtons();
    }

    private void setTrusted() {
        SecurityFrequency freq = tile.getFreq();
        if (freq != null && tile.ownerUUID != null && tile.ownerUUID.equals(getMinecraft().player.getUniqueID())) {
            addTrusted(trustedField.getText());
            trustedField.setText("");
            updateButtons();
        }
    }

    private void addTrusted(String trusted) {
        if (PacketAddTrusted.validateNameLength(trusted.length())) {
            Mekanism.packetHandler.sendToServer(new PacketAddTrusted(tile.getPos(), trusted));
        }
    }

    private void updateButtons() {
        SecurityFrequency freq = tile.getFreq();
        if (tile.ownerUUID != null) {
            scrollList.setText(tile.getFreq() == null ? Collections.emptyList() : tile.getFreq().getTrustedUsernameCache());
            removeButton.field_230693_o_ = scrollList.hasSelection();
        }

        if (freq != null && tile.ownerUUID != null && tile.ownerUUID.equals(getMinecraft().player.getUniqueID())) {
            publicButton.field_230693_o_ = freq.getSecurityMode() != SecurityMode.PUBLIC;
            privateButton.field_230693_o_ = freq.getSecurityMode() != SecurityMode.PRIVATE;
            trustedButton.field_230693_o_ = freq.getSecurityMode() != SecurityMode.TRUSTED;
            overrideButton.field_230693_o_ = true;
        } else {
            publicButton.field_230693_o_ = false;
            privateButton.field_230693_o_ = false;
            trustedButton.field_230693_o_ = false;
            overrideButton.field_230693_o_ = false;
        }
    }

    @Override
    public void func_231023_e_() {
        super.func_231023_e_();
        updateButtons();
    }

    @Override
    public boolean func_231044_a_(double mouseX, double mouseY, int button) {
        updateButtons();
        return super.func_231044_a_(mouseX, mouseY, button);
    }

    @Override
    protected void drawForegroundText(@Nonnull MatrixStack matrix, int mouseX, int mouseY) {
        renderTitleText(matrix, 4);
        ITextComponent ownerComponent = OwnerDisplay.of(tile.ownerUUID, tile.clientOwner).getTextComponent();
        drawString(matrix, ownerComponent, getXSize() - 7 - getStringWidth(ownerComponent), (getYSize() - 96) + 2, titleTextColor());
        drawString(matrix, MekanismLang.INVENTORY.translate(), 8, (getYSize() - 96) + 2, titleTextColor());
        drawCenteredText(matrix, MekanismLang.TRUSTED_PLAYERS.translate(), 74, 57, 0x787878);
        if (tile.getFreq() != null) {
            drawString(matrix, MekanismLang.SECURITY.translate(tile.getFreq().getSecurityMode()), 13, 103, titleTextColor());
        } else {
            drawString(matrix, MekanismLang.SECURITY_OFFLINE.translateColored(EnumColor.RED), 13, 103, titleTextColor());
        }
        drawTextScaledBound(matrix, MekanismLang.SECURITY_ADD.translate(), 13, 70, titleTextColor(), 20);
        super.drawForegroundText(matrix, mouseX, mouseY);
    }
}