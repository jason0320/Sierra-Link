package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;

public class psp_sierraLinkMK2 extends BaseHullMod {

    public static final String ID2 = "psp_sierraLinkMK2";
    private static final String SOTF_MOD_ID = "secretsofthefrontier";
    private static final String CONCORD = "sotf_sierrasconcord";
    private static final String SHIFT = "sotf_concordshift";
    private static final String INERT_TAG = "sotf_inert";
    private static final String ORIG_TAG = "psp_Original_System_";
    private static final String ACTIVE_TAG = "psp_sierraLink_active";
    private static final String PRIVATE_SPEC_TAG = "psp_sierraLink_privateSpec";
    private static final String MEM_KEY = "$sotf_metSierra";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI v = stats.getVariant();
        if (v == null) return;

        ensurePrivateHullSpec(v);

        if (!v.hasHullMod(CONCORD)) {
            v.addMod(CONCORD);
        }

        if (!v.hasTag(ACTIVE_TAG)) {
            v.addTag(ACTIVE_TAG);
        }

        if (!v.hasTag(INERT_TAG)) {
            v.addTag(INERT_TAG);
        }

        String recorded = getRecordedSystem(v);
        if (recorded == null) {
            String currentSys = v.getHullSpec().getShipSystemId();
            if (currentSys != null && !SHIFT.equals(currentSys)) {
                v.addTag(ORIG_TAG + currentSys);
            }
        }

        v.getHullSpec().setShipSystemId(SHIFT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null || ship.getVariant() == null) return;
        syncInertTag(ship.getVariant(), isSierraCaptain(ship.getCaptain()));
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (member == null || member.getVariant() == null) return;
        syncInertTag(member.getVariant(), isSierraCaptain(member.getCaptain()));
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.getVariant() == null) return;
        syncInertTag(ship.getVariant(), isSierraCaptain(ship.getCaptain()));
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI market, CampaignUIAPI.CoreUITradeMode mode) {
        if (ship == null || ship.getVariant() == null) return true;

        ShipVariantAPI v = ship.getVariant();

        if (!v.hasHullMod(ID2) && v.hasTag(ACTIVE_TAG)) {
            performCleanup(v);
        }

        // Keep inert state in sync even while the refit screen is open.
        syncInertTag(v, isSierraCaptain(ship.getCaptain()));

        return true;
    }

    private void syncInertTag(ShipVariantAPI v, boolean sierraPresent) {
        if (v == null) return;

        if (sierraPresent) {
            v.removeTag(INERT_TAG);
        } else {
            if (!v.hasTag(INERT_TAG)) {
                v.addTag(INERT_TAG);
            }
        }
    }

    private boolean isSierraCaptain(PersonAPI captain) {

        return captain != null && captain.getId().equals("sotf_sierra");
    }

    private void ensurePrivateHullSpec(ShipVariantAPI v) {
        if (v.hasTag(PRIVATE_SPEC_TAG)) return;

        // Detach this variant from the shared hull spec so the ship-system swap
        // only affects this exact ship/variant.
        ShipVariantAPI copy = v.clone();
        ShipHullSpecAPI privateSpec = copy.getHullSpec();
        v.setHullSpecAPI(privateSpec);

        v.addTag(PRIVATE_SPEC_TAG);
    }

    private void performCleanup(ShipVariantAPI v) {
        v.removePermaMod(CONCORD);
        v.removePermaMod(HullMods.PHASE_FIELD);

        String recorded = getRecordedSystem(v);
        if (recorded != null) {
            v.getHullSpec().setShipSystemId(recorded);
            v.removeTag(ORIG_TAG + recorded);
        }

        v.removeTag(ACTIVE_TAG);
        v.removeTag(PRIVATE_SPEC_TAG);
    }

    private String getRecordedSystem(ShipVariantAPI v) {
        for (String tag : v.getTags()) {
            if (tag.startsWith(ORIG_TAG)) {
                return tag.substring(ORIG_TAG.length());
            }
        }
        return null;
    }

    private boolean sotfEnabled() {
        return Global.getSettings().getModManager().isModEnabled(SOTF_MOD_ID);
    }

    private static boolean playerHasMetSierra() {
        if (Global.getSector() == null) return false;
        return Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean(MEM_KEY);
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return playerHasMetSierra() && sotfEnabled();
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && sotfEnabled();
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!sotfEnabled()) return "Requires Secrets of the Frontier";
        return null;
    }
}