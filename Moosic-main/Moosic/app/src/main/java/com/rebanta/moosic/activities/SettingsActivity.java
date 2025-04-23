package com.rebanta.moosic.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.rebanta.moosic.ApplicationClass;
import com.rebanta.moosic.R;
import com.rebanta.moosic.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final SettingsSharedPrefManager settingsSharedPrefManager = new SettingsSharedPrefManager(this);

        binding.downloadOverCellular.setOnCheckChangeListener(settingsSharedPrefManager::setDownloadOverCellular);
        binding.highQualityTrack.setOnCheckChangeListener(settingsSharedPrefManager::setHighQualityTrack);
        binding.storeInCache.setOnCheckChangeListener(settingsSharedPrefManager::setStoreInCache);
        binding.explicit.setOnCheckChangeListener(settingsSharedPrefManager::setExplicit);

        binding.downloadOverCellular.setChecked(settingsSharedPrefManager.getDownloadOverCellular());
        binding.highQualityTrack.setChecked(settingsSharedPrefManager.getHighQualityTrack());
        binding.storeInCache.setChecked(settingsSharedPrefManager.getStoreInCache());
        binding.explicit.setChecked(settingsSharedPrefManager.getExplicit());

        binding.themeChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            settingsSharedPrefManager.setTheme(checkedId == R.id.dark ? "dark" : checkedId == R.id.light ? "light" : "system");
            ApplicationClass.updateTheme();
        });

        binding.themeChipGroup.check(settingsSharedPrefManager.getTheme().equals("dark") ? R.id.dark : settingsSharedPrefManager.getTheme().equals("light") ? R.id.light : R.id.system);

    }

    public void backPress(View view) {
        finish();
    }

    public static final class SettingsSharedPrefManager {
        SharedPreferences sharedPreferences;
        public SettingsSharedPrefManager(Context context){
            sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        }

        public void setDownloadOverCellular(boolean value){
            sharedPreferences.edit().putBoolean("download_over_cellular", value).apply();
        }
        public boolean getDownloadOverCellular(){
            return sharedPreferences.getBoolean("download_over_cellular", true);
        }

        public void setHighQualityTrack(boolean value){
            sharedPreferences.edit().putBoolean("high_quality_track", value).apply();
        }
        public boolean getHighQualityTrack(){
            return sharedPreferences.getBoolean("high_quality_track", true);
        }

        public void setStoreInCache(boolean value){
            sharedPreferences.edit().putBoolean("store_in_cache", value).apply();
        }
        public boolean getStoreInCache(){
            return sharedPreferences.getBoolean("store_in_cache", true);
        }

        public void setExplicit(boolean value){
            sharedPreferences.edit().putBoolean("explicit", value).apply();
        }
        public boolean getExplicit() {
            return sharedPreferences.getBoolean("explicit", true);
        }

        public void setTheme(String theme){
            sharedPreferences.edit().putString("theme", theme).apply();
        }
        public String getTheme() {
            return sharedPreferences.getString("theme", "system");
        }
    }
}