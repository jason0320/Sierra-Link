package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;

public class psp_sierraLinkMK2 extends BaseHullMod {

    public static final String ID2 = "psp_sierraLinkMK2";
    private static final String SOTF_MOD_ID = "secretsofthefrontier";
    private static final String CONCORD = "sotf_sierrasconcord";
    private static final String SHIFT = "sotf_concordshift";
    private static final String INERT_TAG = "sotf_inert";
    private static final String ORIG_TAG = "psp_Original_System_";
    private static final String ACTIVE_TAG = "psp_sierraLink_active";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI v = stats.getVariant();
        if (v == null) return;

        // --- PART 1: APPLY EFFECTS ---
        // This runs as long as the hullmod is equipped
        if (!v.hasHullMod(CONCORD)) v.addMod(CONCORD);
        if (!v.hasHullMod(HullMods.PHASE_FIELD)) v.addPermaMod(HullMods.PHASE_FIELD);
        if (!v.hasTag(INERT_TAG)) v.addTag(INERT_TAG);
        if (!v.hasTag(ACTIVE_TAG)) v.addTag(ACTIVE_TAG);

        String recorded = getRecordedSystem(v);
        if (recorded == null) {
            String currentSys = v.getHullSpec().getShipSystemId();
            if (!SHIFT.equals(currentSys)) {
                v.addTag(ORIG_TAG + currentSys);
            }
        }
        v.getHullSpec().setShipSystemId(SHIFT);
    }

    /**
     * canBeAddedOrRemovedNow is called constantly while the UI is open.
     * We use this to detect the exact moment the hullmod is missing but our tag is still there.
     */
    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI market, CampaignUIAPI.CoreUITradeMode mode) {
        if (ship == null || ship.getVariant() == null) return true;
        ShipVariantAPI v = ship.getVariant();

        // If the hullmod is GONE but the ACTIVE_TAG is still there, the player just clicked "Remove"
        if (!v.hasHullMod(ID2) && v.hasTag(ACTIVE_TAG)) {
            performCleanup(v);
        }
        return true;
    }

    private void performCleanup(ShipVariantAPI v) {
        // 1. Remove the sidecar mods
        v.removeMod(CONCORD);
        v.removePermaMod(HullMods.PHASE_FIELD);

        // 2. Restore the Ship System
        String recorded = getRecordedSystem(v);
        if (recorded != null) {
            // CRITICAL: We update the spec and the variant's view of the spec
            v.getHullSpec().setShipSystemId(recorded);
            v.removeTag(ORIG_TAG + recorded);
        }

        // 3. Remove the marker tags
        v.removeTag(INERT_TAG);
        v.removeTag(ACTIVE_TAG);
    }

    private String getRecordedSystem(ShipVariantAPI v) {
        for (String tag : v.getTags()) {
            if (tag.startsWith(ORIG_TAG)) return tag.substring(ORIG_TAG.length());
        }
        return null;
    }

    // --- Standard UI Methods ---
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        if (Global.getSector() == null) return false;
        return Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean("$sotf_metSierra");
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && Global.getSettings().getModManager().isModEnabled(SOTF_MOD_ID);
    }
}