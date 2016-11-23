package cn.garymb.ygomobile.settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.core.ResCheckTask;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.plus.PreferenceFragmentPlus;
import cn.garymb.ygomobile.utils.BitmapUtil;
import cn.garymb.ygomobile.utils.IOUtils;
import cn.garymb.ygomobile.plus.VUiKit;

import static cn.garymb.ygomobile.Constants.*;
import static cn.garymb.ygomobile.core.ResCheckTask.getDatapath;

public class SettingFragment extends PreferenceFragmentPlus {

    public SettingFragment() {

    }

    @Override
    protected SharedPreferences getSharedPreferences() {
        return AppsSettings.get().getSharedPreferences();
    }

    private AppsSettings mSettings;
    private boolean isInit = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        mSettings = AppsSettings.get();

        addPreferencesFromResource(R.xml.preference_game);
        bind(PREF_GAME_PATH, mSettings.getResourcePath());
        bind(PREF_GAME_VERSION, mSettings.getCoreConfigVersion());

        bind(PREF_SOUND_EFFECT, mSettings.isSoundEffect());
        bind(PREF_LOCK_SCREEN, mSettings.isLockSreenOrientation());
        bind(PREF_FONT_ANTIALIAS, mSettings.isFontAntiAlias());
        bind(PREF_IMMERSIVE_MODE, mSettings.isImmerSiveMode());
        bind(PREF_PENDULUM_SCALE, mSettings.isPendulumScale());
        bind(PREF_OPENGL_VERSION, Constants.PREF_DEF_OPENGL_VERSION);
        bind(PREF_IMAGE_QUALITY, Constants.PREF_DEF_IMAGE_QUALITY);

