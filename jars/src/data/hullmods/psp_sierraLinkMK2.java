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
    private static final String DUMMY_MOD = "andrada_mods"; // any vanilla hullmod
    private static boolean cleanupInProgress = false;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (cleanupInProgress) return;

        ShipVariantAPI v = stats.getVariant();
        if (v == null) return;

        // Re-initialize if variant was reset (tags lost but spec still has SHIFT)
        if (!v.hasTag(PRIVATE_SPEC_TAG) && SHIFT.equals(v.getHullSpec().getShipSystemId())) {
            // Spec already cloned (has SHIFT), just restore tags
            v.addTag(PRIVATE_SPEC_TAG);
        }

        if (!v.hasTag(ACTIVE_TAG)) v.addTag(ACTIVE_TAG);
        if (!v.hasHullMod(CONCORD)) v.addMod(CONCORD);
        if (!v.hasTag(INERT_TAG)) v.addTag(INERT_TAG);

        String recorded = getRecordedSystem(v);
        if (recorded == null) {
            // Only record original if current spec doesn't already have SHIFT set by us
            String hullSpecOriginal = Global.getSettings().getHullSpec(v.getHullSpec().getHullId()).getShipSystemId();
            if (hullSpecOriginal != null && !SHIFT.equals(hullSpecOriginal)) {
                v.addTag(ORIG_TAG + hullSpecOriginal);
            }
        }

        ensurePrivateHullSpec(v);
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

        ShipVariantAPI v = member.getVariant();

        // Cleanup detection: ACTIVE_TAG present but hullmod gone
        if (v.hasTag(ACTIVE_TAG) && !v.hasHullMod(ID2) && !v.getPermaMods().contains(ID2)) {
            if (!cleanupInProgress) {
                performCleanup(v);
            }
            return;
        }

        syncInertTag(v, isSierraCaptain(member.getCaptain()));
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
        syncInertTag(v, isSierraCaptain(ship.getCaptain()));

        // Trigger cleanup when: mod is present, system is SHIFT, but tags lost (variant was reset)
        // AND mod is being removed (detected via hasShift but no ACTIVE_TAG)
        if (!v.hasHullMod(ID2) && SHIFT.equals(v.getHullSpec().getShipSystemId())) {
            performCleanup(v);
        }

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

        try {
            ShipHullSpecAPI cloned = (ShipHullSpecAPI) ReflectionUtils.invoke("clone", v.getHullSpec());
            if (cloned == null) return;
            v.setHullSpecAPI(cloned);
        } catch (Throwable e) {
            return;
        }

        v.addTag(PRIVATE_SPEC_TAG);
    }

    private void performCleanup(ShipVariantAPI v) {
        cleanupInProgress = true;
        try {
            v.removePermaMod(CONCORD);
            v.removePermaMod(HullMods.PHASE_FIELD);

            // Instead of swapping specs, just fix the system ID on whatever spec is current
            String recorded = getRecordedSystem(v);
            String restoreTo = recorded != null ? recorded
                    : Global.getSettings().getHullSpec(v.getHullSpec().getHullId()).getShipSystemId();

            v.getHullSpec().setShipSystemId(restoreTo);

            if (recorded != null) v.removeTag(ORIG_TAG + recorded);
            v.removeTag(ACTIVE_TAG);
            v.removeTag(PRIVATE_SPEC_TAG);
            v.removeTag(INERT_TAG);

        } finally {
            cleanupInProgress = false;
        }
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