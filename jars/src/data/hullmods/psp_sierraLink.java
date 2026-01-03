package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
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

    // Cross-mod constants
    private static final String SOTF_MOD_ID = "secretsofthefrontier";
    private static final String CONCORD = "sotf_sierrasconcord";
    private static final String INERT_TAG = "sotf_inert";

    // Memory keys (support both with/without '$' just in case)
    private static final String MEM_KEY    = "$sotf_metSierra";

    private boolean sotfEnabled() {
        try {
            return Global.getSettings().getModManager().isModEnabled(SOTF_MOD_ID);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Returns true iff the player's memory has the met-Sierra flag set to true. */
    private static boolean playerHasMetSierra() {
        if (Global.getSector() == null) return false;
        MemoryAPI mem = Global.getSector().getPlayerMemoryWithoutUpdate();
        if (mem == null) return false;
        if (mem.contains(MEM_KEY)) return mem.getBoolean(MEM_KEY);
        return false;
    }

    /**
     * NEW: Control whether this hullmod appears in the refit screen's mod picker.
     * If the player hasn't met Sierra, keep it hidden.
     */
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return playerHasMetSierra();
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        // Keep it selectable only when SOTF is present; returning true avoids the hullmod being greyed-out when present.
        return ship != null && sotfEnabled();
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!sotfEnabled()) return "Requires Secrets of the Frontier";
        return null;
    }

    /**
     * This hook is called by the refit UI whenever add/remove is checked.
     * We piggyback on it to install/remove the dependent hullmod in a way that
     * preserves S-modding: add as a normal mod; only auto-remove if it isn't an S-mod.
     */
    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI market, CoreUITradeMode mode) {
        if (ship == null) return true;
        if (!sotfEnabled()) return true;

        ShipVariantAPI v = ship.getVariant();
        if (v == null) return true;

        boolean hasLink = v.hasHullMod(ID) || v.getPermaMods().contains(ID);

        if (hasLink) {
            // Ensure Concord is present as a REGULAR mod so the player can S-mod it later.
            if (!v.hasHullMod(CONCORD)) {
                v.addMod(CONCORD);                 // NOT addPermaMod!
                v.addTag(INERT_TAG);            // Just in case something left it inert
            }
        } else {
            // Our link was removed: if Concord isn't S-modded, clean it up.
            if (v.hasHullMod(CONCORD)) {
                v.removePermaMod(CONCORD);
            }
            // Always clear the inert tag if present
            if (v.getTags().contains(INERT_TAG)) {
                v.removeTag(INERT_TAG);
            }
        }

        return true; // allow add/remove
    }

}