        bind(PREF_GAME_FONT, mSettings.getFontPath());
        bind(PREF_USE_EXTRA_CARD_CARDS, mSettings.isUseExtraCards());
        bind(SETTINGS_COVER, new File(mSettings.getCoreSkinPath(), Constants.CORE_SKIN_COVER).getAbsolutePath());
        bind(SETTINGS_CARD_BG, new File(mSettings.getCoreSkinPath(), Constants.CORE_SKIN_BG).getAbsolutePath());
        isInit = false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        super.onPreferenceChange(preference, value);
        if (!isInit) {
            String key = preference.getKey();
            if (PREF_PENDULUM_SCALE.equals(key)) {
                setPendlumScale((Boolean) value);
            }
            if (preference instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                mSharedPreferences.edit().putBoolean(preference.getKey(), checkBoxPreference.isChecked()).apply();
                return true;
            }
            boolean rs = super.onPreferenceChange(preference, value);
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                mSharedPreferences.edit().putString(preference.getKey(), listPreference.getValue()).apply();
            }
            return rs;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (PREF_GAME_FONT.equals(key)) {
            //选择ttf字体文件，保存
            showFileChooser(preference, "*/*.ttf", getString(R.string.dialog_select_font));
        } else if (SETTINGS_COVER.equals(key)) {
            //显示图片对话框？
            String outFile = new File(mSettings.getCoreSkinPath(), Constants.CORE_SKIN_COVER).getAbsolutePath();
            showImageDialog(preference, getString(R.string.card_cover),
                    outFile,
                    true, Constants.CORE_SKIN_CARD_COVER_SIZE[0], Constants.CORE_SKIN_CARD_COVER_SIZE[1]);
        } else if (SETTINGS_CARD_BG.equals(key)) {
            //显示图片对话框？
            String outFile = new File(mSettings.getCoreSkinPath(), Constants.CORE_SKIN_BG).getAbsolutePath();
            showImageDialog(preference, getString(R.string.game_bg),outFile, true, Constants.CORE_SKIN_BG_SIZE[0], Constants.CORE_SKIN_BG_SIZE[1]);
        } else if (PREF_USE_EXTRA_CARD_CARDS.equals(key)) {
            CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
            if (checkBoxPreference.isChecked()) {
                showFileChooser(checkBoxPreference, "*/*.cdb", getString(R.string.dialog_select_database));
            } else {
                mSettings.setUseExtraCards(false);
            }
        }
        return false;
    }

    @Override
    protected void onChooseFileFail(Preference preference) {
        super.onChooseFileFail(preference);
    }

    @Override
    protected void onChooseFileOk(Preference preference, String file) {
        String key = preference.getKey();
        Log.i("kk", "onChooseFileOk:"+key+",file="+file);
        if (SETTINGS_COVER.equals(key)||SETTINGS_CARD_BG.equals(key)) {
            super.onChooseFileOk(preference, file);
            onPreferenceClick(preference);
        }
        if (PREF_USE_EXTRA_CARD_CARDS.equals(key)) {
            copyDataBase(preference, file);
        } else {
            super.onChooseFileOk(preference, file);
        }
    }

    private void showImageDialog(Preference preference, String title,String outFile,boolean isJpeg,int outWidth,int outHeight) {
        int width = getResources().getDisplayMetrics().widthPixels;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ImageView imageView = new ImageView(getActivity());
        FrameLayout frameLayout=new FrameLayout(getActivity());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        builder.setTitle(title);
        FrameLayout.LayoutParams layoutParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = VUiKit.dpToPx(10);
        layoutParams.leftMargin = VUiKit.dpToPx(10);
        layoutParams.rightMargin = VUiKit.dpToPx(10);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        frameLayout.addView(imageView, layoutParams);
        builder.setView(frameLayout);
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.settings, (dlg, s) -> {
            showImageCropChooser(preference, getString(R.string.dialog_select_image), outFile,
                    isJpeg, outWidth, outHeight);
            dlg.dismiss();
        });
        builder.setNeutralButton(android.R.string.cancel, (dlg, s) -> {
            dlg.dismiss();
        });
        builder.show();
        Bitmap bmp = BitmapUtil.getBitmapFromFile(outFile, width, -1);
        imageView.setImageBitmap(bmp);
    }

    private void copyDataBase(Preference preference, String file) {
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
        ProgressDialog dlg = ProgressDialog.show(getActivity(), null, getString(R.string.copy_databse));
        VUiKit.defer().when(() -> {
            File db = new File(mSettings.getResourcePath(), Constants.DATABASE_NAME);
            InputStream in = null;
            try {
                if(!TextUtils.equals(file, db.getAbsolutePath())){
                    if (db.exists()) {
                        db.delete();
                    }
                    in = new FileInputStream(file);
                    //复制
                    IOUtils.copyToFile(in, db.getAbsolutePath());
                }
                //处理数据
                ResCheckTask.doSomeTrickOnDatabase(db.getAbsolutePath());
                return true;
            } catch (Exception e) {

            } finally {
                IOUtils.close(in);
            }
            return false;
        }).fail((e) -> {
            dlg.dismiss();
            mSettings.setUseExtraCards(false);
            checkBoxPreference.setChecked(false);
            Toast.makeText(getActivity(), R.string.restart_app, Toast.LENGTH_SHORT).show();
        }).done((ok) -> {
            dlg.dismiss();
            checkBoxPreference.setChecked(ok);
            mSettings.setUseExtraCards(ok);
            Toast.makeText(getActivity(), R.string.restart_app, Toast.LENGTH_SHORT).show();
        });
    }

    private void setPendlumScale(boolean ok) {
        File file = new File(mSettings.getCoreSkinPath(), Constants.CORE_SKIN_PENDULUM_PATH);
        if (ok) {
            //rename
            ProgressDialog dlg = ProgressDialog.show(getActivity(), null, getString(R.string.coping_pendulum_image));
            VUiKit.defer().when(() -> {
                try {
                    File toPath = new File(mSettings.getResourcePath(), Constants.CORE_SKIN_PENDULUM_PATH);
                    IOUtils.createFolder(toPath);
                    IOUtils.copyFilesFromAssets(getActivity(), getDatapath(Constants.CORE_SKIN_PENDULUM_PATH),
                            toPath.getAbsolutePath(), false);
                } catch (IOException e) {
                }
            }).done((re) -> {
                dlg.dismiss();
            });
        } else {
            IOUtils.delete(file);
        }
    }
}

