package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.model.ThemeValue;
import biz.dealnote.messenger.mvp.view.IThemeView;
import biz.dealnote.mvp.core.AbsPresenter;


public class ThemePresenter extends AbsPresenter<IThemeView> {

    private final List<ThemeValue> data;

    public ThemePresenter(@Nullable Bundle savedInstanceState) {
        super(savedInstanceState);
        this.data = createInitialData();
    }

    private ArrayList<ThemeValue> createInitialData() {
        ArrayList<ThemeValue> categories = new ArrayList<>();
        categories.add(new ThemeValue("#448AFF", "#1E88E5", "ice", "Ice"));
        categories.add(new ThemeValue("#448AFF", "#82B1FF", "old_ice", "Old Ice"));
        categories.add(new ThemeValue("#FF9800", "#FFA726", "fire", "Fire"));
        categories.add(new ThemeValue("#FF0000", "#F44336", "red", "Red"));
        categories.add(new ThemeValue("#9800ff", "#8500ff", "violet", "Violet"));
        categories.add(new ThemeValue("#444444", "#777777", "gray", "Gray"));
        categories.add(new ThemeValue("#448AFF", "#8500ff", "blue_violet", "Ice Violet"));
        categories.add(new ThemeValue("#448AFF", "#FF0000", "blue_red", "Ice Red"));
        categories.add(new ThemeValue("#448AFF", "#FFA726", "blue_yellow", "Ice Fire"));
        categories.add(new ThemeValue("#FF9800", "#8500ff", "yellow_violet", "Fire Violet"));
        categories.add(new ThemeValue("#8500ff", "#FF9800", "violet_yellow", "Violet Fire"));
        categories.add(new ThemeValue("#8500ff", "#268000", "violet_green", "Violet Green"));
        categories.add(new ThemeValue("#268000", "#8500ff", "green_violet", "Green Violet"));
        categories.add(new ThemeValue("#9800ff", "#F44336", "violet_red", "Violet Red"));
        categories.add(new ThemeValue("#F44336", "#9800ff", "red_violet", "Red Violet"));
        categories.add(new ThemeValue("#F8DF00", "#F44336", "yellow_red", "Fire Red"));
        categories.add(new ThemeValue("#448AFF", "#4CAF50", "ice_green", "Ice Green"));
        return categories;
    }

    @Override
    public void onGuiCreated(@NonNull IThemeView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayData(data);
    }
}
