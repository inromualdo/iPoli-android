package io.ipoli.android.store.viewmodels;

import android.content.Context;

import org.threeten.bp.LocalDate;

import io.ipoli.android.store.Upgrade;

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 5/23/17.
 */

public class UpgradeViewModel {
    private final String title;
    private final String shortDescription;
    private final String longDescription;
    private final int price;
    private final int image;
    private final Upgrade upgrade;
    private final LocalDate expirationDate;

    public UpgradeViewModel(Context context, Upgrade upgrade) {
        this(context, upgrade, null);
    }

    public UpgradeViewModel(Context context, Upgrade upgrade, LocalDate expirationDate) {
        this.title = context.getString(upgrade.title);
        this.shortDescription = context.getString(upgrade.subTitle);
        this.longDescription = context.getString(upgrade.longDesc);
        this.price = upgrade.price;
        this.image = upgrade.picture;
        this.upgrade = upgrade;
        this.expirationDate = expirationDate;
    }

    public String getTitle() {
        return title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public int getPrice() {
        return price;
    }

    public int getImage() {
        return image;
    }

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public boolean isUnlocked() {
        return expirationDate != null;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public boolean requiresUpgrade() {
        return upgrade.requiredUpgrade != null;
    }

    public Upgrade getRequiredUpgrade() {
        return upgrade.requiredUpgrade;
    }
}
