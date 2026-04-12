package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

/**
 * psp_sierraLink
 * Installs/removes Secrets of the Frontier's "Sierra's Concord" when this hullmod
 * is added/removed – but installs it as a *regular* hullmod so the player can S-mod it.
 *
 * Also cleans up the inert tag when this hullmod is removed.
 */
public class psp_sierraLink extends BaseHullMod {

    public static final String ID = "psp_sierraLink";
    private static final String SOTF_MOD_ID = "secretsofthefrontier";
    private static final String CONCORD = "sotf_sierrasconcord";
    private static final String INERT_TAG = "sotf_inert";
    private static final String ACTIVE_TAG = "psp_sierraLink_active";
    private static final String MEM_KEY = "$sotf_metSierra";
    private static final String DUMMY_MOD = "andrada_mods"; // any vanilla hullmod

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI v = stats.getVariant();
        if (v == null) return;

        // 1. Install features while mod is active
        if (!v.hasHullMod(CONCORD)) {
            v.addMod(CONCORD);
        }
        if (!v.hasTag(INERT_TAG)) {
            v.addTag(INERT_TAG);
        }

        // 2. Add the marker tag so we can detect removal later
        if (!v.hasTag(ACTIVE_TAG)) {
            v.addTag(ACTIVE_TAG);
        }
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI market, CoreUITradeMode mode) {
        if (ship == null || ship.getVariant() == null) return true;
        if (!sotfEnabled()) return true;

        ShipVariantAPI v = ship.getVariant();

        // Check if mod is actually gone but our marker tag is still there
        boolean hasMod = v.hasHullMod(ID) || v.getPermaMods().contains(ID);

        if (!hasMod && v.hasTag(ACTIVE_TAG)) {
            // Instant Cleanup
            v.removePermaMod(CONCORD);
            v.removeTag(INERT_TAG);
            v.removeTag(ACTIVE_TAG);
        }
        return true;
    }

    // --- Utility & UI Methods ---

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